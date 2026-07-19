package com.woliveiras.petit.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Durable provider-neutral entry point. Interactive authorization is never launched here. */
@HiltWorker
class AutomaticBackupWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted parameters: WorkerParameters,
  private val runner: AutomaticBackupRunner,
  private val operationIds: PeriodicBackupOperationIdStore,
) : CoroutineWorker(context, parameters) {
  override suspend fun doWork(): Result {
    val operationId = operationIds.operationIdFor(id.toString(), runAttemptCount)
    return when (runner.run(operationId)) {
      AutomaticBackupOutcome.SUCCESS -> Result.success()
      AutomaticBackupOutcome.FAILURE -> Result.failure()
      AutomaticBackupOutcome.RETRY -> Result.retry()
    }
  }
}
