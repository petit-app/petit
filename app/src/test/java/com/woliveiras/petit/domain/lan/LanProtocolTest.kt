package com.woliveiras.petit.domain.lan

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.UUID
import org.junit.Test

class LanProtocolTest {
  @Test
  fun everyProtocolMessageRoundTripsThroughTheVersionedCodec() {
    val messages =
      listOf(
        LanMessage.Hello(
          version = LanProtocol.CURRENT_VERSION,
          deviceId = CLIENT_ID,
          lastSyncTimestamp = 101,
          nonce = LanBytes(ByteArray(LanProtocol.NONCE_SIZE) { 1 }),
          authenticationTag = LanBytes(ByteArray(LanProtocol.HMAC_SIZE) { 2 }),
        ),
        LanMessage.HelloAck(
          version = LanProtocol.CURRENT_VERSION,
          deviceId = SERVER_ID,
          lastSyncTimestamp = 202,
          clientNonce = LanBytes(ByteArray(LanProtocol.NONCE_SIZE) { 1 }),
          serverNonce = LanBytes(ByteArray(LanProtocol.NONCE_SIZE) { 3 }),
          authenticationTag = LanBytes(ByteArray(LanProtocol.HMAC_SIZE) { 4 }),
        ),
        LanMessage.Changeset(
          batchId = BATCH_ID,
          sinceTimestamp = 100,
          payload = LanBytes("records".toByteArray()),
        ),
        LanMessage.Ack(batchId = BATCH_ID, newSyncTimestamp = 303),
        LanMessage.Error(LanProtocolError.AUTHENTICATION_FAILED, "rejected"),
        LanMessage.Close("complete"),
      )

    messages.forEach { message ->
      assertThat(LanMessageCodec.decode(LanMessageCodec.encode(message))).isEqualTo(message)
    }
  }

  @Test
  fun frameCodecRejectsAnAdvertisedLengthBeforeAllocatingIt() {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { it.writeInt(LanProtocol.MAX_FRAME_BYTES + 1) }

    val failure = runCatching { LanFrameCodec.read(ByteArrayInputStream(bytes.toByteArray())) }

    assertThat(failure.exceptionOrNull()).isInstanceOf(LanProtocolException::class.java)
    assertThat((failure.exceptionOrNull() as LanProtocolException).error)
      .isEqualTo(LanProtocolError.FRAME_TOO_LARGE)
  }

  @Test
  fun changesetAndFrameLimitsAreEnforced() {
    val oversized = LanBytes(ByteArray(LanProtocol.MAX_CHANGESET_BYTES + 1))
    val message = LanMessage.Changeset(BATCH_ID, 0, oversized)

    val messageFailure = runCatching { LanMessageCodec.encode(message) }
    val frameFailure = runCatching {
      LanFrameCodec.write(ByteArrayOutputStream(), ByteArray(LanProtocol.MAX_FRAME_BYTES + 1))
    }

    assertThat((messageFailure.exceptionOrNull() as LanProtocolException).error)
      .isEqualTo(LanProtocolError.CHANGESET_TOO_LARGE)
    assertThat((frameFailure.exceptionOrNull() as LanProtocolException).error)
      .isEqualTo(LanProtocolError.FRAME_TOO_LARGE)
  }

  @Test
  fun lowerCanonicalUuidInitiatesExactlyOneOfTwoSimultaneousSessions() {
    assertThat(LanSessionDirection.shouldInitiate(CLIENT_ID, SERVER_ID)).isTrue()
    assertThat(LanSessionDirection.shouldInitiate(SERVER_ID, CLIENT_ID)).isFalse()
    assertThat(runCatching { LanSessionDirection.shouldInitiate(CLIENT_ID, CLIENT_ID) }.isFailure)
      .isTrue()
  }

  companion object {
    const val CLIENT_ID = "00000000-0000-0000-0000-000000000001"
    const val SERVER_ID = "00000000-0000-0000-0000-000000000002"
    val BATCH_ID: String = UUID.fromString("10000000-0000-0000-0000-000000000001").toString()
  }
}
