package com.woliveiras.petit.presentation.feature.familygroup

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.NearbyTransferRepository
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.PairingError
import com.woliveiras.petit.domain.model.PairingState
import com.woliveiras.petit.domain.pairing.PairingCredentialsGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
  val pairingState: PairingState = PairingState.Idle,
  val isCreatingGroup: Boolean = true,
  val familyGroupKey: String? = null,
  val localDeviceId: String? = null,
  val pairingCodeInput: String = "",
  val isPairingCodeInvalid: Boolean = false,
  val pairingPersisted: Boolean = false,
)

@HiltViewModel
class PairingViewModel
@Inject
constructor(
  private val nearbyTransferRepository: NearbyTransferRepository,
  private val familyGroupRepository: FamilyGroupRepository,
  private val credentialsGenerator: PairingCredentialsGenerator,
) : ViewModel() {

  private val _uiState = MutableStateFlow(PairingUiState())
  val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

  init {
    observePairingState()
  }

  private fun observePairingState() {
    viewModelScope.launch {
      nearbyTransferRepository.pairingState.collect { state ->
        _uiState.update { it.copy(pairingState = state) }
        if (state is PairingState.Paired && !_uiState.value.pairingPersisted) {
          persistAuthorizedPairing(state)
        }
      }
    }
  }

  fun onPairingCodeChanged(value: String) {
    val normalized = value.filter(Char::isDigit).take(4)
    _uiState.update { it.copy(pairingCodeInput = normalized, isPairingCodeInvalid = false) }
  }

  fun startAdvertising() {
    if (!ensureTransportAvailable()) return
    viewModelScope.launch {
      val existingLocal = familyGroupRepository.localDevice.first()
      val existingKey = familyGroupRepository.getFamilyGroupKey()
      val credentials = credentialsGenerator.generate()
      val deviceId = existingLocal?.id ?: familyGroupRepository.getOrCreateLocalDeviceId()
      val familyGroupKey = existingKey ?: credentials.familyGroupKey
      _uiState.update {
        it.copy(
          isCreatingGroup = true,
          familyGroupKey = familyGroupKey,
          localDeviceId = deviceId,
          pairingPersisted = false,
        )
      }
      nearbyTransferRepository.startAdvertising(Build.MODEL, deviceId, familyGroupKey)
    }
  }

  fun startDiscovery() {
    val code = _uiState.value.pairingCodeInput
    if (code.length != 4) {
      _uiState.update { it.copy(isPairingCodeInvalid = true) }
      return
    }
    if (!ensureTransportAvailable()) return
    viewModelScope.launch {
      val existingLocal = familyGroupRepository.localDevice.first()
      val deviceId = existingLocal?.id ?: familyGroupRepository.getOrCreateLocalDeviceId()
      _uiState.update {
        it.copy(
          isCreatingGroup = false,
          familyGroupKey = null,
          localDeviceId = deviceId,
          pairingPersisted = false,
        )
      }
      nearbyTransferRepository.startDiscovery(Build.MODEL, deviceId, code)
    }
  }

  fun requestConnection(endpointId: String) {
    viewModelScope.launch { nearbyTransferRepository.requestConnection(endpointId) }
  }

  fun acceptConnection(endpointId: String) {
    viewModelScope.launch { nearbyTransferRepository.acceptConnection(endpointId) }
  }

  fun rejectConnection(endpointId: String) {
    viewModelScope.launch { nearbyTransferRepository.rejectConnection(endpointId) }
  }

  fun cancel() {
    nearbyTransferRepository.disconnect()
    _uiState.update {
      it.copy(
        pairingState = PairingState.Idle,
        familyGroupKey = null,
        localDeviceId = null,
        pairingPersisted = false,
      )
    }
  }

  fun onPermissionDenied() {
    _uiState.update { it.copy(pairingState = PairingState.Error(PairingError.PermissionDenied)) }
  }

  private fun ensureTransportAvailable(): Boolean {
    if (nearbyTransferRepository.isAvailable()) return true
    _uiState.update {
      it.copy(pairingState = PairingState.Error(PairingError.PlayServicesUnavailable))
    }
    return false
  }

  private suspend fun persistAuthorizedPairing(state: PairingState.Paired) {
    val localId = _uiState.value.localDeviceId ?: return
    val now = System.currentTimeMillis()
    try {
      familyGroupRepository.persistAuthorizedPairing(
        familyGroupKey = state.familyGroupKey,
        localMember =
          FamilyGroupMember(
            id = localId,
            deviceName = Build.MODEL,
            familyGroupKey = state.familyGroupKey,
            isLocalDevice = true,
            lastSyncAt = null,
            createdAt = now,
            updatedAt = now,
          ),
        remoteMember =
          FamilyGroupMember(
            id = state.deviceId,
            deviceName = state.deviceName,
            familyGroupKey = state.familyGroupKey,
            isLocalDevice = false,
            lastSyncAt = null,
            createdAt = now,
            updatedAt = now,
          ),
      )
      _uiState.update { it.copy(familyGroupKey = state.familyGroupKey, pairingPersisted = true) }
    } catch (_: SecurityException) {
      nearbyTransferRepository.disconnect()
      _uiState.update { it.copy(pairingState = PairingState.Error(PairingError.RevokedMember)) }
    } catch (_: Exception) {
      nearbyTransferRepository.disconnect()
      _uiState.update { it.copy(pairingState = PairingState.Error(PairingError.PersistenceFailed)) }
    }
  }

  override fun onCleared() {
    nearbyTransferRepository.disconnect()
    super.onCleared()
  }
}
