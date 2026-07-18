package com.woliveiras.petit.domain.lan

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.LinkedHashSet
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class LanSecurePacket(val sequence: Long, val ciphertext: LanBytes)

sealed interface LanHandshakeResult {
  data class Accepted(val ack: LanMessage.HelloAck, val channel: LanSecureChannel) :
    LanHandshakeResult

  data class Rejected(val error: LanProtocolError, val detail: String) : LanHandshakeResult {
    val response = LanMessage.Error(error, detail)
    val close = LanMessage.Close("handshake rejected")
  }
}

class LanHandshakeClient(
  localDeviceId: String,
  expectedServerDeviceId: String,
  groupKey: String,
  private val scope: LanSessionScope = LanSessionScope.CLINICAL,
  private val nonceSource: () -> ByteArray = ::secureNonce,
) {
  private val localDeviceId = canonicalUuid(localDeviceId)
  private val expectedServerDeviceId = canonicalUuid(expectedServerDeviceId)
  private val key = decodeStrongGroupKey(groupKey)
  private var pendingHello: LanMessage.Hello? = null

  fun createHello(lastSyncTimestamp: Long): LanMessage.Hello {
    require(lastSyncTimestamp >= 0)
    check(pendingHello == null) { "A handshake is already pending" }
    val nonce = requireNonce(nonceSource())
    return LanMessage.Hello(
        version = LanProtocol.CURRENT_VERSION,
        deviceId = localDeviceId,
        lastSyncTimestamp = lastSyncTimestamp,
        nonce = LanBytes(nonce),
        authenticationTag =
          LanBytes(
            hmac(
              key,
              helloAuthenticationInput(
                LanProtocol.CURRENT_VERSION,
                localDeviceId,
                lastSyncTimestamp,
                nonce,
                scope,
              ),
            )
          ),
        scope = scope,
      )
      .also { pendingHello = it }
  }

  fun accept(ack: LanMessage.HelloAck): LanSecureChannel {
    val hello = pendingHello ?: protocolError(LanProtocolError.REPLAY_DETECTED, "No pending HELLO")
    if (ack.version != LanProtocol.CURRENT_VERSION) {
      protocolError(LanProtocolError.UNSUPPORTED_VERSION, "Unsupported HELLO_ACK version")
    }
    if (ack.deviceId != expectedServerDeviceId) {
      protocolError(LanProtocolError.UNKNOWN_MEMBER, "HELLO_ACK came from an unexpected member")
    }
    if (ack.scope != scope) {
      protocolError(LanProtocolError.AUTHENTICATION_FAILED, "HELLO_ACK changed session scope")
    }
    if (!constantTimeEquals(hello.nonce, ack.clientNonce)) {
      protocolError(LanProtocolError.REPLAY_DETECTED, "HELLO_ACK does not match this session")
    }
    val expectedTag =
      hmac(
        key,
        ackAuthenticationInput(
          hello,
          ack.deviceId,
          ack.lastSyncTimestamp,
          ack.serverNonce.copyToByteArray(),
        ),
      )
    if (!constantTimeEquals(expectedTag, ack.authenticationTag.copyToByteArray())) {
      protocolError(LanProtocolError.AUTHENTICATION_FAILED, "Invalid HELLO_ACK authentication")
    }
    pendingHello = null
    return deriveChannel(key, hello, ack, isClient = true)
  }
}

class LanHandshakeServer(
  localDeviceId: String,
  allowedMemberIds: Set<String>,
  groupKey: String,
  membershipOnlyMemberIds: Set<String> = allowedMemberIds,
  private val nonceSource: () -> ByteArray = ::secureNonce,
  private val nonceRegistry: LanNonceRegistry = InMemoryLanNonceRegistry(),
) {
  private val localDeviceId = canonicalUuid(localDeviceId)
  private val allowedMemberIds = allowedMemberIds.map(::canonicalUuid).toSet()
  private val membershipOnlyMemberIds = membershipOnlyMemberIds.map(::canonicalUuid).toSet()
  private val key = decodeStrongGroupKey(groupKey)

  fun accept(hello: LanMessage.Hello, lastSyncTimestamp: Long): LanHandshakeResult {
    require(lastSyncTimestamp >= 0)
    if (hello.version != LanProtocol.CURRENT_VERSION) {
      return rejected(LanProtocolError.UNSUPPORTED_VERSION, "Unsupported protocol version")
    }
    val peerId = runCatching { canonicalUuid(hello.deviceId) }.getOrNull()
    val allowedForScope =
      when (hello.scope) {
        LanSessionScope.CLINICAL -> allowedMemberIds
        LanSessionScope.MEMBERSHIP_ONLY -> membershipOnlyMemberIds
      }
    if (peerId == null || peerId == localDeviceId || peerId !in allowedForScope) {
      return rejected(LanProtocolError.UNKNOWN_MEMBER, "Unknown family group member")
    }
    if (
      hello.nonce.size != LanProtocol.NONCE_SIZE ||
        hello.authenticationTag.size != LanProtocol.HMAC_SIZE
    ) {
      return rejected(LanProtocolError.INVALID_MESSAGE, "Invalid HELLO fields")
    }
    val expectedTag =
      hmac(
        key,
        helloAuthenticationInput(
          hello.version,
          peerId,
          hello.lastSyncTimestamp,
          hello.nonce.copyToByteArray(),
          hello.scope,
        ),
      )
    if (!constantTimeEquals(expectedTag, hello.authenticationTag.copyToByteArray())) {
      return rejected(LanProtocolError.AUTHENTICATION_FAILED, "Invalid group authentication")
    }
    if (!nonceRegistry.recordIfNew(peerId, hello.nonce)) {
      return rejected(LanProtocolError.REPLAY_DETECTED, "HELLO nonce was already used")
    }
    val serverNonce = requireNonce(nonceSource())
    val ack =
      LanMessage.HelloAck(
        version = LanProtocol.CURRENT_VERSION,
        deviceId = localDeviceId,
        lastSyncTimestamp = lastSyncTimestamp,
        clientNonce = hello.nonce,
        serverNonce = LanBytes(serverNonce),
        authenticationTag =
          LanBytes(
            hmac(key, ackAuthenticationInput(hello, localDeviceId, lastSyncTimestamp, serverNonce))
          ),
        scope = hello.scope,
      )
    return LanHandshakeResult.Accepted(ack, deriveChannel(key, hello, ack, isClient = false))
  }

  private fun rejected(error: LanProtocolError, detail: String) =
    LanHandshakeResult.Rejected(error, detail)
}

interface LanNonceRegistry {
  /** Atomically records a peer nonce, returning false when it has already been observed. */
  fun recordIfNew(deviceId: String, nonce: LanBytes): Boolean
}

class InMemoryLanNonceRegistry(private val capacity: Int = 1024) : LanNonceRegistry {
  private val entries = LinkedHashSet<String>()

  init {
    require(capacity > 0)
  }

  @Synchronized
  override fun recordIfNew(deviceId: String, nonce: LanBytes): Boolean {
    val key = "$deviceId:${Base64.getEncoder().encodeToString(nonce.copyToByteArray())}"
    if (!entries.add(key)) return false
    if (entries.size > capacity) entries.remove(entries.first())
    return true
  }
}

class LanSecureChannel
internal constructor(
  private val outgoingKey: ByteArray,
  private val incomingKey: ByteArray,
  private val outgoingDirection: String,
  private val incomingDirection: String,
) {
  private var nextOutgoingSequence = 0L
  private var nextIncomingSequence = 0L

  @Synchronized
  fun seal(message: LanMessage): LanSecurePacket {
    if (message is LanMessage.Hello || message is LanMessage.HelloAck) {
      protocolError(LanProtocolError.INVALID_MESSAGE, "Handshake messages cannot be encrypted")
    }
    val sequence = nextOutgoingSequence
    check(sequence != Long.MAX_VALUE) { "Secure channel sequence exhausted" }
    val plaintext = LanMessageCodec.encode(message)
    val ciphertext = crypt(Cipher.ENCRYPT_MODE, outgoingKey, outgoingDirection, sequence, plaintext)
    nextOutgoingSequence++
    return LanSecurePacket(sequence, LanBytes(ciphertext))
  }

  @Synchronized
  fun open(packet: LanSecurePacket): LanMessage {
    if (packet.sequence < nextIncomingSequence) {
      protocolError(LanProtocolError.REPLAY_DETECTED, "Protected packet was already processed")
    }
    if (packet.sequence != nextIncomingSequence) {
      protocolError(LanProtocolError.INVALID_SEQUENCE, "Protected packet arrived out of order")
    }
    if (packet.ciphertext.size > LanProtocol.MAX_FRAME_BYTES) {
      protocolError(LanProtocolError.FRAME_TOO_LARGE, "Protected packet is too large")
    }
    val plaintext =
      try {
        crypt(
          Cipher.DECRYPT_MODE,
          incomingKey,
          incomingDirection,
          packet.sequence,
          packet.ciphertext.copyToByteArray(),
        )
      } catch (exception: AEADBadTagException) {
        throw LanProtocolException(
          LanProtocolError.AUTHENTICATION_FAILED,
          "Protected packet authentication failed",
          exception,
        )
      }
    val message = LanMessageCodec.decode(plaintext)
    if (message is LanMessage.Hello || message is LanMessage.HelloAck) {
      protocolError(LanProtocolError.INVALID_MESSAGE, "Handshake message on protected channel")
    }
    nextIncomingSequence++
    return message
  }

  private fun crypt(
    mode: Int,
    key: ByteArray,
    direction: String,
    sequence: Long,
    input: ByteArray,
  ): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val ivPrefix = hmac(key, "petit-lan/iv/$direction".toByteArray()).copyOf(4)
    val iv = ByteBuffer.allocate(12).put(ivPrefix).putLong(sequence).array()
    cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
    cipher.updateAAD(
      ByteBuffer.allocate(4 + direction.length + 8)
        .putInt(LanProtocol.CURRENT_VERSION)
        .put(direction.toByteArray(StandardCharsets.US_ASCII))
        .putLong(sequence)
        .array()
    )
    return cipher.doFinal(input)
  }
}

private fun deriveChannel(
  groupKey: ByteArray,
  hello: LanMessage.Hello,
  ack: LanMessage.HelloAck,
  isClient: Boolean,
): LanSecureChannel {
  val sessionKey =
    hmac(
      groupKey,
      binaryInput("petit-lan/session") {
        writeInt(LanProtocol.CURRENT_VERSION)
        writeString(hello.deviceId)
        writeString(ack.deviceId)
        writeInt(hello.scope.ordinal)
        writeBytes(hello.nonce)
        writeBytes(ack.serverNonce)
      },
    )
  val clientToServer = hmac(sessionKey, "petit-lan/key/client-to-server".toByteArray())
  val serverToClient = hmac(sessionKey, "petit-lan/key/server-to-client".toByteArray())
  return if (isClient) {
    LanSecureChannel(clientToServer, serverToClient, "c2s", "s2c")
  } else {
    LanSecureChannel(serverToClient, clientToServer, "s2c", "c2s")
  }
}

private fun helloAuthenticationInput(
  version: Int,
  deviceId: String,
  lastSyncTimestamp: Long,
  nonce: ByteArray,
  scope: LanSessionScope,
) =
  binaryInput("petit-lan/hello") {
    writeInt(version)
    writeString(deviceId)
    writeLong(lastSyncTimestamp)
    writeBytes(LanBytes(nonce))
    writeInt(scope.ordinal)
  }

private fun ackAuthenticationInput(
  hello: LanMessage.Hello,
  serverDeviceId: String,
  serverLastSyncTimestamp: Long,
  serverNonce: ByteArray,
) =
  binaryInput("petit-lan/hello-ack") {
    writeInt(hello.version)
    writeString(hello.deviceId)
    writeString(serverDeviceId)
    writeLong(hello.lastSyncTimestamp)
    writeLong(serverLastSyncTimestamp)
    writeBytes(hello.nonce)
    writeBytes(LanBytes(serverNonce))
    writeInt(hello.scope.ordinal)
  }

private inline fun binaryInput(label: String, block: DataOutputStream.() -> Unit): ByteArray {
  val output = ByteArrayOutputStream()
  DataOutputStream(output).use { data ->
    data.writeString(label)
    data.block()
  }
  return output.toByteArray()
}

private fun DataOutputStream.writeString(value: String) {
  val bytes = value.toByteArray(StandardCharsets.UTF_8)
  writeInt(bytes.size)
  write(bytes)
}

private fun DataOutputStream.writeBytes(value: LanBytes) {
  val bytes = value.copyToByteArray()
  writeInt(bytes.size)
  write(bytes)
}

private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
  Mac.getInstance("HmacSHA256").run {
    init(SecretKeySpec(key, "HmacSHA256"))
    doFinal(data)
  }

private fun constantTimeEquals(first: LanBytes, second: LanBytes): Boolean =
  constantTimeEquals(first.copyToByteArray(), second.copyToByteArray())

private fun constantTimeEquals(first: ByteArray, second: ByteArray): Boolean =
  MessageDigest.isEqual(first, second)

private fun decodeStrongGroupKey(encoded: String): ByteArray {
  val decoded =
    try {
      Base64.getUrlDecoder().decode(encoded)
    } catch (exception: IllegalArgumentException) {
      throw LanProtocolException(
        LanProtocolError.AUTHENTICATION_FAILED,
        "Family group key is malformed",
        exception,
      )
    }
  if (decoded.size < 32) {
    protocolError(LanProtocolError.AUTHENTICATION_FAILED, "Family group key is too weak")
  }
  return decoded
}

private fun requireNonce(value: ByteArray): ByteArray {
  if (value.size != LanProtocol.NONCE_SIZE) {
    protocolError(LanProtocolError.INVALID_MESSAGE, "Nonce must be ${LanProtocol.NONCE_SIZE} bytes")
  }
  return value.copyOf()
}

private fun canonicalUuid(value: String): String =
  try {
    UUID.fromString(value).toString()
  } catch (exception: IllegalArgumentException) {
    throw LanProtocolException(LanProtocolError.INVALID_MESSAGE, "Invalid device ID", exception)
  }

private fun secureNonce(): ByteArray =
  ByteArray(LanProtocol.NONCE_SIZE).also(SecureRandom()::nextBytes)

private fun protocolError(error: LanProtocolError, message: String): Nothing =
  throw LanProtocolException(error, message)
