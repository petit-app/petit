package com.woliveiras.petit.presentation.feature.tasks

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.presentation.util.TaskDisplayText
import com.woliveiras.petit.presentation.util.TaskDisplayTextResolver
import com.woliveiras.petit.worker.TaskScheduler
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TaskDisplayViewModelsTest {
  private val dispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val scheduler = NoOpTaskScheduler()
  private val resolver = TaskDisplayTextResolver { _, _ ->
    TaskDisplayText("Título localizado", "Descrição localizada")
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun activeAndCompletedListsDecorateCopiesWithoutRewritingRepositoryTasks() =
    runTest(dispatcher) {
      val pending = task("pending", TaskStatus.PENDING)
      val completed = task("completed", TaskStatus.COMPLETED)
      val repository =
        FakeTaskRepository().apply {
          dueInRange.value = listOf(pending)
          completedTasks.value = listOf(completed)
        }
      val preferences = FakeUserPreferencesRepository()

      val activeViewModel = TaskListViewModel(context, repository, scheduler, resolver, preferences)
      val completedViewModel =
        CompletedTasksViewModel(context, repository, scheduler, resolver, preferences)
      advanceUntilIdle()

      assertThat(activeViewModel.uiState.value.tasks.single().title).isEqualTo("Título localizado")
      assertThat(activeViewModel.uiState.value.tasks.single().description)
        .isEqualTo("Descrição localizada")
      assertThat(completedViewModel.uiState.value.tasks.single().title)
        .isEqualTo("Título localizado")
      assertThat(repository.dueInRange.value.single()).isEqualTo(pending)
      assertThat(repository.completedTasks.value.single()).isEqualTo(completed)
    }

  @Test
  fun sameTaskListViewModelRedecoratesOnLanguageOnlyChangesAndPreservesStoredAndCustomText() =
    runTest(dispatcher) {
      val automatic =
        task("auto", TaskStatus.PENDING)
          .copy(
            kind = TaskKind.VACCINATION,
            title = "Persisted automatic title",
            description = "Persisted automatic description",
          )
      val custom =
        task("custom", TaskStatus.PENDING)
          .copy(title = "  Caregiver CUSTOM title  ", description = "Caregiver\nCUSTOM description")
      val repository = FakeTaskRepository().apply { oneShotDueInRange = listOf(automatic, custom) }
      val preferences = FakeUserPreferencesRepository(AppLanguage.ENGLISH)
      val languageResolver = TaskDisplayTextResolver { task, language ->
        if (task.kind == TaskKind.CUSTOM) {
          TaskDisplayText(task.title, task.description)
        } else {
          when (language) {
            AppLanguage.PORTUGUESE_BR ->
              TaskDisplayText("Título automático PT", "Descrição automática PT")
            else -> TaskDisplayText("Automatic title EN", "Automatic description EN")
          }
        }
      }
      val viewModel =
        TaskListViewModel(context, repository, scheduler, languageResolver, preferences)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.tasks.map { it.title })
        .containsExactly("Automatic title EN", custom.title)
        .inOrder()

      preferences.updateLanguage(AppLanguage.PORTUGUESE_BR)
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.tasks.map { it.title })
        .containsExactly("Título automático PT", custom.title)
        .inOrder()

      preferences.updateLanguage(AppLanguage.ENGLISH)
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.tasks.map { it.title })
        .containsExactly("Automatic title EN", custom.title)
        .inOrder()
      assertThat(repository.oneShotDueInRange).containsExactly(automatic, custom).inOrder()
      assertThat(custom.description).isEqualTo("Caregiver\nCUSTOM description")
    }

  @Test
  fun languageChangeAfterFilterChangeCannotRestoreTasksFromCancelledOldFilter() =
    runTest(dispatcher) {
      val oldFilterTask = task("old-filter", TaskStatus.PENDING)
      val currentFilterTask = task("current-filter", TaskStatus.PENDING)
      val repository =
        FakeTaskRepository().apply {
          dueInRangeBySubscription += MutableStateFlow(listOf(oldFilterTask))
          dueInRangeBySubscription += MutableStateFlow(listOf(currentFilterTask))
        }
      val preferences = FakeUserPreferencesRepository(AppLanguage.ENGLISH)
      val delayedResolver = TaskDisplayTextResolver { task, language ->
        if (task.id == oldFilterTask.id && language == AppLanguage.PORTUGUESE_BR) {
          delay(100)
        }
        TaskDisplayText("${task.id} ${language?.code}", task.description)
      }
      val viewModel =
        TaskListViewModel(context, repository, scheduler, delayedResolver, preferences)
      advanceUntilIdle()

      viewModel.setFilter(TaskFilter.TODAY)
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.tasks.single().id).isEqualTo(currentFilterTask.id)

      preferences.updateLanguage(AppLanguage.PORTUGUESE_BR)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.tasks.single().id).isEqualTo(currentFilterTask.id)
      assertThat(viewModel.uiState.value.tasks.single().title).isEqualTo("current-filter pt-BR")
      assertThat(repository.rangeSubscriptions).isEqualTo(2)
    }

  @Test
  fun completedListKeepsRawTaskWhenOneResolutionFailsAndContinuesDecoratingOthers() =
    runTest(dispatcher) {
      val failing = task("failing", TaskStatus.COMPLETED)
      val healthy = task("healthy", TaskStatus.COMPLETED)
      val repository =
        FakeTaskRepository().apply { completedTasks.value = listOf(failing, healthy) }
      val resolverWithFailure = TaskDisplayTextResolver { task, _ ->
        if (task.id == failing.id) error("Resolver database failure")
        TaskDisplayText("Localized healthy", "Localized healthy description")
      }
      val viewModel =
        CompletedTasksViewModel(
          context,
          repository,
          scheduler,
          resolverWithFailure,
          FakeUserPreferencesRepository(),
        )

      advanceUntilIdle()

      assertThat(viewModel.uiState.value.isLoading).isFalse()
      assertThat(viewModel.uiState.value.tasks.map { it.title })
        .containsExactly(failing.title, "Localized healthy")
        .inOrder()
      assertThat(repository.completedTasks.value).containsExactly(failing, healthy).inOrder()
    }

  @Test
  fun taskActionFailuresUsePtBrResourcesWithoutLeakingTechnicalMessages() =
    runTest(dispatcher) {
      val localizedContext =
        context.createConfigurationContext(
          Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("pt-BR"))
          }
        )
      val repository = FakeTaskRepository().apply { failStatusUpdate = true }
      val preferences = FakeUserPreferencesRepository(AppLanguage.PORTUGUESE_BR)
      val activeViewModel =
        TaskListViewModel(localizedContext, repository, scheduler, resolver, preferences)
      val completedViewModel =
        CompletedTasksViewModel(localizedContext, repository, scheduler, resolver, preferences)
      val activeEvent = async { activeViewModel.events.first() }
      val completedEvent = async { completedViewModel.events.first() }
      runCurrent()

      activeViewModel.completeTask("task-1")
      completedViewModel.reactivateTask("task-2")
      advanceUntilIdle()

      assertThat((activeEvent.await() as TaskListEvent.Error).message)
        .isEqualTo("Erro ao concluir tarefa")
      assertThat((completedEvent.await() as CompletedTasksEvent.Error).message)
        .isEqualTo("Erro ao reativar tarefa")
    }

  private fun task(id: String, status: TaskStatus) =
    Task(
      id = id,
      kind = TaskKind.CUSTOM,
      title = "Stored English title",
      description = "Stored English description",
      scheduledFor = LocalDateTime.now().plusDays(1),
      status = status,
      createdAt = 1L,
      updatedAt = 1L,
    )

  private class FakeTaskRepository : TaskRepository {
    val dueInRange = MutableStateFlow<List<Task>>(emptyList())
    val completedTasks = MutableStateFlow<List<Task>>(emptyList())
    var oneShotDueInRange: List<Task>? = null
    val dueInRangeBySubscription = mutableListOf<Flow<List<Task>>>()
    var rangeSubscriptions = 0
    var failStatusUpdate = false

    override fun getPendingTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getAllActiveTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksForPet(petId: String): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getTaskById(id: String): Task? = null

    override fun getTasksDueToday(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueThisWeek(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueThisMonth(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> =
      dueInRangeBySubscription.getOrNull(rangeSubscriptions++)
        ?: oneShotDueInRange?.let(::flowOf)
        ?: dueInRange

    override fun getNextTasks(limit: Int): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getPastDueTasks(): List<Task> = emptyList()

    override fun getCompletedTasks(): Flow<List<Task>> = completedTasks

    override suspend fun saveTask(task: Task) = Unit

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) {
      if (failStatusUpdate) {
        error("Provider detail in English: update rejected")
      }
    }

    override suspend fun deleteTask(id: String) = Unit

    override suspend fun deleteTasksByReferenceEntity(entityId: String) = Unit

    override suspend fun getUsedSubjectNames(kind: TaskKind, petId: String?): List<String> =
      emptyList()
  }

  private class FakeUserPreferencesRepository(language: AppLanguage = AppLanguage.ENGLISH) :
    UserPreferencesRepository {
    private val state = MutableStateFlow(UserPreferences(AppTheme.SYSTEM, language))
    override val userPreferences: Flow<UserPreferences> = state

    override suspend fun updateTheme(theme: AppTheme) {
      state.value = state.value.copy(theme = theme)
    }

    override suspend fun updateLanguage(language: AppLanguage) {
      state.value = state.value.copy(language = language)
    }

    override suspend fun setOnboardingCompleted() {
      state.value = state.value.copy(hasCompletedOnboarding = true)
    }
  }

  private class NoOpTaskScheduler : TaskScheduler {
    override fun scheduleTask(task: Task) = Unit

    override fun cancelTask(taskId: String) = Unit

    override fun cancelAllTasks() = Unit
  }
}
