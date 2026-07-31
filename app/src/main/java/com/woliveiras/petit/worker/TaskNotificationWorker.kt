package com.woliveiras.petit.worker

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woliveiras.petit.MainActivity
import com.woliveiras.petit.PetitApplication
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.initialLanguageOrSystem
import com.woliveiras.petit.presentation.util.TaskDisplayText
import com.woliveiras.petit.presentation.util.TaskDisplayTextResolver
import com.woliveiras.petit.presentation.util.forLanguage
import com.woliveiras.petit.presentation.util.taskSubjectLabel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Worker that triggers task notifications. Runs at the scheduled time and shows a notification to
 * the user.
 */
@HiltWorker
class TaskNotificationWorker
@AssistedInject
constructor(
  @Assisted private val context: Context,
  @Assisted private val params: WorkerParameters,
  private val taskRepository: TaskRepository,
  private val taskDisplayTextResolver: TaskDisplayTextResolver,
  private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()

    return try {
      val task = taskRepository.getTaskById(taskId) ?: return Result.failure()

      if (task.status == TaskStatus.COMPLETED) {
        return Result.success()
      }

      val language = userPreferencesRepository.initialLanguageOrSystem()
      val displayText =
        try {
          taskDisplayTextResolver.resolve(task, language)
        } catch (cancellation: CancellationException) {
          throw cancellation
        } catch (failure: Exception) {
          Log.w(
            TAG,
            "Using persisted text for task $taskId after display resolution failed",
            failure,
          )
          TaskDisplayText(task.title, task.description)
        }
      val subject =
        taskSubjectLabel(context.forLanguage(language), task)?.takeIf { it != displayText.title }
      showNotification(task.id, displayText.title, displayText.description, subject, task.kind)

      Result.success()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (e: Exception) {
      Log.w(TAG, "Attempt $runAttemptCount failed for task $taskId", e)
      if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }
  }

  private fun showNotification(
    taskId: String,
    title: String,
    description: String?,
    subject: String?,
    kind: TaskKind,
  ) {
    if (
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(EXTRA_TASK_ID, taskId)
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val emoji =
      when (kind) {
        TaskKind.VACCINATION -> "💉"
        TaskKind.DEWORMING -> "🪱"
        TaskKind.WEIGHT -> "⚖️"
        TaskKind.MEDICATION -> "💊"
        TaskKind.CUSTOM -> "🔔"
      }

    val contentText =
      listOfNotNull(subject, description).joinToString(separator = " - ").ifBlank {
        context.getString(R.string.app_name)
      }

    val notification =
      NotificationCompat.Builder(context, PetitApplication.TASKS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("$emoji $title")
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(taskId.hashCode(), notification)
  }

  companion object {
    private const val TAG = "TaskNotificationWorker"
    const val KEY_TASK_ID = "task_id"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val MAX_RETRIES = 3
  }
}
