package com.woliveiras.petit.data.lan

import com.woliveiras.petit.data.local.dao.LanSyncDao
import com.woliveiras.petit.data.local.entity.LanSeenNonceEntity
import com.woliveiras.petit.domain.lan.LanBytes
import com.woliveiras.petit.domain.lan.LanNonceRegistry
import java.security.MessageDigest

/** Room-backed replay registry used from the session runner's IO dispatcher. */
class RoomLanNonceRegistry(private val dao: LanSyncDao) : LanNonceRegistry {
  @Synchronized
  override fun recordIfNew(deviceId: String, nonce: LanBytes): Boolean {
    val digest =
      MessageDigest.getInstance("SHA-256")
        .digest(deviceId.toByteArray(Charsets.UTF_8) + nonce.copyToByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    val inserted =
      dao.insertSeenNonce(LanSeenNonceEntity(digest, System.currentTimeMillis())) != -1L
    if (inserted) dao.trimSeenNonces(MAX_NONCES)
    return inserted
  }

  private companion object {
    const val MAX_NONCES = 4_096
  }
}
