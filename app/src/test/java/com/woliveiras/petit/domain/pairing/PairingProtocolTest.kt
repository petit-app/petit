package com.woliveiras.petit.domain.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PairingProtocolTest {

  @Test
  fun authorizationRequestRoundTripsWithoutTheGroupKey() {
    val request =
      PairingMessage.AuthorizationRequest(
        code = "0042",
        deviceId = "receiver-id",
        deviceName = "Receiver",
      )

    val encoded = PairingProtocol.encode(request)

    assertThat(encoded).doesNotContain("familyGroupKey")
    assertThat(PairingProtocol.decode(encoded)).isEqualTo(request)
  }

  @Test
  fun authorizationAcceptedCarriesStablePeerIdentityAndGroupKey() {
    val accepted =
      PairingMessage.AuthorizationAccepted(
        familyGroupKey = "secret-key",
        deviceId = "sender-id",
        deviceName = "Sender",
      )

    assertThat(PairingProtocol.decode(PairingProtocol.encode(accepted))).isEqualTo(accepted)
  }

  @Test
  fun malformedOrUnknownMessagesAreRejected() {
    assertThat(PairingProtocol.decode("not-json")).isNull()
    assertThat(PairingProtocol.decode("""{"type":"UNKNOWN"}""")).isNull()
  }

  @Test
  fun advertiserRejectsIncorrectCodeAndAcceptsTheNextCorrectAttempt() {
    val session =
      PairingAuthorizationSession(
        pairingCode = PairingCode("0042", expiresAtMillis = 2_000L),
        familyGroupKey = "secret-key",
        localDeviceId = "sender-id",
        localDeviceName = "Sender",
      )

    val rejected =
      session.authorize(
        request = PairingMessage.AuthorizationRequest("0000", "wrong-id", "Wrong"),
        nowMillis = 1_500L,
      )
    val accepted =
      session.authorize(
        request = PairingMessage.AuthorizationRequest("0042", "receiver-id", "Receiver"),
        nowMillis = 1_500L,
      )

    assertThat(rejected).isInstanceOf(PairingAuthorizationResult.Rejected::class.java)
    assertThat(accepted)
      .isEqualTo(
        PairingAuthorizationResult.Accepted(
          response = PairingMessage.AuthorizationAccepted("secret-key", "sender-id", "Sender"),
          remoteDeviceId = "receiver-id",
          remoteDeviceName = "Receiver",
        )
      )
  }

  @Test
  fun expiredCodeCannotAuthorizeAConnection() {
    val session =
      PairingAuthorizationSession(
        pairingCode = PairingCode("0042", expiresAtMillis = 2_000L),
        familyGroupKey = "secret-key",
        localDeviceId = "sender-id",
        localDeviceName = "Sender",
      )

    val result =
      session.authorize(
        request = PairingMessage.AuthorizationRequest("0042", "receiver-id", "Receiver"),
        nowMillis = 2_000L,
      )

    assertThat(result)
      .isEqualTo(PairingAuthorizationResult.Rejected(PairingRejectionReason.CodeExpired))
  }

  @Test
  fun revokedStableIdentityCannotReuseTheOldGroupAuthorization() {
    val session =
      PairingAuthorizationSession(
        pairingCode = PairingCode("0042", expiresAtMillis = 2_000L),
        familyGroupKey = "secret-key",
        localDeviceId = "sender-id",
        localDeviceName = "Sender",
        revokedMemberIds = setOf("removed-id"),
      )

    val result =
      session.authorize(
        PairingMessage.AuthorizationRequest("0042", "removed-id", "Removed device"),
        nowMillis = 1_500L,
      )

    assertThat(result)
      .isEqualTo(PairingAuthorizationResult.Rejected(PairingRejectionReason.RevokedMember))
  }
}
