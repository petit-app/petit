package com.woliveiras.petit.presentation.feature.pets

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.presentation.util.uiFailureText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PetDeleteConfirmationUiState(
  val isLoading: Boolean = true,
  val pet: Pet? = null,
  val petName: String = "",
  val weightEntriesCount: Int = 0,
  val vaccinationEntriesCount: Int = 0,
  val dewormingEntriesCount: Int = 0,
  val isDeleting: Boolean = false,
  val isDeleted: Boolean = false,
  val error: String? = null,
) {
  val totalRecordsCount: Int
    get() = weightEntriesCount + vaccinationEntriesCount + dewormingEntriesCount
}

sealed class PetDeleteConfirmationEvent {
  data object PetDeleted : PetDeleteConfirmationEvent()

  data class Error(val message: String) : PetDeleteConfirmationEvent()
}

@HiltViewModel
class PetDeleteConfirmationViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val weightEntryRepository: WeightEntryRepository,
  private val vaccinationEntryRepository: VaccinationEntryRepository,
  private val dewormingEntryRepository: DewormingEntryRepository,
) : ViewModel() {

  private val petId: String = checkNotNull(savedStateHandle["petId"])

  private val _uiState = MutableStateFlow(PetDeleteConfirmationUiState())
  val uiState: StateFlow<PetDeleteConfirmationUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<PetDeleteConfirmationEvent>()
  val events: SharedFlow<PetDeleteConfirmationEvent> = _events.asSharedFlow()

  init {
    loadPetData()
  }

  private fun loadPetData() {
    viewModelScope.launch {
      try {
        val pet = petRepository.getPetById(petId)
        val weightCount = weightEntryRepository.countEntriesForPet(petId)
        val vaccinationCount = vaccinationEntryRepository.countEntriesForPet(petId)
        val dewormingCount = dewormingEntryRepository.countEntriesForPet(petId)

        _uiState.value =
          _uiState.value.copy(
            isLoading = false,
            pet = pet,
            petName = pet?.name ?: "",
            weightEntriesCount = weightCount,
            vaccinationEntriesCount = vaccinationCount,
            dewormingEntriesCount = dewormingCount,
          )
      } catch (e: Exception) {
        _uiState.value =
          _uiState.value.copy(
            isLoading = false,
            error = e.uiFailureText(context, R.string.pet_error_delete_load),
          )
      }
    }
  }

  fun deletePet() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isDeleting = true)
      try {
        petRepository.deletePet(petId)
        _uiState.value = _uiState.value.copy(isDeleted = true)
      } catch (e: Exception) {
        _events.emit(
          PetDeleteConfirmationEvent.Error(e.uiFailureText(context, R.string.pet_error_delete))
        )
      } finally {
        _uiState.value = _uiState.value.copy(isDeleting = false)
      }
    }
  }
}
