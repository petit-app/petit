package com.woliveiras.petit.data.lan

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.lan.LanBytes
import com.woliveiras.petit.domain.lan.LanHandshakeClient
import com.woliveiras.petit.domain.lan.LanMessage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanTwoProcessIntegrationTest {
  @Test
  fun protectedTcpHandshakeCrossesAnAndroidProcessBoundary() = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val callerContext = instrumentation.targetContext
    val portFile = File(callerContext.cacheDir, RemoteLanPeerService.PORT_FILE).apply { delete() }
    val connected = CountDownLatch(1)
    val serviceConnection =
      object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
          connected.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit
      }
    val intent = Intent(callerContext, RemoteLanPeerService::class.java)
    assertThat(callerContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE))
      .isTrue()
    try {
      assertThat(connected.await(5, TimeUnit.SECONDS)).isTrue()
      val port = waitForPort(portFile)
      val handshake =
        LanHandshakeClient(
          localDeviceId = RemoteLanPeerService.CLIENT_ID,
          expectedServerDeviceId = RemoteLanPeerService.SERVER_ID,
          groupKey = RemoteLanPeerService.GROUP_KEY,
        )
      LanSocketConnection.connect("127.0.0.1", port).use { connection ->
        connection.sendPlain(handshake.createHello(0L))
        val channel = handshake.accept(connection.receivePlain() as LanMessage.HelloAck)
        val changeset = LanMessage.Changeset(BATCH_ID, 42L, LanBytes("payload".toByteArray()))
        connection.sendProtected(channel.seal(changeset))
        assertThat(channel.open(connection.receiveProtected()))
          .isEqualTo(LanMessage.Ack(BATCH_ID, 42L))
        connection.sendProtected(channel.seal(LanMessage.Close("ping")))

        assertThat(channel.open(connection.receiveProtected())).isEqualTo(LanMessage.Close("pong"))
      }
    } finally {
      callerContext.unbindService(serviceConnection)
    }
  }

  private companion object {
    const val BATCH_ID = "00000000-0000-0000-0000-000000000003"
  }

  private fun waitForPort(file: File): Int {
    repeat(50) {
      file
        .takeIf { it.exists() }
        ?.readText()
        ?.trim()
        ?.toIntOrNull()
        ?.let {
          return it
        }
      Thread.sleep(100)
    }
    error("Remote LAN peer did not publish its port")
  }
}
