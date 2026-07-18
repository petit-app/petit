package com.woliveiras.petit.domain.lan

import com.google.common.truth.Truth.assertThat
import java.util.Base64
import org.junit.Test

class LanHandshakeTest {
  private val groupKey = key(7)
  private val members = setOf(CLIENT_ID, SERVER_ID)

  @Test
  fun bilateralHandshakeCreatesDirectionalProtectedChannels() {
    val client = client(groupKey)
    val server = server(groupKey)
    val hello = client.createHello(lastSyncTimestamp = 10)
    val accepted = server.accept(hello, lastSyncTimestamp = 20) as LanHandshakeResult.Accepted
    val clientChannel = client.accept(accepted.ack)
    val serverChannel = accepted.channel

    val changeset = LanMessage.Changeset(BATCH_ID, 10, LanBytes("health changes".toByteArray()))
    val request = clientChannel.seal(changeset)
    assertThat(serverChannel.open(request)).isEqualTo(changeset)

    val ack = LanMessage.Ack(BATCH_ID, 21)
    val response = serverChannel.seal(ack)
    val wrongDirection = runCatching { clientChannel.open(request) }
    assertThat((wrongDirection.exceptionOrNull() as LanProtocolException).error)
      .isEqualTo(LanProtocolError.AUTHENTICATION_FAILED)
    assertThat(clientChannel.open(response)).isEqualTo(ack)
  }

  @Test
  fun serverRejectsVersionKeyAndUnknownMemberBeforeCreatingAChannel() {
    val validHello = client(groupKey).createHello(0)
    val invalidVersion = validHello.copy(version = LanProtocol.CURRENT_VERSION + 1)
    val invalidKey = client(key(8)).createHello(0)
    val unknownMember =
      LanHandshakeClient(
          localDeviceId = UNKNOWN_ID,
          expectedServerDeviceId = SERVER_ID,
          groupKey = groupKey,
          nonceSource = { nonce(4) },
        )
        .createHello(0)
    val server = server(groupKey)

    assertRejected(server.accept(invalidVersion, 0), LanProtocolError.UNSUPPORTED_VERSION)
    assertRejected(server.accept(invalidKey, 0), LanProtocolError.AUTHENTICATION_FAILED)
    assertRejected(server.accept(unknownMember, 0), LanProtocolError.UNKNOWN_MEMBER)
  }

  @Test
  fun clientAuthenticatesTheServerMemberAndHandshakeTag() {
    val client = client(groupKey)
    val accepted = server(groupKey).accept(client.createHello(0), 0) as LanHandshakeResult.Accepted
    val unknownServerAck = accepted.ack.copy(deviceId = UNKNOWN_ID)
    val tamperedTag =
      accepted.ack.copy(authenticationTag = accepted.ack.authenticationTag.flipLastBit())

    assertProtocolError(
      runCatching { client.accept(unknownServerAck) },
      LanProtocolError.UNKNOWN_MEMBER,
    )
    assertProtocolError(
      runCatching { client.accept(tamperedTag) },
      LanProtocolError.AUTHENTICATION_FAILED,
    )
  }

  @Test
  fun helloReplayAndProtectedPacketReplayAreRejected() {
    val client = client(groupKey)
    val server = server(groupKey)
    val hello = client.createHello(0)
    val accepted = server.accept(hello, 0) as LanHandshakeResult.Accepted

    assertRejected(server.accept(hello, 0), LanProtocolError.REPLAY_DETECTED)

    val packet = client.accept(accepted.ack).seal(LanMessage.Close("done"))
    assertThat(accepted.channel.open(packet)).isEqualTo(LanMessage.Close("done"))
    assertProtocolError(
      runCatching { accepted.channel.open(packet) },
      LanProtocolError.REPLAY_DETECTED,
    )
  }

  @Test
  fun sharedNonceRegistryRejectsReplayAcrossServerConnections() {
    val registry = InMemoryLanNonceRegistry()
    val hello = client(groupKey).createHello(0)
    val first = LanHandshakeServer(SERVER_ID, members, groupKey, nonceRegistry = registry)
    val second = LanHandshakeServer(SERVER_ID, members, groupKey, nonceRegistry = registry)

    assertThat(first.accept(hello, 0)).isInstanceOf(LanHandshakeResult.Accepted::class.java)
    assertRejected(second.accept(hello, 0), LanProtocolError.REPLAY_DETECTED)
  }

  @Test
  fun encryptedPacketTamperingIsRejectedWithoutAdvancingTheSequence() {
    val client = client(groupKey)
    val accepted = server(groupKey).accept(client.createHello(0), 0) as LanHandshakeResult.Accepted
    val clientChannel = client.accept(accepted.ack)
    val packet = clientChannel.seal(LanMessage.Ack(BATCH_ID, 42))
    val tampered = packet.copy(ciphertext = packet.ciphertext.flipLastBit())

    assertProtocolError(
      runCatching { accepted.channel.open(tampered) },
      LanProtocolError.AUTHENTICATION_FAILED,
    )
    assertThat(accepted.channel.open(packet)).isEqualTo(LanMessage.Ack(BATCH_ID, 42))
  }

  @Test
  fun revokedIdentityCanAuthenticateOnlyAMembershipOnlySession() {
    val departureClient =
      LanHandshakeClient(
        localDeviceId = CLIENT_ID,
        expectedServerDeviceId = SERVER_ID,
        groupKey = groupKey,
        scope = LanSessionScope.MEMBERSHIP_ONLY,
        nonceSource = { nonce(6) },
      )
    val server =
      LanHandshakeServer(
        localDeviceId = SERVER_ID,
        allowedMemberIds = setOf(SERVER_ID),
        membershipOnlyMemberIds = members,
        groupKey = groupKey,
        nonceSource = { nonce(7) },
      )

    val accepted = server.accept(departureClient.createHello(0), 0)
    val clinical = client(groupKey).createHello(0)

    assertThat(accepted).isInstanceOf(LanHandshakeResult.Accepted::class.java)
    assertThat((accepted as LanHandshakeResult.Accepted).ack.scope)
      .isEqualTo(LanSessionScope.MEMBERSHIP_ONLY)
    assertRejected(server.accept(clinical, 0), LanProtocolError.UNKNOWN_MEMBER)
  }

  private fun client(key: String) =
    LanHandshakeClient(
      localDeviceId = CLIENT_ID,
      expectedServerDeviceId = SERVER_ID,
      groupKey = key,
      nonceSource = { nonce(1) },
    )

  private fun server(key: String) =
    LanHandshakeServer(
      localDeviceId = SERVER_ID,
      allowedMemberIds = members,
      groupKey = key,
      nonceSource = { nonce(2) },
    )

  private fun assertRejected(result: LanHandshakeResult, error: LanProtocolError) {
    assertThat(result).isInstanceOf(LanHandshakeResult.Rejected::class.java)
    assertThat((result as LanHandshakeResult.Rejected).error).isEqualTo(error)
  }

  private fun assertProtocolError(result: Result<*>, error: LanProtocolError) {
    assertThat(result.exceptionOrNull()).isInstanceOf(LanProtocolException::class.java)
    assertThat((result.exceptionOrNull() as LanProtocolException).error).isEqualTo(error)
  }

  private fun key(value: Int): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { value.toByte() })

  private fun nonce(value: Int) = ByteArray(LanProtocol.NONCE_SIZE) { value.toByte() }

  private fun LanBytes.flipLastBit(): LanBytes {
    val changed = copyToByteArray()
    changed[changed.lastIndex] = (changed.last().toInt() xor 1).toByte()
    return LanBytes(changed)
  }

  companion object {
    const val CLIENT_ID = LanProtocolTest.CLIENT_ID
    const val SERVER_ID = LanProtocolTest.SERVER_ID
    const val UNKNOWN_ID = "00000000-0000-0000-0000-000000000099"
    val BATCH_ID = LanProtocolTest.BATCH_ID
  }
}
