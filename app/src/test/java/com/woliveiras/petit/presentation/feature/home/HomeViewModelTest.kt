package com.woliveiras.petit.presentation.feature.home

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.TimelineRepository
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetHealthSummary
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.domain.model.TimelineEvent
import com.woliveiras.petit.domain.model.TimelineEventType
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.domain.usecase.GetPetHealthSummaryAction
import com.woliveiras.petit.presentation.util.TaskDisplayText
import com.woliveiras.petit.presentation.util.TaskDisplayTextResolver
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  private val dispatcher: TestDispatcher = StandardTestDispatcher()
  private val identityTaskDisplayTextResolver = TaskDisplayTextResolver { task, _ ->
    TaskDisplayText(task.title, task.description)
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
  fun healthAndTimelineFailuresKeepOtherPetsVisible() =
    runTest(dispatcher) {
      val firstPet = pet("first")
      val secondPet = pet("second")
      val viewModel =
        HomeViewModel(
          petRepository = FakePetRepository(listOf(firstPet, secondPet)),
          weightEntryRepository = FakeWeightEntryRepository(),
          getPetHealthSummary =
            FakeHealthSummaryAction(
              mapOf(
                firstPet.id to
                  Result.success(PetHealthSummary(firstPet.id, overallStatus = HealthStatus.OK)),
                secondPet.id to Result.failure(IllegalStateException("health source unavailable")),
              )
            ),
          taskRepository = FakeTaskRepository(),
          timelineRepository = FailingTimelineRepository(),
          taskDisplayTextResolver = identityTaskDisplayTextResolver,
          userPreferencesRepository = FakeUserPreferencesRepository(),
        )

      advanceUntilIdle()

      assertThat(viewModel.uiState.value.isLoading).isFalse()
      assertThat(viewModel.uiState.value.pets.map { it.pet.id }).containsExactly("first", "second")
      assertThat(viewModel.uiState.value.pets.first().overallStatus).isEqualTo(HealthStatus.OK)
      assertThat(viewModel.uiState.value.pets.last().isHealthSummaryAvailable).isFalse()
      assertThat(viewModel.uiState.value.isAllGood).isFalse()
      assertThat(viewModel.uiState.value.recentActivity).isEmpty()
      assertThat(viewModel.uiState.value.upcomingTimeline).isEmpty()
    }

  @Test
  fun healthChangesRefreshDashboardWhenUpcomingTimelineChanges() =
    runTest(dispatcher) {
      val pet = pet("mimi")
      val healthSummary = MutableHealthSummaryAction(PetHealthSummary(pet.id))
      val timeline = MutableTimelineRepository()
      val viewModel =
        HomeViewModel(
          petRepository = FakePetRepository(listOf(pet)),
          weightEntryRepository = FakeWeightEntryRepository(),
          getPetHealthSummary = healthSummary,
          taskRepository = FakeTaskRepository(),
          timelineRepository = timeline,
          taskDisplayTextResolver = identityTaskDisplayTextResolver,
          userPreferencesRepository = FakeUserPreferencesRepository(),
        )
      advanceUntilIdle()

      healthSummary.summary =
        PetHealthSummary(
          petId = pet.id,
          overallStatus = HealthStatus.SCHEDULED,
          nextVaccinationDate = LocalDate.of(2026, 7, 20),
        )
      timeline.upcoming.value =
        listOf(
          TimelineEvent(
            id = "vaccination-due",
            eventType = TimelineEventType.VACCINATION_DUE,
            petId = pet.id,
            petName = pet.name,
            petPhotoUri = null,
            eventDate = LocalDate.of(2026, 7, 20),
            title = "Vaccination",
            subtitle = null,
            isFuture = true,
          )
        )
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.pets.single().overallStatus)
        .isEqualTo(HealthStatus.SCHEDULED)
      assertThat(viewModel.uiState.value.alerts.single().petWithSummary.pet.id).isEqualTo(pet.id)
    }

  @Test
  fun nextTasksAreLimitedToFiveAndReportWhenMoreAreAvailable() =
    runTest(dispatcher) {
      val taskRepository =
        FakeTaskRepository().apply {
          dueToday.value = listOf(task("1", 1), task("2", 2))
          dueThisWeek.value = listOf(task("3", 3), task("4", 4))
          dueThisMonth.value = listOf(task("5", 5), task("6", 6))
        }
      val viewModel =
        HomeViewModel(
          petRepository = FakePetRepository(listOf(pet("mimi"))),
          weightEntryRepository = FakeWeightEntryRepository(),
          getPetHealthSummary =
            FakeHealthSummaryAction(mapOf("mimi" to Result.success(PetHealthSummary("mimi")))),
          taskRepository = taskRepository,
          timelineRepository = MutableTimelineRepository(),
          taskDisplayTextResolver = identityTaskDisplayTextResolver,
          userPreferencesRepository = FakeUserPreferencesRepository(),
        )

      advanceUntilIdle()

      assertThat(viewModel.uiState.value.nextTasks.map { it.id })
        .containsExactly("1", "2", "3", "4", "5")
        .inOrder()
      assertThat(viewModel.uiState.value.hasMoreTasks).isTrue()
    }

  @Test
  fun automaticTaskTextIsDecoratedOnlyInDashboardPresentationState() =
    runTest(dispatcher) {
      val storedTask = task("automatic", 1).copy(title = "Stored English title")
      val taskRepository = FakeTaskRepository().apply { dueToday.value = listOf(storedTask) }
      val viewModel =
        HomeViewModel(
          petRepository = FakePetRepository(listOf(pet("mimi"))),
          weightEntryRepository = FakeWeightEntryRepository(),
          getPetHealthSummary =
            FakeHealthSummaryAction(mapOf("mimi" to Result.success(PetHealthSummary("mimi")))),
          taskRepository = taskRepository,
          timelineRepository = MutableTimelineRepository(),
          taskDisplayTextResolver =
            TaskDisplayTextResolver { _, _ ->
              TaskDisplayText("Título localizado", "Descrição localizada")
            },
          userPreferencesRepository = FakeUserPreferencesRepository(),
        )

      advanceUntilIdle()

      assertThat(viewModel.uiState.value.nextTasks.single().title).isEqualTo("Título localizado")
      assertThat(viewModel.uiState.value.nextTasks.single().description)
        .isEqualTo("Descrição localizada")
      assertThat(taskRepository.dueToday.value.single()).isEqualTo(storedTask)
    }

  @Test
  fun recentAndUpcomingTimelineEventsRespectDashboardLimits() =
    runTest(dispatcher) {
      val timeline =
        MutableTimelineRepository().apply {
          recent.value = (1..12).map { timelineEvent("recent-$it", it) }
          upcoming.value = (1..7).map { timelineEvent("upcoming-$it", it) }
        }
      val viewModel =
        HomeViewModel(
          petRepository = FakePetRepository(listOf(pet("mimi"))),
          weightEntryRepository = FakeWeightEntryRepository(),
          getPetHealthSummary =
            FakeHealthSummaryAction(mapOf("mimi" to Result.success(PetHealthSummary("mimi")))),
          taskRepository = FakeTaskRepository(),
          timelineRepository = timeline,
          taskDisplayTextResolver = identityTaskDisplayTextResolver,
          userPreferencesRepository = FakeUserPreferencesRepository(),
        )

      advanceUntilIdle()

      assertThat(viewModel.uiState.value.recentActivity.map { it.id })
        .containsExactlyElementsIn((1..10).map { "recent-$it" })
        .inOrder()
      assertThat(viewModel.uiState.value.upcomingTimeline.map { it.id })
        .containsExactlyElementsIn((1..5).map { "upcoming-$it" })
        .inOrder()
    }

  private fun pet(id: String) =
    Pet(id = id, name = id, petType = PetType.CAT, createdAt = 1L, updatedAt = 1L)

  private fun task(id: String, day: Long) =
    Task(
      id = id,
      kind = TaskKind.CUSTOM,
      title = "Task $id",
      scheduledFor = LocalDateTime.of(2026, 7, 17, 9, 0).plusDays(day),
      createdAt = 1L,
      updatedAt = 1L,
    )

  private fun timelineEvent(id: String, day: Int) =
    TimelineEvent(
      id = id,
      eventType = TimelineEventType.WEIGHT,
      petId = "mimi",
      petName = "Mimi",
      petPhotoUri = null,
      eventDate = LocalDate.of(2026, 7, 1).plusDays(day.toLong()),
      title = id,
      subtitle = null,
      isFuture = false,
    )

  private class FakeHealthSummaryAction(
    private val results: Map<String, Result<PetHealthSummary>>
  ) : GetPetHealthSummaryAction {
    override suspend fun execute(petId: String): PetHealthSummary =
      results.getValue(petId).getOrThrow()
  }

  private class MutableHealthSummaryAction(var summary: PetHealthSummary) :
    GetPetHealthSummaryAction {
    override suspend fun execute(petId: String): PetHealthSummary = summary
  }

  private class FakePetRepository(pets: List<Pet>) : PetRepository {
    private val pets = MutableStateFlow(pets)

    override fun getAllPets(): Flow<List<Pet>> = pets

    override suspend fun getPetById(id: String): Pet? = pets.value.find { it.id == id }

    override fun getPetByIdFlow(id: String): Flow<Pet?> =
      MutableStateFlow(pets.value.find { it.id == id })

    override fun getPetCount(): Flow<Int> = MutableStateFlow(pets.value.size)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class FakeWeightEntryRepository : WeightEntryRepository {
    override fun getWeightEntriesForPet(petId: String): Flow<List<WeightEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun getLatestWeightEntry(petId: String): WeightEntry? = null

    override suspend fun getWeightEntryById(id: String): WeightEntry? = null

    override fun getLatestWeightEntryFlow(petId: String): Flow<WeightEntry?> =
      MutableStateFlow(null)

    override fun getWeightEntriesForChart(petId: String, limit: Int): Flow<List<WeightEntry>> =
      MutableStateFlow(emptyList())

    override fun observeWeightChanges(): Flow<Long?> = MutableStateFlow(null)

    override suspend fun saveWeightEntry(entry: WeightEntry) = Unit

    override suspend fun deleteWeightEntry(id: String) = Unit

    override suspend fun countEntriesForPet(petId: String): Int = 0
  }

  private class FakeTaskRepository : TaskRepository {
    val dueToday = MutableStateFlow<List<Task>>(emptyList())
    val dueThisWeek = MutableStateFlow<List<Task>>(emptyList())
    val dueThisMonth = MutableStateFlow<List<Task>>(emptyList())

    override fun getPendingTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getAllActiveTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksForPet(petId: String): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getTaskById(id: String): Task? = null

    override fun getTasksDueToday(): Flow<List<Task>> = dueToday

    override fun getTasksDueThisWeek(): Flow<List<Task>> = dueThisWeek

    override fun getTasksDueThisMonth(): Flow<List<Task>> = dueThisMonth

    override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> =
      MutableStateFlow(emptyList())

    override fun getNextTasks(limit: Int): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getPastDueTasks(): List<Task> = emptyList()

    override fun getCompletedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun saveTask(task: Task) = Unit

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) = Unit

    override suspend fun deleteTask(id: String) = Unit

    override suspend fun deleteTasksByReferenceEntity(entityId: String) = Unit
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

  private class FailingTimelineRepository : TimelineRepository {
    override fun getRecentActivity(): Flow<List<TimelineEvent>> = flow {
      error("timeline unavailable")
    }

    override fun getUpcomingEvents(daysAhead: Int): Flow<List<TimelineEvent>> = flow {
      error("timeline unavailable")
    }

    override fun getOverdueEvents(): Flow<List<TimelineEvent>> = MutableStateFlow(emptyList())

    override fun getAllActivity(
      startDate: java.time.LocalDate,
      endDate: java.time.LocalDate,
    ): Flow<List<TimelineEvent>> = MutableStateFlow(emptyList())
  }

  private class MutableTimelineRepository : TimelineRepository {
    val recent = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val upcoming = MutableStateFlow<List<TimelineEvent>>(emptyList())

    override fun getRecentActivity(): Flow<List<TimelineEvent>> = recent

    override fun getUpcomingEvents(daysAhead: Int): Flow<List<TimelineEvent>> = upcoming

    override fun getOverdueEvents(): Flow<List<TimelineEvent>> = MutableStateFlow(emptyList())

    override fun getAllActivity(
      startDate: LocalDate,
      endDate: LocalDate,
    ): Flow<List<TimelineEvent>> = MutableStateFlow(emptyList())
  }
}
