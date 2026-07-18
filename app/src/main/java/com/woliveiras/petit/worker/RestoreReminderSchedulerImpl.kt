package com.woliveiras.petit.worker

import com.woliveiras.petit.domain.backup.restore.RestoreReminderScheduler
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import javax.inject.Inject
import javax.inject.Singleton

/** Rebuilds device-local WorkManager reminders from the restored Room snapshot. */
@Singleton
class RestoreReminderSchedulerImpl
@Inject
constructor(
  private val exportImportUseCase: ExportImportUseCase,
  private val taskScheduler: TaskScheduler,
) : RestoreReminderScheduler {
  override suspend fun rescheduleCurrentTasks() {
    val tasks =
      exportImportUseCase.exportBackupSnapshot().tasks.filter {
        it.deletedAt == null && it.isPending
      }
    taskScheduler.cancelAllTasks()
    tasks.forEach(taskScheduler::scheduleTask)
  }
}
