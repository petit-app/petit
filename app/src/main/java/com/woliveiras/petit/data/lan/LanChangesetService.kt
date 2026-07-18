package com.woliveiras.petit.data.lan

import androidx.room.withTransaction
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.LanAppliedBatchEntity
import com.woliveiras.petit.data.local.entity.LanOutboundAckEntity
import com.woliveiras.petit.data.local.entity.LanSyncPeerEntity
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.lan.LanBytes
import com.woliveiras.petit.domain.lan.LanChangesetBatcher
import com.woliveiras.petit.domain.lan.LanProtocolError
import com.woliveiras.petit.domain.lan.LanProtocolException
import com.woliveiras.petit.domain.lan.LanSessionScope
import com.woliveiras.petit.domain.lan.PreparedLanChangeset
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.MembershipChangeType
import com.woliveiras.petit.domain.model.SyncLog
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import com.woliveiras.petit.domain.usecase.MergeDataUseCase
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

data class AppliedLanChangeset(val acknowledgedCursor: Long, val replayed: Boolean)

/** Durable changeset boundary shared by TCP sessions, retry handling, Room and SyncLog. */
@Singleton
class LanChangesetService
@Inject
constructor(
  private val database: PetitDatabase,
  private val exportImportUseCase: ExportImportUseCase,
  private val mergeDataUseCase: MergeDataUseCase,
  private val familyGroupRepository: FamilyGroupRepository,
) {
  suspend fun prepareClinical(peerId: String, groupKey: String): List<PreparedLanChangeset> {
    val groupId = MembershipChange.groupIdForKey(groupKey)
    val stateId = peerStateId(peerId, groupId)
    val bundle =
      exportImportUseCase
        .exportShareableSince(0L)
        .copy(
          membershipChanges =
            familyGroupRepository.getMembershipChanges().filter { it.groupId == groupId }
        )
    val acknowledged = database.lanSyncDao().getAcknowledgedBatchIds(stateId).toSet()
    return LanChangesetBatcher.create(stateId, 0L, bundle, acknowledged)
  }

  suspend fun prepareDeparture(
    peerId: String,
    departure: com.woliveiras.petit.domain.model.PendingDeparture,
  ): List<PreparedLanChangeset> {
    require(MembershipChange.groupIdForKey(departure.deliveryKey) == departure.change.groupId) {
      "Departure credential does not match its group"
    }
    val empty = exportImportUseCase.exportShareableSince(Long.MAX_VALUE)
    return LanChangesetBatcher.create(
      peerStateId(peerId, departure.change.groupId),
      0L,
      empty.copy(membershipChanges = listOf(departure.change)),
    )
  }

  suspend fun outboundCursor(peerId: String, groupKey: String): Long =
    database
      .lanSyncDao()
      .getPeer(peerStateId(peerId, MembershipChange.groupIdForKey(groupKey)))
      ?.outboundCursor ?: 0L

  suspend fun acknowledgeOutbound(
    peerId: String,
    peerName: String,
    groupId: String,
    batch: PreparedLanChangeset,
  ) {
    val stateId = peerStateId(peerId, groupId)
    database.withTransaction {
      val current = database.lanSyncDao().getPeer(stateId)?.outboundCursor ?: 0L
      batch.constituentBatchIds.forEach { constituentId ->
        database
          .lanSyncDao()
          .upsertOutboundAck(LanOutboundAckEntity(stateId, constituentId, batch.cursor))
      }
      database
        .lanSyncDao()
        .upsertPeer(
          LanSyncPeerEntity(
            peerId = stateId,
            outboundCursor = maxOf(current, batch.cursor),
            updatedAt = System.currentTimeMillis(),
          )
        )
      familyGroupRepository.updateLastSyncAt(peerId)
      database
        .syncLogDao()
        .insertSyncLog(
          SyncLog(
              id = "lan-out:$stateId:${batch.batchId}",
              peerId = peerId,
              peerName = peerName,
              syncTimestamp = System.currentTimeMillis(),
              entitiesSent = batch.bundle.entityCount,
              entitiesReceived = 0,
              conflictsResolved = 0,
              syncType = "LAN",
            )
            .toEntity()
        )
      batch.bundle.membershipChanges
        .filter { it.type == MembershipChangeType.LEAVE }
        .forEach { familyGroupRepository.acknowledgeDeparture(it.groupId, it.memberId) }
    }
  }

  suspend fun apply(
    peerId: String,
    peerName: String,
    batchId: String,
    cursor: Long,
    payload: LanBytes,
    scope: LanSessionScope,
    groupId: String,
  ): AppliedLanChangeset {
    val stateId = peerStateId(peerId, groupId)
    val applied =
      database.withTransaction {
        database.lanSyncDao().getAppliedBatch(batchId, stateId)?.let {
          return@withTransaction AppliedLanChangeset(it.acknowledgedCursor, replayed = true)
        }
        val bundle = decodeAndValidate(payload)
        validateMembershipGroup(groupId, bundle)
        if (scope == LanSessionScope.MEMBERSHIP_ONLY) validateDeparture(peerId, groupId, bundle)
        mergeDataUseCase(
          bundle,
          peerId,
          peerName,
          syncType = "LAN",
          syncLogId = batchId,
          reconcileAssociation = false,
        )
        val inserted =
          database
            .lanSyncDao()
            .insertAppliedBatch(
              LanAppliedBatchEntity(
                batchId = batchId,
                peerId = stateId,
                acknowledgedCursor = cursor,
                appliedAt = System.currentTimeMillis(),
              )
            )
        check(inserted != -1L) { "Concurrent changeset replay escaped transaction serialization" }
        AppliedLanChangeset(cursor, replayed = false)
      }
    familyGroupRepository.reconcileActiveAssociation()
    return applied
  }

  private fun peerStateId(peerId: String, groupId: String) = "$groupId:$peerId"

  private fun decodeAndValidate(payload: LanBytes): ExportBundle {
    val bundle =
      try {
        ExportBundle.fromJson(JSONObject(String(payload.copyToByteArray(), StandardCharsets.UTF_8)))
      } catch (exception: Exception) {
        throw LanProtocolException(
          LanProtocolError.INVALID_MESSAGE,
          "Malformed changeset payload",
          exception,
        )
      }
    val errors = ExportBundle.validate(bundle)
    if (errors.isNotEmpty()) {
      throw LanProtocolException(LanProtocolError.INVALID_MESSAGE, errors.first())
    }
    return bundle
  }

  private fun validateMembershipGroup(groupId: String, bundle: ExportBundle) {
    if (bundle.membershipChanges.any { it.groupId != groupId }) {
      throw LanProtocolException(
        LanProtocolError.AUTHENTICATION_FAILED,
        "Membership event does not belong to the authenticated group",
      )
    }
  }

  private fun validateDeparture(peerId: String, groupId: String, bundle: ExportBundle) {
    val clinicalCount =
      bundle.pets.size +
        bundle.weightEntries.size +
        bundle.vaccinationEntries.size +
        bundle.dewormingEntries.size +
        bundle.tasks.size
    val changes = bundle.membershipChanges
    if (
      clinicalCount != 0 ||
        changes.size != 1 ||
        changes.single().groupId != groupId ||
        changes.single().memberId != peerId ||
        changes.single().type != MembershipChangeType.LEAVE
    ) {
      throw LanProtocolException(
        LanProtocolError.AUTHENTICATION_FAILED,
        "Membership-only sessions may deliver only the caller's departure",
      )
    }
  }
}
