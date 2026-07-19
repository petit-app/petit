package com.woliveiras.petit.presentation.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupSettings
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.domain.usecase.backup.BackupConnectionCoordinator
import com.woliveiras.petit.domain.usecase.backup.BackupSettingsCoordinator
import com.woliveiras.petit.domain.usecase.backup.ManualBackupHistoryRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BackupSettingsError {
  SETTINGS_UPDATE_FAILED,
  AUTHORIZATION_UNAVAILABLE,
  DISCONNECT_FAILED,
}

data class BackupSettingsUiState(
  val settings: BackupSettings = BackupSettings(),
  val authorization: BackupAuthorizationState = BackupAuthorizationState.Disconnected,
  val attempts: List<BackupAttempt> = emptyList(),
  val manualAttemptStatus: BackupAttemptStatus? = null,
  val isUpdatingSettings: Boolean = false,
  val error: BackupSettingsError? = null,
)

@HiltViewModel
class BackupSettingsViewModel
internal constructor(
  private val settingsRepository: BackupSettingsRepository,
  private val attemptRepository: BackupAttemptRepository,
  private val settingsCoordinator: BackupSettingsCoordinator,
  private val connectionCoordinator: BackupConnectionCoordinator,
  private val manualRunner: ManualBackupHistoryRunner,
  private val authorizationState: StateFlow<BackupAuthorizationState>,
  private val backupIdFactory: () -> String,
) : ViewModel() {
  @Inject
  constructor(
    settingsRepository: BackupSettingsRepository,
    attemptRepository: BackupAttemptRepository,
    settingsCoordinator: BackupSettingsCoordinator,
    connectionCoordinator: BackupConnectionCoordinator,
    manualRunner: ManualBackupHistoryRunner,
    authorizationGateway: com.woliveiras.petit.domain.backup.BackupAuthorizationGateway,
  ) : this(
    settingsRepository,
    attemptRepository,
    settingsCoordinator,
    connectionCoordinator,
    manualRunner,
    authorizationGateway.state,
    { UUID.randomUUID().toString() },
  )

  private val mutableUiState = MutableStateFlow(BackupSettingsUiState())
  val uiState: StateFlow<BackupSettingsUiState> = mutableUiState.asStateFlow()
  private var manualJob: Job? = null

  init {
    viewModelScope.launch { connectionCoordinator.refresh() }
    viewModelScope.launch {
      combine(settingsRepository.settings, authorizationState, attemptRepository.attempts) {
          settings,
          authorization,
          attempts ->
          Triple(settings, authorization, attempts)
        }
        .collect { (settings, authorization, attempts) ->
          mutableUiState.update {
            it.copy(
              settings = settings,
              authorization = authorization,
              attempts = attempts.take(HISTORY_PREVIEW_SIZE),
            )
          }
        }
    }
  }

  fun setAutomaticBackupEnabled(enabled: Boolean) = updateSetting {
    settingsCoordinator.setAutomaticBackupEnabled(enabled)
  }

  fun setNetworkRequirement(requirement: BackupNetworkRequirement) = updateSetting {
    settingsCoordinator.setNetworkRequirement(requirement)
  }

  fun setNotifyAfterSuccess(enabled: Boolean) = updateSetting {
    settingsCoordinator.setNotifyAfterSuccess(enabled)
  }

  fun backUpNow() {
    if (manualJob?.isActive == true) return
    manualJob =
      viewModelScope.launch {
        mutableUiState.update { it.copy(manualAttemptStatus = null, error = null) }
        if (authorizationState.value !is BackupAuthorizationState.Authorized) {
          when (connectionCoordinator.authorize()) {
            BackupAuthorizationResult.Authorized -> Unit
            BackupAuthorizationResult.Cancelled -> {
              mutableUiState.update {
                it.copy(manualAttemptStatus = BackupAttemptStatus.AUTHORIZATION_REQUIRED)
              }
              return@launch
            }
            is BackupAuthorizationResult.Unavailable -> {
              mutableUiState.update {
                it.copy(error = BackupSettingsError.AUTHORIZATION_UNAVAILABLE)
              }
              return@launch
            }
          }
        }
        val status = manualRunner.run(backupIdFactory())
        mutableUiState.update { it.copy(manualAttemptStatus = status) }
      }
  }

  fun authorize() {
    viewModelScope.launch {
      if (connectionCoordinator.authorize() is BackupAuthorizationResult.Unavailable) {
        mutableUiState.update { it.copy(error = BackupSettingsError.AUTHORIZATION_UNAVAILABLE) }
      }
    }
  }

  fun disconnect() {
    viewModelScope.launch {
      try {
        connectionCoordinator.disconnect()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        mutableUiState.update { it.copy(error = BackupSettingsError.DISCONNECT_FAILED) }
      }
    }
  }

  fun clearError() {
    mutableUiState.update { it.copy(error = null) }
  }

  private fun updateSetting(update: suspend () -> Unit) {
    if (mutableUiState.value.isUpdatingSettings) return
    mutableUiState.update { it.copy(isUpdatingSettings = true, error = null) }
    viewModelScope.launch {
      try {
        update()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        mutableUiState.update { it.copy(error = BackupSettingsError.SETTINGS_UPDATE_FAILED) }
      } finally {
        mutableUiState.update { it.copy(isUpdatingSettings = false) }
      }
    }
  }

  private companion object {
    const val HISTORY_PREVIEW_SIZE = 3
  }
}
