package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.PendingDeparture
import com.woliveiras.petit.domain.model.SyncLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Repository interface for family group operations. */
interface FamilyGroupRepository {

  /** Flow of the current family group info, or null if not in a group. */
  val familyGroupInfo: Flow<FamilyGroupInfo?>

  /** Flow of the local device member, or null if not in a group. */
  val localDevice: Flow<FamilyGroupMember?>

  /** Flow indicating whether sync is enabled. */
  val isSyncEnabled: Flow<Boolean>

  /** Security outbox may remain after the visible group association has been cleared. */
  val hasPendingDeparture: Flow<Boolean>
    get() = flowOf(false)

  /** Get the current family group key from preferences. */
  suspend fun getFamilyGroupKey(): String?

  /** Return the installation-stable device UUID, creating it once when necessary. */
  suspend fun getOrCreateLocalDeviceId(): String = error("Stable device identity is unavailable")

  /** Create a new family group and register the local device as the first member. */
  suspend fun createFamilyGroup(deviceName: String): String

  /** Join an existing family group with the given key and register the local device. */
  suspend fun joinFamilyGroup(familyGroupKey: String, deviceName: String)

  /** Persist local and remote identities only after the transport authorizes the peer. */
  suspend fun persistAuthorizedPairing(
    familyGroupKey: String,
    localMember: FamilyGroupMember,
    remoteMember: FamilyGroupMember,
  )

  /** Add a remote member to the family group. */
  suspend fun addRemoteMember(member: FamilyGroupMember)

  /** Leave the current family group. */
  suspend fun leaveFamilyGroup()

  /** Remove a member from the family group. */
  suspend fun removeMember(memberId: String)

  /** Rename the local device while preserving its stable identity and group key. */
  suspend fun renameLocalDevice(deviceName: String): Unit = error("Rename is not implemented")

  /** Return normalized membership changes retained for peer propagation. */
  suspend fun getMembershipChanges(): List<MembershipChange> = emptyList()

  /** Return offline departures whose group credential is restricted to membership-only delivery. */
  suspend fun getPendingDepartures(): List<PendingDeparture> = emptyList()

  /** Discard the restricted credential after a peer acknowledges the departure. */
  suspend fun acknowledgeDeparture(groupId: String, memberId: String) = Unit

  /** Apply and retain peer membership changes idempotently. */
  suspend fun applyMembershipChanges(changes: List<MembershipChange>) = Unit

  /** Reconcile visible credentials after membership changes have committed in Room. */
  suspend fun reconcileActiveAssociation() = Unit

  /** Whether the stable identity is allowed to use the supplied group key. */
  suspend fun isMemberAuthorized(memberId: String, familyGroupKey: String): Boolean = true

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
