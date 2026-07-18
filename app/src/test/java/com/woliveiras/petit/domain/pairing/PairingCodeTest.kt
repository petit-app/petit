package com.woliveiras.petit.domain.pairing

import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test

class PairingCodeTest {

  private val clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)

  @Test
  fun generatedCodeAlwaysContainsExactlyFourDigits() {
    val generator = PairingCodeGenerator(randomInt = { bound -> bound - 1 }, clock = clock)

    val code = generator.generate()

    assertThat(code.value).matches("[0-9]{4}")
    assertThat(code.value).isEqualTo("9999")
  }

  @Test
  fun validationAcceptsMatchingCodeBeforeExpiry() {
    val code = PairingCode(value = "0042", expiresAtMillis = 2_000L)

    assertThat(code.validate("0042", nowMillis = 1_999L)).isEqualTo(PairingCodeValidation.Valid)
  }

  @Test
  fun validationRejectsIncorrectCodeAndAllowsAnotherAttempt() {
    val code = PairingCode(value = "0042", expiresAtMillis = 2_000L)

    assertThat(code.validate("0000", nowMillis = 1_500L)).isEqualTo(PairingCodeValidation.Incorrect)
    assertThat(code.validate("0042", nowMillis = 1_500L)).isEqualTo(PairingCodeValidation.Valid)
  }

  @Test
  fun validationRejectsExpiredCodeAtExpiryBoundary() {
    val code = PairingCode(value = "0042", expiresAtMillis = 2_000L)

    assertThat(code.validate("0042", nowMillis = 2_000L)).isEqualTo(PairingCodeValidation.Expired)
  }

  @Test
  fun validationRejectsMalformedInputWithoutConsumingTheCode() {
    val code = PairingCode(value = "0042", expiresAtMillis = 2_000L)

    assertThat(code.validate("42", nowMillis = 1_500L)).isEqualTo(PairingCodeValidation.Malformed)
    assertThat(code.validate("0042", nowMillis = 1_500L)).isEqualTo(PairingCodeValidation.Valid)
  }
}
