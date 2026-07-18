package com.woliveiras.petit.worker

import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupFailureCategory
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupCreationResult
import com.woliveiras.petit.domain.usecase.backup.BackupRevisionCompletion
import com.woliveiras.petit.domain.usecase.backup.BackupTriggerCoordinator
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import com.woliveiras.petit.domain.usecase.backup.NoOpBackupRevisionCompletion
import com.woliveiras.petit.domain.usecase.backup.ProviderNeutralBackupExecutionGate
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.sync.withLock

enum class AutomaticBackupOutcome {
  SUCCESS,
  FAILURE,
  RETRY,
}

class AutomaticBackupRunner
internal constructor(
  private val createBackupAction: CreateBackupAction,
  private val attemptRepository: BackupAttemptRepository,
  private val clock: Clock,
  private val notificationDispatcher: BackupNotificationDispatcher,
  private val revisionCompletion: BackupRevisionCompletion,
) {
  @Inject
  constructor(
    createBackupAction: CreateBackupAction,
    attemptRepository: BackupAttemptRepository,
    clock: Clock,
    notificationDispatcher: AndroidBackupNotificationDispatcher,
    revisionCompletion: BackupTriggerCoordinator,
  ) : this(
    createBackupAction,
    attemptRepository,
    clock,
    notificationDispatcher as BackupNotificationDispatcher,
    revisionCompletion,
  )

  constructor(
    createBackupAction: CreateBackupAction,
    attemptRepository: BackupAttemptRepository,
    clock: Clock,
  ) : this(
    createBackupAction,
    attemptRepository,
    clock,
    NoOpBackupNotificationDispatcher,
    NoOpBackupRevisionCompletion,
  )

  internal constructor(
    createBackupAction: CreateBackupAction,
    attemptRepository: BackupAttemptRepository,
    clock: Clock,
    notificationDispatcher: BackupNotificationDispatcher,
  ) : this(
    createBackupAction,
    attemptRepository,
    clock,
    notificationDispatcher,
    NoOpBackupRevisionCompletion,
  )

  suspend fun run(attemptId: String): AutomaticBackupOutcome {
    val targetRevision = revisionCompletion.capture()
    val existing = attemptRepository.getAttempt(attemptId)
    val startedAt = existing?.startedAt ?: clock.instant()
    attemptRepository.upsert(
      BackupAttempt(
        id = attemptId,
        trigger = BackupTrigger.AUTOMATIC,
        startedAt = startedAt,
        status = BackupAttemptStatus.RUNNING,
      )
    )

    return try {
      val result =
        ProviderNeutralBackupExecutionGate.mutex.withLock {
          createBackupAction
            .execute(backupId = attemptId, trigger = BackupTrigger.AUTOMATIC)
            .also { created ->
              if (created is BackupCreationResult.Success) {
                revisionCompletion.completed(targetRevision)
              }
            }
        }
      when (result) {
        is BackupCreationResult.Success -> {
          record(
            attemptId = attemptId,
            startedAt = startedAt,
            status = BackupAttemptStatus.SUCCEEDED,
            completedAt = clock.instant(),
            archiveSizeBytes = result.metadata.archiveSizeBytes,
            contentCounts = result.metadata.contentCounts,
          )
          AutomaticBackupOutcome.SUCCESS
        }
        BackupCreationResult.AuthorizationRequired -> {
          record(
            attemptId,
            startedAt,
            BackupAttemptStatus.AUTHORIZATION_REQUIRED,
            clock.instant(),
            failureCategory = BackupFailureCategory.AUTHORIZATION_REQUIRED,
          )
          AutomaticBackupOutcome.FAILURE
        }
        BackupCreationResult.QuotaExceeded -> {
          record(
            attemptId,
            startedAt,
            BackupAttemptStatus.FAILED,
            clock.instant(),
            failureCategory = BackupFailureCategory.QUOTA_EXCEEDED,
          )
          AutomaticBackupOutcome.FAILURE
        }
        is BackupCreationResult.RetryableFailure -> {
          record(
            attemptId,
            startedAt,
            BackupAttemptStatus.RETRYING,
            failureCategory = BackupFailureCategory.RETRYABLE,
          )
          AutomaticBackupOutcome.RETRY
        }
        is BackupCreationResult.PermanentFailure -> {
          record(
            attemptId,
            startedAt,
            BackupAttemptStatus.FAILED,
            clock.instant(),
            failureCategory = BackupFailureCategory.PERMANENT,
          )
          AutomaticBackupOutcome.FAILURE
        }
      }
    } catch (cancellation: CancellationException) {
      record(attemptId, startedAt, BackupAttemptStatus.CANCELLED, clock.instant())
      throw cancellation
    } catch (_: Exception) {
      record(
        attemptId,
        startedAt,
        BackupAttemptStatus.FAILED,
        clock.instant(),
        failureCategory = BackupFailureCategory.PERMANENT,
      )
      AutomaticBackupOutcome.FAILURE
    }
  }

  private suspend fun record(
    attemptId: String,
    startedAt: Instant,
    status: BackupAttemptStatus,
    completedAt: Instant? = null,
    archiveSizeBytes: Long? = null,
    contentCounts: com.woliveiras.petit.domain.backup.BackupContentCounts? = null,
    failureCategory: BackupFailureCategory? = null,
  ) {
    val attempt =
      BackupAttempt(
        id = attemptId,
        trigger = BackupTrigger.AUTOMATIC,
        startedAt = startedAt,
        completedAt = completedAt,
        status = status,
        archiveSizeBytes = archiveSizeBytes,
        contentCounts = contentCounts,
        failureCategory = failureCategory,
      )
    attemptRepository.upsert(attempt)
    try {
      notificationDispatcher.dispatch(attempt)
    } catch (_: Exception) {
      // Notification delivery must never change a completed backup outcome.
    }
  }
}
