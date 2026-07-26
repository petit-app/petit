package com.woliveiras.petit.presentation.feature.vaccination

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.SpeciesCareCatalog
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.VaccinationDraft
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccinationValidationError
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.domain.model.validate
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

/** UI state for vaccination screens. */
data class VaccinationUiState(
  val petId: String = "",
  val petName: String = "",
  val petType: PetType = PetType.OTHER,
  val today: LocalDate = LocalDate.now(),
  val isLoading: Boolean = true,
  val latestVaccinations: List<VaccinationEntry> = emptyList(),
  val allVaccinations: List<VaccinationEntry> = emptyList(),
  val form: VaccinationFormState = VaccinationFormState(),
) {
  /** Vaccinations grouped by type, showing only the latest of each. */
  val vaccinationsByType: Map<VaccineType, VaccinationEntry>
    get() = latestVaccinations.associateBy { it.vaccineType }

  /** Vaccine types available for this pet's species. */
  val availableVaccineTypes: List<VaccineType>
    get() {
      val catalogTypes = SpeciesCareCatalog.vaccinePresets(petType).map { it.vaccineType }
      val historicalType = form.historicalVaccineType
      return if (
        historicalType != null &&
          form.vaccineType == historicalType &&
          historicalType !in catalogTypes
      ) {
        catalogTypes + historicalType
      } else {
        catalogTypes
      }
    }
}

/** Form-specific state for vaccination entry creation/editing. */
data class VaccinationFormState(
  val isEditMode: Boolean = false,
  val editingEntryId: String? = null,
  val editingCreatedAt: Long? = null,
  val vaccineType: VaccineType = VaccineType.OTHER,
  val historicalVaccineType: VaccineType? = null,
  val customName: String = "",
  val applicationDate: LocalDate = LocalDate.now(),
  val nextDueDate: LocalDate? = null,
  val veterinarian: String = "",
  val clinic: String = "",
  val batchNumber: String = "",
  val note: String = "",
  val isSaving: Boolean = false,
  val vaccineTypeError: String? = null,
  val applicationDateError: String? = null,
  val veterinarianError: String? = null,
  val clinicError: String? = null,
  val batchNumberError: String? = null,
  val noteError: String? = null,
  val customNameError: String? = null,
)

/** Events emitted by vaccination ViewModel. */
sealed class VaccinationEvent {
  data class VaccinationSaved(val petId: String) : VaccinationEvent()

  data class VaccinationDeleted(val petId: String) : VaccinationEvent()

  data class Error(val message: String) : VaccinationEvent()
}

@HiltViewModel
class VaccinationViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val vaccinationRepository: VaccinationEntryRepository,
  private val autoTaskService: AutoTaskService,
  private val clock: Clock,
) : ViewModel() {

  private val petId: String = savedStateHandle.get<String>("petId") ?: ""

  private val _uiState =
    MutableStateFlow(
      VaccinationUiState(
        petId = petId,
        today = LocalDate.now(clock),
        form = VaccinationFormState(applicationDate = LocalDate.now(clock)),
      )
    )
  val uiState: StateFlow<VaccinationUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<VaccinationEvent>()
  val events: SharedFlow<VaccinationEvent> = _events.asSharedFlow()
  private var entryLoadedForEdit: VaccinationEntry? = null

  init {
    loadPetInfo()
    loadVaccinations()
  }

  private fun loadPetInfo() {
    viewModelScope.launch {
      petRepository.getPetById(petId)?.let { pet ->
        _uiState.update { state ->
          val availableTypes = SpeciesCareCatalog.vaccinePresets(pet.petType).map { it.vaccineType }
          val selectedType =
            state.form.vaccineType.takeIf { state.form.isEditMode } ?: availableTypes.first()
          state.copy(
            petName = pet.name,
            petType = pet.petType,
            form = state.form.copy(vaccineType = selectedType),
          )
        }
      }
    }
  }

  private fun loadVaccinations() {
    viewModelScope.launch {
      vaccinationRepository.getLatestVaccinationsForPet(petId).collect { latest ->
        _uiState.update { it.copy(latestVaccinations = latest, isLoading = false) }
      }
    }
    viewModelScope.launch {
      vaccinationRepository.getVaccinationEntriesForPet(petId).collect { all ->
        _uiState.update { it.copy(allVaccinations = all) }
      }
    }
  }

  // ===== Form methods =====

  fun updateVaccineType(type: VaccineType) {
    _uiState.update { state ->
      val form = state.form
      if (type !in state.availableVaccineTypes) return@update state
      state.copy(
        form =
          form.copy(
            vaccineType = type,
            customName = if (type == VaccineType.OTHER) form.customName else "",
            vaccineTypeError = null,
            customNameError = null,
          )
      )
    }
  }

  fun updateCustomName(name: String) {
    _uiState.update { it.copy(form = it.form.copy(customName = name, customNameError = null)) }
  }

  fun updateApplicationDate(date: LocalDate) {
    _uiState.update { state ->
      val form = state.form
      state.copy(form = form.copy(applicationDate = date, applicationDateError = null))
    }
  }

  fun updateNextDueDate(date: LocalDate?) {
    _uiState.update { it.copy(form = it.form.copy(nextDueDate = date)) }
  }

  fun updateVeterinarian(value: String) {
    _uiState.update { it.copy(form = it.form.copy(veterinarian = value, veterinarianError = null)) }
  }

  fun updateClinic(value: String) {
    _uiState.update { it.copy(form = it.form.copy(clinic = value, clinicError = null)) }
  }

  fun updateBatchNumber(value: String) {
    _uiState.update { it.copy(form = it.form.copy(batchNumber = value, batchNumberError = null)) }
  }

  fun updateNote(value: String) {
    _uiState.update { it.copy(form = it.form.copy(note = value, noteError = null)) }
  }

  /** Load an existing entry for editing. */
  fun loadEntryForEdit(entryId: String) {
    viewModelScope.launch {
      val entry = vaccinationRepository.getVaccinationEntryById(entryId)
      if (entry != null) {
        entryLoadedForEdit = entry
        _uiState.update {
          it.copy(
            form =
              it.form.copy(
                isEditMode = true,
                editingEntryId = entry.id,
                editingCreatedAt = entry.createdAt,
                vaccineType = entry.vaccineType,
                historicalVaccineType = entry.vaccineType,
                customName = entry.customVaccineTypeName ?: "",
                applicationDate = entry.applicationDate,
                nextDueDate = entry.nextDueDate,
                veterinarian = entry.veterinarian ?: "",
                clinic = entry.clinic ?: "",
                batchNumber = entry.batchNumber ?: "",
                note = entry.note ?: "",
              )
          )
        }
      }
    }
  }

  fun resetForm() {
    entryLoadedForEdit = null
    _uiState.update { state -> state.copy(form = newFormFor(state.petType)) }
  }

  private fun newFormFor(petType: PetType) =
    VaccinationFormState(
      vaccineType = SpeciesCareCatalog.vaccinePresets(petType).first().vaccineType,
      applicationDate = LocalDate.now(clock),
    )

  fun saveVaccination() {
    val state = _uiState.value
    val form = state.form

    val validationErrors =
      VaccinationDraft(
          petType = state.petType,
          vaccineType = form.vaccineType,
          historicalVaccineType = form.historicalVaccineType,
          customName = form.customName,
          applicationDate = form.applicationDate,
          nextDueDate = form.nextDueDate,
          veterinarian = form.veterinarian,
          clinic = form.clinic,
          batchNumber = form.batchNumber,
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
          VaccinationEntry(
            id = form.editingEntryId ?: UUID.randomUUID().toString(),
            petId = petId,
            vaccineType = form.vaccineType,
            customVaccineTypeName = customVaccineNameForSave(form),
            applicationDate = form.applicationDate,
            nextDueDate = form.nextDueDate,
            veterinarian = form.veterinarian.trim().ifBlank { null },
            clinic = form.clinic.trim().ifBlank { null },
            batchNumber = form.batchNumber.trim().ifBlank { null },
            note = form.note.trim().ifBlank { null },
            createdAt = form.editingCreatedAt ?: now,
            updatedAt = now,
            syncStatus = SyncStatus.LOCAL_ONLY,
          )

        vaccinationRepository.saveVaccinationEntry(entry)

        // Create/update automatic task if nextDueDate is set
        try {
          autoTaskService.handleVaccinationSaved(entry)
        } catch (failure: Exception) {
          failure.rethrowIfCancellation()
          // DB saved but auto-task failed — non-critical
        }

        resetForm()

        _events.emit(VaccinationEvent.VaccinationSaved(petId))
      } catch (e: Exception) {
        _events.emit(
          VaccinationEvent.Error(e.uiFailureText(context, R.string.vaccination_error_save))
        )
      } finally {
        _uiState.update { it.copy(form = it.form.copy(isSaving = false)) }
      }
    }
  }

  private fun customVaccineNameForSave(form: VaccinationFormState): String? {
    if (form.vaccineType != VaccineType.OTHER) return null
    val original = entryLoadedForEdit
    return if (
      form.isEditMode &&
        original?.vaccineType == VaccineType.OTHER &&
        form.customName == original.customVaccineTypeName.orEmpty()
    ) {
      original.customVaccineTypeName
    } else {
      form.customName.trim().ifBlank { null }
    }
  }

  private fun applyValidationErrors(errors: List<VaccinationValidationError>) {
    _uiState.update { state ->
      state.copy(
        form =
          state.form.copy(
            vaccineTypeError =
              context.getString(R.string.vaccination_error_type_not_applicable).takeIf {
                VaccinationValidationError.VACCINE_TYPE_NOT_APPLICABLE in errors
              },
            customNameError =
              when {
                VaccinationValidationError.CUSTOM_NAME_REQUIRED in errors ->
                  context.getString(R.string.vaccination_error_custom_name_required)
                VaccinationValidationError.CUSTOM_NAME_TOO_LONG in errors ->
                  context.getString(R.string.vaccination_error_field_max_length, 100)
                else -> null
              },
            applicationDateError =
              when {
                VaccinationValidationError.APPLICATION_DATE_IN_FUTURE in errors ->
                  context.getString(R.string.vaccination_error_date_future)
                VaccinationValidationError.NEXT_DUE_DATE_NOT_AFTER_APPLICATION in errors ->
                  context.getString(R.string.vaccination_error_next_dose_after)
                else -> null
              },
            veterinarianError =
              context.getString(R.string.vaccination_error_field_max_length, 100).takeIf {
                VaccinationValidationError.VETERINARIAN_TOO_LONG in errors
              },
            clinicError =
              context.getString(R.string.vaccination_error_field_max_length, 100).takeIf {
                VaccinationValidationError.CLINIC_TOO_LONG in errors
              },
            batchNumberError =
              context.getString(R.string.vaccination_error_field_max_length, 50).takeIf {
                VaccinationValidationError.BATCH_NUMBER_TOO_LONG in errors
              },
            noteError =
              context.getString(R.string.vaccination_error_note_max_length).takeIf {
                VaccinationValidationError.NOTE_TOO_LONG in errors
              },
          )
      )
    }
  }

  fun deleteEntry(entryId: String) {
    viewModelScope.launch {
      try {
        vaccinationRepository.deleteVaccinationEntry(entryId)
        autoTaskService.handleVaccinationDeleted(entryId)
      } catch (e: Exception) {
        _events.emit(
          VaccinationEvent.Error(e.uiFailureText(context, R.string.vaccination_error_delete))
        )
      }
    }
  }

  /** Delete the currently editing entry and navigate back. */
  fun deleteCurrentEntry() {
    val entryId = _uiState.value.form.editingEntryId ?: return
    viewModelScope.launch {
      try {
        vaccinationRepository.deleteVaccinationEntry(entryId)
        autoTaskService.handleVaccinationDeleted(entryId)
        _events.emit(VaccinationEvent.VaccinationDeleted(petId))
      } catch (e: Exception) {
        _events.emit(
          VaccinationEvent.Error(e.uiFailureText(context, R.string.vaccination_error_delete))
        )
      }
    }
  }
}
