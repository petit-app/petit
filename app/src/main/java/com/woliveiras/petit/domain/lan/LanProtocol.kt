package com.woliveiras.petit.domain.lan

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

object LanProtocol {
  const val CURRENT_VERSION = 1
  const val NONCE_SIZE = 32
  const val HMAC_SIZE = 32
  const val MAX_CHANGESET_BYTES = 512 * 1024
  const val MAX_FRAME_BYTES = 1024 * 1024

  internal const val MAGIC = 0x50544C4E // PTLN
  internal const val MAX_TEXT_BYTES = 512
}

enum class LanProtocolError {
  UNSUPPORTED_VERSION,
  AUTHENTICATION_FAILED,
  UNKNOWN_MEMBER,
  REPLAY_DETECTED,
  INVALID_SEQUENCE,
  INVALID_MESSAGE,
  FRAME_TOO_LARGE,
  CHANGESET_TOO_LARGE,
}

enum class LanSessionScope {
  CLINICAL,
  MEMBERSHIP_ONLY,
}

class LanProtocolException(val error: LanProtocolError, message: String, cause: Throwable? = null) :
  IllegalArgumentException(message, cause)

class LanBytes(bytes: ByteArray) {
  private val value = bytes.copyOf()

  val size: Int
    get() = value.size

  fun copyToByteArray(): ByteArray = value.copyOf()

  override fun equals(other: Any?): Boolean = other is LanBytes && value.contentEquals(other.value)

  override fun hashCode(): Int = value.contentHashCode()

  override fun toString(): String = "LanBytes(size=${value.size})"
}

sealed interface LanMessage {
  data class Hello(
    val version: Int,
    val deviceId: String,
    val lastSyncTimestamp: Long,
    val nonce: LanBytes,
    val authenticationTag: LanBytes,
    val scope: LanSessionScope = LanSessionScope.CLINICAL,
  ) : LanMessage

  data class HelloAck(
    val version: Int,
    val deviceId: String,
    val lastSyncTimestamp: Long,
    val clientNonce: LanBytes,
    val serverNonce: LanBytes,
    val authenticationTag: LanBytes,
    val scope: LanSessionScope = LanSessionScope.CLINICAL,
  ) : LanMessage

  data class Changeset(val batchId: String, val sinceTimestamp: Long, val payload: LanBytes) :
    LanMessage

  data class Ack(val batchId: String, val newSyncTimestamp: Long) : LanMessage

  data class Error(val error: LanProtocolError, val detail: String) : LanMessage

  data class Close(val reason: String) : LanMessage
}

object LanMessageCodec {
  private const val TYPE_HELLO = 1
  private const val TYPE_HELLO_ACK = 2
  private const val TYPE_CHANGESET = 3
  private const val TYPE_ACK = 4
  private const val TYPE_ERROR = 5
  private const val TYPE_CLOSE = 6

  fun encode(message: LanMessage): ByteArray {
    validate(message)
    val output = ByteArrayOutputStream()
    DataOutputStream(output).use { data ->
      data.writeInt(LanProtocol.MAGIC)
      data.writeInt(
        when (message) {
          is LanMessage.Hello -> message.version
          is LanMessage.HelloAck -> message.version
          else -> LanProtocol.CURRENT_VERSION
        }
      )
      when (message) {
        is LanMessage.Hello -> {
          data.writeByte(TYPE_HELLO)
          data.writeString(message.deviceId)
          data.writeLong(message.lastSyncTimestamp)
          data.writeBytesWithLength(message.nonce)
          data.writeBytesWithLength(message.authenticationTag)
          data.writeInt(message.scope.ordinal)
        }
        is LanMessage.HelloAck -> {
          data.writeByte(TYPE_HELLO_ACK)
          data.writeString(message.deviceId)
          data.writeLong(message.lastSyncTimestamp)
          data.writeBytesWithLength(message.clientNonce)
          data.writeBytesWithLength(message.serverNonce)
          data.writeBytesWithLength(message.authenticationTag)
          data.writeInt(message.scope.ordinal)
        }
        is LanMessage.Changeset -> {
          data.writeByte(TYPE_CHANGESET)
          data.writeString(message.batchId)
          data.writeLong(message.sinceTimestamp)
          data.writeBytesWithLength(message.payload)
        }
        is LanMessage.Ack -> {
          data.writeByte(TYPE_ACK)
          data.writeString(message.batchId)
          data.writeLong(message.newSyncTimestamp)
        }
        is LanMessage.Error -> {
          data.writeByte(TYPE_ERROR)
          data.writeInt(message.error.ordinal)
          data.writeString(message.detail)
        }
        is LanMessage.Close -> {
          data.writeByte(TYPE_CLOSE)
          data.writeString(message.reason)
        }
      }
    }
    return output.toByteArray().also {
      if (it.size > LanProtocol.MAX_FRAME_BYTES) {
        throw LanProtocolException(LanProtocolError.FRAME_TOO_LARGE, "Encoded frame is too large")
      }
    }
  }

  fun decode(encoded: ByteArray): LanMessage {
    if (encoded.isEmpty() || encoded.size > LanProtocol.MAX_FRAME_BYTES) {
      throw LanProtocolException(LanProtocolError.FRAME_TOO_LARGE, "Invalid frame size")
    }
    try {
      val input = DataInputStream(ByteArrayInputStream(encoded))
      if (input.readInt() != LanProtocol.MAGIC) invalid("Invalid protocol magic")
      val version = input.readInt()
      val type = input.readUnsignedByte()
      if (type !in TYPE_HELLO..TYPE_HELLO_ACK && version != LanProtocol.CURRENT_VERSION) {
        throw LanProtocolException(
          LanProtocolError.UNSUPPORTED_VERSION,
          "Unsupported protocol version: $version",
        )
      }
      val message =
        when (type) {
          TYPE_HELLO ->
            LanMessage.Hello(
              version,
              input.readString(),
              input.readLong(),
              input.readBytesWithLength(LanProtocol.NONCE_SIZE),
              input.readBytesWithLength(LanProtocol.HMAC_SIZE),
              input.readScope(),
            )
          TYPE_HELLO_ACK ->
            LanMessage.HelloAck(
              version,
              input.readString(),
              input.readLong(),
              input.readBytesWithLength(LanProtocol.NONCE_SIZE),
              input.readBytesWithLength(LanProtocol.NONCE_SIZE),
              input.readBytesWithLength(LanProtocol.HMAC_SIZE),
              input.readScope(),
            )
          TYPE_CHANGESET ->
            LanMessage.Changeset(
              input.readString(),
              input.readLong(),
              input.readBytesWithLength(LanProtocol.MAX_CHANGESET_BYTES),
            )
          TYPE_ACK -> LanMessage.Ack(input.readString(), input.readLong())
          TYPE_ERROR -> {
            val ordinal = input.readInt()
            val error = LanProtocolError.entries.getOrNull(ordinal) ?: invalid("Unknown error code")
            LanMessage.Error(error, input.readString())
          }
          TYPE_CLOSE -> LanMessage.Close(input.readString())
          else -> invalid("Unknown message type")
        }
      if (input.available() != 0) invalid("Trailing message bytes")
      validate(message)
      return message
    } catch (exception: LanProtocolException) {
      throw exception
    } catch (exception: EOFException) {
      throw LanProtocolException(
        LanProtocolError.INVALID_MESSAGE,
        "Truncated protocol message",
        exception,
      )
    } catch (exception: Exception) {
      throw LanProtocolException(
        LanProtocolError.INVALID_MESSAGE,
        "Malformed protocol message",
        exception,
      )
    }
  }

  private fun validate(message: LanMessage) {
    when (message) {
      is LanMessage.Hello -> {
        requireDeviceId(message.deviceId)
        requireSize(message.nonce, LanProtocol.NONCE_SIZE, "client nonce")
        requireSize(message.authenticationTag, LanProtocol.HMAC_SIZE, "HELLO tag")
        requireTimestamp(message.lastSyncTimestamp)
      }
      is LanMessage.HelloAck -> {
        requireDeviceId(message.deviceId)
        requireSize(message.clientNonce, LanProtocol.NONCE_SIZE, "client nonce")
        requireSize(message.serverNonce, LanProtocol.NONCE_SIZE, "server nonce")
        requireSize(message.authenticationTag, LanProtocol.HMAC_SIZE, "HELLO_ACK tag")
        requireTimestamp(message.lastSyncTimestamp)
      }
      is LanMessage.Changeset -> {
        requireUuid(message.batchId, "batch ID")
        requireTimestamp(message.sinceTimestamp)
        if (message.payload.size > LanProtocol.MAX_CHANGESET_BYTES) {
          throw LanProtocolException(
            LanProtocolError.CHANGESET_TOO_LARGE,
            "Changeset exceeds ${LanProtocol.MAX_CHANGESET_BYTES} bytes",
          )
        }
      }
      is LanMessage.Ack -> {
        requireUuid(message.batchId, "batch ID")
        requireTimestamp(message.newSyncTimestamp)
      }
      is LanMessage.Error -> requireText(message.detail)
      is LanMessage.Close -> requireText(message.reason)
    }
  }

  private fun requireDeviceId(value: String) = requireUuid(value, "device ID")

  private fun requireUuid(value: String, label: String) {
    try {
      require(UUID.fromString(value).toString() == value.lowercase())
    } catch (exception: Exception) {
      throw LanProtocolException(LanProtocolError.INVALID_MESSAGE, "Invalid $label", exception)
    }
  }

  private fun requireSize(value: LanBytes, expected: Int, label: String) {
    if (value.size != expected) invalid("Invalid $label size")
  }

  private fun requireTimestamp(value: Long) {
    if (value < 0) invalid("Timestamp must be non-negative")
  }

  private fun requireText(value: String) {
    val size = value.toByteArray(StandardCharsets.UTF_8).size
    if (size > LanProtocol.MAX_TEXT_BYTES) invalid("Text is too long")
  }

  private fun DataOutputStream.writeString(value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    if (bytes.size > LanProtocol.MAX_TEXT_BYTES) invalid("Text is too long")
    writeInt(bytes.size)
    write(bytes)
  }

  private fun DataInputStream.readString(): String {
    val size = readInt()
    if (size !in 0..LanProtocol.MAX_TEXT_BYTES) invalid("Invalid text length")
    return String(ByteArray(size).also(::readFully), StandardCharsets.UTF_8)
  }

  private fun DataOutputStream.writeBytesWithLength(value: LanBytes) {
    val bytes = value.copyToByteArray()
    writeInt(bytes.size)
    write(bytes)
  }

  private fun DataInputStream.readBytesWithLength(maximum: Int): LanBytes {
    val size = readInt()
    if (size !in 0..maximum) {
      val error =
        if (maximum == LanProtocol.MAX_CHANGESET_BYTES) LanProtocolError.CHANGESET_TOO_LARGE
        else LanProtocolError.INVALID_MESSAGE
      throw LanProtocolException(error, "Invalid byte field length")
    }
    return LanBytes(ByteArray(size).also(::readFully))
  }

  private fun DataInputStream.readScope(): LanSessionScope {
    val ordinal = readInt()
    return LanSessionScope.entries.getOrNull(ordinal) ?: invalid("Unknown session scope")
  }

  private fun invalid(message: String): Nothing =
    throw LanProtocolException(LanProtocolError.INVALID_MESSAGE, message)
}

object LanFrameCodec {
  fun write(output: OutputStream, payload: ByteArray) {
    if (payload.isEmpty() || payload.size > LanProtocol.MAX_FRAME_BYTES) {
      throw LanProtocolException(LanProtocolError.FRAME_TOO_LARGE, "Invalid frame size")
    }
    DataOutputStream(output).apply {
      writeInt(payload.size)
      write(payload)
      flush()
    }
  }

  fun read(input: InputStream): ByteArray {
    val data = DataInputStream(input)
    val size =
      try {
        data.readInt()
      } catch (exception: EOFException) {
        throw LanProtocolException(LanProtocolError.INVALID_MESSAGE, "Truncated frame", exception)
      }
    if (size !in 1..LanProtocol.MAX_FRAME_BYTES) {
      throw LanProtocolException(LanProtocolError.FRAME_TOO_LARGE, "Invalid frame size: $size")
    }
    return try {
      ByteArray(size).also(data::readFully)
    } catch (exception: EOFException) {
      throw LanProtocolException(LanProtocolError.INVALID_MESSAGE, "Truncated frame", exception)
    }
  }
}

object LanSessionDirection {
  fun shouldInitiate(localDeviceId: String, remoteDeviceId: String): Boolean {
    val local = canonicalUuid(localDeviceId)
    val remote = canonicalUuid(remoteDeviceId)
    require(local != remote) { "A device cannot open a session with itself" }
    return local < remote
  }

  private fun canonicalUuid(value: String): String = UUID.fromString(value).toString()
}
