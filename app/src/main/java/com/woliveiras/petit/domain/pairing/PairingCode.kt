package com.woliveiras.petit.domain.pairing

import java.security.SecureRandom
import java.time.Clock

private const val CODE_SPACE = 10_000
private const val CODE_LENGTH = 4
private const val DEFAULT_VALIDITY_MILLIS = 5 * 60 * 1_000L

/** A short-lived decimal code used to authorize one Nearby pairing attempt. */
data class PairingCode(val value: String, val expiresAtMillis: Long) {

  init {
    require(value.length == CODE_LENGTH && value.all(Char::isDigit)) {
      "Pairing code must contain exactly four digits"
    }
  }

  fun validate(candidate: String, nowMillis: Long): PairingCodeValidation =
    when {
      candidate.length != CODE_LENGTH || !candidate.all(Char::isDigit) ->
        PairingCodeValidation.Malformed
      nowMillis >= expiresAtMillis -> PairingCodeValidation.Expired
      candidate != value -> PairingCodeValidation.Incorrect
      else -> PairingCodeValidation.Valid
    }
}

enum class PairingCodeValidation {
  Valid,
  Incorrect,
  Expired,
  Malformed,
}

class PairingCodeGenerator(
  private val randomInt: (Int) -> Int = SecureRandom()::nextInt,
  private val clock: Clock = Clock.systemUTC(),
  private val validityMillis: Long = DEFAULT_VALIDITY_MILLIS,
) {

  init {
    require(validityMillis > 0) { "Pairing code validity must be positive" }
  }

  fun generate(): PairingCode {
    val value =
      randomInt(CODE_SPACE).coerceIn(0, CODE_SPACE - 1).toString().padStart(CODE_LENGTH, '0')
    return PairingCode(value = value, expiresAtMillis = clock.millis() + validityMillis)
  }
}
