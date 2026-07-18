package com.woliveiras.petit.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LanSyncSchedulerTest {
  private lateinit var context: Context
  private lateinit var workManager: WorkManager
  private lateinit var scheduler: LanSyncScheduler

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    WorkManagerTestInitHelper.initializeTestWorkManager(
      context,
      Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
    )
    workManager = WorkManager.getInstance(context)
    scheduler = LanSyncScheduler(context)
  }

  @Test
  fun scheduleIsUniquePeriodicConnectedWorkWithExponentialBackoff() {
    scheduler.schedule()
    scheduler.schedule()

    val infos = workManager.getWorkInfosForUniqueWork(LanSyncScheduler.UNIQUE_WORK_NAME).get()

    assertThat(infos).hasSize(1)
    assertThat(infos.single().tags).contains(LanSyncScheduler.WORK_TAG)
    assertThat(infos.single().state).isEqualTo(WorkInfo.State.ENQUEUED)
    assertThat(LanSyncScheduler.REPEAT_INTERVAL_MINUTES).isEqualTo(15L)
    assertThat(LanSyncScheduler.BACKOFF_SECONDS).isEqualTo(30L)
  }

  @Test
  fun cancelStopsTheUniquePeriodicWork() {
    scheduler.schedule()

    scheduler.cancel()

    val infos = workManager.getWorkInfosForUniqueWork(LanSyncScheduler.UNIQUE_WORK_NAME).get()
    assertThat(infos.single().state).isEqualTo(WorkInfo.State.CANCELLED)
  }
}
