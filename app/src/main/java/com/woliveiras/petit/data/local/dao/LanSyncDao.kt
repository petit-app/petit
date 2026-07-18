package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.woliveiras.petit.data.local.entity.LanAppliedBatchEntity
import com.woliveiras.petit.data.local.entity.LanOutboundAckEntity
import com.woliveiras.petit.data.local.entity.LanSeenNonceEntity
import com.woliveiras.petit.data.local.entity.LanSyncPeerEntity

@Dao
interface LanSyncDao {
  @Query("SELECT * FROM lan_sync_peers WHERE peerId = :peerId")
  suspend fun getPeer(peerId: String): LanSyncPeerEntity?

  @Upsert suspend fun upsertPeer(peer: LanSyncPeerEntity)

  @Query("SELECT batchId FROM lan_outbound_acks WHERE peerId = :peerId")
  suspend fun getAcknowledgedBatchIds(peerId: String): List<String>

  @Upsert suspend fun upsertOutboundAck(ack: LanOutboundAckEntity)

  @Query("SELECT * FROM lan_applied_batches WHERE batchId = :batchId AND peerId = :peerId")
  suspend fun getAppliedBatch(batchId: String, peerId: String): LanAppliedBatchEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAppliedBatch(batch: LanAppliedBatchEntity): Long

  @Query("DELETE FROM lan_sync_peers") suspend fun deleteAllPeers()

  @Query("DELETE FROM lan_applied_batches") suspend fun deleteAllBatches()

  @Query("DELETE FROM lan_outbound_acks") suspend fun deleteAllOutboundAcks()

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertSeenNonce(nonce: LanSeenNonceEntity): Long

  @Query(
    "DELETE FROM lan_seen_nonces WHERE nonceKey IN " +
      "(SELECT nonceKey FROM lan_seen_nonces ORDER BY seenAt DESC LIMIT -1 OFFSET :keep)"
  )
  fun trimSeenNonces(keep: Int)

  @Query("DELETE FROM lan_seen_nonces") suspend fun deleteAllSeenNonces()
}
