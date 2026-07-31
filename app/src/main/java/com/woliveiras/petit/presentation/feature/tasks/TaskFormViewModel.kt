package com.woliveiras.petit.presentation.feature.tasks

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.domain.model.TaskSubjectControl
import com.woliveiras.petit.domain.model.TaskSubjectOptions
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.presentation.util.rethrowIfCancellation
import com.woliveiras.petit.presentation.util.taskSubjectLabel
import com.woliveiras.petit.presentation.util.uiFailureText
import com.woliveiras.petit.worker.TaskScheduler
import com.woliveiras.petit.worker.TaskSeriesCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for task form screen. */
data class TaskFormUiState(
  val isEditMode: Boolean = false,
  val editingTaskId: String? = null,
  val title: String = "",
  val description: String = "",
  val selectedPetId: String? = null,
  val selectedPetName: String? = null,
  val kind: TaskKind = TaskKind.CUSTOM,
  val subjectCode: String? = null,
  val subjectName: String = "",
  val subjectSuggestions: List<String> = emptyList(),
  val scheduledDate: LocalDateTime = LocalDateTime.now().plusHours(1),
  val repeat: TaskRepeatFormState = TaskRepeatFormState(),
  val isAutomaticTask: Boolean = false,
  val availablePets: List<Pet> = emptyList(),
  val isSaving: Boolean = false,
  val titleError: String? = null,
  val dateError: String? = null,
  val descriptionError: String? = null,
  val subjectError: String? = null,
  val repeatError: String? = null,
) {
  val subjectControl: TaskSubjectControl
    get() = TaskSubjectOptions.controlFor(kind)

  val vaccineOptions: List<VaccineType>
    get() =
      TaskSubjectOptions.vaccineOptions(
        availablePets.firstOrNull { it.id == selectedPetId }?.petType
      )

  val antiparasiticOptions: List<DewormingType>
    get() = TaskSubjectOptions.antiparasiticOptions()

  val requiresSubjectFreeText: Boolean
    get() = TaskSubjectOptions.requiresFreeText(subjectControl, subjectCode)

  /** Automatic health tasks follow the health record, so they never expose repeat controls. */
  val showsRepeatControls: Boolean
    get() = !isAutomaticTask

  /** Only a saved repeating task can be stopped. */
  val canStopSeries: Boolean
    get() = isEditMode && !isAutomaticTask && repeat.repeats
}

/** Events emitted by TaskFormViewModel. */
sealed class TaskFormEvent {
  data object TaskSaved : TaskFormEvent()

  data object TaskDeleted : TaskFormEvent()

  data object SeriesStopped : TaskFormEvent()

  data class Error(val message: String) : TaskFormEvent()
}

@HiltViewModel
class TaskFormViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val taskRepository: TaskRepository,
  private val petRepository: PetRepository,
  private val taskScheduler: TaskScheduler,
  private val taskSeriesCoordinator: TaskSeriesCoordinator,
) : ViewModel() {

  private val taskId: String? = savedStateHandle.get<String>("taskId")

  private val _uiState = MutableStateFlow(TaskFormUiState())
  val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<TaskFormEvent>()
  val events: SharedFlow<TaskFormEvent> = _events.asSharedFlow()

  /** Once the caregiver writes a title, the subject never overwrites it. */
  private var titleEditedByCaregiver = false
  private var usedSubjectNames: List<String> = emptyList()

  init {
    loadPets()
    if (taskId != null) {
      loadTaskForEdit(taskId)
    }
  }

  private fun loadPets() {
    viewModelScope.launch {
      petRepository.getAllPets().collect { pets ->
        _uiState.update { it.copy(availablePets = pets) }
      }
    }
  }

  private fun loadTaskForEdit(taskId: String) {
    viewModelScope.launch {
      val task = taskRepository.getTaskById(taskId)
      if (task != null) {
        val petName = task.petId?.let { petId -> petRepository.getPetById(petId)?.name }
        titleEditedByCaregiver = true
        _uiState.update {
          it.copy(
            isEditMode = true,
            editingTaskId = task.id,
            title = task.title,
            description = task.description ?: "",
            selectedPetId = task.petId,
            selectedPetName = petName,
            kind = task.kind,
            subjectCode = task.subjectCode,
            subjectName = task.subjectName ?: "",
            scheduledDate = task.scheduledFor,
            repeat = TaskRepeatFormState.of(task.recurrence),
            isAutomaticTask = task.isAutomatic,
          )
        }
        loadSubjectSuggestions()
      }
    }
  }

  fun updateTitle(value: String) {
    titleEditedByCaregiver = value.isNotBlank()
    _uiState.update { it.copy(title = value, titleError = null) }
  }

  fun updateDescription(value: String) {
    _uiState.update { it.copy(description = value, descriptionError = null) }
  }

  fun updateSelectedPet(petId: String?, petName: String?) {
    _uiState.update { state ->
      val updated = state.copy(selectedPetId = petId, selectedPetName = petName)
      val keepsSubject =
        updated.subjectControl != TaskSubjectControl.VACCINE ||
          updated.subjectCode == null ||
          updated.subjectCode in updated.vaccineOptions.map(VaccineType::name)
      (if (keepsSubject) updated else updated.clearSubject()).withPrefilledTitle()
    }
    loadSubjectSuggestions()
  }

  fun updateKind(kind: TaskKind) {
    _uiState.update { state ->
      val sameControl = TaskSubjectOptions.controlFor(kind) == state.subjectControl
      state
        .copy(kind = kind)
        .let { if (sameControl) it else it.clearSubject() }
        .withPrefilledTitle()
    }
    loadSubjectSuggestions()
  }

  fun updateSubjectCode(code: String?) {
    _uiState.update { state ->
      val keepsName =
        state.subjectControl != TaskSubjectControl.VACCINE || code == VaccineType.OTHER.name
      state
        .copy(
          subjectCode = code,
          subjectName = if (keepsName) state.subjectName else "",
          subjectError = null,
        )
        .withPrefilledTitle()
    }
  }

  fun updateSubjectName(value: String) {
    _uiState.update { state ->
      state
        .copy(
          subjectName = value,
          subjectError = null,
          subjectSuggestions = TaskSubjectOptions.matchingSuggestions(value, usedSubjectNames),
        )
        .withPrefilledTitle()
    }
  }

  fun updateScheduledDate(date: LocalDateTime) {
    _uiState.update { it.copy(scheduledDate = date, dateError = null) }
  }

  fun updateRepeat(repeat: TaskRepeatFormState) {
    _uiState.update { it.copy(repeat = repeat, repeatError = null) }
  }

  fun updateRepeatPreset(preset: RepeatPreset) {
    _uiState.update { state ->
      val repeat = state.repeat
      state.copy(
        repeat =
          repeat.copy(
            preset = preset,
            unit = preset.unit ?: repeat.unit,
            interval = if (preset.unit == null) repeat.interval else preset.interval.toString(),
          ),
        repeatError = null,
      )
    }
  }

  fun saveTask() {
    val state = _uiState.value

    if (state.subjectControl != TaskSubjectControl.NONE) {
      if (state.requiresSubjectFreeText && state.subjectName.isBlank()) {
        _uiState.update {
          it.copy(subjectError = context.getString(R.string.task_validation_subject_required))
        }
        return
      }
      if (state.subjectName.trim().length > MAX_SUBJECT_LENGTH) {
        _uiState.update {
          it.copy(subjectError = context.getString(R.string.task_validation_subject_max_length))
        }
        return
      }
    }

    if (state.title.isBlank()) {
      _uiState.update {
        it.copy(titleError = context.getString(R.string.task_validation_title_required))
      }
      return
    }

    if (state.title.length > MAX_TITLE_LENGTH) {
      _uiState.update {
        it.copy(titleError = context.getString(R.string.task_validation_title_max_length))
      }
      return
    }

    if (state.description.length > 500) {
      _uiState.update {
        it.copy(
          descriptionError = context.getString(R.string.task_validation_description_max_length)
        )
      }
      return
    }

    if (state.scheduledDate.isBefore(LocalDateTime.now())) {
      _uiState.update {
        it.copy(dateError = context.getString(R.string.task_validation_date_future))
      }
      return
    }

    if (state.repeat.repeats && !state.repeat.isValidFor(state.scheduledDate)) {
      _uiState.update {
        it.copy(repeatError = context.getString(R.string.repeat_validation_invalid))
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true) }

      try {
        val now = System.currentTimeMillis()
        val existingTask =
          if (state.isEditMode) {
            taskRepository.getTaskById(state.editingTaskId!!)
          } else null

        // A changed rule or a changed start restarts the series position, so an end condition
        // counted in occurrences starts over instead of ending the series immediately.
        val keepsSeriesPosition =
          existingTask != null &&
            existingTask.recurrence == state.repeat.recurrence &&
            existingTask.scheduledFor == state.scheduledDate

        val task =
          Task(
            id = state.editingTaskId ?: UUID.randomUUID().toString(),
            petId = state.selectedPetId,
            kind = state.kind,
            referenceEntityId = null,
            subjectCode =
              state.subjectCode.takeIf { state.subjectControl != TaskSubjectControl.NONE },
            subjectName =
              state.subjectName
                .trim()
                .ifBlank { null }
                ?.takeIf { state.subjectControl != TaskSubjectControl.NONE },
            title = state.title.trim(),
            description = state.description.trim().ifBlank { null },
            scheduledFor = state.scheduledDate,
            status = TaskStatus.PENDING,
            recurrence = state.repeat.recurrence,
            seriesId = existingTask?.seriesId,
            occurrenceIndex = if (keepsSeriesPosition) existingTask.occurrenceIndex else 0,
            createdAt = existingTask?.createdAt ?: now,
            updatedAt = now,
          )

        taskRepository.saveTask(task)

        try {
          taskScheduler.scheduleTask(task)
        } catch (failure: Exception) {
          failure.rethrowIfCancellation()
          // DB saved but schedule failed — non-critical
        }

        _events.emit(TaskFormEvent.TaskSaved)
      } catch (e: Exception) {
        _events.emit(TaskFormEvent.Error(e.uiFailureText(context, R.string.task_error_save)))
      } finally {
        _uiState.update { it.copy(isSaving = false) }
      }
    }
  }

  fun deleteTask() {
    val id = _uiState.value.editingTaskId ?: return
    viewModelScope.launch {
      try {
        taskScheduler.cancelTask(id)
        taskRepository.deleteTask(id)
        _events.emit(TaskFormEvent.TaskDeleted)
      } catch (e: Exception) {
        _events.emit(TaskFormEvent.Error(e.uiFailureText(context, R.string.task_error_delete)))
      }
    }
  }

  fun stopSeries() {
    val id = _uiState.value.editingTaskId ?: return
    viewModelScope.launch {
      try {
        taskSeriesCoordinator.stopSeries(id)
        _events.emit(TaskFormEvent.SeriesStopped)
      } catch (e: Exception) {
        _events.emit(TaskFormEvent.Error(e.uiFailureText(context, R.string.task_error_delete)))
      }
    }
  }

  private fun loadSubjectSuggestions() {
    val state = _uiState.value
    if (state.subjectControl != TaskSubjectControl.MEDICATION) {
      usedSubjectNames = emptyList()
      _uiState.update { it.copy(subjectSuggestions = emptyList()) }
      return
    }
    viewModelScope.launch {
      usedSubjectNames =
        try {
          taskRepository.getUsedSubjectNames(state.kind, state.selectedPetId)
        } catch (failure: Exception) {
          failure.rethrowIfCancellation()
          emptyList()
        }
      _uiState.update {
        it.copy(
          subjectSuggestions =
            TaskSubjectOptions.matchingSuggestions(it.subjectName, usedSubjectNames)
        )
      }
    }
  }

  private fun TaskFormUiState.clearSubject(): TaskFormUiState =
    copy(
      subjectCode = null,
      subjectName = "",
      subjectError = null,
      subjectSuggestions = emptyList(),
    )

  private fun TaskFormUiState.withPrefilledTitle(): TaskFormUiState =
    if (titleEditedByCaregiver) this
    else
      copy(
        title =
          taskSubjectLabel(context, kind, subjectCode, subjectName).orEmpty().take(MAX_TITLE_LENGTH)
      )

  private companion object {
    /** Both caps match the column limits validated on import. */
    const val MAX_TITLE_LENGTH = 100
    const val MAX_SUBJECT_LENGTH = 100
  }
}
