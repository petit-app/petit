package com.woliveiras.petit.data.lan

import com.google.common.truth.Truth.assertThat
import java.io.Closeable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NsdServiceManagerTest {

  @Test
  fun advertisesRequiredMetadataAndIgnoresTheLocalDevice() = runTest {
    val backend = FakeNsdBackend()
    val manager =
      NsdServiceManager(
        backend = backend,
        localDeviceId = LOCAL_DEVICE_ID,
        groupId = GROUP_ID,
        port = 4242,
        scope = backgroundScope,
      )

    manager.start()

    assertThat(backend.advertisement)
      .isEqualTo(
        NsdAdvertisement(
          serviceType = NsdServiceManager.SERVICE_TYPE,
          serviceName = "Petit-$LOCAL_DEVICE_ID",
          deviceId = LOCAL_DEVICE_ID,
          groupId = GROUP_ID,
          port = 4242,
        )
      )

    backend.resolve(
      NsdResolvedPeer(
        serviceName = "Petit-self",
        deviceId = LOCAL_DEVICE_ID,
        groupId = GROUP_ID,
        hostAddress = "192.0.2.1",
        port = 4242,
      )
    )

    assertThat(manager.peers.value).isEmpty()
  }

  @Test
  fun resolvesMultiplePeersAndDeduplicatesUpdatesByDeviceId() = runTest {
    val backend = FakeNsdBackend()
    val manager = manager(backend)
    manager.start()

    backend.resolve(peer(PEER_A_ID, "192.0.2.10", 4010))
    backend.resolve(peer(PEER_B_ID, "192.0.2.20", 4020))
    backend.resolve(peer(PEER_A_ID, "192.0.2.11", 4011))

    assertThat(manager.peers.value.map { it.deviceId })
      .containsExactly(PEER_A_ID, PEER_B_ID)
      .inOrder()
    assertThat(manager.peers.value.first { it.deviceId == PEER_A_ID }.hostAddress)
      .isEqualTo("192.0.2.11")
    assertThat(manager.peers.value.first { it.deviceId == PEER_A_ID }.port).isEqualTo(4011)
  }

  @Test
  fun configurableTimeoutStopsDiscoveryButKeepsTheAdvertisementRegistered() = runTest {
    val backend = FakeNsdBackend()
    val manager = manager(backend, timeoutMillis = 250)
    manager.start()

    advanceTimeBy(250)
    runCurrent()

    assertThat(backend.discoveryCloseCount).isEqualTo(1)
    assertThat(backend.registrationCloseCount).isEqualTo(0)

    manager.stop()

    assertThat(backend.discoveryCloseCount).isEqualTo(1)
    assertThat(backend.registrationCloseCount).isEqualTo(1)
  }

  @Test
  fun stopCleansUpRegistrationAndDiscoveryExactlyOnce() = runTest {
    val backend = FakeNsdBackend()
    val manager = manager(backend)
    manager.start()
    backend.resolve(peer(PEER_A_ID, "192.0.2.10", 4010))

    manager.stop()
    manager.stop()

    assertThat(backend.discoveredServiceType).isEqualTo(NsdServiceManager.SERVICE_TYPE)
    assertThat(backend.discoveryCloseCount).isEqualTo(1)
    assertThat(backend.registrationCloseCount).isEqualTo(1)
    assertThat(manager.peers.value).isEmpty()
  }

  private fun kotlinx.coroutines.test.TestScope.manager(
    backend: FakeNsdBackend,
    timeoutMillis: Long = 60_000,
  ) =
    NsdServiceManager(
      backend = backend,
      localDeviceId = LOCAL_DEVICE_ID,
      groupId = GROUP_ID,
      port = 4242,
      scope = backgroundScope,
      discoveryTimeoutMillis = timeoutMillis,
    )

  private fun peer(deviceId: String, host: String, port: Int) =
    NsdResolvedPeer(
      serviceName = "Petit-$deviceId",
      deviceId = deviceId,
      groupId = GROUP_ID,
      hostAddress = host,
      port = port,
    )

  private class FakeNsdBackend : NsdBackend {
    var advertisement: NsdAdvertisement? = null
    var discoveredServiceType: String? = null
    var registrationCloseCount = 0
    var discoveryCloseCount = 0
    private var onServiceFound: ((NsdServiceReference) -> Unit)? = null
    private var onResolved: ((Result<NsdResolvedPeer>) -> Unit)? = null

    override fun register(advertisement: NsdAdvertisement): Closeable {
      this.advertisement = advertisement
      return RecordingCloseable { registrationCloseCount++ }
    }

    override fun discover(
      serviceType: String,
      onServiceFound: (NsdServiceReference) -> Unit,
    ): Closeable {
      discoveredServiceType = serviceType
      this.onServiceFound = onServiceFound
      return RecordingCloseable { discoveryCloseCount++ }
    }

    override fun resolve(
      service: NsdServiceReference,
      callback: (Result<NsdResolvedPeer>) -> Unit,
    ) {
      onResolved = callback
    }

    fun resolve(peer: NsdResolvedPeer) {
      onServiceFound?.invoke(NsdServiceReference("ref", peer.serviceName))
      onResolved?.invoke(Result.success(peer))
    }
  }

  private class RecordingCloseable(private val onFirstClose: () -> Unit) : Closeable {
    private var closed = false

    override fun close() {
      if (closed) return
      closed = true
      onFirstClose()
    }
  }

  companion object {
    private const val LOCAL_DEVICE_ID = "00000000-0000-0000-0000-000000000001"
    private const val PEER_A_ID = "00000000-0000-0000-0000-000000000002"
    private const val PEER_B_ID = "00000000-0000-0000-0000-000000000003"
    private const val GROUP_ID = "group-hash"
  }
}
