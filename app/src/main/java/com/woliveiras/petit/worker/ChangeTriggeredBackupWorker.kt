package com.woliveiras.petit.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupFailureCategory
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.revision.RestorableRevision
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionRepository
import com.woliveiras.petit.domain.usecase.backup.BackupCreationResult
import com.woliveiras.petit.domain.usecase.backup.BackupRevisionCompletion
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import com.woliveiras.petit.domain.usecase.backup.ProviderNeutralBackupExecutionGate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

enum class ChangeTriggeredBackupOutcome {
  SUCCESS,
  FAILURE,
  RETRY,
}

class ChangeTriggeredBackupRunner
@Inject
constructor(
  private val createBackupAction: CreateBackupAction,
  private val attempts: BackupAttemptRepository,
  private val settings: BackupSettingsRepository,
  private val authorization: BackupAuthorizationGateway,
  private val revisions: RestorableRevisionRepository,
  private val completion: BackupRevisionCompletion,
  private val clock: Clock,
) {
  suspend fun run(attemptId: String, target: RestorableRevision): ChangeTriggeredBackupOutcome {
    val revisionState = revisions.state.first()
    if (target <= revisionState.completed || !settings.getSettings().automaticBackupEnabled) {
      return ChangeTriggeredBackupOutcome.SUCCESS
    }
    val startedAt = attempts.getAttempt(attemptId)?.startedAt ?: clock.instant()
    if (authorization.state.value !is BackupAuthorizationState.Authorized) {
      recordFailure(
        attemptId,
        startedAt,
        BackupAttemptStatus.AUTHORIZATION_REQUIRED,
        BackupFailureCategory.AUTHORIZATION_REQUIRED,
      )
      return ChangeTriggeredBackupOutcome.FAILURE
    }
    attempts.upsert(
      BackupAttempt(
        id = attemptId,
        trigger = BackupTrigger.DATA_CHANGE,
        startedAt = startedAt,
        status = BackupAttemptStatus.RUNNING,
      )
    )
    return try {
      val result =
        ProviderNeutralBackupExecutionGate.mutex.withLock {
          if (target <= revisions.state.first().completed) {
            return ChangeTriggeredBackupOutcome.SUCCESS
          }
          createBackupAction.execute(attemptId, BackupTrigger.DATA_CHANGE).also { created ->
            if (created is BackupCreationResult.Success) completion.completed(target)
          }
        }
      when (result) {
        is BackupCreationResult.Success -> {
          attempts.upsert(
            BackupAttempt(
              id = attemptId,
              trigger = BackupTrigger.DATA_CHANGE,
              startedAt = startedAt,
              completedAt = clock.instant(),
              status = BackupAttemptStatus.SUCCEEDED,
              archiveSizeBytes = result.metadata.archiveSizeBytes,
              contentCounts = result.metadata.contentCounts,
            )
          )
          ChangeTriggeredBackupOutcome.SUCCESS
        }
        BackupCreationResult.AuthorizationRequired -> {
          recordFailure(
            attemptId,
            startedAt,
            BackupAttemptStatus.AUTHORIZATION_REQUIRED,
            BackupFailureCategory.AUTHORIZATION_REQUIRED,
          )
          ChangeTriggeredBackupOutcome.FAILURE
        }
        BackupCreationResult.QuotaExceeded -> {
          recordFailure(
            attemptId,
            startedAt,
            BackupAttemptStatus.FAILED,
            BackupFailureCategory.QUOTA_EXCEEDED,
          )
          ChangeTriggeredBackupOutcome.FAILURE
        }
        is BackupCreationResult.RetryableFailure -> {
          recordFailure(
            attemptId,
            startedAt,
            BackupAttemptStatus.RETRYING,
            BackupFailureCategory.RETRYABLE,
            completed = false,
          )
          ChangeTriggeredBackupOutcome.RETRY
        }
        is BackupCreationResult.PermanentFailure -> {
          recordFailure(
            attemptId,
            startedAt,
            BackupAttemptStatus.FAILED,
            BackupFailureCategory.PERMANENT,
          )
          ChangeTriggeredBackupOutcome.FAILURE
        }
      }
    } catch (cancellation: CancellationException) {
      recordFailure(attemptId, startedAt, BackupAttemptStatus.CANCELLED, null)
      throw cancellation
    } catch (_: Exception) {
      recordFailure(
        attemptId,
        startedAt,
        BackupAttemptStatus.FAILED,
        BackupFailureCategory.PERMANENT,
      )
      ChangeTriggeredBackupOutcome.FAILURE
    }
  }

  private suspend fun recordFailure(
    attemptId: String,
    startedAt: java.time.Instant,
    status: BackupAttemptStatus,
    category: BackupFailureCategory?,
    completed: Boolean = true,
  ) {
    attempts.upsert(
      BackupAttempt(
        id = attemptId,
        trigger = BackupTrigger.DATA_CHANGE,
        startedAt = startedAt,
        completedAt = clock.instant().takeIf { completed },
        status = status,
        failureCategory = category,
      )
    )
  }
}

@HiltWorker
class ChangeTriggeredBackupWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted parameters: WorkerParameters,
  private val runner: ChangeTriggeredBackupRunner,
) : CoroutineWorker(context, parameters) {
  override suspend fun doWork(): Result {
    val rawRevision =
      inputData.getLong(WorkManagerChangeTriggeredBackupScheduler.INPUT_TARGET_REVISION, -1)
    if (rawRevision < 0) return Result.failure()
    return when (runner.run(id.toString(), RestorableRevision(rawRevision))) {
      ChangeTriggeredBackupOutcome.SUCCESS -> Result.success()
      ChangeTriggeredBackupOutcome.FAILURE -> Result.failure()
      ChangeTriggeredBackupOutcome.RETRY -> Result.retry()
    }
  }
}
