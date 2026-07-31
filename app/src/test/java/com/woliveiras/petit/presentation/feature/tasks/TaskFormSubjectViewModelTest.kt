package com.woliveiras.petit.presentation.feature.tasks

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.worker.TaskScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TaskFormSubjectViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun vaccinationOffersOnlyTheVaccinesOfTheSelectedSpecies() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      advanceUntilIdle()

      viewModel.updateKind(TaskKind.VACCINATION)
      viewModel.updateSelectedPet("pet-1", "Mimi")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.vaccineOptions).contains(VaccineType.FELV)
      assertThat(viewModel.uiState.value.vaccineOptions).doesNotContain(VaccineType.DHPP)
    }

  @Test
  fun changingKindClearsTheSubjectAlreadyChosen() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      advanceUntilIdle()

      viewModel.updateKind(TaskKind.VACCINATION)
      viewModel.updateSubjectCode(VaccineType.RABIES.name)
      viewModel.updateKind(TaskKind.DEWORMING)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.subjectCode).isNull()
      assertThat(viewModel.uiState.value.subjectName).isEmpty()
    }

  @Test
  fun theSubjectPrefillsTheTitleUntilTheCaregiverWritesOne() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      advanceUntilIdle()

      viewModel.updateKind(TaskKind.MEDICATION)
      viewModel.updateSubjectName("Apoquel")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.title).isEqualTo("Apoquel")

      viewModel.updateTitle("Remédio da manhã")
      viewModel.updateSubjectName("Prednisolona")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.title).isEqualTo("Remédio da manhã")
    }

  @Test
  fun otherVaccineWithoutFreeTextIsRejected() =
    runTest(dispatcher) {
      val repository = FakeTaskRepository()
      val viewModel = viewModel(repository)
      advanceUntilIdle()

      viewModel.updateKind(TaskKind.VACCINATION)
      viewModel.updateSubjectCode(VaccineType.OTHER.name)
      viewModel.updateTitle("Vacina especial")
      viewModel.saveTask()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.subjectError)
        .isEqualTo(context.getString(R.string.task_validation_subject_required))
      assertThat(repository.saved).isEmpty()
    }

  @Test
  fun theChosenSubjectIsPersistedWithTheTask() =
    runTest(dispatcher) {
      val repository = FakeTaskRepository()
      val viewModel = viewModel(repository)
      advanceUntilIdle()

      viewModel.updateKind(TaskKind.DEWORMING)
      viewModel.updateSubjectCode(DewormingType.EXTERNAL.name)
      viewModel.updateSubjectName("Frontline")
      viewModel.saveTask()
      advanceUntilIdle()

      val saved = repository.saved.single()
      assertThat(saved.subjectCode).isEqualTo(DewormingType.EXTERNAL.name)
      assertThat(saved.subjectName).isEqualTo("Frontline")
    }

  @Test
  fun kindsWithoutASubjectNeverPersistOne() =
    runTest(dispatcher) {
      val repository = FakeTaskRepository()
      val viewModel = viewModel(repository)
      advanceUntilIdle()

      viewModel.updateKind(TaskKind.MEDICATION)
      viewModel.updateSubjectName("Apoquel")
      viewModel.updateKind(TaskKind.CUSTOM)
      viewModel.updateTitle("Passear com o Lino")
      viewModel.saveTask()
      advanceUntilIdle()

      val saved = repository.saved.single()
      assertThat(saved.subjectCode).isNull()
      assertThat(saved.subjectName).isNull()
    }

  @Test
  fun medicationSuggestsNamesAlreadyUsed() =
    runTest(dispatcher) {
      val repository = FakeTaskRepository(usedSubjectNames = listOf("Apoquel", "Antibiótico"))
      val viewModel = viewModel(repository)
      advanceUntilIdle()

      viewModel.updateKind(TaskKind.MEDICATION)
      advanceUntilIdle()
      viewModel.updateSubjectName("apo")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.subjectSuggestions).containsExactly("Apoquel")
    }

  @Test
  fun aSubjectLongerThanTheColumnAllowsIsRejected() =
    runTest(dispatcher) {
      val repository = FakeTaskRepository()
      val viewModel = viewModel(repository)
      advanceUntilIdle()

      viewModel.updateKind(TaskKind.MEDICATION)
      viewModel.updateSubjectName("a".repeat(101))
      viewModel.saveTask()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.subjectError)
        .isEqualTo(context.getString(R.string.task_validation_subject_max_length))
      assertThat(repository.saved).isEmpty()
    }

  @Test
  fun editingATaskRestoresItsStoredSubject() =
    runTest(dispatcher) {
      val stored =
        Task(
          id = "task-1",
          kind = TaskKind.DEWORMING,
          subjectCode = DewormingType.INTERNAL.name,
          subjectName = "Drontal",
          title = "Vermífugo",
          scheduledFor = java.time.LocalDateTime.of(2026, 1, 1, 9, 0),
          createdAt = 1L,
          updatedAt = 1L,
        )
      val repository = FakeTaskRepository(storedTask = stored)
      val viewModel =
        TaskFormViewModel(
          savedStateHandle = SavedStateHandle(mapOf("taskId" to stored.id)),
          context = context,
          taskRepository = repository,
          petRepository = FakePetRepository(),
          taskScheduler = NoOpTaskScheduler(),
        )
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.subjectCode).isEqualTo(DewormingType.INTERNAL.name)
      assertThat(viewModel.uiState.value.subjectName).isEqualTo("Drontal")

      viewModel.updateSubjectName("Drontal Plus")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.title).isEqualTo("Vermífugo")
    }

  private fun viewModel(repository: TaskRepository = FakeTaskRepository()) =
    TaskFormViewModel(
      savedStateHandle = SavedStateHandle(),
      context = context,
      taskRepository = repository,
      petRepository = FakePetRepository(),
      taskScheduler = NoOpTaskScheduler(),
    )

  private class FakeTaskRepository(
    private val usedSubjectNames: List<String> = emptyList(),
    private val storedTask: Task? = null,
  ) : TaskRepository {
    val saved = mutableListOf<Task>()

    override fun getPendingTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getAllActiveTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksForPet(petId: String): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getTaskById(id: String): Task? = storedTask?.takeIf { it.id == id }

    override fun getTasksDueToday(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueThisWeek(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueThisMonth(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> =
      MutableStateFlow(emptyList())

    override fun getNextTasks(limit: Int): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getPastDueTasks(): List<Task> = emptyList()

    override fun getCompletedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun saveTask(task: Task) {
      saved += task
    }

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) = Unit

    override suspend fun deleteTask(id: String) = Unit

    override suspend fun deleteTasksByReferenceEntity(entityId: String) = Unit

    override suspend fun getUsedSubjectNames(kind: TaskKind, petId: String?): List<String> =
      usedSubjectNames
  }

  private class FakePetRepository : PetRepository {
    private val pet =
      Pet(
        id = "pet-1",
        name = "Mimi",
        petType = PetType.CAT,
        sex = Sex.UNKNOWN,
        createdAt = 1L,
        updatedAt = 1L,
      )

    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(listOf(pet))

    override suspend fun getPetById(id: String): Pet? = pet.takeIf { it.id == id }

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(pet)

    override fun getPetCount(): Flow<Int> = MutableStateFlow(1)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class NoOpTaskScheduler : TaskScheduler {
    override fun scheduleTask(task: Task) = Unit

    override fun cancelTask(taskId: String) = Unit

    override fun cancelAllTasks() = Unit
  }
}
