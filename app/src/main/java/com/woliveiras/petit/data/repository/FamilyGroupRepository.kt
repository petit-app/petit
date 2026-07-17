package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.SyncLog
import kotlinx.coroutines.flow.Flow

/** Repository interface for family group operations. */
interface FamilyGroupRepository {

  /** Flow of the current family group info, or null if not in a group. */
  val familyGroupInfo: Flow<FamilyGroupInfo?>

  /** Flow of the local device member, or null if not in a group. */
  val localDevice: Flow<FamilyGroupMember?>

  /** Flow indicating whether sync is enabled. */
  val isSyncEnabled: Flow<Boolean>

  /** Get the current family group key from preferences. */
  suspend fun getFamilyGroupKey(): String?

  /** Create a new family group and register the local device as the first member. */
  suspend fun createFamilyGroup(deviceName: String): String

  /** Join an existing family group with the given key and register the local device. */
  suspend fun joinFamilyGroup(familyGroupKey: String, deviceName: String)

  /** Add a remote member to the family group. */
  suspend fun addRemoteMember(member: FamilyGroupMember)

  /** Leave the current family group. */
  suspend fun leaveFamilyGroup()

  /** Remove a member from the family group. */
  suspend fun removeMember(memberId: String)

  /** Update the last sync timestamp for a member. */
  suspend fun updateLastSyncAt(memberId: String)

  /** Enable or disable sync. */
  suspend fun setSyncEnabled(enabled: Boolean)

  /** Record a sync log entry. */
  suspend fun recordSyncLog(syncLog: SyncLog)

  /** Get all sync logs. */
  fun getSyncLogs(): Flow<List<SyncLog>>

  /** Get the most recent sync log. */
  suspend fun getLatestSyncLog(): SyncLog?

  /** Clear all locally persisted family-group credentials and sync settings. */
  suspend fun resetLocalPreferences()
}
