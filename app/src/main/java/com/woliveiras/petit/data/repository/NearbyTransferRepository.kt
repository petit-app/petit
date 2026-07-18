package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.PairingState
import com.woliveiras.petit.domain.model.TransferState
import kotlinx.coroutines.flow.Flow

/** Repository interface for device-to-device transfer via Nearby Connections. */
interface NearbyTransferRepository {

  /** Whether the Google Play Services transport is available on this device. */
  fun isAvailable(): Boolean = true

  /** Flow of the current pairing state. */
  val pairingState: Flow<PairingState>

  /** Flow of the current transfer state. */
  val transferState: Flow<TransferState>

  /** Start advertising this device for pairing. */
  suspend fun startAdvertising(deviceName: String, deviceId: String, familyGroupKey: String)

  /** Stop advertising. */
  fun stopAdvertising()

  /** Start discovering nearby devices. */
  suspend fun startDiscovery(deviceName: String, deviceId: String, pairingCode: String)

  /** Explicitly request a connection to a discovered endpoint. */
  suspend fun requestConnection(endpointId: String)

  /** Stop discovery. */
  fun stopDiscovery()

  /** Accept a connection request from a remote device. */
  suspend fun acceptConnection(endpointId: String)

  /** Reject a connection request from a remote device. */
  suspend fun rejectConnection(endpointId: String)

  /** Send an export bundle to the connected device. */
  suspend fun sendData(endpointId: String, bundle: ExportBundle)

  /** The name of the currently connected peer device. */
  val connectedPeerName: String?

  /** The endpoint ID of the currently connected peer device. */
  val connectedPeerId: String?

  /** Disconnect from the remote device. */
  fun disconnect()
}
