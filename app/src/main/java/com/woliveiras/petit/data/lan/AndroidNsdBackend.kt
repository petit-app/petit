package com.woliveiras.petit.data.lan

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.io.Closeable
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("DEPRECATION")
class AndroidNsdBackend(private val nsdManager: NsdManager) : NsdBackend {
  private val references = ConcurrentHashMap<String, NsdServiceInfo>()
  @Volatile private var errorListener: (Throwable) -> Unit = {}

  override fun setErrorListener(listener: (Throwable) -> Unit) {
    errorListener = listener
  }

  override fun register(advertisement: NsdAdvertisement): Closeable {
    val serviceInfo =
      NsdServiceInfo().apply {
        serviceName = advertisement.serviceName
        serviceType = advertisement.serviceType
        port = advertisement.port
        setAttribute(TXT_DEVICE_ID, advertisement.deviceId)
        setAttribute(TXT_GROUP_ID, advertisement.groupId)
        setAttribute(TXT_PORT, advertisement.port.toString())
      }
    return RegistrationHandle(nsdManager, errorListener).also { listener ->
      nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }
  }

  override fun discover(
    serviceType: String,
    onServiceFound: (NsdServiceReference) -> Unit,
  ): Closeable =
    DiscoveryHandle(nsdManager, errorListener) { serviceInfo ->
        val reference =
          NsdServiceReference(
            id = UUID.randomUUID().toString(),
            serviceName = serviceInfo.serviceName,
          )
        references[reference.id] = serviceInfo
        onServiceFound(reference)
      }
      .also { listener ->
        listener.onClosed = { references.clear() }
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
      }

  override fun resolve(service: NsdServiceReference, callback: (Result<NsdResolvedPeer>) -> Unit) {
    val serviceInfo = references[service.id]
    if (serviceInfo == null) {
      callback(Result.failure(NsdBackendException("Unknown NSD service reference")))
      return
    }
    nsdManager.resolveService(
      serviceInfo,
      object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
          references.remove(service.id)
          callback(Result.failure(NsdBackendException("NSD resolve failed: $errorCode")))
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
          references.remove(service.id)
          callback(runCatching { serviceInfo.toResolvedPeer() })
        }
      },
    )
  }

  private fun NsdServiceInfo.toResolvedPeer(): NsdResolvedPeer {
    val deviceId = requiredTextAttribute(TXT_DEVICE_ID)
    val groupId = requiredTextAttribute(TXT_GROUP_ID)
    val advertisedPort = requiredTextAttribute(TXT_PORT).toIntOrNull()
    if (advertisedPort == null || advertisedPort !in 1..MAX_PORT || advertisedPort != port) {
      throw NsdBackendException("Invalid NSD port metadata")
    }
    val address = host?.hostAddress
    if (address.isNullOrBlank()) throw NsdBackendException("NSD service has no host address")
    return NsdResolvedPeer(serviceName, deviceId, groupId, address, advertisedPort)
  }

  private fun NsdServiceInfo.requiredTextAttribute(key: String): String {
    val value = attributes[key]?.toString(StandardCharsets.UTF_8)?.trim()
    if (value.isNullOrEmpty()) throw NsdBackendException("Missing NSD TXT attribute: $key")
    return value
  }

  private class RegistrationHandle(
    private val nsdManager: NsdManager,
    private val onError: (Throwable) -> Unit,
  ) : NsdManager.RegistrationListener, Closeable {
    private val closed = AtomicBoolean(false)
    private val registered = AtomicBoolean(false)

    override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
      registered.set(true)
      if (closed.get()) unregister()
    }

    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
      onError(NsdBackendException("NSD registration failed: $errorCode"))
    }

    override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
      registered.set(false)
    }

    override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
      onError(NsdBackendException("NSD unregistration failed: $errorCode"))
    }

    override fun close() {
      if (!closed.compareAndSet(false, true)) return
      if (registered.get()) unregister()
    }

    private fun unregister() {
      if (!registered.compareAndSet(true, false)) return
      try {
        nsdManager.unregisterService(this)
      } catch (_: IllegalArgumentException) {
        // Android rejects listeners which have already been released.
      }
    }
  }

  private class DiscoveryHandle(
    private val nsdManager: NsdManager,
    private val onError: (Throwable) -> Unit,
    private val onFound: (NsdServiceInfo) -> Unit,
  ) : NsdManager.DiscoveryListener, Closeable {
    private val closed = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    var onClosed: () -> Unit = {}

    override fun onDiscoveryStarted(serviceType: String) {
      started.set(true)
      if (closed.get()) stopDiscovery()
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
      onError(NsdBackendException("NSD discovery failed: $errorCode"))
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
      onError(NsdBackendException("NSD discovery stop failed: $errorCode"))
    }

    override fun onDiscoveryStopped(serviceType: String) {
      started.set(false)
    }

    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
      if (!closed.get()) onFound(serviceInfo)
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

    override fun close() {
      if (!closed.compareAndSet(false, true)) return
      onClosed()
      if (started.get()) stopDiscovery()
    }

    private fun stopDiscovery() {
      if (!started.compareAndSet(true, false)) return
      try {
        nsdManager.stopServiceDiscovery(this)
      } catch (_: IllegalArgumentException) {
        // Android rejects listeners which have already been released.
      }
    }
  }

  companion object {
    const val TXT_DEVICE_ID = "deviceId"
    const val TXT_GROUP_ID = "groupId"
    const val TXT_PORT = "port"

    private const val MAX_PORT = 65_535
  }
}

class NsdBackendException(message: String) : IllegalStateException(message)
