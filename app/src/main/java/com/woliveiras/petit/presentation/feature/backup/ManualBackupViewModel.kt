package com.woliveiras.petit.presentation.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupCreationResult
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManualBackupUiState(
  val authorization: BackupAuthorizationState = BackupAuthorizationState.Disconnected,
  val operation: ManualBackupOperation = ManualBackupOperation.Idle,
)

sealed interface ManualBackupOperation {
  data object Idle : ManualBackupOperation

  data object Authorizing : ManualBackupOperation

  data object AuthorizationRequired : ManualBackupOperation

  data class Creating(val progress: BackupProgress?) : ManualBackupOperation

  data class Complete(val metadata: BackupMetadata) : ManualBackupOperation

  data object QuotaExceeded : ManualBackupOperation

  data class RetryableFailure(val message: String) : ManualBackupOperation

  data class PermanentFailure(val message: String) : ManualBackupOperation

  data class Unavailable(val reason: String?) : ManualBackupOperation
}

/** Provider-independent manual backup state holder. UI copy is supplied by localized resources. */
@HiltViewModel
class ManualBackupViewModel
internal constructor(
  private val authorizationGateway: BackupAuthorizationGateway,
  private val createBackup: CreateBackupAction,
  private val backupIdFactory: () -> String,
) : ViewModel() {
  @Inject
  constructor(
    authorizationGateway: BackupAuthorizationGateway,
    createBackup: CreateBackupAction,
  ) : this(authorizationGateway, createBackup, { UUID.randomUUID().toString() })

  private val mutableUiState =
    MutableStateFlow(ManualBackupUiState(authorization = authorizationGateway.state.value))
  val uiState: StateFlow<ManualBackupUiState> = mutableUiState.asStateFlow()

  private var operationJob: Job? = null
  private var pendingBackupId: String? = null

  init {
    viewModelScope.launch {
      authorizationGateway.state.collect { authorization ->
        mutableUiState.update { state ->
          val transitioned = authorization != state.authorization
          val operation =
            if (!transitioned) {
              state.operation
            } else {
              when (authorization) {
                BackupAuthorizationState.Authorizing -> ManualBackupOperation.Authorizing
                BackupAuthorizationState.Disconnected,
                BackupAuthorizationState.AuthorizationRequired ->
                  ManualBackupOperation.AuthorizationRequired
                is BackupAuthorizationState.Unavailable ->
                  ManualBackupOperation.Unavailable(authorization.reason)
                is BackupAuthorizationState.Authorized -> state.operation
              }
            }
          state.copy(authorization = authorization, operation = operation)
        }
      }
    }
  }

  fun authorizeAndBackUp() {
    if (operationJob?.isActive == true) return
    operationJob =
      viewModelScope.launch {
        mutableUiState.update { it.copy(operation = ManualBackupOperation.Authorizing) }
        when (val result = authorizationGateway.authorize()) {
          BackupAuthorizationResult.Authorized -> executeBackup()
          BackupAuthorizationResult.Cancelled ->
            mutableUiState.update {
              it.copy(operation = ManualBackupOperation.AuthorizationRequired)
            }
          is BackupAuthorizationResult.Unavailable ->
            mutableUiState.update {
              it.copy(operation = ManualBackupOperation.Unavailable(result.reason))
            }
        }
      }
  }

  fun backUpNow() {
    if (operationJob?.isActive == true) return
    operationJob = viewModelScope.launch { executeBackup() }
  }

  fun cancel() {
    operationJob?.cancel()
    operationJob = null
    mutableUiState.update { it.copy(operation = ManualBackupOperation.Idle) }
  }

  private suspend fun executeBackup() {
    val backupId = pendingBackupId ?: backupIdFactory().also { pendingBackupId = it }
    mutableUiState.update { it.copy(operation = ManualBackupOperation.Creating(progress = null)) }
    try {
      when (
        val result =
          createBackup.execute(backupId, BackupTrigger.MANUAL) { progress ->
            mutableUiState.update {
              it.copy(operation = ManualBackupOperation.Creating(progress = progress))
            }
          }
      ) {
        is BackupCreationResult.Success -> {
          pendingBackupId = null
          mutableUiState.update {
            it.copy(operation = ManualBackupOperation.Complete(result.metadata))
          }
        }
        BackupCreationResult.AuthorizationRequired ->
          mutableUiState.update { it.copy(operation = ManualBackupOperation.AuthorizationRequired) }
        BackupCreationResult.QuotaExceeded ->
          mutableUiState.update { it.copy(operation = ManualBackupOperation.QuotaExceeded) }
        is BackupCreationResult.RetryableFailure ->
          mutableUiState.update {
            it.copy(operation = ManualBackupOperation.RetryableFailure(result.message))
          }
        is BackupCreationResult.PermanentFailure -> {
          pendingBackupId = null
          mutableUiState.update {
            it.copy(operation = ManualBackupOperation.PermanentFailure(result.message))
          }
        }
      }
    } catch (cancellation: CancellationException) {
      mutableUiState.update { it.copy(operation = ManualBackupOperation.Idle) }
      throw cancellation
    } catch (_: Exception) {
      pendingBackupId = null
      mutableUiState.update {
        it.copy(operation = ManualBackupOperation.PermanentFailure("Backup could not be completed"))
      }
    }
  }
}
