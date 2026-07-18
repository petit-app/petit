package com.woliveiras.petit.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.woliveiras.petit.data.lan.LanSyncCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class LanSyncWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted parameters: WorkerParameters,
  private val coordinator: LanSyncCoordinator,
) : CoroutineWorker(context, parameters) {
  override suspend fun doWork(): Result =
    if (coordinator.attemptNow()) Result.success() else Result.retry()
}

@Singleton
class LanSyncScheduler @Inject constructor(@ApplicationContext context: Context) {
  private val workManager = WorkManager.getInstance(context)

  fun schedule() {
    workManager.enqueueUniquePeriodicWork(
      UNIQUE_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      periodicRequest(),
    )
  }

  fun cancel() {
    workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
  }

  internal fun periodicRequest(): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<LanSyncWorker>(REPEAT_INTERVAL_MINUTES, TimeUnit.MINUTES)
      .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(BACKOFF_SECONDS))
      .addTag(WORK_TAG)
      .build()

  companion object {
    const val UNIQUE_WORK_NAME = "petit_lan_sync"
    const val WORK_TAG = "lan_sync"
    const val REPEAT_INTERVAL_MINUTES = 15L
    const val BACKOFF_SECONDS = 30L
  }
}
