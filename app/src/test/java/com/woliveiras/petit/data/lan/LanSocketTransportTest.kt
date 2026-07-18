package com.woliveiras.petit.data.lan

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.lan.LanMessage
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LanSocketTransportTest {
  @Test
  fun loopbackServerFramesMessagesAndReleasesItsPortIdempotently() = runBlocking {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val received = AtomicReference<LanMessage>()
    val server =
      LanTcpServer(scope) { connection ->
        received.set(connection.receivePlain())
        connection.sendPlain(LanMessage.Close("done"))
      }
    val port = server.start()

    LanSocketConnection.connect("127.0.0.1", port).use { client ->
      client.sendPlain(
        LanMessage.Error(com.woliveiras.petit.domain.lan.LanProtocolError.INVALID_MESSAGE, "test")
      )
      assertThat(client.receivePlain()).isEqualTo(LanMessage.Close("done"))
    }

    assertThat(received.get())
      .isEqualTo(
        LanMessage.Error(com.woliveiras.petit.domain.lan.LanProtocolError.INVALID_MESSAGE, "test")
      )
    server.closeAndJoin()
    server.close()
    scope.cancel()
  }
}
