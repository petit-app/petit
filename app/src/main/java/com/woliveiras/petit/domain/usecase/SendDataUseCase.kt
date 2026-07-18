package com.woliveiras.petit.domain.usecase

import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.data.repository.NearbyTransferRepository
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
) {

  /** Exports all local data and sends it to the connected device. */
  suspend operator fun invoke(endpointId: String, peerName: String) {
    val bundle = exportImportUseCase.exportShareable()
    nearbyTransferRepository.sendData(endpointId, bundle)
    database
      .syncLogDao()
      .insertSyncLog(
        SyncLog(
            id = UUID.randomUUID().toString(),
            peerId = endpointId,
            peerName = peerName,
            syncTimestamp = System.currentTimeMillis(),
            entitiesSent = bundle.entityCount,
            entitiesReceived = 0,
            conflictsResolved = 0,
            syncType = "SEND",
          )
          .toEntity()
      )
  }
}
