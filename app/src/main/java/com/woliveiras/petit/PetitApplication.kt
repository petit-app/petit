package com.woliveiras.petit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.backup.restore.RestoreBackupUseCase
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.usecase.backup.BackupTriggerCoordinator
import com.woliveiras.petit.util.LocaleApplicator
import com.woliveiras.petit.worker.LanSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class PetitApplication : Application(), Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory
  @Inject lateinit var familyGroupRepository: FamilyGroupRepository
  @Inject lateinit var lanSyncScheduler: LanSyncScheduler
  @Inject lateinit var restoreBackupUseCase: RestoreBackupUseCase
  @Inject lateinit var backupTriggerCoordinator: BackupTriggerCoordinator
  @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
  @Inject lateinit var localeApplicator: LocaleApplicator
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    runBlocking {
      initializeLanguageBeforeNotifications(
        context = this@PetitApplication,
        userPreferencesRepository = userPreferencesRepository,
        localeApplicator = localeApplicator,
        createNotificationChannel = ::createNotificationChannel,
      )
    }
    runBlocking(Dispatchers.IO) { restoreBackupUseCase.recoverInterruptedRestore() }
    applicationScope.launch {
      familyGroupRepository.isSyncEnabled.collect { shouldSchedule ->
        if (shouldSchedule) lanSyncScheduler.schedule() else lanSyncScheduler.cancel()
      }
    }
    backupTriggerCoordinator.start(applicationScope)
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

internal suspend fun UserPreferencesRepository.initialLanguageOrSystem(): AppLanguage =
  try {
    userPreferences.first().language
  } catch (cancellation: CancellationException) {
    throw cancellation
  } catch (_: Exception) {
    AppLanguage.SYSTEM
  }

internal suspend fun initializeLanguageBeforeNotifications(
  context: android.content.Context,
  userPreferencesRepository: UserPreferencesRepository,
  localeApplicator: LocaleApplicator,
  createNotificationChannel: () -> Unit,
) {
  val language = userPreferencesRepository.initialLanguageOrSystem()
  localeApplicator.applyLanguageAtStartup(context, language)
  createNotificationChannel()
}
