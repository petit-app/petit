package com.woliveiras.petit.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupSchedulerTest {
  private lateinit var workManager: WorkManager
  private lateinit var scheduler: BackupScheduler

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    WorkManagerTestInitHelper.initializeTestWorkManager(
      context,
      Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
    )
    workManager = WorkManager.getInstance(context)
    scheduler = WorkManagerBackupScheduler(workManager)
  }

  @Test
  fun scheduleUpdatesOneDailyInexactRequestWithApprovedConstraintsAndBackoff() {
    scheduler.schedulePeriodic(BackupNetworkRequirement.UNMETERED)
    val originalId =
      workManager
        .getWorkInfosForUniqueWork(WorkManagerBackupScheduler.PERIODIC_WORK_NAME)
        .get()
        .single()
        .id
    WorkManagerBackupScheduler(workManager).schedulePeriodic(BackupNetworkRequirement.CONNECTED)

    val active =
      workManager
        .getWorkInfosForUniqueWork(WorkManagerBackupScheduler.PERIODIC_WORK_NAME)
        .get()
        .filter { it.state != WorkInfo.State.CANCELLED }
    val connectedRequest =
      WorkManagerBackupScheduler.periodicRequest(BackupNetworkRequirement.CONNECTED)

    assertThat(active).hasSize(1)
    assertThat(active.single().id).isEqualTo(originalId)
    assertThat(active.single().tags).contains(WorkManagerBackupScheduler.PERIODIC_WORK_TAG)
    assertThat(connectedRequest.workSpec.intervalDuration)
      .isEqualTo(WorkManagerBackupScheduler.REPEAT_INTERVAL_HOURS * 60L * 60L * 1000L)
    assertThat(connectedRequest.workSpec.constraints.requiredNetworkType)
      .isEqualTo(NetworkType.CONNECTED)
    assertThat(connectedRequest.workSpec.constraints.requiresBatteryNotLow()).isTrue()
    assertThat(connectedRequest.workSpec.constraints.requiresStorageNotLow()).isTrue()
    assertThat(connectedRequest.workSpec.backoffDelayDuration)
      .isEqualTo(WorkManagerBackupScheduler.BACKOFF_SECONDS * 1000L)
    assertThat(connectedRequest.workSpec.backoffPolicy).isEqualTo(BackoffPolicy.EXPONENTIAL)
  }

  @Test
  fun cancelStopsTheUniquePeriodicRequest() {
    scheduler.schedulePeriodic(BackupNetworkRequirement.UNMETERED)

    scheduler.cancelPeriodic()

    val infos =
      workManager.getWorkInfosForUniqueWork(WorkManagerBackupScheduler.PERIODIC_WORK_NAME).get()
    assertThat(infos.single().state).isEqualTo(WorkInfo.State.CANCELLED)
  }
}
