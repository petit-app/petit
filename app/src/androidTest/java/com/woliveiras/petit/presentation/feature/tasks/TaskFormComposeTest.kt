package com.woliveiras.petit.presentation.feature.tasks

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskRecurrence
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.ui.theme.PetitTheme
import com.woliveiras.petit.worker.TaskScheduler
import com.woliveiras.petit.worker.TaskSeriesCoordinator
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.TextStyle
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

  @Test
  fun medicationTasksAskWhichMedicineItIs() {
    composeRule.setContent { PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = vm()) } }
    composeRule.waitForIdle()

    selectKind(context.getString(R.string.task_kind_medication))

    composeRule
      .onNodeWithText(context.getString(R.string.task_field_subject_medication))
      .assertIsDisplayed()
  }

  @Test
  fun customTasksDoNotAskForASubject() {
    composeRule.setContent { PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = vm()) } }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.task_field_subject_medication))
      .assertDoesNotExist()
    composeRule
      .onNodeWithText(context.getString(R.string.task_field_subject_vaccine))
      .assertDoesNotExist()
  }

  @Test
  fun medicationSuggestionsAreLabelledAndFillTheFieldWhenPicked() {
    composeRule.setContent {
      PetitTheme {
        TaskFormScreen(onNavigateBack = {}, viewModel = vm(usedSubjectNames = listOf("Apoquel")))
      }
    }
    composeRule.waitForIdle()

    selectKind(context.getString(R.string.task_kind_medication))
    composeRule
      .onNodeWithText(context.getString(R.string.task_field_subject_medication))
      .performTextInput("apo")
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.task_field_subject_suggestions))
      .assertIsDisplayed()

    composeRule.onNodeWithText("Apoquel").performClick()
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.task_field_subject_medication))
      .assertTextContains("Apoquel")
  }

  @Test
  fun theOtherVaccineAsksForItsName() {
    val viewModel = vm()
    composeRule.setContent {
      PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = viewModel) }
    }
    composeRule.waitForIdle()

    selectKind(context.getString(R.string.task_kind_vaccination))

    composeRule
      .onNodeWithText(context.getString(R.string.vaccination_field_custom_name))
      .assertDoesNotExist()

    composeRule.runOnUiThread { viewModel.updateSubjectCode(VaccineType.OTHER.name) }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.vaccination_field_custom_name))
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun newTasksStartWithoutARepeatRule() {
    composeRule.setContent { PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = vm()) } }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.task_field_repeat))
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithText(context.getString(R.string.repeat_preset_none)).assertIsDisplayed()
    composeRule.onNodeWithText(context.getString(R.string.repeat_field_end)).assertDoesNotExist()
  }

  @Test
  fun pickingACadenceExplainsItInPlainLanguage() {
    val viewModel = vm()
    composeRule.setContent {
      PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = viewModel) }
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      viewModel.updateRepeat(
        TaskRepeatFormState(
          preset = RepeatPreset.CUSTOM,
          interval = "3",
          unit = RecurrenceUnit.WEEKS,
        )
      )
    }
    composeRule.waitForIdle()

    val summary = context.resources.getQuantityString(R.plurals.repeat_summary_weeks, 3, 3)
    composeRule.onNodeWithText(summary).performScrollTo().assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.repeat_field_end))
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun theCustomCadenceOpensTheIntervalEditor() {
    val viewModel = vm()
    composeRule.setContent {
      PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = viewModel) }
    }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.repeat_field_interval))
      .assertDoesNotExist()

    composeRule.runOnUiThread { viewModel.updateRepeatPreset(RepeatPreset.CUSTOM) }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.repeat_field_interval))
      .performScrollTo()
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.repeat_field_unit))
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun anHourlyCadenceOffersTheDailyWindow() {
    val viewModel = vm()
    composeRule.setContent {
      PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = viewModel) }
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      viewModel.updateRepeatPreset(RepeatPreset.CUSTOM)
      viewModel.updateRepeat(
        TaskRepeatFormState(
          preset = RepeatPreset.CUSTOM,
          interval = "8",
          unit = RecurrenceUnit.HOURS,
        )
      )
    }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.repeat_field_window))
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun weekdayChipsAnnounceTheirFullDayName() {
    val viewModel = vm()
    composeRule.setContent {
      PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = viewModel) }
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread { viewModel.updateRepeatPreset(RepeatPreset.WEEKLY) }
    composeRule.waitForIdle()

    val monday =
      DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, context.resources.configuration.locales[0])
    composeRule
      .onNodeWithContentDescription(monday, substring = true)
      .performScrollTo()
      .assertHasClickAction()
  }

  @Test
  fun editingARepeatingTaskOffersToStopTheSeriesAndAsksForConfirmation() {
    val repeating =
      Task(
        id = "task-1",
        kind = TaskKind.CUSTOM,
        title = "Passear",
        scheduledFor = LocalDateTime.now().plusDays(1),
        recurrence = TaskRecurrence(interval = 1, unit = RecurrenceUnit.DAYS),
        seriesId = "series-1",
        createdAt = 1L,
        updatedAt = 1L,
      )
    composeRule.setContent {
      PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = vm(editing = repeating)) }
    }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.task_repeat_stop))
      .performScrollTo()
      .performClick()
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.task_repeat_stop_confirm_title))
      .assertIsDisplayed()
  }

  @Test
  fun automaticTasksExplainThatTheirScheduleComesFromTheHealthRecord() {
    val automatic =
      Task(
        id = "auto_vacc_entry-1",
        kind = TaskKind.VACCINATION,
        title = "Vacina",
        scheduledFor = LocalDateTime.now().plusDays(1),
        createdAt = 1L,
        updatedAt = 1L,
      )
    composeRule.setContent {
      PetitTheme { TaskFormScreen(onNavigateBack = {}, viewModel = vm(editing = automatic)) }
    }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.task_auto_repeat_explanation))
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithText(context.getString(R.string.task_field_repeat)).assertDoesNotExist()
  }

  private fun selectKind(kindLabel: String) {
    composeRule.onNodeWithText(context.getString(R.string.task_field_kind)).performClick()
    composeRule.onNodeWithText(kindLabel).performClick()
    composeRule.waitForIdle()
  }

  private fun vm(usedSubjectNames: List<String> = emptyList(), editing: Task? = null) =
    TaskFormViewModel(
      savedStateHandle =
        SavedStateHandle(editing?.let { mapOf("taskId" to it.id) } ?: emptyMap<String, Any>()),
      context = ApplicationProvider.getApplicationContext(),
      taskRepository = FormTaskRepository(usedSubjectNames, editing),
      petRepository = FormPetRepository(),
      taskScheduler = FormTaskScheduler(),
      taskSeriesCoordinator = FormTaskSeriesCoordinator(),
    )

  private class FormTaskRepository(
    private val usedSubjectNames: List<String> = emptyList(),
    private val editing: Task? = null,
  ) : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    override fun getPendingTasks(): Flow<List<Task>> = tasks

    override fun getAllActiveTasks(): Flow<List<Task>> = tasks

    override fun getTasksForPet(petId: String): Flow<List<Task>> = tasks

    override suspend fun getTaskById(id: String): Task? = editing?.takeIf { it.id == id }

    override fun getTasksDueToday(): Flow<List<Task>> = tasks

    override fun getTasksDueThisWeek(): Flow<List<Task>> = tasks

    override fun getTasksDueThisMonth(): Flow<List<Task>> = tasks

    override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> = tasks

    override fun getNextTasks(limit: Int): Flow<List<Task>> = tasks

    override suspend fun getPastDueTasks(): List<Task> = emptyList()

    override suspend fun getPendingRecurringTasks(): List<Task> = emptyList()

    override fun getCompletedTasks(): Flow<List<Task>> = tasks

    override suspend fun saveTask(task: Task) = Unit

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) = Unit

    override suspend fun deleteTask(id: String) = Unit

    override suspend fun deleteTasksByReferenceEntity(entityId: String) = Unit

    override suspend fun getUsedSubjectNames(kind: TaskKind, petId: String?): List<String> =
      usedSubjectNames
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

    override fun scheduleTaskAt(taskId: String, scheduledFor: java.time.LocalDateTime) = Unit

    override fun cancelTask(taskId: String) = Unit

    override fun cancelAllTasks() = Unit
  }

  private class FormTaskSeriesCoordinator : TaskSeriesCoordinator {
    override suspend fun completeOccurrence(taskId: String) = Unit

    override suspend fun stopSeries(taskId: String) = Unit

    override suspend fun onNotificationDelivered(task: Task): Task = task

    override fun scheduleFollowUp(task: Task) = Unit

    override suspend fun reconcilePendingSeries() = Unit
  }
}
