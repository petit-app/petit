package com.woliveiras.petit.data.lan

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.woliveiras.petit.domain.lan.LanHandshakeResult
import com.woliveiras.petit.domain.lan.LanHandshakeServer
import com.woliveiras.petit.domain.lan.LanMessage
import java.io.File
import java.net.ServerSocket
import java.util.Base64
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking

/** Debug-only peer used to exercise the protected protocol in a second Android process. */
class RemoteLanPeerService : Service() {
  private var server: ServerSocket? = null

  override fun onCreate() {
    super.onCreate()
    val socket = ServerSocket(0)
    server = socket
    File(cacheDir, PORT_FILE).writeText(socket.localPort.toString())
    thread(name = "remote-lan-test-peer") {
      runBlocking {
        runCatching {
          socket.accept().use { accepted ->
            LanSocketConnection.accepted(accepted).use { connection ->
              val hello = connection.receivePlain() as LanMessage.Hello
              val handshake =
                LanHandshakeServer(
                  localDeviceId = SERVER_ID,
                  allowedMemberIds = setOf(CLIENT_ID),
                  groupKey = GROUP_KEY,
                )
              val result = handshake.accept(hello, 0L) as LanHandshakeResult.Accepted
              connection.sendPlain(result.ack)
              val request = result.channel.open(connection.receiveProtected())
              check(request is LanMessage.Changeset)
              connection.sendProtected(
                result.channel.seal(LanMessage.Ack(request.batchId, request.sinceTimestamp))
              )
              check(result.channel.open(connection.receiveProtected()) == LanMessage.Close("ping"))
              connection.sendProtected(result.channel.seal(LanMessage.Close("pong")))
            }
          }
        }
      }
      stopSelf()
    }
  }

  override fun onBind(intent: Intent?): IBinder = Binder()

  override fun onDestroy() {
    runCatching { server?.close() }
    File(cacheDir, PORT_FILE).delete()
    super.onDestroy()
  }

  companion object {
    const val PORT_FILE = "remote-lan-peer.port"
    const val CLIENT_ID = "00000000-0000-0000-0000-000000000001"
    const val SERVER_ID = "00000000-0000-0000-0000-000000000002"
    val GROUP_KEY: String =
      Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 9 })
  }
}
