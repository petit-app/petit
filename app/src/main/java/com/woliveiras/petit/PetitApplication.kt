package com.woliveiras.petit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.backup.restore.RestoreBackupUseCase
import com.woliveiras.petit.worker.LanSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class PetitApplication : Application(), Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory
  @Inject lateinit var familyGroupRepository: FamilyGroupRepository
  @Inject lateinit var lanSyncScheduler: LanSyncScheduler
  @Inject lateinit var restoreBackupUseCase: RestoreBackupUseCase

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      runCatching { restoreBackupUseCase.recoverInterruptedRestore() }
    }
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      familyGroupRepository.isSyncEnabled.collect { shouldSchedule ->
        if (shouldSchedule) lanSyncScheduler.schedule() else lanSyncScheduler.cancel()
      }
    }
  }

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

  private fun createNotificationChannel() {
    val channel =
      NotificationChannel(
          TASKS_CHANNEL_ID,
          getString(R.string.notification_channel_reminders),
          NotificationManager.IMPORTANCE_HIGH,
        )
        .apply {
          description = getString(R.string.notification_channel_reminders_description)
          enableVibration(true)
        }

    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
  }

  companion object {
    const val TASKS_CHANNEL_ID = "petit_reminders"
  }
}
