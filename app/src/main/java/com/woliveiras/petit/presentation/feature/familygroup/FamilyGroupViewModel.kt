package com.woliveiras.petit.presentation.feature.familygroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.SyncLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FamilyGroupUiState(
  val familyGroupInfo: FamilyGroupInfo? = null,
  val isSyncEnabled: Boolean = false,
  val latestSyncLog: SyncLog? = null,
  val isLoading: Boolean = true,
  val operationFailed: Boolean = false,
  val hasLeftGroup: Boolean = false,
)

@HiltViewModel
class FamilyGroupViewModel
@Inject
constructor(private val familyGroupRepository: FamilyGroupRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(FamilyGroupUiState())
  val uiState: StateFlow<FamilyGroupUiState> = _uiState.asStateFlow()

  init {
    observeFamilyGroup()
    observeSyncEnabled()
    loadLatestSync()
  }

  private fun observeFamilyGroup() {
    viewModelScope.launch {
      familyGroupRepository.familyGroupInfo.collect { info ->
        _uiState.update { it.copy(familyGroupInfo = info, isLoading = false) }
      }
    }
  }

  private fun observeSyncEnabled() {
    viewModelScope.launch {
      familyGroupRepository.isSyncEnabled.collect { enabled ->
        _uiState.update { it.copy(isSyncEnabled = enabled) }
      }
    }
  }

  private fun loadLatestSync() {
    viewModelScope.launch {
      val latest = familyGroupRepository.getLatestSyncLog()
      _uiState.update { it.copy(latestSyncLog = latest) }
    }
  }

  fun setSyncEnabled(enabled: Boolean) {
    viewModelScope.launch { familyGroupRepository.setSyncEnabled(enabled) }
  }

  fun removeMember(memberId: String) {
    viewModelScope.launch {
      runCatching { familyGroupRepository.removeMember(memberId) }
        .onFailure { _uiState.update { it.copy(operationFailed = true) } }
    }
  }

  fun renameLocalDevice(deviceName: String) {
    viewModelScope.launch {
      runCatching { familyGroupRepository.renameLocalDevice(deviceName) }
        .onFailure { _uiState.update { it.copy(operationFailed = true) } }
    }
  }

  fun leaveGroup() {
    viewModelScope.launch {
      runCatching { familyGroupRepository.leaveFamilyGroup() }
        .onSuccess { _uiState.update { it.copy(hasLeftGroup = true) } }
        .onFailure { _uiState.update { it.copy(operationFailed = true) } }
    }
  }
}
