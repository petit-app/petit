package com.woliveiras.petit.presentation.feature.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.presentation.util.AppDisplayFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for task settings screen. */
data class TaskSettingsUiState(
  val isLoading: Boolean = true,
  val vaccinationEnabled: Boolean = true,
  val vaccinationDaysBefore: Int = 7,
  val dewormingEnabled: Boolean = true,
  val dewormingDaysBefore: Int = 7,
  val weightEnabled: Boolean = false,
  val weightIntervalDays: Int = 30,
  val notificationHour: Int = 9,
  val notificationMinute: Int = 0,
  val notificationTimeFormatted: String = "",
  val showTimePicker: Boolean = false,
)

@HiltViewModel
class TaskSettingsViewModel
@Inject
constructor(
  @ApplicationContext context: Context,
  private val reminderPreferencesRepository: ReminderPreferencesRepository,
) : ViewModel() {

  private val displayFormatter = AppDisplayFormatter(context)
  private val _uiState = MutableStateFlow(TaskSettingsUiState())
  val uiState: StateFlow<TaskSettingsUiState> = _uiState.asStateFlow()

  init {
    loadPreferences()
  }

  private fun loadPreferences() {
    viewModelScope.launch {
      reminderPreferencesRepository.preferences.collect { prefs ->
        _uiState.update {
          it.copy(
            isLoading = false,
            vaccinationEnabled = prefs.vaccinationRemindersEnabled,
            vaccinationDaysBefore = prefs.vaccinationDaysBefore,
            dewormingEnabled = prefs.dewormingRemindersEnabled,
            dewormingDaysBefore = prefs.dewormingDaysBefore,
            weightEnabled = prefs.weightRemindersEnabled,
            weightIntervalDays = prefs.weightReminderIntervalDays,
            notificationHour = prefs.defaultNotificationHour,
            notificationMinute = prefs.defaultNotificationMinute,
            notificationTimeFormatted =
              displayFormatter.time(
                LocalTime.of(prefs.defaultNotificationHour, prefs.defaultNotificationMinute)
              ),
          )
        }
      }
    }
  }

  fun toggleVaccinationTasks(enabled: Boolean) {
    _uiState.update { it.copy(vaccinationEnabled = enabled) }
    viewModelScope.launch {
      reminderPreferencesRepository.updateVaccinationSettings(
        enabled,
        _uiState.value.vaccinationDaysBefore,
      )
    }
  }

  fun updateVaccinationDaysBefore(days: Int) {
    _uiState.update { it.copy(vaccinationDaysBefore = days) }
    viewModelScope.launch {
      reminderPreferencesRepository.updateVaccinationSettings(
        _uiState.value.vaccinationEnabled,
        days,
      )
    }
  }

  fun toggleDewormingTasks(enabled: Boolean) {
    _uiState.update { it.copy(dewormingEnabled = enabled) }
    viewModelScope.launch {
      reminderPreferencesRepository.updateDewormingSettings(
        enabled,
        _uiState.value.dewormingDaysBefore,
      )
    }
  }

  fun updateDewormingDaysBefore(days: Int) {
    _uiState.update { it.copy(dewormingDaysBefore = days) }
    viewModelScope.launch {
      reminderPreferencesRepository.updateDewormingSettings(_uiState.value.dewormingEnabled, days)
    }
  }

  fun toggleWeightTasks(enabled: Boolean) {
    _uiState.update { it.copy(weightEnabled = enabled) }
    viewModelScope.launch {
      reminderPreferencesRepository.updateWeightSettings(enabled, _uiState.value.weightIntervalDays)
    }
  }

  fun updateWeightIntervalDays(days: Int) {
    _uiState.update { it.copy(weightIntervalDays = days) }
    viewModelScope.launch {
      reminderPreferencesRepository.updateWeightSettings(_uiState.value.weightEnabled, days)
    }
  }

  fun showTimePicker() {
    _uiState.update { it.copy(showTimePicker = true) }
  }

  fun hideTimePicker() {
    _uiState.update { it.copy(showTimePicker = false) }
  }

  fun updateNotificationTime(hour: Int, minute: Int) {
    _uiState.update {
      it.copy(
        notificationHour = hour,
        notificationMinute = minute,
        notificationTimeFormatted = displayFormatter.time(LocalTime.of(hour, minute)),
        showTimePicker = false,
      )
    }
    viewModelScope.launch { reminderPreferencesRepository.updateNotificationTime(hour, minute) }
  }
}
