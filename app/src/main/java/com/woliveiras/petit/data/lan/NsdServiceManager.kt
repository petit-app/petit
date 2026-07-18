package com.woliveiras.petit.data.lan

import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NsdServiceManager(
  private val backend: NsdBackend,
  private val localDeviceId: String,
  private val groupId: String,
  private val port: Int,
  private val scope: CoroutineScope,
  private val discoveryTimeoutMillis: Long = DEFAULT_DISCOVERY_TIMEOUT_MILLIS,
) : Closeable {
  private val lock = Any()
  private val peersByDeviceId = linkedMapOf<String, NsdResolvedPeer>()
  private val _peers = MutableStateFlow<List<NsdResolvedPeer>>(emptyList())
  private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)

  private var session = 0L
  private var registration: Closeable? = null
  private var discovery: Closeable? = null
  private var timeoutJob: Job? = null

  val peers: StateFlow<List<NsdResolvedPeer>> = _peers.asStateFlow()
  val errors: SharedFlow<Throwable> = _errors

  init {
    require(localDeviceId.isNotBlank())
    require(groupId.isNotBlank())
    require(port in 1..MAX_PORT)
    require(discoveryTimeoutMillis > 0)
  }

  fun start() {
    val activeSession: Long
    synchronized(lock) {
      if (registration != null) return
      session++
      activeSession = session
      backend.setErrorListener { error ->
        _errors.tryEmit(error)
        stop()
      }
      registration =
        backend.register(
          NsdAdvertisement(
            serviceType = SERVICE_TYPE,
            serviceName = "$SERVICE_NAME_PREFIX$localDeviceId",
            deviceId = localDeviceId,
            groupId = groupId,
            port = port,
          )
        )
      discovery = backend.discover(SERVICE_TYPE) { service -> resolve(service, activeSession) }
      timeoutJob =
        scope.launch {
          delay(discoveryTimeoutMillis)
          stopDiscovery(activeSession)
        }
    }
  }

  fun stop() {
    val activeRegistration: Closeable?
    val activeDiscovery: Closeable?
    val activeTimeout: Job?
    synchronized(lock) {
      session++
      activeRegistration = registration
      activeDiscovery = discovery
      activeTimeout = timeoutJob
      registration = null
      discovery = null
      timeoutJob = null
      peersByDeviceId.clear()
      _peers.value = emptyList()
    }
    activeTimeout?.cancel()
    activeDiscovery?.close()
    activeRegistration?.close()
  }

  override fun close() = stop()

  private fun resolve(service: NsdServiceReference, activeSession: Long) {
    backend.resolve(service) { result ->
      val peer = result.getOrNull() ?: return@resolve
      synchronized(lock) {
        if (session != activeSession || registration == null) return@synchronized
        if (peer.deviceId == localDeviceId || peer.groupId != groupId) return@synchronized
        if (peer.deviceId.isBlank() || peer.hostAddress.isBlank() || peer.port !in 1..MAX_PORT) {
          return@synchronized
        }
        peersByDeviceId[peer.deviceId] = peer
        _peers.value = peersByDeviceId.values.sortedBy(NsdResolvedPeer::deviceId)
      }
    }
  }

  private fun stopDiscovery(activeSession: Long) {
    val activeDiscovery: Closeable?
    synchronized(lock) {
      if (session != activeSession) return
      activeDiscovery = discovery
      discovery = null
      timeoutJob = null
    }
    activeDiscovery?.close()
  }

  companion object {
    const val SERVICE_TYPE = "_petit._tcp."
    const val DEFAULT_DISCOVERY_TIMEOUT_MILLIS = 10_000L

    private const val SERVICE_NAME_PREFIX = "Petit-"
    private const val MAX_PORT = 65_535
  }
}
