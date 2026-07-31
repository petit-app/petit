package com.woliveiras.petit.presentation.feature.tasks

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.ui.theme.PetitTheme
import com.woliveiras.petit.worker.TaskScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskFormComposeTest {

  @get:Rule val composeRule = createComposeRule()

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun scheduleFieldsAnnounceThemselvesAsTappableInsteadOfDisabled() {
    composeRule.setContent { PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = vm()) } }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithContentDescription(context.getString(R.string.task_field_date), substring = true)
      .assertHasClickAction()
    composeRule
      .onNodeWithContentDescription(context.getString(R.string.task_field_time), substring = true)
      .assertHasClickAction()
  }

  @Test
  fun tappingTheDateFieldOpensTheDatePicker() {
    composeRule.setContent { PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = vm()) } }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithContentDescription(context.getString(R.string.task_field_date), substring = true)
      .performClick()

    composeRule.onNodeWithText(context.getString(R.string.action_ok)).assertIsDisplayed()
  }

  @Test
  fun tappingTheTimeFieldOpensTheTimePicker() {
    composeRule.setContent { PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = vm()) } }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithContentDescription(context.getString(R.string.task_field_time), substring = true)
      .performClick()

    composeRule.onNodeWithText(context.getString(R.string.action_ok)).assertIsDisplayed()
  }

  private fun vm() =
    TaskFormViewModel(
      savedStateHandle = SavedStateHandle(),
      context = ApplicationProvider.getApplicationContext(),
      taskRepository = FormTaskRepository(),
      petRepository = FormPetRepository(),
      taskScheduler = FormTaskScheduler(),
    )

  private class FormTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    override fun getPendingTasks(): Flow<List<Task>> = tasks

    override fun getAllActiveTasks(): Flow<List<Task>> = tasks

    override fun getTasksForPet(petId: String): Flow<List<Task>> = tasks

    override suspend fun getTaskById(id: String): Task? = null

    override fun getTasksDueToday(): Flow<List<Task>> = tasks

    override fun getTasksDueThisWeek(): Flow<List<Task>> = tasks

    override fun getTasksDueThisMonth(): Flow<List<Task>> = tasks

    override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> = tasks

    override fun getNextTasks(limit: Int): Flow<List<Task>> = tasks

    override suspend fun getPastDueTasks(): List<Task> = emptyList()

    override fun getCompletedTasks(): Flow<List<Task>> = tasks

    override suspend fun saveTask(task: Task) = Unit

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) = Unit

    override suspend fun deleteTask(id: String) = Unit

    override suspend fun deleteTasksByReferenceEntity(entityId: String) = Unit
  }

  private class FormPetRepository : PetRepository {
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

  private class FormTaskScheduler : TaskScheduler {
    override fun scheduleTask(task: Task) = Unit

    override fun cancelTask(taskId: String) = Unit

    override fun cancelAllTasks() = Unit
  }
}
