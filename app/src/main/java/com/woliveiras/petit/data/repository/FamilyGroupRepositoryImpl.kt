package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.woliveiras.petit.data.local.dao.FamilyGroupMemberDao
import com.woliveiras.petit.data.local.dao.SyncLogDao
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.MembershipChangeEntity
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.MembershipChangeType
import com.woliveiras.petit.domain.model.PendingDeparture
import com.woliveiras.petit.domain.model.SyncLog
import com.woliveiras.petit.domain.pairing.PairingCredentialsGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.familyGroupDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "family_group_preferences")

/** Implementation of FamilyGroupRepository using Room and DataStore. */
@Singleton
class FamilyGroupRepositoryImpl
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val familyGroupMemberDao: FamilyGroupMemberDao,
  private val syncLogDao: SyncLogDao,
  private val database: PetitDatabase,
) : FamilyGroupRepository {

  private object PreferencesKeys {
    val FAMILY_GROUP_KEY = stringPreferencesKey("family_group_key")
    val LOCAL_DEVICE_ID = stringPreferencesKey("local_device_id")
    val LOCAL_DEVICE_NAME = stringPreferencesKey("local_device_name")
    val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override val familyGroupInfo: Flow<FamilyGroupInfo?> =
    context.familyGroupDataStore.data
      .map { prefs -> prefs[PreferencesKeys.FAMILY_GROUP_KEY] }
      .flatMapLatest { key ->
        if (key == null) {
          flowOf(null)
        } else {
          familyGroupMemberDao.getMembersByGroupKey(key).map { entities ->
            if (entities.isEmpty()) {
              null
            } else {
              FamilyGroupInfo(
                familyGroupKey = key,
                members = entities.map { it.toDomain() },
                createdAt = entities.minOf { it.createdAt },
              )
            }
          }
        }
      }

  override val localDevice: Flow<FamilyGroupMember?> =
    familyGroupMemberDao.getLocalDeviceFlow().map { it?.toDomain() }

  override val isSyncEnabled: Flow<Boolean> =
    context.familyGroupDataStore.data.map { prefs -> prefs[PreferencesKeys.SYNC_ENABLED] ?: false }

  override val hasPendingDeparture: Flow<Boolean> =
    database.membershipChangeDao().observePendingDepartureCount().map { it > 0 }

  override suspend fun getFamilyGroupKey(): String? {
    val preferences = context.familyGroupDataStore.data.first()
    val key = preferences[PreferencesKeys.FAMILY_GROUP_KEY] ?: return null
    reconcileActiveAssociation(key, preferences[PreferencesKeys.LOCAL_DEVICE_ID])
    return context.familyGroupDataStore.data.first()[PreferencesKeys.FAMILY_GROUP_KEY]
  }

  override suspend fun getOrCreateLocalDeviceId(): String {
    val existing = context.familyGroupDataStore.data.first()[PreferencesKeys.LOCAL_DEVICE_ID]
    if (existing != null) return existing
    return UUID.randomUUID().toString().also { id ->
      context.familyGroupDataStore.edit { prefs -> prefs[PreferencesKeys.LOCAL_DEVICE_ID] = id }
    }
  }

  override suspend fun createFamilyGroup(deviceName: String): String {
    val familyGroupKey = generateFamilyGroupKey()
    val deviceId = getOrCreateLocalDeviceId()
    val now = System.currentTimeMillis()

    context.familyGroupDataStore.edit { prefs ->
      prefs[PreferencesKeys.FAMILY_GROUP_KEY] = familyGroupKey
      prefs[PreferencesKeys.LOCAL_DEVICE_ID] = deviceId
      prefs[PreferencesKeys.LOCAL_DEVICE_NAME] = deviceName
      prefs[PreferencesKeys.SYNC_ENABLED] = true
    }

    val localMember =
      FamilyGroupMember(
        id = deviceId,
        deviceName = deviceName,
        familyGroupKey = familyGroupKey,
        isLocalDevice = true,
        lastSyncAt = null,
        createdAt = now,
        updatedAt = now,
      )
    familyGroupMemberDao.insertMember(localMember.toEntity())

    return familyGroupKey
  }

  override suspend fun joinFamilyGroup(familyGroupKey: String, deviceName: String) {
    val deviceId = getOrCreateLocalDeviceId()
    val now = System.currentTimeMillis()

    context.familyGroupDataStore.edit { prefs ->
      prefs[PreferencesKeys.FAMILY_GROUP_KEY] = familyGroupKey
      prefs[PreferencesKeys.LOCAL_DEVICE_ID] = deviceId
      prefs[PreferencesKeys.LOCAL_DEVICE_NAME] = deviceName
      prefs[PreferencesKeys.SYNC_ENABLED] = true
    }

    val localMember =
      FamilyGroupMember(
        id = deviceId,
        deviceName = deviceName,
        familyGroupKey = familyGroupKey,
        isLocalDevice = true,
        lastSyncAt = null,
        createdAt = now,
        updatedAt = now,
      )
    familyGroupMemberDao.insertMember(localMember.toEntity())
  }

  override suspend fun persistAuthorizedPairing(
    familyGroupKey: String,
    localMember: FamilyGroupMember,
    remoteMember: FamilyGroupMember,
  ) {
    require(localMember.isLocalDevice) { "Local pairing member must be marked as local" }
    require(!remoteMember.isLocalDevice) { "Remote pairing member cannot be marked as local" }
    require(localMember.familyGroupKey == familyGroupKey)
    require(remoteMember.familyGroupKey == familyGroupKey)

    val groupId = MembershipChange.groupIdForKey(familyGroupKey)
    val terminalIds =
      getMembershipChanges()
        .filter {
          it.groupId == groupId &&
            it.type in setOf(MembershipChangeType.REMOVE, MembershipChangeType.LEAVE)
        }
        .mapTo(mutableSetOf()) { it.memberId }
    val existingRemote = familyGroupMemberDao.getMemberByIdIncludingDeleted(remoteMember.id)
    if (
      (existingRemote?.familyGroupKey == familyGroupKey && existingRemote.deletedAt != null) ||
        remoteMember.id in terminalIds
    ) {
      throw SecurityException("Remote member identity was revoked")
    }
    if (localMember.id in terminalIds) {
      throw SecurityException("Local member identity left this group")
    }

    val previousLocal = familyGroupMemberDao.getLocalDevice()
    val previousRemote = familyGroupMemberDao.getMemberById(remoteMember.id)
    familyGroupMemberDao.insertPairingMembers(
      listOf(localMember.toEntity(), remoteMember.toEntity())
    )
    try {
      context.familyGroupDataStore.edit { prefs ->
        prefs[PreferencesKeys.FAMILY_GROUP_KEY] = familyGroupKey
        prefs[PreferencesKeys.LOCAL_DEVICE_ID] = localMember.id
        prefs[PreferencesKeys.LOCAL_DEVICE_NAME] = localMember.deviceName
        prefs[PreferencesKeys.SYNC_ENABLED] = true
      }
    } catch (error: Exception) {
      if (previousLocal == null) familyGroupMemberDao.deleteMember(localMember.id)
      else familyGroupMemberDao.insertMember(previousLocal)
      if (previousRemote == null) familyGroupMemberDao.deleteMember(remoteMember.id)
      else familyGroupMemberDao.insertMember(previousRemote)
      throw error
    }
  }

  override suspend fun addRemoteMember(member: FamilyGroupMember) {
    familyGroupMemberDao.insertMember(member.toEntity())
  }

  override suspend fun leaveFamilyGroup() {
    val key = getFamilyGroupKey() ?: return
    val local = familyGroupMemberDao.getLocalDevice()
    database.withTransaction {
      if (local != null) {
        appendMembershipChange(
          MembershipChange(
            groupId = MembershipChange.groupIdForKey(key),
            memberId = local.id,
            type = MembershipChangeType.LEAVE,
            timestamp = nextTimestamp(local.updatedAt),
          ),
          deliveryKey = key,
        )
      }
      familyGroupMemberDao.softDeleteAllByGroupKey(
        key,
        local?.let { nextTimestamp(it.updatedAt) } ?: System.currentTimeMillis(),
      )
    }
    reconcileActiveAssociation()
  }

  override suspend fun removeMember(memberId: String) {
    val member = familyGroupMemberDao.getMemberByIdIncludingDeleted(memberId) ?: return
    if (member.deletedAt != null) return
    val timestamp = nextTimestamp(member.updatedAt)
    database.withTransaction {
      familyGroupMemberDao.softDeleteMember(memberId, timestamp)
      appendMembershipChange(
        MembershipChange(
          groupId = MembershipChange.groupIdForKey(member.familyGroupKey),
          memberId = memberId,
          type = MembershipChangeType.REMOVE,
          timestamp = timestamp,
        )
      )
    }
  }

  override suspend fun renameLocalDevice(deviceName: String) {
    val normalizedName = deviceName.trim()
    require(normalizedName.isNotEmpty()) { "Device name cannot be blank" }
    require(normalizedName.length <= 80) { "Device name is too long" }
    val local = familyGroupMemberDao.getLocalDevice() ?: error("No local group member")
    if (local.deviceName == normalizedName) return
    val timestamp = nextTimestamp(local.updatedAt)
    database.withTransaction {
      familyGroupMemberDao.insertMember(
        local.copy(deviceName = normalizedName, updatedAt = timestamp)
      )
      appendMembershipChange(
        MembershipChange(
          groupId = MembershipChange.groupIdForKey(local.familyGroupKey),
          memberId = local.id,
          type = MembershipChangeType.RENAME,
          deviceName = normalizedName,
          timestamp = timestamp,
        )
      )
    }
    reconcileActiveAssociation()
  }

  override suspend fun getMembershipChanges(): List<MembershipChange> =
    database.membershipChangeDao().getAll().map { it.toDomain() }

  override suspend fun getPendingDepartures(): List<PendingDeparture> =
    database.membershipChangeDao().getPendingDepartures().map { entity ->
      PendingDeparture(entity.toDomain(), checkNotNull(entity.deliveryKey))
    }

  override suspend fun acknowledgeDeparture(groupId: String, memberId: String) {
    database.membershipChangeDao().clearDeliveryKey(groupId, memberId)
  }

  override suspend fun applyMembershipChanges(changes: List<MembershipChange>) {
    val accepted =
      MembershipChange.normalize(changes).filter { change ->
        val member = familyGroupMemberDao.getMemberByIdIncludingDeleted(change.memberId)
        member != null && MembershipChange.groupIdForKey(member.familyGroupKey) == change.groupId
      }
    for (change in accepted) {
      val member = checkNotNull(familyGroupMemberDao.getMemberByIdIncludingDeleted(change.memberId))
      when (change.type) {
        MembershipChangeType.RENAME -> {
          val name = change.deviceName?.trim().orEmpty()
          if (
            name.isNotEmpty() &&
              name.length <= 80 &&
              member.deletedAt == null &&
              change.timestamp > member.updatedAt
          ) {
            familyGroupMemberDao.insertMember(
              member.copy(deviceName = name, updatedAt = change.timestamp)
            )
          }
        }
        MembershipChangeType.REMOVE,
        MembershipChangeType.LEAVE -> {
          val effectiveTime = maxOf(member.updatedAt, member.deletedAt ?: Long.MIN_VALUE)
          if (change.timestamp >= effectiveTime) {
            familyGroupMemberDao.insertMember(
              member.copy(updatedAt = change.timestamp, deletedAt = change.timestamp)
            )
          }
        }
      }
    }
    if (accepted.isNotEmpty()) {
      saveMembershipChanges(MembershipChange.normalize(getMembershipChanges() + accepted))
    }
  }

  override suspend fun reconcileActiveAssociation() {
    val preferences = context.familyGroupDataStore.data.first()
    val key = preferences[PreferencesKeys.FAMILY_GROUP_KEY] ?: return
    reconcileActiveAssociation(key, preferences[PreferencesKeys.LOCAL_DEVICE_ID])
  }

  private suspend fun reconcileActiveAssociation(key: String, localId: String?) {
    if (localId == null) {
      clearActiveAssociation()
      return
    }
    val local = familyGroupMemberDao.getMemberByIdIncludingDeleted(localId)
    if (local == null) return
    if (local.familyGroupKey != key || local.deletedAt != null) {
      clearActiveAssociation()
      return
    }
    context.familyGroupDataStore.edit { prefs ->
      prefs[PreferencesKeys.LOCAL_DEVICE_NAME] = local.deviceName
    }
  }

  override suspend fun isMemberAuthorized(memberId: String, familyGroupKey: String): Boolean {
    val member = familyGroupMemberDao.getMemberByIdIncludingDeleted(memberId) ?: return true
    return member.familyGroupKey == familyGroupKey && member.deletedAt == null
  }

  override suspend fun updateLastSyncAt(memberId: String) {
    familyGroupMemberDao.updateLastSyncAt(memberId)
  }

  override suspend fun setSyncEnabled(enabled: Boolean) {
    context.familyGroupDataStore.edit { prefs -> prefs[PreferencesKeys.SYNC_ENABLED] = enabled }
  }

  override suspend fun recordSyncLog(syncLog: SyncLog) {
    syncLogDao.insertSyncLog(syncLog.toEntity())
  }

  override fun getSyncLogs(): Flow<List<SyncLog>> {
    return syncLogDao.getAllSyncLogs().map { entities -> entities.map { it.toDomain() } }
  }

  override suspend fun getLatestSyncLog(): SyncLog? {
    return syncLogDao.getLatestSyncLog()?.toDomain()
  }

  override suspend fun resetLocalPreferences() {
    val stableId = context.familyGroupDataStore.data.first()[PreferencesKeys.LOCAL_DEVICE_ID]
    context.familyGroupDataStore.edit { prefs ->
      prefs.clear()
      if (stableId != null) prefs[PreferencesKeys.LOCAL_DEVICE_ID] = stableId
    }
    database.membershipChangeDao().deleteAll()
  }

  private fun generateFamilyGroupKey(): String {
    return PairingCredentialsGenerator().generate().familyGroupKey
  }

  private suspend fun appendMembershipChange(
    change: MembershipChange,
    deliveryKey: String? = null,
  ) {
    val currentEntity = database.membershipChangeDao().get(change.groupId, change.memberId)
    val selected =
      MembershipChange.normalize(listOfNotNull(currentEntity?.toDomain(), change)).single()
    database
      .membershipChangeDao()
      .upsert(selected.toEntity(deliveryKey ?: currentEntity?.deliveryKey))
  }

  private suspend fun saveMembershipChanges(changes: List<MembershipChange>) {
    MembershipChange.normalize(changes).forEach {
      val deliveryKey = database.membershipChangeDao().get(it.groupId, it.memberId)?.deliveryKey
      database.membershipChangeDao().upsert(it.toEntity(deliveryKey))
    }
  }

  private suspend fun clearActiveAssociation() {
    context.familyGroupDataStore.edit { prefs ->
      prefs.remove(PreferencesKeys.FAMILY_GROUP_KEY)
      prefs.remove(PreferencesKeys.LOCAL_DEVICE_NAME)
      prefs[PreferencesKeys.SYNC_ENABLED] = false
    }
  }

  private fun nextTimestamp(previous: Long): Long = maxOf(System.currentTimeMillis(), previous + 1)

  private fun MembershipChangeEntity.toDomain() =
    MembershipChange(groupId, memberId, MembershipChangeType.valueOf(type), deviceName, timestamp)

  private fun MembershipChange.toEntity(deliveryKey: String? = null) =
    MembershipChangeEntity(groupId, memberId, type.name, deviceName, timestamp, deliveryKey)
}
