package com.woliveiras.petit.domain.pairing

import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.inject.Inject

data class PairingCredentials(val deviceId: String, val familyGroupKey: String)

class PairingCredentialsGenerator @Inject constructor() {
  private val secureRandom = SecureRandom()

  fun generate(): PairingCredentials {
    val keyBytes = ByteArray(32).also(secureRandom::nextBytes)
    return PairingCredentials(
      deviceId = UUID.randomUUID().toString(),
      familyGroupKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes),
    )
  }
}
