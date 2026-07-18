package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.woliveiras.petit.data.local.dao.FamilyGroupMemberDao
import com.woliveiras.petit.data.local.dao.SyncLogDao
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
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

  override suspend fun getFamilyGroupKey(): String? {
    return context.familyGroupDataStore.data.first()[PreferencesKeys.FAMILY_GROUP_KEY]
  }

  override suspend fun createFamilyGroup(deviceName: String): String {
    val familyGroupKey = generateFamilyGroupKey()
    val deviceId = UUID.randomUUID().toString()
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
    val deviceId = UUID.randomUUID().toString()
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
    familyGroupMemberDao.deleteAllByGroupKey(key)
    context.familyGroupDataStore.edit { prefs ->
      prefs.remove(PreferencesKeys.FAMILY_GROUP_KEY)
      prefs.remove(PreferencesKeys.LOCAL_DEVICE_ID)
      prefs.remove(PreferencesKeys.LOCAL_DEVICE_NAME)
      prefs[PreferencesKeys.SYNC_ENABLED] = false
    }
  }

  override suspend fun removeMember(memberId: String) {
    familyGroupMemberDao.softDeleteMember(memberId)
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
    context.familyGroupDataStore.edit { it.clear() }
  }

  private fun generateFamilyGroupKey(): String {
    return PairingCredentialsGenerator().generate().familyGroupKey
  }
}
