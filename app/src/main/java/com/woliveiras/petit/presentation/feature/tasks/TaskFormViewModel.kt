package com.woliveiras.petit.presentation.feature.tasks

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.presentation.util.rethrowIfCancellation
import com.woliveiras.petit.presentation.util.uiFailureText
import com.woliveiras.petit.worker.TaskScheduler
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
  val scheduledDate: LocalDateTime = LocalDateTime.now().plusHours(1),
  val availablePets: List<Pet> = emptyList(),
  val isSaving: Boolean = false,
  val titleError: String? = null,
  val dateError: String? = null,
  val descriptionError: String? = null,
)

/** Events emitted by TaskFormViewModel. */
sealed class TaskFormEvent {
  data object TaskSaved : TaskFormEvent()

  data object TaskDeleted : TaskFormEvent()

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
) : ViewModel() {

  private val taskId: String? = savedStateHandle.get<String>("taskId")

  private val _uiState = MutableStateFlow(TaskFormUiState())
  val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<TaskFormEvent>()
  val events: SharedFlow<TaskFormEvent> = _events.asSharedFlow()

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
        _uiState.update {
          it.copy(
            isEditMode = true,
            editingTaskId = task.id,
            title = task.title,
            description = task.description ?: "",
            selectedPetId = task.petId,
            selectedPetName = petName,
            kind = task.kind,
            scheduledDate = task.scheduledFor,
          )
        }
      }
    }
  }

  fun updateTitle(value: String) {
    _uiState.update { it.copy(title = value, titleError = null) }
  }

  fun updateDescription(value: String) {
    _uiState.update { it.copy(description = value, descriptionError = null) }
  }

  fun updateSelectedPet(petId: String?, petName: String?) {
    _uiState.update { it.copy(selectedPetId = petId, selectedPetName = petName) }
  }

  fun updateKind(kind: TaskKind) {
    _uiState.update { it.copy(kind = kind) }
  }

  fun updateScheduledDate(date: LocalDateTime) {
    _uiState.update { it.copy(scheduledDate = date, dateError = null) }
  }

  fun saveTask() {
    val state = _uiState.value

    if (state.title.isBlank()) {
      _uiState.update {
        it.copy(titleError = context.getString(R.string.task_validation_title_required))
      }
      return
    }

    if (state.title.length > 100) {
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

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true) }

      try {
        val now = System.currentTimeMillis()
        val existingTask =
          if (state.isEditMode) {
            taskRepository.getTaskById(state.editingTaskId!!)
          } else null

        val task =
          Task(
            id = state.editingTaskId ?: UUID.randomUUID().toString(),
            petId = state.selectedPetId,
            kind = state.kind,
            referenceEntityId = null,
            title = state.title.trim(),
            description = state.description.trim().ifBlank { null },
            scheduledFor = state.scheduledDate,
            status = TaskStatus.PENDING,
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
}
