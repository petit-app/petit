package com.woliveiras.petit.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.domain.backup.revision.RestorableRevision
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChangeTriggeredBackupSchedulerTest {
  private lateinit var workManager: WorkManager
  private lateinit var scheduler: WorkManagerChangeTriggeredBackupScheduler

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    WorkManagerTestInitHelper.initializeTestWorkManager(
      context,
      Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
    )
    workManager = WorkManager.getInstance(context)
    scheduler = WorkManagerChangeTriggeredBackupScheduler(workManager, TestWorker::class.java)
  }

  @Test
  fun rapidChangesReplaceOneFiveMinuteRequestWithLatestRevisionAndConstraints() {
    scheduler.debounce(RestorableRevision(1), BackupNetworkRequirement.UNMETERED)
    scheduler.debounce(RestorableRevision(2), BackupNetworkRequirement.CONNECTED)

    val active =
      workManager
        .getWorkInfosForUniqueWork(WorkManagerChangeTriggeredBackupScheduler.UNIQUE_WORK_NAME)
        .get()
        .filter { it.state != WorkInfo.State.CANCELLED }
    val latest =
      scheduler.request(RestorableRevision(2), BackupNetworkRequirement.CONNECTED).workSpec

    assertThat(active).hasSize(1)
    assertThat(active.single().tags).contains(WorkManagerChangeTriggeredBackupScheduler.WORK_TAG)
    assertThat(latest.initialDelay)
      .isEqualTo(WorkManagerChangeTriggeredBackupScheduler.INITIAL_DELAY_MINUTES * 60_000L)
    assertThat(
        latest.input.getLong(WorkManagerChangeTriggeredBackupScheduler.INPUT_TARGET_REVISION, -1)
      )
      .isEqualTo(2L)
    assertThat(latest.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
    assertThat(latest.constraints.requiresBatteryNotLow()).isTrue()
    assertThat(latest.constraints.requiresStorageNotLow()).isTrue()
    assertThat(latest.backoffDelayDuration)
      .isEqualTo(WorkManagerChangeTriggeredBackupScheduler.BACKOFF_SECONDS * 1_000L)
    assertThat(latest.backoffPolicy).isEqualTo(BackoffPolicy.EXPONENTIAL)
  }

  @Test
  fun cancelStopsTheUniquePendingRequest() {
    scheduler.debounce(RestorableRevision(1), BackupNetworkRequirement.UNMETERED)

    scheduler.cancel()

    val infos =
      workManager
        .getWorkInfosForUniqueWork(WorkManagerChangeTriggeredBackupScheduler.UNIQUE_WORK_NAME)
        .get()
    assertThat(infos.single().state).isEqualTo(WorkInfo.State.CANCELLED)
  }

  class TestWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Result.success()
  }
}
