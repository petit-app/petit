package com.woliveiras.petit.domain.usecase.backup

import com.woliveiras.petit.data.repository.BackupSettings
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.worker.BackupScheduler
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class BackupSettingsCoordinator
@Inject
constructor(
  private val repository: BackupSettingsRepository,
  private val scheduler: BackupScheduler,
) {
  private val updateMutex = Mutex()

  suspend fun setAutomaticBackupEnabled(enabled: Boolean) =
    updateMutex.withLock {
      updateAtomically { previous ->
        repository.updateAutomaticBackupEnabled(enabled)
        previous.copy(automaticBackupEnabled = enabled)
      }
    }

  suspend fun setNetworkRequirement(requirement: BackupNetworkRequirement) =
    updateMutex.withLock {
      updateAtomically { previous ->
        repository.updateNetworkRequirement(requirement)
        previous.copy(networkRequirement = requirement)
      }
    }

  suspend fun setNotifyAfterSuccess(enabled: Boolean) {
    repository.updateNotifyAfterSuccess(enabled)
  }

  private suspend fun updateAtomically(change: suspend (BackupSettings) -> BackupSettings) {
    val previous = repository.getSettings()
    try {
      reconcileSchedule(change(previous))
    } catch (cancellation: CancellationException) {
      restore(previous)
      throw cancellation
    } catch (failure: Exception) {
      restore(previous)
      throw failure
    }
  }

  private suspend fun restore(settings: BackupSettings) =
    withContext(NonCancellable) {
      repository.updateAutomaticBackupEnabled(settings.automaticBackupEnabled)
      repository.updateNetworkRequirement(settings.networkRequirement)
      reconcileSchedule(settings)
    }

  private fun reconcileSchedule(settings: BackupSettings) {
    if (settings.automaticBackupEnabled) {
      scheduler.schedulePeriodic(settings.networkRequirement)
    } else {
      scheduler.cancelPeriodic()
    }
  }
}

@Singleton
class BackupConnectionCoordinator
@Inject
constructor(
  private val authorizationGateway: BackupAuthorizationGateway,
  private val settingsCoordinator: BackupSettingsCoordinator,
) : BackupConnectionController {
  override suspend fun refresh(): BackupAuthorizationState = authorizationGateway.refresh()

  override suspend fun authorize(): BackupAuthorizationResult = authorizationGateway.authorize()

  /** Revokes authorization and periodic work without invoking any remote deletion operation. */
  override suspend fun disconnect() {
    settingsCoordinator.setAutomaticBackupEnabled(false)
    authorizationGateway.disconnect()
  }
}

interface BackupConnectionController {
  suspend fun refresh(): BackupAuthorizationState

  suspend fun authorize(): BackupAuthorizationResult

  suspend fun disconnect()
}
