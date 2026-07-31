package com.woliveiras.petit.presentation.feature.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.presentation.util.TaskDisplayTextResolver
import com.woliveiras.petit.worker.TaskScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for completed tasks screen. */
data class CompletedTasksUiState(val isLoading: Boolean = true, val tasks: List<Task> = emptyList())

/** Events emitted by CompletedTasksViewModel. */
sealed class CompletedTasksEvent {
  data class Error(val message: String) : CompletedTasksEvent()
}

@HiltViewModel
class CompletedTasksViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val taskRepository: TaskRepository,
  private val taskScheduler: TaskScheduler,
  private val taskDisplayTextResolver: TaskDisplayTextResolver,
  private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(CompletedTasksUiState())
  val uiState: StateFlow<CompletedTasksUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<CompletedTasksEvent>()
  val events: SharedFlow<CompletedTasksEvent> = _events.asSharedFlow()

  init {
    loadCompletedTasks()
  }

  private fun loadCompletedTasks() {
    viewModelScope.launch {
      combine(
          taskRepository.getCompletedTasks(),
          userPreferencesRepository.userPreferences
            .map { preferences -> preferences.language }
            .distinctUntilChanged(),
        ) { tasks, language ->
          tasks to language
        }
        .collect { (tasks, language) ->
          _uiState.update { it.copy(isLoading = false, tasks = displayTasks(tasks, language)) }
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

  fun reactivateTask(taskId: String) {
    viewModelScope.launch {
      try {
        val task = taskRepository.getTaskById(taskId)
        if (task == null) {
          _events.emit(CompletedTasksEvent.Error(context.getString(R.string.task_error_reactivate)))
          return@launch
        }
        // A completed occurrence of a series already has a pending successor. Reactivating it would
        // leave two pending rows sharing the same rule, so the series would fork.
        if (task.isRecurring) {
          _events.emit(
            CompletedTasksEvent.Error(context.getString(R.string.task_error_reactivate_series))
          )
          return@launch
        }
        taskRepository.updateTaskStatus(taskId, TaskStatus.PENDING)
        // Reschedule notification
        taskScheduler.scheduleTask(task.copy(status = TaskStatus.PENDING))
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        _events.emit(CompletedTasksEvent.Error(context.getString(R.string.task_error_reactivate)))
      }
    }
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch {
      try {
        taskRepository.deleteTask(taskId)
      } catch (_: Exception) {
        _events.emit(CompletedTasksEvent.Error(context.getString(R.string.task_error_delete)))
      }
    }
  }
}
