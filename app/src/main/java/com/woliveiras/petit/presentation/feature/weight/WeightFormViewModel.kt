package com.woliveiras.petit.presentation.feature.weight

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.presentation.util.AppDisplayFormatter
import com.woliveiras.petit.presentation.util.rethrowIfCancellation
import com.woliveiras.petit.presentation.util.uiFailureText
import com.woliveiras.petit.worker.AutoTaskService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
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

enum class WeightUnit(val label: String, val suffix: String) {
  KG("kg", "kg"),
  GRAMS("g", "g"),
}

private val decimalInputPattern = Regex("^-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)$")

data class WeightFormUiState(
  val petId: String = "",
  val petName: String = "",
  val weightValue: String = "",
  val weightUnit: WeightUnit = WeightUnit.KG,
  val date: LocalDate = LocalDate.now(),
  val note: String = "",
  val isLoading: Boolean = false,
  val isSaving: Boolean = false,
  val isEditMode: Boolean = false,
  val editingEntryId: String? = null,
  val weightEntries: List<WeightEntry> = emptyList(),
  val weightError: String? = null,
  val dateError: String? = null,
  val noteError: String? = null,
)

sealed class WeightFormEvent {
  data class WeightSaved(val petId: String) : WeightFormEvent()

  data class WeightDeleted(val petId: String) : WeightFormEvent()

  data class Error(val message: String) : WeightFormEvent()
}

@HiltViewModel
class WeightFormViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val weightEntryRepository: WeightEntryRepository,
  private val autoTaskService: AutoTaskService,
  private val clock: Clock,
) : ViewModel() {

  private val petId: String = savedStateHandle.get<String>("petId") ?: ""

  private val _uiState =
    MutableStateFlow(WeightFormUiState(petId = petId, date = LocalDate.now(clock)))
  val uiState: StateFlow<WeightFormUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<WeightFormEvent>()
  val events: SharedFlow<WeightFormEvent> = _events.asSharedFlow()

  init {
    loadPetInfo()
    loadWeightEntries()
  }

  private fun loadPetInfo() {
    viewModelScope.launch {
      petRepository.getPetById(petId)?.let { pet ->
        _uiState.update { it.copy(petName = pet.name) }
      }
    }
  }

  private fun loadWeightEntries() {
    viewModelScope.launch {
      weightEntryRepository.getWeightEntriesForPet(petId).collect { entries ->
        _uiState.update { it.copy(weightEntries = entries) }
      }
    }
  }

  fun updateWeight(weight: String) {
    val normalized = weight.replace(',', '.')

    if (normalized.isEmpty() || normalized == "-" || decimalInputPattern.matches(normalized)) {
      _uiState.update { it.copy(weightValue = normalized, weightError = null) }
    }
  }

  fun updateWeightUnit(unit: WeightUnit) {
    _uiState.update { it.copy(weightUnit = unit) }
  }

  fun updateDate(date: LocalDate) {
    _uiState.update { it.copy(date = date, dateError = null) }
  }

  fun updateNote(note: String) {
    _uiState.update { it.copy(note = note, noteError = null) }
  }

  fun editEntry(entry: WeightEntry) {
    _uiState.update {
      it.copy(
        isEditMode = true,
        editingEntryId = entry.id,
        weightValue = AppDisplayFormatter(context).decimal(entry.weightKg, fractionDigits = 2),
        weightUnit = WeightUnit.KG,
        date = entry.date,
        note = entry.note ?: "",
      )
    }
  }

  /** Load a weight entry by ID for editing */
  fun loadEntryForEdit(entryId: String) {
    viewModelScope.launch {
      val entry = weightEntryRepository.getWeightEntryById(entryId)
      if (entry != null) {
        editEntry(entry)
      }
    }
  }

  fun cancelEdit() {
    _uiState.update {
      it.copy(
        isEditMode = false,
        editingEntryId = null,
        weightValue = "",
        weightUnit = WeightUnit.KG,
        date = LocalDate.now(clock),
        note = "",
      )
    }
  }

  fun saveWeight() {
    val state = _uiState.value

    // Validate and convert weight to grams
    val weightValue = AppDisplayFormatter(context).parseDecimal(state.weightValue)

    // Validation based on unit
    val maxWeight = if (state.weightUnit == WeightUnit.KG) 50.0 else 50000.0
    if (
      weightValue == null || !weightValue.isFinite() || weightValue <= 0 || weightValue > maxWeight
    ) {
      _uiState.update { it.copy(weightError = context.getString(R.string.weight_error_invalid)) }
      return
    }

    if (state.date.isAfter(LocalDate.now(clock))) {
      _uiState.update { it.copy(dateError = context.getString(R.string.weight_error_date_future)) }
      return
    }

    if (state.note.length > 500) {
      _uiState.update {
        it.copy(noteError = context.getString(R.string.weight_error_note_max_length))
      }
      return
    }

    // Convert to grams
    val weightGrams =
      when (state.weightUnit) {
        WeightUnit.KG -> (weightValue * 1000).toInt()
        WeightUnit.GRAMS -> weightValue.toInt()
      }

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true) }

      try {
        val now = clock.millis()

        val entry =
          WeightEntry(
            id = state.editingEntryId ?: UUID.randomUUID().toString(),
            petId = petId,
            date = state.date,
            weightGrams = weightGrams,
            note = state.note.trim().ifBlank { null },
            createdAt =
              if (state.isEditMode) {
                state.weightEntries.find { it.id == state.editingEntryId }?.createdAt ?: now
              } else {
                now
              },
            updatedAt = now,
            syncStatus = SyncStatus.LOCAL_ONLY,
          )

        weightEntryRepository.saveWeightEntry(entry)

        // Create automatic task for next weighing
        try {
          autoTaskService.handleWeightSaved(petId, _uiState.value.petName)
        } catch (failure: Exception) {
          failure.rethrowIfCancellation()
          // DB saved but auto-task failed — non-critical
        }

        _uiState.update {
          it.copy(
            isEditMode = false,
            editingEntryId = null,
            weightValue = "",
            weightUnit = WeightUnit.KG,
            date = LocalDate.now(clock),
            note = "",
          )
        }

        _events.emit(WeightFormEvent.WeightSaved(petId))
      } catch (e: Exception) {
        _events.emit(WeightFormEvent.Error(e.uiFailureText(context, R.string.weight_error_save)))
      } finally {
        _uiState.update { it.copy(isSaving = false) }
      }
    }
  }

  fun deleteEntry(entryId: String) {
    viewModelScope.launch {
      try {
        weightEntryRepository.deleteWeightEntry(entryId)
        if (_uiState.value.editingEntryId == entryId) {
          cancelEdit()
        }
      } catch (e: Exception) {
        _events.emit(WeightFormEvent.Error(e.uiFailureText(context, R.string.weight_error_delete)))
      }
    }
  }

  /** Delete the currently editing entry and navigate back. */
  fun deleteCurrentEntry() {
    val entryId = _uiState.value.editingEntryId ?: return
    viewModelScope.launch {
      try {
        weightEntryRepository.deleteWeightEntry(entryId)
        _events.emit(WeightFormEvent.WeightDeleted(petId))
      } catch (e: Exception) {
        _events.emit(WeightFormEvent.Error(e.uiFailureText(context, R.string.weight_error_delete)))
      }
    }
  }
}
