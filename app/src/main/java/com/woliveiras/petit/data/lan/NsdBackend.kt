package com.woliveiras.petit.data.lan

import java.io.Closeable

data class NsdAdvertisement(
  val serviceType: String,
  val serviceName: String,
  val deviceId: String,
  val groupId: String,
  val port: Int,
)

data class NsdServiceReference(val id: String, val serviceName: String)

data class NsdResolvedPeer(
  val serviceName: String,
  val deviceId: String,
  val groupId: String,
  val hostAddress: String,
  val port: Int,
)

interface NsdBackend {
  fun setErrorListener(listener: (Throwable) -> Unit) = Unit

  fun register(advertisement: NsdAdvertisement): Closeable

  fun discover(serviceType: String, onServiceFound: (NsdServiceReference) -> Unit): Closeable

  fun resolve(service: NsdServiceReference, callback: (Result<NsdResolvedPeer>) -> Unit)
}
