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
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.WeightEntry
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** UI State for Pet Detail screen. */
data class PetDetailUiState(
  val isLoading: Boolean = true,
  val pet: Pet? = null,
  val latestWeight: WeightEntry? = null,
  val weightRecordCount: Int = 0,
  val vaccinationRecordCount: Int = 0,
  val dewormingRecordCount: Int = 0,
  val vaccinationStatus: HealthStatus = HealthStatus.OK,
  val dewormingStatus: HealthStatus = HealthStatus.OK,
  val nextVaccinationDays: Long? = null,
  val nextDewormingDays: Long? = null,
  val error: String? = null,
)

sealed class PetDetailEvent {
  data object PetDeleted : PetDetailEvent()

  data class Error(val message: String) : PetDetailEvent()
}

@HiltViewModel
class PetDetailViewModel
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

  private val _uiState = MutableStateFlow(PetDetailUiState())
  val uiState: StateFlow<PetDetailUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<PetDetailEvent>()
  val events: SharedFlow<PetDetailEvent> = _events.asSharedFlow()

  init {
    loadPetDetails()
  }

  private fun loadPetDetails() {
    viewModelScope.launch {
      try {
        combine(
            petRepository.getPetByIdFlow(petId),
            weightEntryRepository.getWeightEntriesForPet(petId),
            vaccinationEntryRepository.getLatestVaccinationsForPet(petId),
            dewormingEntryRepository.getLatestDewormingsForPet(petId),
          ) { pet, weightEntries, vaccinations, dewormings ->
            if (pet == null) {
              return@combine PetDetailUiState(
                isLoading = false,
                error = context.getString(R.string.pet_error_not_found),
              )
            }

            val latestWeight = weightEntries.maxByOrNull { it.date }
            val vaccinationStatus = getWorstStatus(vaccinations.map { it.status })
            val dewormingStatus = getWorstStatus(dewormings.map { it.status })

            val nextVaccinationDays =
              vaccinations
                .mapNotNull { it.nextDueDate }
                .minOrNull()
                ?.let { java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), it) }

            val nextDewormingDays =
              dewormings
                .mapNotNull { it.nextDueDate }
                .minOrNull()
                ?.let { java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), it) }

            PetDetailUiState(
              isLoading = false,
              pet = pet,
              latestWeight = latestWeight,
              weightRecordCount = weightEntries.size,
              vaccinationRecordCount = vaccinations.size,
              dewormingRecordCount = dewormings.size,
              vaccinationStatus = vaccinationStatus,
              dewormingStatus = dewormingStatus,
              nextVaccinationDays = nextVaccinationDays,
              nextDewormingDays = nextDewormingDays,
            )
          }
          .collect { state -> _uiState.value = state }
      } catch (e: Exception) {
        _uiState.value =
          _uiState.value.copy(
            isLoading = false,
            error = e.uiFailureText(context, R.string.pet_error_load_data),
          )
      }
    }
  }
}

/** Gets the worst (highest priority) HealthStatus from a list. OVERDUE > SCHEDULED > OK */
private fun getWorstStatus(statuses: List<HealthStatus>): HealthStatus {
  return statuses.maxByOrNull { status ->
    when (status) {
      HealthStatus.OVERDUE -> 2
      HealthStatus.SCHEDULED -> 1
      HealthStatus.OK -> 0
    }
  } ?: HealthStatus.OK
}
