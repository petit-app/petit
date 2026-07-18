package com.woliveiras.petit.domain.usecase

import androidx.room.withTransaction
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.MergeResult
import com.woliveiras.petit.domain.model.SyncLog
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Use case that merges a received ExportBundle into local data using the existing import logic. */
@Singleton
class MergeDataUseCase
@Inject
constructor(
  private val exportImportUseCase: ExportImportUseCase,
  private val database: PetitDatabase,
  private val familyGroupRepository: FamilyGroupRepository,
) {

  /**
   * Merges the received bundle with local data using last-write-wins by updatedAt.
   *
   * @param bundle The data received from the remote device.
   * @param peerId The ID of the peer that sent the data.
   * @param peerName The display name of the peer device.
   * @param replace If true, replaces all local data; if false, merges by updatedAt.
   * @return The result of the merge operation.
   */
  suspend operator fun invoke(
    bundle: ExportBundle,
    peerId: String,
    peerName: String,
    replace: Boolean = false,
    syncType: String = if (replace) "REPLACE" else "MERGE",
    syncLogId: String = UUID.randomUUID().toString(),
    reconcileAssociation: Boolean = true,
  ): MergeResult {
    val resolution = if (replace) ConflictResolution.REPLACE else ConflictResolution.MERGE

    val result =
      database.withTransaction {
        val result = exportImportUseCase.importDataWithinTransaction(bundle, resolution)
        familyGroupRepository.applyMembershipChanges(bundle.membershipChanges)
        familyGroupRepository.updateLastSyncAt(peerId)
        val syncLog =
          SyncLog(
            id = syncLogId,
            peerId = peerId,
            peerName = peerName,
            syncTimestamp = System.currentTimeMillis(),
            entitiesSent = 0,
            entitiesReceived = bundle.entityCount,
            conflictsResolved = result.conflictsResolved,
            syncType = syncType,
          )
        database.syncLogDao().insertSyncLog(syncLog.toEntity())
        result
      }
    if (reconcileAssociation) familyGroupRepository.reconcileActiveAssociation()
    return result
  }
}
