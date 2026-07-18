package com.woliveiras.petit.data.repository

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.PairingError
import com.woliveiras.petit.domain.model.PairingState
import com.woliveiras.petit.domain.model.TransferState
import com.woliveiras.petit.domain.pairing.PairingAuthorizationResult
import com.woliveiras.petit.domain.pairing.PairingAuthorizationSession
import com.woliveiras.petit.domain.pairing.PairingCodeGenerator
import com.woliveiras.petit.domain.pairing.PairingMessage
import com.woliveiras.petit.domain.pairing.PairingProtocol
import com.woliveiras.petit.domain.pairing.PairingRejectionReason
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Nearby transport with an explicit, short-lived authorization handshake. */
@Singleton
class NearbyTransferRepositoryImpl
@Inject
constructor(@ApplicationContext private val context: Context) : NearbyTransferRepository {

  companion object {
    private const val SERVICE_ID = "com.woliveiras.petit.familygroup"
    private const val MAX_PAYLOAD_SIZE = 10 * 1024 * 1024
  }

  private enum class PairingRole {
    Advertiser,
    Discoverer,
  }

  private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }
  private val clock = Clock.systemUTC()

  private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
  override val pairingState: Flow<PairingState> = _pairingState.asStateFlow()

  private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
  override val transferState: Flow<TransferState> = _transferState.asStateFlow()

  private val connectedEndpointIdRef = AtomicReference<String?>(null)
  private val pendingDeviceName = AtomicReference<String?>(null)
  private val pairingRole = AtomicReference<PairingRole?>(null)
  private val localDeviceId = AtomicReference<String?>(null)
  private val localDeviceName = AtomicReference<String?>(null)
  private val enteredPairingCode = AtomicReference<String?>(null)
  private val authorizationSession = AtomicReference<PairingAuthorizationSession?>(null)
  private val pendingPairingError = AtomicReference<PairingError?>(null)
  private val receivedPayloadData = StringBuffer()

  override val connectedPeerName: String?
    get() = pendingDeviceName.get()

  override val connectedPeerId: String?
    get() = connectedEndpointIdRef.get()

  override fun isAvailable(): Boolean =
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
      ConnectionResult.SUCCESS

  private val connectionLifecycleCallback =
    object : ConnectionLifecycleCallback() {
      override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
        pendingDeviceName.set(info.endpointName)
        if (pairingRole.get() == PairingRole.Discoverer) {
          connectionsClient.acceptConnection(endpointId, payloadCallback).addOnFailureListener {
            failPairing(PairingError.ConnectionFailed, endpointId)
          }
        } else {
          _pairingState.value = PairingState.ConnectionRequested(info.endpointName, endpointId)
        }
      }

      override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        if (result.status.statusCode != ConnectionsStatusCodes.STATUS_OK) {
          failPairing(PairingError.ConnectionFailed, endpointId)
          return
        }

        connectedEndpointIdRef.set(endpointId)
        if (pairingRole.get() == PairingRole.Discoverer) {
          val code = enteredPairingCode.get()
          val deviceId = localDeviceId.get()
          val deviceName = localDeviceName.get()
          if (code == null || deviceId == null || deviceName == null) {
            failPairing(PairingError.MalformedAuthorization, endpointId)
            return
          }
          sendPairingMessage(
            endpointId,
            PairingMessage.AuthorizationRequest(code, deviceId, deviceName),
          )
        }
      }

      override fun onDisconnected(endpointId: String) {
        connectedEndpointIdRef.compareAndSet(endpointId, null)
        _pairingState.value =
          pendingPairingError.getAndSet(null)?.let(PairingState::Error) ?: PairingState.Idle
        _transferState.value = TransferState.Idle
      }
    }

  private val payloadCallback =
    object : PayloadCallback() {
      override fun onPayloadReceived(endpointId: String, payload: Payload) {
        if (payload.type != Payload.Type.BYTES || endpointId != connectedEndpointIdRef.get()) return
        payload.asBytes()?.let { bytes ->
          val data = String(bytes, Charsets.UTF_8)
          val pairingMessage = PairingProtocol.decode(data)
          if (pairingMessage != null) {
            handlePairingMessage(endpointId, pairingMessage)
          } else if (receivedPayloadData.length + data.length > MAX_PAYLOAD_SIZE) {
            receivedPayloadData.setLength(0)
            _transferState.value = TransferState.Error("Received payload too large")
          } else {
            receivedPayloadData.append(data)
          }
        }
      }

      override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        if (endpointId != connectedEndpointIdRef.get()) return
        when (update.status) {
          PayloadTransferUpdate.Status.IN_PROGRESS ->
            _transferState.value =
              TransferState.Receiving(update.bytesTransferred, update.totalBytes)
          PayloadTransferUpdate.Status.SUCCESS -> finishReceivedPayload()
          PayloadTransferUpdate.Status.FAILURE -> {
            receivedPayloadData.setLength(0)
            _transferState.value = TransferState.Error("Transfer failed")
          }
          PayloadTransferUpdate.Status.CANCELED -> {
            receivedPayloadData.setLength(0)
            _transferState.value = TransferState.Idle
          }
        }
      }
    }

  private val endpointDiscoveryCallback =
    object : EndpointDiscoveryCallback() {
      override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
        pendingDeviceName.set(info.endpointName)
        _pairingState.value = PairingState.EndpointFound(info.endpointName, endpointId)
      }

      override fun onEndpointLost(endpointId: String) {
        val state = _pairingState.value
        if (state is PairingState.EndpointFound && state.endpointId == endpointId) {
          _pairingState.value = PairingState.Error(PairingError.EndpointLost)
        }
      }
    }

  override suspend fun startAdvertising(
    deviceName: String,
    deviceId: String,
    familyGroupKey: String,
  ) {
    cleanupPairingReferences()
    pairingRole.set(PairingRole.Advertiser)
    localDeviceId.set(deviceId)
    localDeviceName.set(deviceName)
    val code = PairingCodeGenerator(clock = clock).generate()
    authorizationSession.set(
      PairingAuthorizationSession(code, familyGroupKey, deviceId, deviceName)
    )
    _pairingState.value = PairingState.WaitingForConnection(code.value)
    val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
    connectionsClient
      .startAdvertising(deviceName, SERVICE_ID, connectionLifecycleCallback, options)
      .addOnFailureListener { failPairing(PairingError.AdvertisingFailed) }
  }

  override fun stopAdvertising() {
    connectionsClient.stopAdvertising()
    authorizationSession.set(null)
    if (_pairingState.value is PairingState.WaitingForConnection) {
      _pairingState.value = PairingState.Idle
    }
  }

  override suspend fun startDiscovery(deviceName: String, deviceId: String, pairingCode: String) {
    require(pairingCode.length == 4 && pairingCode.all(Char::isDigit))
    cleanupPairingReferences()
    pairingRole.set(PairingRole.Discoverer)
    localDeviceId.set(deviceId)
    localDeviceName.set(deviceName)
    enteredPairingCode.set(pairingCode)
    _pairingState.value = PairingState.WaitingForConnection(code = "")
    val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
    connectionsClient
      .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
      .addOnFailureListener { failPairing(PairingError.DiscoveryFailed) }
  }

  override fun stopDiscovery() {
    connectionsClient.stopDiscovery()
    if (_pairingState.value is PairingState.WaitingForConnection) {
      _pairingState.value = PairingState.Idle
    }
  }

  override suspend fun requestConnection(endpointId: String) {
    val deviceName = localDeviceName.get() ?: return
    connectionsClient
      .requestConnection(deviceName, endpointId, connectionLifecycleCallback)
      .addOnFailureListener { failPairing(PairingError.ConnectionFailed, endpointId) }
  }

  override suspend fun acceptConnection(endpointId: String) {
    connectionsClient.acceptConnection(endpointId, payloadCallback).addOnFailureListener {
      failPairing(PairingError.ConnectionFailed, endpointId)
    }
  }

  override suspend fun rejectConnection(endpointId: String) {
    connectionsClient.rejectConnection(endpointId)
    _pairingState.value = PairingState.Idle
  }

  override suspend fun sendData(endpointId: String, bundle: ExportBundle) {
    require(endpointId == connectedEndpointIdRef.get()) { "Endpoint is not authorized" }
    val bytes = bundle.toJson().toString().toByteArray(Charsets.UTF_8)
    _transferState.value = TransferState.Sending(0, bytes.size.toLong())
    val payload = Payload.fromBytes(bytes)
    connectionsClient.sendPayload(endpointId, payload).addOnFailureListener {
      _transferState.value = TransferState.Error("Transfer failed")
    }
  }

  override fun disconnect() {
    connectedEndpointIdRef.getAndSet(null)?.let(connectionsClient::disconnectFromEndpoint)
    connectionsClient.stopAdvertising()
    connectionsClient.stopDiscovery()
    pendingDeviceName.set(null)
    receivedPayloadData.setLength(0)
    cleanupPairingReferences()
    _pairingState.value = PairingState.Idle
    _transferState.value = TransferState.Idle
  }

  private fun handlePairingMessage(endpointId: String, message: PairingMessage) {
    when (message) {
      is PairingMessage.AuthorizationRequest -> authorizeRequest(endpointId, message)
      is PairingMessage.AuthorizationAccepted -> {
        if (pairingRole.get() != PairingRole.Discoverer) {
          failPairing(PairingError.MalformedAuthorization, endpointId)
        } else {
          markAuthorized(endpointId, message.familyGroupKey, message.deviceId, message.deviceName)
        }
      }
      is PairingMessage.AuthorizationRejected ->
        failPairing(message.reason.toPairingError(), endpointId)
    }
  }

  private fun authorizeRequest(endpointId: String, request: PairingMessage.AuthorizationRequest) {
    val session = authorizationSession.get()
    if (pairingRole.get() != PairingRole.Advertiser || session == null) {
      failPairing(PairingError.MalformedAuthorization, endpointId)
      return
    }
    when (val result = session.authorize(request, clock.millis())) {
      is PairingAuthorizationResult.Accepted -> {
        sendPairingMessage(endpointId, result.response)
        markAuthorized(
          endpointId,
          session.familyGroupKey,
          result.remoteDeviceId,
          result.remoteDeviceName,
        )
      }
      is PairingAuthorizationResult.Rejected -> {
        connectionsClient
          .sendPayload(
            endpointId,
            Payload.fromBytes(
              PairingProtocol.encode(PairingMessage.AuthorizationRejected(result.reason))
                .toByteArray(Charsets.UTF_8)
            ),
          )
          .addOnCompleteListener { failPairing(result.reason.toPairingError(), endpointId) }
      }
    }
  }

  private fun sendPairingMessage(endpointId: String, message: PairingMessage) {
    connectionsClient
      .sendPayload(
        endpointId,
        Payload.fromBytes(PairingProtocol.encode(message).toByteArray(Charsets.UTF_8)),
      )
      .addOnFailureListener { failPairing(PairingError.ConnectionFailed, endpointId) }
  }

  private fun markAuthorized(
    endpointId: String,
    familyGroupKey: String,
    peerDeviceId: String,
    peerDeviceName: String,
  ) {
    connectionsClient.stopAdvertising()
    connectionsClient.stopDiscovery()
    connectedEndpointIdRef.set(endpointId)
    pendingDeviceName.set(peerDeviceName)
    _pairingState.value =
      PairingState.Paired(familyGroupKey, peerDeviceName, peerDeviceId, endpointId)
    authorizationSession.set(null)
  }

  private fun failPairing(
    reason: PairingError,
    endpointId: String? = connectedEndpointIdRef.get(),
  ) {
    connectionsClient.stopAdvertising()
    connectionsClient.stopDiscovery()
    authorizationSession.set(null)
    if (endpointId != null) {
      pendingPairingError.set(reason)
      connectionsClient.disconnectFromEndpoint(endpointId)
    } else {
      _pairingState.value = PairingState.Error(reason)
    }
  }

  private fun finishReceivedPayload() {
    val data = receivedPayloadData.toString()
    if (data.isEmpty()) return
    try {
      val bundle = ExportBundle.fromJson(JSONObject(data))
      val errors = ExportBundle.validate(bundle)
      _transferState.value =
        if (errors.isEmpty()) TransferState.Complete(bundle)
        else TransferState.Error("Invalid data received from peer")
    } catch (_: Exception) {
      _transferState.value = TransferState.Error("Failed to parse received data")
    } finally {
      receivedPayloadData.setLength(0)
    }
  }

  private fun PairingRejectionReason.toPairingError(): PairingError =
    when (this) {
      PairingRejectionReason.IncorrectCode -> PairingError.IncorrectCode
      PairingRejectionReason.CodeExpired -> PairingError.CodeExpired
      PairingRejectionReason.MalformedRequest -> PairingError.MalformedAuthorization
    }

  private fun cleanupPairingReferences() {
    pairingRole.set(null)
    localDeviceId.set(null)
    localDeviceName.set(null)
    enteredPairingCode.set(null)
    authorizationSession.set(null)
    pendingPairingError.set(null)
  }
}
