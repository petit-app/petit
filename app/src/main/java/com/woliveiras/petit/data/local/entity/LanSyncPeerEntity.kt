package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-peer high-water mark advanced only after the peer acknowledges an outbound changeset. */
@Entity(tableName = "lan_sync_peers")
data class LanSyncPeerEntity(
  @PrimaryKey val peerId: String,
  val outboundCursor: Long,
  val updatedAt: Long,
)

/** Durable replay ledger that makes a lost ACK safe across process restarts. */
@Entity(tableName = "lan_applied_batches", primaryKeys = ["batchId", "peerId"])
data class LanAppliedBatchEntity(
  val batchId: String,
  val peerId: String,
  val acknowledgedCursor: Long,
  val appliedAt: Long,
)

/** Stable batch IDs acknowledged at the inclusive cursor boundary. */
@Entity(tableName = "lan_outbound_acks", primaryKeys = ["peerId", "batchId"])
data class LanOutboundAckEntity(val peerId: String, val batchId: String, val cursor: Long)

/** Persisted HELLO nonce digest prevents replay across sockets and process restarts. */
@Entity(tableName = "lan_seen_nonces")
data class LanSeenNonceEntity(@PrimaryKey val nonceKey: String, val seenAt: Long)
