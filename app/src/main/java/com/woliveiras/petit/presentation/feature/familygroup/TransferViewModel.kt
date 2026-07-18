package com.woliveiras.petit.presentation.feature.familygroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.NearbyTransferRepository
import com.woliveiras.petit.domain.model.MergeResult
import com.woliveiras.petit.domain.model.TransferError
import com.woliveiras.petit.domain.model.TransferState
import com.woliveiras.petit.domain.usecase.MergeDataUseCase
import com.woliveiras.petit.domain.usecase.SendDataUseCase
import com.woliveiras.petit.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferUiState(
  val transferState: TransferState = TransferState.Idle,
  val mergeResult: MergeResult? = null,
  val isMerging: Boolean = false,
  val isSending: Boolean = false,
  val showReplaceConfirmation: Boolean = false,
)

@HiltViewModel
class TransferViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val nearbyTransferRepository: NearbyTransferRepository,
  private val sendDataUseCase: SendDataUseCase,
  private val mergeDataUseCase: MergeDataUseCase,
) : ViewModel() {

  private val mode: String = savedStateHandle["mode"] ?: Screen.FamilyGroupTransfer.MODE_SEND

  private val _uiState =
    MutableStateFlow(TransferUiState(isSending = mode == Screen.FamilyGroupTransfer.MODE_SEND))
  val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

  init {
    observeTransferState()
    if (mode == Screen.FamilyGroupTransfer.MODE_SEND) {
      autoSend()
    }
  }

  private fun observeTransferState() {
    viewModelScope.launch {
      nearbyTransferRepository.transferState.collect { state ->
        _uiState.update { it.copy(transferState = state) }
      }
    }
  }

  private fun autoSend() {
    viewModelScope.launch {
      val endpointId = nearbyTransferRepository.connectedPeerId
      if (endpointId != null) {
        try {
          sendDataUseCase(
            endpointId,
            nearbyTransferRepository.connectedPeerName.orEmpty(),
            nearbyTransferRepository.connectedPeerDeviceId ?: endpointId,
          )
        } catch (_: IllegalArgumentException) {
          _uiState.update {
            it.copy(transferState = TransferState.Error(TransferError.PayloadTooLarge))
          }
        } catch (_: Exception) {
          _uiState.update {
            it.copy(transferState = TransferState.Error(TransferError.TransferFailed))
          }
        }
      } else {
        _uiState.update {
          it.copy(transferState = TransferState.Error(TransferError.NoConnectedDevice))
        }
      }
    }
  }

  fun retry() {
    if (_uiState.value.isSending) autoSend()
  }

  fun cancelTransfer() {
    nearbyTransferRepository.cancelTransfer()
  }

  fun requestReplace() {
    _uiState.update { it.copy(showReplaceConfirmation = true) }
  }

  fun dismissReplace() {
    _uiState.update { it.copy(showReplaceConfirmation = false) }
  }

  fun confirmReplace() {
    _uiState.update { it.copy(showReplaceConfirmation = false) }
    mergeReceivedData(replace = true)
  }

  fun mergeReceivedData(replace: Boolean = false) {
    val state = _uiState.value.transferState
    if (state !is TransferState.Complete) return

    val peerId =
      nearbyTransferRepository.connectedPeerDeviceId
        ?: nearbyTransferRepository.connectedPeerId
        ?: ""
    val peerName = nearbyTransferRepository.connectedPeerName ?: ""

    viewModelScope.launch {
      _uiState.update { it.copy(isMerging = true) }
      try {
        val result = mergeDataUseCase(state.bundle, peerId, peerName, replace)
        _uiState.update { it.copy(mergeResult = result, isMerging = false) }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isMerging = false, transferState = TransferState.Error(TransferError.MergeFailed))
        }
      }
    }
  }

  fun disconnect() {
    nearbyTransferRepository.disconnect()
  }
}
