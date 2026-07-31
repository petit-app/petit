package com.woliveiras.petit.worker

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskRecurrence
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.presentation.util.TaskDisplayText
import com.woliveiras.petit.presentation.util.TaskDisplayTextResolver
import com.woliveiras.petit.presentation.util.recurrenceSummary
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class TaskNotificationWorkerLocalizationTest {
  private val application = ApplicationProvider.getApplicationContext<Application>()
  private val context =
    application.createConfigurationContext(
      Configuration(application.resources.configuration).apply {
        setLocale(Locale.forLanguageTag("pt-BR"))
      }
    )
  private val manager by lazy {
    application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  @Before
  fun setUp() {
    shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    manager.cancelAll()
  }

  @After
  fun tearDown() {
    manager.cancelAll()
  }

  @Test
  fun notificationUsesResolvedPtBrTextInsteadOfPersistedEnglishFallback() = runTest {
    val storedTask =
      Task(
        id = "auto_vacc_vacc-1",
        petId = "pet-1",
        kind = TaskKind.VACCINATION,
        referenceEntityId = "vacc-1",
        title = "Mimi - Rabies",
        description = "Automatic reminder for upcoming care",
        scheduledFor = LocalDateTime.of(2026, 7, 26, 9, 0),
        createdAt = 1L,
        updatedAt = 1L,
      )
    val repository = FakeTaskRepository(storedTask)
    val resolver = TaskDisplayTextResolver { _, language ->
      assertThat(language).isEqualTo(AppLanguage.PORTUGUESE_BR)
      TaskDisplayText("Mimi - Antirrábica", "Lembrete automático para o próximo cuidado")
    }
    val worker = buildWorker(repository, resolver)

    val result = worker.doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    val notification = manager.activeNotifications.single().notification
    val title = notification.extras.getCharSequence("android.title").toString()
    val text = notification.extras.getCharSequence("android.text").toString()
    assertThat(title).isEqualTo("💉 Mimi - Antirrábica")
    assertThat(text).isEqualTo("Lembrete automático para o próximo cuidado")
    assertThat("$title $text").doesNotContain("Rabies")
    assertThat("$title $text").doesNotContain("Automatic reminder")
    assertThat(repository.task).isEqualTo(storedTask)
  }

  @Test
  fun resolverFailureUsesExactPersistedTextAndStillEmitsNotification() = runTest {
    val storedTask =
      Task(
        id = "auto_vacc_vacc-1",
        petId = "pet-1",
        kind = TaskKind.VACCINATION,
        title = "Persisted title",
        description = "Persisted description",
        scheduledFor = LocalDateTime.of(2026, 7, 26, 9, 0),
        createdAt = 1L,
        updatedAt = 1L,
      )
    val worker =
      buildWorker(
        FakeTaskRepository(storedTask),
        TaskDisplayTextResolver { _, _ -> error("resolver unavailable") },
      )

    val result = worker.doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    val notification = manager.activeNotifications.single().notification
    assertThat(notification.extras.getCharSequence("android.title").toString())
      .isEqualTo("💉 Persisted title")
    assertThat(notification.extras.getCharSequence("android.text").toString())
      .isEqualTo("Persisted description")
  }

  @Test
  fun resolverCancellationRemainsStructured() = runTest {
    val cancellation = CancellationException("cancelled")
    val storedTask =
      Task(
        id = "auto_vacc_vacc-1",
        kind = TaskKind.VACCINATION,
        title = "Persisted title",
        scheduledFor = LocalDateTime.of(2026, 7, 26, 9, 0),
        createdAt = 1L,
        updatedAt = 1L,
      )
    val worker =
      buildWorker(
        FakeTaskRepository(storedTask),
        TaskDisplayTextResolver { _, _ -> throw cancellation },
      )

    val thrown = runCatching { worker.doWork() }.exceptionOrNull()

    assertThat(thrown).isSameInstanceAs(cancellation)
    assertThat(manager.activeNotifications).isEmpty()
  }

  @Test
  fun preferenceFailureFallsBackToSystemAndStillEmitsNotification() = runTest {
    val storedTask =
      Task(
        id = "auto_vacc_vacc-1",
        kind = TaskKind.VACCINATION,
        title = "Persisted title",
        scheduledFor = LocalDateTime.of(2026, 7, 26, 9, 0),
        createdAt = 1L,
        updatedAt = 1L,
      )
    var receivedLanguage: AppLanguage? = null
    val resolver = TaskDisplayTextResolver { _, language ->
      receivedLanguage = language
      TaskDisplayText("Resolved title", "Resolved description")
    }
    val preferences =
      FakeUserPreferencesRepository(flow { throw IllegalStateException("DataStore unavailable") })
    val worker = buildWorker(FakeTaskRepository(storedTask), resolver, preferences)

    val result = worker.doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    assertThat(receivedLanguage).isEqualTo(AppLanguage.SYSTEM)
    assertThat(manager.activeNotifications).hasLength(1)
  }

  @Test
  fun preferenceCancellationRemainsStructured() = runTest {
    val cancellation = CancellationException("cancelled")
    val storedTask =
      Task(
        id = "auto_vacc_vacc-1",
        kind = TaskKind.VACCINATION,
        title = "Persisted title",
        scheduledFor = LocalDateTime.of(2026, 7, 26, 9, 0),
        createdAt = 1L,
        updatedAt = 1L,
      )
    val preferences = FakeUserPreferencesRepository(flow { throw cancellation })
    val worker =
      buildWorker(
        FakeTaskRepository(storedTask),
        TaskDisplayTextResolver { _, _ -> error("resolver must not run") },
        preferences,
      )

    val thrown = runCatching { worker.doWork() }.exceptionOrNull()

    assertThat(thrown).isSameInstanceAs(cancellation)
    assertThat(manager.activeNotifications).isEmpty()
  }

  @Test
  fun theSubjectLeadsTheNotificationTextWhenItAddsSomethingToTheTitle() = runTest {
    val storedTask =
      medicationTask(subjectName = "Apoquel", description = "Uma vez ao dia após a refeição")
    val worker =
      buildWorker(
        FakeTaskRepository(storedTask),
        TaskDisplayTextResolver { _, _ ->
          TaskDisplayText("Remédio da Mimi", storedTask.description)
        },
      )

    val result = worker.doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    assertThat(notificationText()).isEqualTo("Apoquel - Uma vez ao dia após a refeição")
  }

  @Test
  fun aSubjectThatRepeatsTheTitleIsNotShownTwice() = runTest {
    val storedTask = medicationTask(subjectName = "Apoquel", description = "Uma vez ao dia")
    val worker =
      buildWorker(
        FakeTaskRepository(storedTask),
        TaskDisplayTextResolver { _, _ -> TaskDisplayText("Apoquel", storedTask.description) },
      )

    worker.doWork()

    assertThat(notificationText()).isEqualTo("Uma vez ao dia")
  }

  @Test
  fun withoutASubjectOrDescriptionTheAppNameIsUsed() = runTest {
    val storedTask = medicationTask(subjectName = null, description = null)
    val worker =
      buildWorker(
        FakeTaskRepository(storedTask),
        TaskDisplayTextResolver { _, _ -> TaskDisplayText("Remédio da Mimi", null) },
      )

    worker.doWork()

    assertThat(notificationText()).isEqualTo(context.getString(R.string.app_name))
  }

  @Test
  fun aRepeatingTaskSaysSoInTheNotificationText() = runTest {
    val storedTask =
      medicationTask(subjectName = "Apoquel", description = "Após a refeição")
        .copy(
          recurrence = TaskRecurrence(interval = 8, unit = RecurrenceUnit.HOURS),
          seriesId = "series-1",
        )
    val worker =
      buildWorker(
        FakeTaskRepository(storedTask),
        TaskDisplayTextResolver { _, _ ->
          TaskDisplayText("Remédio da Mimi", storedTask.description)
        },
      )

    worker.doWork()

    val cadence = recurrenceSummary(context, storedTask.recurrence!!)
    assertThat(notificationText()).isEqualTo("Apoquel - Após a refeição - $cadence")
  }

  @Test
  fun theFollowUpOccurrenceIsOnlyEnqueuedAfterTheNotificationIsPosted() = runTest {
    val storedTask =
      medicationTask(subjectName = "Apoquel", description = "Após a refeição")
        .copy(
          recurrence = TaskRecurrence(interval = 8, unit = RecurrenceUnit.HOURS),
          seriesId = "series-1",
        )
    var notificationsWhenFollowUpWasEnqueued = -1
    val coordinator =
      object : TaskSeriesCoordinator {
        override suspend fun completeOccurrence(taskId: String) = Unit

        override suspend fun stopSeries(taskId: String) = Unit

        override suspend fun onNotificationDelivered(task: Task): Task = task

        override fun scheduleFollowUp(task: Task) {
          notificationsWhenFollowUpWasEnqueued = manager.activeNotifications.size
        }

        override suspend fun reconcilePendingSeries() = Unit
      }
    val worker =
      buildWorker(
        FakeTaskRepository(storedTask),
        TaskDisplayTextResolver { _, _ ->
          TaskDisplayText("Remédio da Mimi", storedTask.description)
        },
        coordinator = coordinator,
      )

    val result = worker.doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    assertThat(notificationsWhenFollowUpWasEnqueued).isEqualTo(1)
  }

  private fun medicationTask(subjectName: String?, description: String?) =
    Task(
      id = "auto_vacc_vacc-1",
      petId = "pet-1",
      kind = TaskKind.MEDICATION,
      subjectName = subjectName,
      title = "Remédio da Mimi",
      description = description,
      scheduledFor = LocalDateTime.of(2026, 7, 26, 9, 0),
      createdAt = 1L,
      updatedAt = 1L,
    )

  private fun notificationText(): String =
    manager.activeNotifications
      .single()
      .notification
      .extras
      .getCharSequence("android.text")
      .toString()

  private fun buildWorker(
    repository: TaskRepository,
    resolver: TaskDisplayTextResolver,
    preferences: UserPreferencesRepository = FakeUserPreferencesRepository(),
    coordinator: TaskSeriesCoordinator = NoOpTaskSeriesCoordinator(),
  ): TaskNotificationWorker =
    TestListenableWorkerBuilder.from(context, TaskNotificationWorker::class.java)
      .setInputData(workDataOf(TaskNotificationWorker.KEY_TASK_ID to "auto_vacc_vacc-1"))
      .setWorkerFactory(
        object : WorkerFactory() {
          override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
          ): ListenableWorker =
            TaskNotificationWorker(
              appContext,
              workerParameters,
              repository,
              resolver,
              preferences,
              coordinator,
            )
        }
      )
      .build()

  private class FakeUserPreferencesRepository(
    override val userPreferences: Flow<UserPreferences> =
      MutableStateFlow(UserPreferences(language = AppLanguage.PORTUGUESE_BR))
  ) : UserPreferencesRepository {

    override suspend fun updateTheme(theme: com.woliveiras.petit.domain.model.AppTheme) = Unit

    override suspend fun updateLanguage(language: AppLanguage) = Unit

    override suspend fun setOnboardingCompleted() = Unit
  }

  private class FakeTaskRepository(val task: Task) : TaskRepository {
    override fun getPendingTasks(): Flow<List<Task>> = MutableStateFlow(listOf(task))

    override fun getAllActiveTasks(): Flow<List<Task>> = MutableStateFlow(listOf(task))

    override fun getTasksForPet(petId: String): Flow<List<Task>> = MutableStateFlow(listOf(task))

    override suspend fun getTaskById(id: String): Task? = task.takeIf { it.id == id }

    override fun getTasksDueToday(): Flow<List<Task>> = MutableStateFlow(listOf(task))

    override fun getTasksDueThisWeek(): Flow<List<Task>> = MutableStateFlow(listOf(task))

    override fun getTasksDueThisMonth(): Flow<List<Task>> = MutableStateFlow(listOf(task))

    override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> =
      MutableStateFlow(listOf(task))

    override fun getNextTasks(limit: Int): Flow<List<Task>> = MutableStateFlow(listOf(task))

    override suspend fun getPastDueTasks(): List<Task> = emptyList()

    override suspend fun getPendingRecurringTasks(): List<Task> = emptyList()

    override fun getCompletedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun saveTask(task: Task) = Unit

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) = Unit

    override suspend fun deleteTask(id: String) = Unit

    override suspend fun deleteTasksByReferenceEntity(entityId: String) = Unit

    override suspend fun getUsedSubjectNames(kind: TaskKind, petId: String?): List<String> =
      emptyList()
  }
}
