package com.woliveiras.petit.presentation.feature.deworming

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.DewormingDraft
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.DewormingValidationError
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.validate
import com.woliveiras.petit.worker.AutoTaskService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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

/** UI state for deworming screens. */
data class DewormingUiState(
  val petId: String = "",
  val petName: String = "",
  val today: LocalDate = LocalDate.now(),
  val isLoading: Boolean = true,
  val latestDewormings: List<DewormingEntry> = emptyList(),
  val allDewormings: List<DewormingEntry> = emptyList(),
  val form: DewormingFormState = DewormingFormState(),
) {
  /** Dewormings grouped by type, showing only the latest of each. */
  val dewormingsByType: Map<DewormingType, DewormingEntry>
    get() = latestDewormings.associateBy { it.type }
}

/** Form-specific state for deworming entry creation/editing. */
data class DewormingFormState(
  val isEditMode: Boolean = false,
  val editingEntryId: String? = null,
  val editingCreatedAt: Long? = null,
  val dewormingType: DewormingType = DewormingType.INTERNAL,
  val medication: String = "",
  val applicationDate: LocalDate = LocalDate.now(),
  val nextDueDate: LocalDate? = null,
  val selectedMonthlyInterval: Int = DewormingType.INTERNAL.defaultIntervalMonths,
  val isIntervalCustom: Boolean = false,
  val customIntervalValue: String = "",
  val customIntervalUnit: DewormingIntervalUnit = DewormingIntervalUnit.MONTHLY,
  val note: String = "",
  val isSaving: Boolean = false,
  val medicationError: String? = null,
  val applicationDateError: String? = null,
  val noteError: String? = null,
)

enum class DewormingIntervalUnit {
  DAILY,
  WEEKLY,
  MONTHLY,
}

/** Events emitted by deworming ViewModel. */
sealed class DewormingEvent {
  data class DewormingSaved(val petId: String) : DewormingEvent()

  data class DewormingDeleted(val petId: String) : DewormingEvent()

  data class Error(val message: String) : DewormingEvent()
}

@HiltViewModel
class DewormingViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val dewormingRepository: DewormingEntryRepository,
  private val autoTaskService: AutoTaskService,
  private val clock: Clock,
) : ViewModel() {

  private val petId: String = savedStateHandle.get<String>("petId") ?: ""

  private val _uiState =
    MutableStateFlow(
      DewormingUiState(
        petId = petId,
        today = LocalDate.now(clock),
        form = DewormingFormState(applicationDate = LocalDate.now(clock)),
      )
    )
  val uiState: StateFlow<DewormingUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<DewormingEvent>()
  val events: SharedFlow<DewormingEvent> = _events.asSharedFlow()

  init {
    loadPetInfo()
    loadDewormings()
  }

  private fun loadPetInfo() {
    viewModelScope.launch {
      petRepository.getPetById(petId)?.let { pet ->
        _uiState.update { it.copy(petName = pet.name) }
      }
    }
  }

  private fun loadDewormings() {
    viewModelScope.launch {
      dewormingRepository.getLatestDewormingsForPet(petId).collect { latest ->
        _uiState.update { it.copy(latestDewormings = latest, isLoading = false) }
      }
    }
    viewModelScope.launch {
      dewormingRepository.getDewormingEntriesForPet(petId).collect { all ->
        _uiState.update { it.copy(allDewormings = all) }
      }
    }
  }

  // ===== Form methods =====

  fun updateDewormingType(type: DewormingType) {
    _uiState.update { state ->
      val form = state.form
      // Auto-suggest next dose date based on deworming interval
      val updatedForm =
        if (!form.isIntervalCustom) {
          form.copy(dewormingType = type, selectedMonthlyInterval = type.defaultIntervalMonths)
        } else {
          form.copy(dewormingType = type)
        }
      val suggestedNextDue =
        if (!form.isEditMode || form.nextDueDate != null) updatedForm.calculateIntervalDueDate()
        else null
      state.copy(form = updatedForm.copy(nextDueDate = suggestedNextDue))
    }
  }

  fun updateMedication(medication: String) {
    _uiState.update {
      it.copy(form = it.form.copy(medication = medication, medicationError = null))
    }
  }

  fun updateApplicationDate(date: LocalDate) {
    _uiState.update { state ->
      val form = state.form
      val updatedForm = form.copy(applicationDate = date, applicationDateError = null)
      state.copy(form = updatedForm.copy(nextDueDate = updatedForm.calculateIntervalDueDate()))
    }
  }

  fun updateNextDueDate(date: LocalDate?) {
    _uiState.update { state ->
      state.copy(form = state.form.copy(nextDueDate = date).withIntervalFromDueDate())
    }
  }

  fun updateMonthlyInterval(months: Int) {
    _uiState.update { state ->
      val form =
        state.form.copy(
          selectedMonthlyInterval = months,
          isIntervalCustom = false,
          customIntervalValue = "",
          customIntervalUnit = DewormingIntervalUnit.MONTHLY,
        )
      state.copy(form = form.copy(nextDueDate = form.calculateIntervalDueDate()))
    }
  }

  fun selectCustomInterval() {
    _uiState.update { state ->
      state.copy(
        form =
          state.form.copy(isIntervalCustom = true, customIntervalValue = "", nextDueDate = null)
      )
    }
  }

  fun updateCustomIntervalValue(value: String) {
    _uiState.update { state ->
      val digits = value.filter { it.isDigit() }
      val form = state.form.copy(customIntervalValue = digits, isIntervalCustom = true)
      state.copy(form = form.copy(nextDueDate = form.calculateIntervalDueDate()))
    }
  }

  fun updateCustomIntervalUnit(unit: DewormingIntervalUnit) {
    _uiState.update { state ->
      val form = state.form.copy(customIntervalUnit = unit, isIntervalCustom = true)
      state.copy(form = form.copy(nextDueDate = form.calculateIntervalDueDate()))
    }
  }

  fun updateNote(value: String) {
    _uiState.update { it.copy(form = it.form.copy(note = value, noteError = null)) }
  }

  /** Load an existing entry for editing. */
  fun loadEntryForEdit(entryId: String) {
    viewModelScope.launch {
      val entry = dewormingRepository.getDewormingEntryById(entryId)
      if (entry != null) {
        _uiState.update {
          it.copy(
            form =
              it.form
                .copy(
                  isEditMode = true,
                  editingEntryId = entry.id,
                  editingCreatedAt = entry.createdAt,
                  dewormingType = entry.type,
                  medication = entry.medication ?: "",
                  applicationDate = entry.applicationDate,
                  nextDueDate = entry.nextDueDate,
                  note = entry.note ?: "",
                )
                .withIntervalFromDueDate()
          )
        }
      }
    }
  }

  fun resetForm() {
    _uiState.update { it.copy(form = DewormingFormState(applicationDate = LocalDate.now(clock))) }
  }

  fun saveDeworming() {
    val form = _uiState.value.form

    val validationErrors =
      DewormingDraft(
          type = form.dewormingType,
          medication = form.medication,
          applicationDate = form.applicationDate,
          nextDueDate = form.nextDueDate,
          note = form.note,
        )
        .validate(clock)
    if (validationErrors.isNotEmpty()) {
      applyValidationErrors(validationErrors)
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(form = it.form.copy(isSaving = true)) }

      try {
        val now = clock.millis()

        val entry =
          DewormingEntry(
            id = form.editingEntryId ?: UUID.randomUUID().toString(),
            petId = petId,
            type = form.dewormingType,
            medication = form.medication.trim(),
            applicationDate = form.applicationDate,
            nextDueDate = form.nextDueDate,
            note = form.note.trim().ifBlank { null },
            createdAt = form.editingCreatedAt ?: now,
            updatedAt = now,
            syncStatus = SyncStatus.LOCAL_ONLY,
          )

        dewormingRepository.saveDewormingEntry(entry)

        // Create/update automatic task if nextDueDate is set
        try {
          autoTaskService.handleDewormingSaved(entry)
        } catch (_: Exception) {
          // DB saved but auto-task failed — non-critical
        }

        _uiState.update {
          it.copy(form = DewormingFormState(applicationDate = LocalDate.now(clock)))
        }

        _events.emit(DewormingEvent.DewormingSaved(petId))
      } catch (e: Exception) {
        _events.emit(
          DewormingEvent.Error(e.message ?: context.getString(R.string.deworming_error_save))
        )
      } finally {
        _uiState.update { it.copy(form = it.form.copy(isSaving = false)) }
      }
    }
  }

  private fun applyValidationErrors(errors: List<DewormingValidationError>) {
    _uiState.update { state ->
      state.copy(
        form =
          state.form.copy(
            medicationError =
              when {
                DewormingValidationError.MEDICATION_REQUIRED in errors ->
                  context.getString(R.string.deworming_error_medication_required)
                DewormingValidationError.MEDICATION_TOO_LONG in errors ->
                  context.getString(R.string.deworming_error_medication_max_length)
                else -> null
              },
            applicationDateError =
              when {
                DewormingValidationError.APPLICATION_DATE_IN_FUTURE in errors ->
                  context.getString(R.string.deworming_error_date_future)
                DewormingValidationError.NEXT_DUE_DATE_NOT_AFTER_APPLICATION in errors ->
                  context.getString(R.string.deworming_error_next_dose_after)
                else -> null
              },
            noteError =
              context.getString(R.string.deworming_error_note_max_length).takeIf {
                DewormingValidationError.NOTE_TOO_LONG in errors
              },
          )
      )
    }
  }

  fun deleteEntry(entryId: String) {
    viewModelScope.launch {
      try {
        dewormingRepository.deleteDewormingEntry(entryId)
        autoTaskService.handleDewormingDeleted(entryId)
      } catch (e: Exception) {
        _events.emit(
          DewormingEvent.Error(e.message ?: context.getString(R.string.deworming_error_delete))
        )
      }
    }
  }

  /** Delete the currently editing entry and navigate back. */
  fun deleteCurrentEntry() {
    val entryId = _uiState.value.form.editingEntryId ?: return
    viewModelScope.launch {
      try {
        dewormingRepository.deleteDewormingEntry(entryId)
        autoTaskService.handleDewormingDeleted(entryId)
        _events.emit(DewormingEvent.DewormingDeleted(petId))
      } catch (e: Exception) {
        _events.emit(
          DewormingEvent.Error(e.message ?: context.getString(R.string.deworming_error_delete))
        )
      }
    }
  }
}

private fun DewormingFormState.calculateIntervalDueDate(): LocalDate? {
  if (!isIntervalCustom) {
    return applicationDate.plusMonths(selectedMonthlyInterval.toLong())
  }

  val value = customIntervalValue.toLongOrNull()?.takeIf { it > 0 } ?: return null
  return when (customIntervalUnit) {
    DewormingIntervalUnit.DAILY -> applicationDate.plusDays(value)
    DewormingIntervalUnit.WEEKLY -> applicationDate.plusWeeks(value)
    DewormingIntervalUnit.MONTHLY -> applicationDate.plusMonths(value)
  }
}

private fun DewormingFormState.withIntervalFromDueDate(): DewormingFormState {
  val dueDate = nextDueDate ?: return this
  for (months in 1..6) {
    if (applicationDate.plusMonths(months.toLong()) == dueDate) {
      return copy(
        selectedMonthlyInterval = months,
        isIntervalCustom = false,
        customIntervalValue = "",
        customIntervalUnit = DewormingIntervalUnit.MONTHLY,
      )
    }
  }

  val totalMonths = ChronoUnit.MONTHS.between(applicationDate, dueDate)
  if (totalMonths > 0 && applicationDate.plusMonths(totalMonths) == dueDate) {
    return copy(
      isIntervalCustom = true,
      customIntervalValue = totalMonths.toString(),
      customIntervalUnit = DewormingIntervalUnit.MONTHLY,
    )
  }

  val totalWeeks = ChronoUnit.WEEKS.between(applicationDate, dueDate)
  if (totalWeeks > 0 && applicationDate.plusWeeks(totalWeeks) == dueDate) {
    return copy(
      isIntervalCustom = true,
      customIntervalValue = totalWeeks.toString(),
      customIntervalUnit = DewormingIntervalUnit.WEEKLY,
    )
  }

  val totalDays = ChronoUnit.DAYS.between(applicationDate, dueDate).coerceAtLeast(0)
  return copy(
    isIntervalCustom = true,
    customIntervalValue = totalDays.toString(),
    customIntervalUnit = DewormingIntervalUnit.DAILY,
  )
}
