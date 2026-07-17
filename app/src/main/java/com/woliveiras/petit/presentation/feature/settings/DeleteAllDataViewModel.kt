package com.woliveiras.petit.presentation.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.usecase.DeleteAllDataAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeleteAllDataUiState(
  val confirmText: String = "",
  val isDeleting: Boolean = false,
  val isDeleted: Boolean = false,
  val errorMessage: String? = null,
)

sealed class DeleteAllDataEvent {
  data object Success : DeleteAllDataEvent()

  data class Error(val message: String) : DeleteAllDataEvent()
}

@HiltViewModel
class DeleteAllDataViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val deleteAllDataUseCase: DeleteAllDataAction,
) : ViewModel() {

  private val _uiState = MutableStateFlow(DeleteAllDataUiState())
  val uiState: StateFlow<DeleteAllDataUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<DeleteAllDataEvent>()
  val events: SharedFlow<DeleteAllDataEvent> = _events.asSharedFlow()

  fun updateConfirmText(text: String) {
    _uiState.update { it.copy(confirmText = text, errorMessage = null) }
  }

  fun deleteAllData(confirmWord: String) {
    if (
      _uiState.value.confirmText != confirmWord ||
        _uiState.value.isDeleting ||
        _uiState.value.isDeleted
    ) {
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
      deleteAllDataUseCase
        .execute()
        .onSuccess {
          _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
          _events.emit(DeleteAllDataEvent.Success)
        }
        .onFailure { error ->
          val message = error.message ?: context.getString(R.string.error_unknown)
          _uiState.update { it.copy(isDeleting = false, errorMessage = message) }
          _events.emit(DeleteAllDataEvent.Error(message))
        }
    }
  }
}
