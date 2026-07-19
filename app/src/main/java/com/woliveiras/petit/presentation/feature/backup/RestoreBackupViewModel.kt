package com.woliveiras.petit.presentation.feature.backup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.restore.RestoreBackupAction
import com.woliveiras.petit.domain.backup.restore.RestoreBackupRequest
import com.woliveiras.petit.domain.backup.restore.RestoreBackupResult
import com.woliveiras.petit.domain.backup.restore.RestoreMode
import com.woliveiras.petit.domain.usecase.backup.BackupConnectionController
import com.woliveiras.petit.domain.usecase.backup.BackupConnectionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestoreBackupUiState(
  val mode: RestoreMode = RestoreMode.MERGE,
  val applyBackupPreferences: Boolean = false,
  val showReplaceConfirmation: Boolean = false,
  val operation: RestoreOperation = RestoreOperation.Idle,
  val lastProgress: BackupProgress? = null,
)

sealed interface RestoreOperation {
  data object Idle : RestoreOperation

  data class Restoring(val progress: BackupProgress?) : RestoreOperation

  data class Success(val result: RestoreBackupResult) : RestoreOperation

  data object AuthorizationRequired : RestoreOperation

  data object InvalidBackup : RestoreOperation

  data object Failed : RestoreOperation
}

@HiltViewModel
class RestoreBackupViewModel
internal constructor(
  private val remoteId: String,
  private val restoreBackup: RestoreBackupAction,
  private val connectionController: BackupConnectionController,
) : ViewModel() {
  @Inject
  constructor(
    savedStateHandle: SavedStateHandle,
    restoreBackup: RestoreBackupAction,
    connectionCoordinator: BackupConnectionCoordinator,
  ) : this(
    requireNotNull(savedStateHandle.get<String>(REMOTE_ID_ARGUMENT)),
    restoreBackup,
    connectionCoordinator as BackupConnectionController,
  )

  internal constructor(
    remoteId: String,
    restoreBackup: RestoreBackupAction,
  ) : this(remoteId, restoreBackup, AlwaysAuthorizedBackupConnectionController)

  private val mutableUiState = MutableStateFlow(RestoreBackupUiState())
  val uiState: StateFlow<RestoreBackupUiState> = mutableUiState.asStateFlow()

  fun selectMode(mode: RestoreMode) {
    if (mutableUiState.value.operation is RestoreOperation.Restoring) return
    mutableUiState.update {
      it.copy(
        mode = mode,
        applyBackupPreferences = if (mode == RestoreMode.REPLACE) true else false,
        showReplaceConfirmation = false,
      )
    }
  }

  fun setApplyBackupPreferences(enabled: Boolean) {
    if (mutableUiState.value.mode == RestoreMode.MERGE) {
      mutableUiState.update { it.copy(applyBackupPreferences = enabled) }
    }
  }

  fun requestRestore() {
    if (mutableUiState.value.mode == RestoreMode.REPLACE) {
      mutableUiState.update { it.copy(showReplaceConfirmation = true) }
    } else {
      executeRestore()
    }
  }

  fun dismissReplaceConfirmation() {
    mutableUiState.update { it.copy(showReplaceConfirmation = false) }
  }

  fun confirmReplace() {
    mutableUiState.update { it.copy(showReplaceConfirmation = false) }
    executeRestore()
  }

  private fun executeRestore() {
    if (mutableUiState.value.operation is RestoreOperation.Restoring) return
    val state = mutableUiState.value
    mutableUiState.update { it.copy(operation = RestoreOperation.Restoring(null)) }
    viewModelScope.launch {
      try {
        if (!ensureAuthorized()) {
          mutableUiState.update { it.copy(operation = RestoreOperation.AuthorizationRequired) }
          return@launch
        }
        var lastBytes = -1L
        val result =
          restoreBackup.execute(
            RestoreBackupRequest(
              remoteId = remoteId,
              mode = state.mode,
              applyBackupPreferences = state.applyBackupPreferences,
            )
          ) { progress ->
            if (progress.bytesTransferred >= lastBytes) {
              lastBytes = progress.bytesTransferred
              mutableUiState.update {
                it.copy(operation = RestoreOperation.Restoring(progress), lastProgress = progress)
              }
            }
          }
        mutableUiState.update { it.copy(operation = RestoreOperation.Success(result)) }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: BackupProviderException.AuthorizationRequired) {
        mutableUiState.update { it.copy(operation = RestoreOperation.AuthorizationRequired) }
      } catch (_: IllegalArgumentException) {
        mutableUiState.update { it.copy(operation = RestoreOperation.InvalidBackup) }
      } catch (_: Exception) {
        mutableUiState.update { it.copy(operation = RestoreOperation.Failed) }
      }
    }
  }

  private suspend fun ensureAuthorized(): Boolean {
    if (connectionController.refresh() is BackupAuthorizationState.Authorized) return true
    return connectionController.authorize() is BackupAuthorizationResult.Authorized
  }

  companion object {
    const val REMOTE_ID_ARGUMENT = "remoteId"
  }
}

private data object AlwaysAuthorizedBackupConnectionController : BackupConnectionController {
  override suspend fun refresh(): BackupAuthorizationState = BackupAuthorizationState.Authorized()

  override suspend fun authorize(): BackupAuthorizationResult = BackupAuthorizationResult.Authorized

  override suspend fun disconnect() = Unit
}
