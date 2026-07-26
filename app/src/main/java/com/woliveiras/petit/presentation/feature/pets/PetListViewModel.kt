package com.woliveiras.petit.presentation.feature.pets

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.domain.usecase.GetPetHealthSummaryAction
import com.woliveiras.petit.presentation.util.rethrowIfCancellation
import com.woliveiras.petit.presentation.util.uiFailureText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Pet with additional details for display. */
data class PetListItem(
  val pet: Pet,
  val latestWeight: WeightEntry? = null,
  val nextVaccineType: VaccineType? = null,
  val nextVaccinationDate: LocalDate? = null,
  val nextDewormingType: DewormingType? = null,
  val nextDewormingDate: LocalDate? = null,
)

/** UI State for Pet List screen. */
data class PetListUiState(
  val isLoading: Boolean = true,
  val pets: List<PetListItem> = emptyList(),
  val isEmpty: Boolean = false,
)

sealed class PetListEvent {
  data class Error(val message: String) : PetListEvent()
}

@HiltViewModel
class PetListViewModel
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val getPetHealthSummary: GetPetHealthSummaryAction,
) : ViewModel() {

  private val _uiState = MutableStateFlow(PetListUiState())
  val uiState: StateFlow<PetListUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<PetListEvent>()
  val events: SharedFlow<PetListEvent> = _events.asSharedFlow()

  init {
    loadPets()
  }

  private fun loadPets() {
    viewModelScope.launch {
      try {
        petRepository.getAllPets().collect { pets ->
          if (pets.isEmpty()) {
            _uiState.value = PetListUiState(isLoading = false, isEmpty = true)
            return@collect
          }

          val petsWithDetails =
            pets.map { pet ->
              try {
                loadPetDetails(pet)
              } catch (failure: Exception) {
                failure.rethrowIfCancellation()
                PetListItem(pet = pet) // show pet without health summary on error
              }
            }
          _uiState.value =
            PetListUiState(isLoading = false, pets = petsWithDetails, isEmpty = false)
        }
      } catch (e: Exception) {
        _uiState.value = PetListUiState(isLoading = false, isEmpty = true)
        _events.emit(PetListEvent.Error(e.uiFailureText(context, R.string.pet_error_load)))
      }
    }
  }

  private suspend fun loadPetDetails(pet: Pet): PetListItem {
    val summary = getPetHealthSummary.execute(pet.id)

    return PetListItem(
      pet = pet,
      latestWeight = summary.latestWeight,
      nextVaccineType = summary.nextVaccineType,
      nextVaccinationDate = summary.nextVaccinationDate,
      nextDewormingType = summary.nextDewormingType,
      nextDewormingDate = summary.nextDewormingDate,
    )
  }
}
