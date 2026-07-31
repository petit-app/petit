package com.woliveiras.petit.worker

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.RecurrenceEnd
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskRecurrence
import com.woliveiras.petit.domain.model.TaskStatus
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TaskSeriesCoordinatorImplTest {

  private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

  @Test
  fun completingARepeatingTaskOpensAndSchedulesTheNextOccurrence() = runTest {
    val task =
      recurringTask(
        scheduledFor = LocalDateTime.of(2026, 3, 10, 8, 0),
        recurrence = TaskRecurrence(interval = 1, unit = RecurrenceUnit.DAYS),
      )
    val repository = RecordingTaskRepository(task)
    val scheduler = RecordingTaskScheduler()
    val coordinator = coordinator(repository, scheduler, LocalDateTime.of(2026, 3, 10, 8, 5))

    coordinator.completeOccurrence(task.id)

    assertThat(repository.statusUpdates).containsExactly(task.id to TaskStatus.COMPLETED)
    val next = repository.saved.single()
    assertThat(next.id).isNotEqualTo(task.id)
    assertThat(next.seriesId).isEqualTo(task.id)
    assertThat(next.occurrenceIndex).isEqualTo(1)
    assertThat(next.status).isEqualTo(TaskStatus.PENDING)
    assertThat(next.scheduledFor).isEqualTo(LocalDateTime.of(2026, 3, 11, 8, 0))
    assertThat(scheduler.scheduled.single().id).isEqualTo(next.id)
  }

  @Test
  fun completingATaskWithoutARepeatRuleDoesNotOpenANewOccurrence() = runTest {
    val task = recurringTask(LocalDateTime.of(2026, 3, 10, 8, 0), recurrence = null)
    val repository = RecordingTaskRepository(task)
    val scheduler = RecordingTaskScheduler()
    val coordinator = coordinator(repository, scheduler, LocalDateTime.of(2026, 3, 10, 8, 5))

    coordinator.completeOccurrence(task.id)

    assertThat(repository.saved).isEmpty()
    assertThat(scheduler.scheduled).isEmpty()
    assertThat(scheduler.cancelled).containsExactly(task.id)
  }

  @Test
  fun completingTheLastOccurrenceOfAFiniteSeriesEndsIt() = runTest {
    val task =
      recurringTask(
          scheduledFor = LocalDateTime.of(2026, 3, 10, 8, 0),
          recurrence =
            TaskRecurrence(
              interval = 1,
              unit = RecurrenceUnit.DAYS,
              end = RecurrenceEnd.AfterOccurrences(2),
            ),
        )
        .copy(occurrenceIndex = 1)
    val repository = RecordingTaskRepository(task)
    val scheduler = RecordingTaskScheduler()
    val coordinator = coordinator(repository, scheduler, LocalDateTime.of(2026, 3, 10, 8, 5))

    coordinator.completeOccurrence(task.id)

    assertThat(repository.statusUpdates).containsExactly(task.id to TaskStatus.COMPLETED)
    assertThat(repository.saved).isEmpty()
  }

  @Test
  fun stoppingASeriesCancelsAndSoftDeletesThePendingOccurrenceOnly() = runTest {
    val task =
      recurringTask(
        scheduledFor = LocalDateTime.of(2026, 3, 10, 8, 0),
        recurrence = TaskRecurrence(interval = 1, unit = RecurrenceUnit.DAYS),
      )
    val repository = RecordingTaskRepository(task)
    val scheduler = RecordingTaskScheduler()
    val coordinator = coordinator(repository, scheduler, LocalDateTime.of(2026, 3, 10, 8, 5))

    coordinator.stopSeries(task.id)

    assertThat(scheduler.cancelled).containsExactly(task.id)
    assertThat(repository.deleted).containsExactly(task.id)
    assertThat(repository.statusUpdates).isEmpty()
  }

  @Test
  fun aFollowUpNotificationMovesThePendingOccurrenceForwardAndKeepsTheNextOneEnqueued() = runTest {
    val task =
      recurringTask(
        scheduledFor = LocalDateTime.of(2026, 3, 10, 8, 0),
        recurrence = TaskRecurrence(interval = 1, unit = RecurrenceUnit.DAYS),
      )
    val repository = RecordingTaskRepository(task)
    val scheduler = RecordingTaskScheduler()
    val coordinator = coordinator(repository, scheduler, LocalDateTime.of(2026, 3, 12, 8, 0))

    val notified = coordinator.onNotificationDelivered(task)

    assertThat(notified.scheduledFor).isEqualTo(LocalDateTime.of(2026, 3, 12, 8, 0))
    assertThat(notified.occurrenceIndex).isEqualTo(2)
    assertThat(repository.saved.single().id).isEqualTo(task.id)
    assertThat(scheduler.scheduledAt).isEmpty()

    coordinator.scheduleFollowUp(notified)

    assertThat(scheduler.scheduledAt)
      .containsExactly(task.id to LocalDateTime.of(2026, 3, 13, 8, 0))
  }

  @Test
  fun anOnTimeNotificationLeavesTheOccurrenceWhereItIs() = runTest {
    val task =
      recurringTask(
        scheduledFor = LocalDateTime.of(2026, 3, 10, 8, 0),
        recurrence = TaskRecurrence(interval = 1, unit = RecurrenceUnit.DAYS),
      )
    val repository = RecordingTaskRepository(task)
    val scheduler = RecordingTaskScheduler()
    val coordinator = coordinator(repository, scheduler, LocalDateTime.of(2026, 3, 10, 8, 0))

    val notified = coordinator.onNotificationDelivered(task)

    assertThat(notified).isEqualTo(task)
    assertThat(repository.saved).isEmpty()
    assertThat(scheduler.scheduledAt).isEmpty()

    coordinator.scheduleFollowUp(notified)

    assertThat(scheduler.scheduledAt)
      .containsExactly(task.id to LocalDateTime.of(2026, 3, 11, 8, 0))
  }

  @Test
  fun startupReconciliationCollapsesMissedOccurrencesAndReschedules() = runTest {
    val task =
      recurringTask(
        scheduledFor = LocalDateTime.of(2026, 3, 1, 8, 0),
        recurrence = TaskRecurrence(interval = 1, unit = RecurrenceUnit.DAYS),
      )
    val repository = RecordingTaskRepository(task, pendingRecurring = listOf(task))
    val scheduler = RecordingTaskScheduler()
    val coordinator = coordinator(repository, scheduler, LocalDateTime.of(2026, 3, 10, 9, 0))

    coordinator.reconcilePendingSeries()

    val moved = repository.saved.single()
    assertThat(moved.scheduledFor).isEqualTo(LocalDateTime.of(2026, 3, 10, 8, 0))
    assertThat(moved.occurrenceIndex).isEqualTo(9)
    assertThat(scheduler.scheduledAt)
      .containsExactly(task.id to LocalDateTime.of(2026, 3, 11, 8, 0))
  }

  @Test
  fun startupReconciliationKeepsAFutureOccurrenceEnqueuedWithoutMovingIt() = runTest {
    val task =
      recurringTask(
        scheduledFor = LocalDateTime.of(2026, 3, 20, 8, 0),
        recurrence = TaskRecurrence(interval = 1, unit = RecurrenceUnit.WEEKS),
      )
    val repository = RecordingTaskRepository(task, pendingRecurring = listOf(task))
    val scheduler = RecordingTaskScheduler()
    val coordinator = coordinator(repository, scheduler, LocalDateTime.of(2026, 3, 10, 9, 0))

    coordinator.reconcilePendingSeries()

    assertThat(repository.saved).isEmpty()
    assertThat(scheduler.scheduledAt)
      .containsExactly(task.id to LocalDateTime.of(2026, 3, 20, 8, 0))
  }

  private fun coordinator(
    repository: TaskRepository,
    scheduler: TaskScheduler,
    now: LocalDateTime,
  ): TaskSeriesCoordinatorImpl =
    TaskSeriesCoordinatorImpl(
      taskRepository = repository,
      taskScheduler = scheduler,
      clock = Clock.fixed(now.atZone(zone).toInstant(), zone),
    )

  private fun recurringTask(scheduledFor: LocalDateTime, recurrence: TaskRecurrence?): Task =
    Task(
      id = "task-1",
      petId = "pet-1",
      kind = TaskKind.MEDICATION,
      title = "Antibiótico",
      scheduledFor = scheduledFor,
      recurrence = recurrence,
      createdAt = 1L,
      updatedAt = 1L,
    )

  private class RecordingTaskRepository(
    private val stored: Task,
    private val pendingRecurring: List<Task> = emptyList(),
  ) : TaskRepository {
    val saved = mutableListOf<Task>()
    val deleted = mutableListOf<String>()
    val statusUpdates = mutableListOf<Pair<String, TaskStatus>>()

    override fun getPendingTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getAllActiveTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksForPet(petId: String): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getTaskById(id: String): Task? = stored.takeIf { it.id == id }

    override fun getTasksDueToday(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueThisWeek(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueThisMonth(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> =
      MutableStateFlow(emptyList())

    override fun getNextTasks(limit: Int): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getPastDueTasks(): List<Task> = emptyList()

    override suspend fun getPendingRecurringTasks(): List<Task> = pendingRecurring

    override fun getCompletedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun saveTask(task: Task) {
      saved += task
    }

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) {
      statusUpdates += id to status
    }

    override suspend fun deleteTask(id: String) {
      deleted += id
    }

    override suspend fun deleteTasksByReferenceEntity(entityId: String) = Unit

    override suspend fun getUsedSubjectNames(kind: TaskKind, petId: String?): List<String> =
      emptyList()
  }

  private class RecordingTaskScheduler : TaskScheduler {
    val scheduled = mutableListOf<Task>()
    val scheduledAt = mutableListOf<Pair<String, LocalDateTime>>()
    val cancelled = mutableListOf<String>()

    override fun scheduleTask(task: Task) {
      scheduled += task
    }

    override fun scheduleTaskAt(taskId: String, scheduledFor: LocalDateTime) {
      scheduledAt += taskId to scheduledFor
    }

    override fun cancelTask(taskId: String) {
      cancelled += taskId
    }

    override fun cancelAllTasks() = Unit
  }
}
