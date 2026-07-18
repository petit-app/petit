package com.woliveiras.petit.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.domain.backup.revision.RestorableRevision
import java.util.concurrent.TimeUnit

interface ChangeTriggeredBackupScheduler {
  fun ensureScheduled(revision: RestorableRevision, networkRequirement: BackupNetworkRequirement)

  fun debounce(revision: RestorableRevision, networkRequirement: BackupNetworkRequirement)

  fun cancel()
}

class WorkManagerChangeTriggeredBackupScheduler(
  private val workManager: WorkManager,
  private val workerClass: Class<out ListenableWorker>,
) : ChangeTriggeredBackupScheduler {
  override fun ensureScheduled(
    revision: RestorableRevision,
    networkRequirement: BackupNetworkRequirement,
  ) {
    workManager.enqueueUniqueWork(
      UNIQUE_WORK_NAME,
      ExistingWorkPolicy.KEEP,
      request(revision, networkRequirement),
    )
  }

  override fun debounce(
    revision: RestorableRevision,
    networkRequirement: BackupNetworkRequirement,
  ) {
    workManager.enqueueUniqueWork(
      UNIQUE_WORK_NAME,
      ExistingWorkPolicy.REPLACE,
      request(revision, networkRequirement),
    )
  }

  override fun cancel() {
    workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
  }

  internal fun request(
    revision: RestorableRevision,
    networkRequirement: BackupNetworkRequirement,
  ): OneTimeWorkRequest =
    OneTimeWorkRequest.Builder(workerClass)
      .setInputData(Data.Builder().putLong(INPUT_TARGET_REVISION, revision.value).build())
      .setInitialDelay(INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
      .setConstraints(
        Constraints.Builder()
          .setRequiredNetworkType(
            when (networkRequirement) {
              BackupNetworkRequirement.CONNECTED -> NetworkType.CONNECTED
              BackupNetworkRequirement.UNMETERED -> NetworkType.UNMETERED
            }
          )
          .setRequiresBatteryNotLow(true)
          .setRequiresStorageNotLow(true)
          .build()
      )
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
      .addTag(WORK_TAG)
      .build()

  companion object {
    const val UNIQUE_WORK_NAME = "petit_change_triggered_backup"
    const val WORK_TAG = "petit_change_triggered_backup"
    const val INITIAL_DELAY_MINUTES = 5L
    const val BACKOFF_SECONDS = 30L
    const val INPUT_TARGET_REVISION = "target_revision"
  }
}
