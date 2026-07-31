package com.woliveiras.petit.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.TimelineRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TimelineEvent
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.domain.model.WeightStatus
import com.woliveiras.petit.domain.usecase.GetPetHealthSummaryAction
import com.woliveiras.petit.presentation.util.TaskDisplayTextResolver
import com.woliveiras.petit.worker.TaskSeriesCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** UI State for the Home Dashboard. */
data class HomeUiState(
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val pets: List<PetWithSummary> = emptyList(),
  val tasksDueToday: List<Task> = emptyList(),
  val tasksDueThisWeek: List<Task> = emptyList(),
  val tasksDueThisMonth: List<Task> = emptyList(),
  val nextTasks: List<Task> = emptyList(),
  val hasMoreTasks: Boolean = false,
  val recentActivity: List<TimelineEvent> = emptyList(),
  val upcomingTimeline: List<TimelineEvent> = emptyList(),
  val isAllGood: Boolean = false,
  val alerts: List<HomeAlert> = emptyList(),
  val isEmpty: Boolean = false,
) {
  companion object {
    const val MAX_HOME_TASKS = 5
  }
}

/** Pet with summary information for display. */
data class PetWithSummary(
  val pet: Pet,
  val isHealthSummaryAvailable: Boolean = true,
  val latestWeight: WeightEntry? = null,
  val weightStatus: WeightStatus = WeightStatus.NO_DATA,
  val overallStatus: HealthStatus = HealthStatus.OK,
  val nextVaccineType: VaccineType? = null,
  val nextVaccinationDate: LocalDate? = null,
  val nextDewormingType: DewormingType? = null,
  val nextDewormingDate: LocalDate? = null,
)

/** A pet whose health requires attention on the dashboard. */
data class HomeAlert(val petWithSummary: PetWithSummary, val relevantDate: LocalDate?)

/**
 * Health-specific dashboard state derived from pet summaries.
 *
 * Keeping this calculation independent from Compose makes the severity and date ordering stable for
 * all dashboard layouts.
 */
data class HomeDashboardState(val isAllGood: Boolean, val alerts: List<HomeAlert>) {
  companion object {
    fun from(pets: List<PetWithSummary>): HomeDashboardState {
      val alerts =
        pets
          .asSequence()
          .filter { it.isHealthSummaryAvailable && it.overallStatus != HealthStatus.OK }
          .map { pet ->
            HomeAlert(
              petWithSummary = pet,
              relevantDate =
                listOfNotNull(pet.nextVaccinationDate, pet.nextDewormingDate).minOrNull(),
            )
          }
          .sortedWith(
            compareBy<HomeAlert>(
              { it.petWithSummary.overallStatus.alertSeverity() },
              { it.relevantDate ?: LocalDate.MAX },
            )
          )
          .toList()

      return HomeDashboardState(
        isAllGood =
          pets.isNotEmpty() && pets.all { it.isHealthSummaryAvailable } && alerts.isEmpty(),
        alerts = alerts,
      )
    }

    private fun HealthStatus.alertSeverity(): Int =
      when (this) {
        HealthStatus.OVERDUE -> 0
        HealthStatus.SCHEDULED -> 1
        HealthStatus.OK -> 2
      }
  }
}

@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val petRepository: PetRepository,
  private val weightEntryRepository: WeightEntryRepository,
  private val getPetHealthSummary: GetPetHealthSummaryAction,
  private val taskRepository: TaskRepository,
  private val timelineRepository: TimelineRepository,
  private val taskDisplayTextResolver: TaskDisplayTextResolver,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val taskSeriesCoordinator: TaskSeriesCoordinator,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private var dashboardJob: Job? = null

  init {
    loadDashboard()
  }

  private fun loadDashboard() {
    dashboardJob?.cancel()
    dashboardJob =
      viewModelScope.launch {
        val timeline =
          combine(
            timelineRepository.getRecentActivity().catch { emit(emptyList()) },
            timelineRepository.getUpcomingEvents(30).catch { emit(emptyList()) },
          ) { recentActivity, upcomingTimeline ->
            DashboardTimeline(recentActivity.take(10), upcomingTimeline.take(5))
          }

        val dashboardData =
          combine(
            petRepository.getAllPets().catch { emit(emptyList()) },
            weightEntryRepository.observeWeightChanges().catch { emit(null) },
            taskRepository.getTasksDueToday().catch { emit(emptyList()) },
            taskRepository.getTasksDueThisWeek().catch { emit(emptyList()) },
            taskRepository.getTasksDueThisMonth().catch { emit(emptyList()) },
          ) { pets, _, tasksToday, tasksThisWeek, tasksThisMonth ->
            DashboardData(pets, tasksToday, tasksThisWeek, tasksThisMonth)
          }
        val localizedDashboardData =
          combine(
            dashboardData,
            userPreferencesRepository.userPreferences
              .map { preferences -> preferences.language }
              .distinctUntilChanged(),
          ) { data, language ->
            data to language
          }

        // Combine pet, health-change, task, language, and timeline sources.
        combine(localizedDashboardData, timeline) { (data, language), dashboardTimeline ->
            Triple(data, language, dashboardTimeline)
          }
          .collect { (data, language, dashboardTimeline) ->
            if (data.pets.isEmpty()) {
              _uiState.value = HomeUiState(isLoading = false, isEmpty = true)
              return@collect
            }

            val petsWithSummary = data.pets.map { pet -> loadPetSummary(pet) }
            val healthDashboard = HomeDashboardState.from(petsWithSummary)

            // Also include overdue tasks in the "today" section
            val overdueTasks = safelyLoad { taskRepository.getPastDueTasks() }.orEmpty()
            val unresolvedTasksDueToday =
              (overdueTasks + data.tasksDueToday).distinctBy { it.id }.sortedBy { it.scheduledFor }
            val tasksDueToday = displayTasks(unresolvedTasksDueToday, language)

            // Exclude today's tasks from week and week's from month
            val todayIds = tasksDueToday.map { it.id }.toSet()
            val tasksDueThisWeek =
              displayTasks(data.tasksDueThisWeek.filter { it.id !in todayIds }, language)
            val weekIds = (todayIds + tasksDueThisWeek.map { it.id }).toSet()
            val tasksDueThisMonth =
              displayTasks(data.tasksDueThisMonth.filter { it.id !in weekIds }, language)

            // Pre-compute merged task list (max 5) for the "Next Tasks" section
            val allTasks =
              (tasksDueToday + tasksDueThisWeek + tasksDueThisMonth)
                .distinctBy { it.id }
                .sortedBy { it.scheduledFor }
            val nextTasks = allTasks.take(HomeUiState.MAX_HOME_TASKS)
            val hasMoreTasks = allTasks.size > HomeUiState.MAX_HOME_TASKS

            _uiState.value =
              HomeUiState(
                isLoading = false,
                isRefreshing = false,
                pets = petsWithSummary,
                tasksDueToday = tasksDueToday,
                tasksDueThisWeek = tasksDueThisWeek,
                tasksDueThisMonth = tasksDueThisMonth,
                nextTasks = nextTasks,
                hasMoreTasks = hasMoreTasks,
                recentActivity = dashboardTimeline.recentActivity,
                upcomingTimeline = dashboardTimeline.upcomingTimeline,
                isAllGood = healthDashboard.isAllGood,
                alerts = healthDashboard.alerts,
                isEmpty = false,
              )
          }
      }
  }

  private suspend fun displayTask(task: Task, language: AppLanguage): Task =
    try {
      taskDisplayTextResolver.resolve(task, language).let { text ->
        task.copy(title = text.title, description = text.description)
      }
    } catch (exception: CancellationException) {
      throw exception
    } catch (_: Exception) {
      task
    }

  private suspend fun displayTasks(tasks: List<Task>, language: AppLanguage): List<Task> =
    buildList {
      tasks.forEach { task -> add(displayTask(task, language)) }
    }

  private data class DashboardData(
    val pets: List<Pet>,
    val tasksDueToday: List<Task>,
    val tasksDueThisWeek: List<Task>,
    val tasksDueThisMonth: List<Task>,
  )

  private data class DashboardTimeline(
    val recentActivity: List<TimelineEvent>,
    val upcomingTimeline: List<TimelineEvent>,
  )

  private suspend fun loadPetSummary(pet: Pet): PetWithSummary {
    val summary =
      try {
        getPetHealthSummary.execute(pet.id)
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        return PetWithSummary(pet = pet, isHealthSummaryAvailable = false)
      }

    return PetWithSummary(
      pet = pet,
      latestWeight = summary.latestWeight,
      weightStatus = summary.weightStatus,
      overallStatus = summary.overallStatus,
      nextVaccineType = summary.nextVaccineType,
      nextVaccinationDate = summary.nextVaccinationDate,
      nextDewormingType = summary.nextDewormingType,
      nextDewormingDate = summary.nextDewormingDate,
    )
  }

  private suspend fun <T> safelyLoad(load: suspend () -> T): T? =
    try {
      load()
    } catch (exception: CancellationException) {
      throw exception
    } catch (_: Exception) {
      null
    }

  fun refresh() {
    _uiState.value = _uiState.value.copy(isRefreshing = true)
    loadDashboard()
  }

  fun completeTask(taskId: String) {
    viewModelScope.launch {
      // Goes through the coordinator so completing a repeating task from Home opens its next
      // occurrence instead of ending the series.
      try {
        taskSeriesCoordinator.completeOccurrence(taskId)
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        // The dashboard has no error surface; the task simply stays pending.
      }
    }
  }
}
