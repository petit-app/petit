package com.woliveiras.petit.data.lan

import com.woliveiras.petit.domain.lan.LanBytes
import com.woliveiras.petit.domain.lan.LanFrameCodec
import com.woliveiras.petit.domain.lan.LanMessage
import com.woliveiras.petit.domain.lan.LanMessageCodec
import com.woliveiras.petit.domain.lan.LanProtocol
import com.woliveiras.petit.domain.lan.LanProtocolError
import com.woliveiras.petit.domain.lan.LanProtocolException
import com.woliveiras.petit.domain.lan.LanSecurePacket
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class LanSocketConnection private constructor(private val socket: Socket) : Closeable {
  init {
    socket.soTimeout = READ_TIMEOUT_MILLIS
    socket.tcpNoDelay = true
  }

  suspend fun sendPlain(message: LanMessage) =
    withContext(Dispatchers.IO) {
      LanFrameCodec.write(socket.getOutputStream(), LanMessageCodec.encode(message))
    }

  suspend fun receivePlain(): LanMessage =
    withContext(Dispatchers.IO) {
      LanMessageCodec.decode(LanFrameCodec.read(socket.getInputStream()))
    }

  suspend fun sendProtected(packet: LanSecurePacket) =
    withContext(Dispatchers.IO) {
      val frame =
        ByteArrayOutputStream().also { output ->
          DataOutputStream(output).use { data ->
            data.writeLong(packet.sequence)
            val ciphertext = packet.ciphertext.copyToByteArray()
            data.writeInt(ciphertext.size)
            data.write(ciphertext)
          }
        }
      LanFrameCodec.write(socket.getOutputStream(), frame.toByteArray())
    }

  suspend fun receiveProtected(): LanSecurePacket =
    withContext(Dispatchers.IO) {
      val frame = LanFrameCodec.read(socket.getInputStream())
      try {
        val input = DataInputStream(ByteArrayInputStream(frame))
        val sequence = input.readLong()
        val size = input.readInt()
        if (sequence < 0 || size !in 1..LanProtocol.MAX_FRAME_BYTES || size != input.available()) {
          throw LanProtocolException(
            LanProtocolError.INVALID_MESSAGE,
            "Invalid protected packet framing",
          )
        }
        LanSecurePacket(sequence, LanBytes(ByteArray(size).also(input::readFully)))
      } catch (exception: LanProtocolException) {
        throw exception
      } catch (exception: Exception) {
        throw LanProtocolException(
          LanProtocolError.INVALID_MESSAGE,
          "Malformed protected packet",
          exception,
        )
      }
    }

  override fun close() {
    runCatching { socket.close() }
  }

  companion object {
    private const val CONNECT_TIMEOUT_MILLIS = 5_000
    private const val READ_TIMEOUT_MILLIS = 10_000

    suspend fun connect(host: String, port: Int): LanSocketConnection =
      withContext(Dispatchers.IO) {
        require(port in 1..65_535)
        val socket = Socket()
        try {
          socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS)
          LanSocketConnection(socket)
        } catch (exception: Exception) {
          socket.close()
          throw exception
        }
      }

    internal fun accepted(socket: Socket) = LanSocketConnection(socket)
  }
}

class LanTcpServer(
  private val scope: CoroutineScope,
  private val onConnection: suspend (LanSocketConnection) -> Unit,
) : Closeable {
  private val closed = AtomicBoolean(true)
  private val permits = Semaphore(MAX_CONCURRENT_CONNECTIONS)
  private val activeConnections = mutableSetOf<LanSocketConnection>()
  private var serverSocket: ServerSocket? = null
  private var acceptJob: Job? = null

  @Synchronized
  fun start(): Int {
    serverSocket?.let {
      return it.localPort
    }
    val server = ServerSocket(0).apply { reuseAddress = true }
    serverSocket = server
    closed.set(false)
    acceptJob =
      scope.launch(Dispatchers.IO) {
        while (!closed.get()) {
          val socket =
            try {
              server.accept()
            } catch (exception: SocketException) {
              if (closed.get()) break else throw exception
            }
          launch {
            permits.withPermit {
              val connection = LanSocketConnection.accepted(socket)
              synchronized(activeConnections) { activeConnections += connection }
              connection.use {
                try {
                  runCatching { onConnection(connection) }
                } finally {
                  synchronized(activeConnections) { activeConnections -= connection }
                }
              }
            }
          }
        }
      }
    return server.localPort
  }

  override fun close() {
    closed.set(true)
    runCatching { serverSocket?.close() }
    synchronized(activeConnections) {
      activeConnections.forEach(LanSocketConnection::close)
      activeConnections.clear()
    }
    serverSocket = null
    acceptJob?.cancel()
    acceptJob = null
  }

  suspend fun closeAndJoin() {
    val job = acceptJob
    close()
    job?.cancelAndJoin()
  }

  private companion object {
    const val MAX_CONCURRENT_CONNECTIONS = 4
  }
}
