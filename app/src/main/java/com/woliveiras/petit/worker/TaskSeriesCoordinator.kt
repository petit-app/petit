package com.woliveiras.petit.worker

import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskRecurrence
import com.woliveiras.petit.domain.model.TaskStatus
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps a repeating task alive.
 *
 * A series is stored as a single pending occurrence plus the occurrences already completed. The
 * pending occurrence only moves forward once the following one has arrived, so the caregiver always
 * has a full interval to mark a dose as done.
 */
interface TaskSeriesCoordinator {
  /** Marks an occurrence as done and, for a series, opens and schedules the next one. */
  suspend fun completeOccurrence(taskId: String)

  /** Cancels the pending occurrence of a series; occurrences already completed stay in history. */
  suspend fun stopSeries(taskId: String)

  /**
   * Called by the notification worker before it renders. Moves the pending occurrence forward when
   * the notification is a follow-up and returns the occurrence to notify about.
   */
  suspend fun onNotificationDelivered(task: Task): Task

  /**
   * Keeps the following notification of a series enqueued. The worker calls this only after posting
   * because the new work replaces the unique work the worker itself is running under.
   */
  fun scheduleFollowUp(task: Task)

  /** Called when the app starts, to recover from reboots, time changes and time zone changes. */
  suspend fun reconcilePendingSeries()
}

@Singleton
class TaskSeriesCoordinatorImpl
@Inject
constructor(
  private val taskRepository: TaskRepository,
  private val taskScheduler: TaskScheduler,
  private val clock: Clock,
) : TaskSeriesCoordinator {

  override suspend fun completeOccurrence(taskId: String) {
    val task = taskRepository.getTaskById(taskId)
    taskScheduler.cancelTask(taskId)
    taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)

    val recurrence = task?.recurrence ?: return
    val next =
      recurrence.advanceBeyond(task.scheduledFor, task.occurrenceIndex, LocalDateTime.now(clock))
        ?: return
    val nextOccurrence =
      task.copy(
        id = UUID.randomUUID().toString(),
        seriesId = task.effectiveSeriesId,
        scheduledFor = next.scheduledFor,
        occurrenceIndex = next.occurrenceIndex,
        status = TaskStatus.PENDING,
        createdAt = clock.millis(),
        updatedAt = clock.millis(),
        deletedAt = null,
        syncStatus = SyncStatus.LOCAL_ONLY,
      )
    taskRepository.saveTask(nextOccurrence)
    taskScheduler.scheduleTask(nextOccurrence)
  }

  override suspend fun stopSeries(taskId: String) {
    taskScheduler.cancelTask(taskId)
    taskRepository.deleteTask(taskId)
  }

  override suspend fun onNotificationDelivered(task: Task): Task {
    val recurrence = task.recurrence ?: return task
    return moveToDueOccurrence(task, recurrence, LocalDateTime.now(clock))
  }

  override fun scheduleFollowUp(task: Task) {
    val recurrence = task.recurrence ?: return
    val now = LocalDateTime.now(clock)
    val following = recurrence.nextOccurrence(task.scheduledFor, task.occurrenceIndex)
    if (following != null && following.isAfter(now)) {
      taskScheduler.scheduleTaskAt(task.id, following)
    }
  }

  override suspend fun reconcilePendingSeries() {
    val now = LocalDateTime.now(clock)
    taskRepository.getPendingRecurringTasks().forEach { task ->
      val recurrence = task.recurrence ?: return@forEach
      val current = moveToDueOccurrence(task, recurrence, now)
      val nextFire =
        if (current.scheduledFor.isAfter(now)) current.scheduledFor
        else recurrence.nextOccurrence(current.scheduledFor, current.occurrenceIndex)
      if (nextFire != null && nextFire.isAfter(now)) {
        taskScheduler.scheduleTaskAt(current.id, nextFire)
      }
    }
  }

  private suspend fun moveToDueOccurrence(
    task: Task,
    recurrence: TaskRecurrence,
    now: LocalDateTime,
  ): Task {
    val due = recurrence.occurrenceDueAt(task.scheduledFor, task.occurrenceIndex, now)
    if (due.scheduledFor == task.scheduledFor) return task
    val moved =
      task.copy(
        seriesId = task.effectiveSeriesId,
        scheduledFor = due.scheduledFor,
        occurrenceIndex = due.occurrenceIndex,
        updatedAt = clock.millis(),
      )
    taskRepository.saveTask(moved)
    return moved
  }
}
