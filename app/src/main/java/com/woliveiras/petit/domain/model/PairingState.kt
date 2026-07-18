package com.woliveiras.petit.domain.model

/** State machine for the device pairing process via Nearby Connections. */
sealed interface PairingState {
  /** No pairing in progress. */
  data object Idle : PairingState

  /** Advertising and waiting for another device to connect. */
  data class WaitingForConnection(val code: String) : PairingState

  /** A discovered endpoint waiting for an explicit user connection request. */
  data class EndpointFound(val deviceName: String, val endpointId: String) : PairingState

  /** A remote device has requested to connect. */
  data class ConnectionRequested(val deviceName: String, val endpointId: String) : PairingState

  /** Pairing completed successfully. */
  data class Paired(
    val familyGroupKey: String,
    val deviceName: String,
    val deviceId: String,
    val endpointId: String,
  ) : PairingState

  /** An error occurred during pairing. */
  data class Error(val reason: PairingError) : PairingState
}

enum class PairingError {
  PermissionDenied,
  PlayServicesUnavailable,
  AdvertisingFailed,
  DiscoveryFailed,
  ConnectionFailed,
  IncorrectCode,
  CodeExpired,
  MalformedAuthorization,
  EndpointLost,
  PersistenceFailed,
}
