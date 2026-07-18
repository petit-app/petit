package com.woliveiras.petit.domain.usecase

import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.NearbyTransferRepository
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.SyncLog
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Use case that serializes local data into an ExportBundle and sends it via Nearby Connections. */
@Singleton
class SendDataUseCase
@Inject
constructor(
  private val exportImportUseCase: ExportImportUseCase,
  private val nearbyTransferRepository: NearbyTransferRepository,
  private val database: PetitDatabase,
  private val familyGroupRepository: FamilyGroupRepository,
) {

  /** Exports all local data and sends it to the connected device. */
  suspend operator fun invoke(endpointId: String, peerName: String, peerMemberId: String) {
    val activeGroupKey = familyGroupRepository.getFamilyGroupKey()
    val activeGroupId = activeGroupKey?.let(MembershipChange::groupIdForKey)
    val bundle =
      exportImportUseCase
        .exportShareable()
        .copy(
          membershipChanges =
            familyGroupRepository.getMembershipChanges().filter { it.groupId == activeGroupId }
        )
    nearbyTransferRepository.sendData(endpointId, bundle)
    database
      .syncLogDao()
      .insertSyncLog(
        SyncLog(
            id = UUID.randomUUID().toString(),
            peerId = peerMemberId,
            peerName = peerName,
            syncTimestamp = System.currentTimeMillis(),
            entitiesSent = bundle.entityCount,
            entitiesReceived = 0,
            conflictsResolved = 0,
            syncType = "SEND",
          )
          .toEntity()
      )
    familyGroupRepository.updateLastSyncAt(peerMemberId)
  }
}
