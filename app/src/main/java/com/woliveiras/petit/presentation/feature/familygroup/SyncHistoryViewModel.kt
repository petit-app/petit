package com.woliveiras.petit.presentation.feature.familygroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.model.SyncLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncHistoryUiState(
  val logs: List<SyncLog> = emptyList(),
  val isLoading: Boolean = true,
  val hasError: Boolean = false,
)

@HiltViewModel
class SyncHistoryViewModel @Inject constructor(familyGroupRepository: FamilyGroupRepository) :
  ViewModel() {
  private val _uiState = MutableStateFlow(SyncHistoryUiState())
  val uiState: StateFlow<SyncHistoryUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      familyGroupRepository
        .getSyncLogs()
        .catch { _uiState.update { it.copy(isLoading = false, hasError = true) } }
        .collect { logs -> _uiState.value = SyncHistoryUiState(logs = logs, isLoading = false) }
    }
  }
}
