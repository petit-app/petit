package com.woliveiras.petit.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.woliveiras.petit.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for scheduling and canceling task notifications. Allows for easier testing with fake
 * implementations.
 */
interface TaskScheduler {
  fun scheduleTask(task: Task)

  /**
   * Schedules the notification of an existing task at [scheduledFor] instead of the time stored on
   * it. A repeating series uses this to keep the following occurrence enqueued while the current
   * one is still waiting to be completed.
   */
  fun scheduleTaskAt(taskId: String, scheduledFor: LocalDateTime)

  fun cancelTask(taskId: String)

  fun cancelAllTasks()
}

/** Implementation of TaskScheduler using WorkManager. */
@Singleton
class TaskSchedulerImpl @Inject constructor(@ApplicationContext private val context: Context) :
  TaskScheduler {

  private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

  override fun scheduleTask(task: Task) = scheduleTaskAt(task.id, task.scheduledFor)

  override fun scheduleTaskAt(taskId: String, scheduledFor: LocalDateTime) {
    val scheduledTimeMillis = scheduledFor.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val currentTimeMillis = System.currentTimeMillis()
    val delayMillis = scheduledTimeMillis - currentTimeMillis
    val effectiveDelay = if (delayMillis <= 0) 0L else delayMillis

    val workRequest =
      OneTimeWorkRequestBuilder<TaskNotificationWorker>()
        .setInitialDelay(effectiveDelay, TimeUnit.MILLISECONDS)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .setInputData(workDataOf(TaskNotificationWorker.KEY_TASK_ID to taskId))
        .addTag(TAG_TASK)
        .addTag("${TAG_TASK_PREFIX}${taskId}")
        .build()

    workManager.enqueueUniqueWork(
      getUniqueWorkName(taskId),
      ExistingWorkPolicy.REPLACE,
      workRequest,
    )
  }

  override fun cancelTask(taskId: String) {
    workManager.cancelUniqueWork(getUniqueWorkName(taskId))
  }

  override fun cancelAllTasks() {
    workManager.cancelAllWorkByTag(TAG_TASK)
  }

  private fun getUniqueWorkName(taskId: String): String {
    return "${TAG_TASK_PREFIX}$taskId"
  }

  companion object {
    private const val TAG_TASK = "petit_task"
    private const val TAG_TASK_PREFIX = "petit_task_"
  }
}
