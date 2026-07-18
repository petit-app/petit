package com.woliveiras.petit.domain.usecase.backup

import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.revision.RestorableRevision
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionRepository
import com.woliveiras.petit.worker.ChangeTriggeredBackupScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes provider operations so manual, periodic, and triggered uploads cannot overlap. */
object ProviderNeutralBackupExecutionGate {
  val mutex = Mutex()
}

interface BackupRevisionCompletion {
  suspend fun capture(): RestorableRevision

  suspend fun completed(revision: RestorableRevision)
}

object NoOpBackupRevisionCompletion : BackupRevisionCompletion {
  override suspend fun capture() = RestorableRevision(0)

  override suspend fun completed(revision: RestorableRevision) = Unit
}

@Singleton
class BackupTriggerCoordinator
@Inject
constructor(
  private val revisions: RestorableRevisionRepository,
  private val settingsRepository: BackupSettingsRepository,
  private val authorizationGateway: BackupAuthorizationGateway,
  private val scheduler: ChangeTriggeredBackupScheduler,
) : BackupRevisionCompletion {
  private val completionMutex = Mutex()
  private var observerJob: Job? = null

  fun start(scope: CoroutineScope) {
    if (observerJob?.isActive == true) return
    observerJob =
      scope.launch {
        var previousCurrent: RestorableRevision? = null
        var previousNetwork: com.woliveiras.petit.domain.backup.BackupNetworkRequirement? = null
        combine(revisions.state, settingsRepository.settings, authorizationGateway.state) {
            revisionState,
            settings,
            authorization ->
            Triple(revisionState, settings, authorization)
          }
          .collect { (revisionState, settings, authorization) ->
            val eligible =
              settings.automaticBackupEnabled &&
                authorization is BackupAuthorizationState.Authorized &&
                revisionState.current > revisionState.completed
            if (!eligible) {
              scheduler.cancel()
            } else if (previousCurrent == null) {
              scheduler.ensureScheduled(revisionState.current, settings.networkRequirement)
            } else {
              if (
                revisionState.current != previousCurrent ||
                  settings.networkRequirement != previousNetwork
              ) {
                scheduler.debounce(revisionState.current, settings.networkRequirement)
              } else {
                scheduler.ensureScheduled(revisionState.current, settings.networkRequirement)
              }
            }
            previousCurrent = revisionState.current
            previousNetwork = settings.networkRequirement
          }
      }
  }

  override suspend fun capture(): RestorableRevision = revisions.stateValue().current

  override suspend fun completed(revision: RestorableRevision) {
    completionMutex.withLock {
      val state = revisions.markCompleted(revision)
      if (state.current <= state.completed) scheduler.cancel()
    }
  }

  private suspend fun RestorableRevisionRepository.stateValue() = state.first()
}
