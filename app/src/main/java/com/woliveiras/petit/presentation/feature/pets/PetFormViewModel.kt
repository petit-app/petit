package com.woliveiras.petit.presentation.feature.pets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.media.PendingCameraPhoto
import com.woliveiras.petit.data.media.PetPhotoStore
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.di.IoDispatcher
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI State for Pet Form screen. */
data class PetFormUiState(
  val isLoading: Boolean = false,
  val isEditMode: Boolean = false,
  val petId: String? = null,
  val petType: PetType = PetType.OTHER,
  val name: String = "",
  val birthDate: LocalDate? = null,
  val sex: Sex = Sex.UNKNOWN,
  val breed: String = "",
  val color: String = "",
  val microchipNumber: String = "",
  val passportNumber: String = "",
  val notes: String = "",
  val photoUri: String? = null,
  val nameError: String? = null,
  val breedError: String? = null,
  val colorError: String? = null,
  val microchipError: String? = null,
  val passportError: String? = null,
  val notesError: String? = null,
  val birthDateError: String? = null,
  val isSaving: Boolean = false,
)

sealed class PetFormEvent {
  data class PetSaved(val petId: String) : PetFormEvent()

  data class Error(val message: String) : PetFormEvent()

  data class LaunchCamera(val uri: Uri) : PetFormEvent()
}

@HiltViewModel
class PetFormViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val photoStorage: PetPhotoStore,
  private val clock: Clock,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

  private val petId: String? = savedStateHandle["petId"]
  private var pendingCameraPhoto: PendingCameraPhoto? = null

  private val _uiState = MutableStateFlow(PetFormUiState())
  val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<PetFormEvent>()
  val events: SharedFlow<PetFormEvent> = _events.asSharedFlow()

  init {
    if (petId != null) {
      loadPet(petId)
    }
  }

  private fun loadPet(petId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val pet = petRepository.getPetById(petId)
        if (pet != null) {
          _uiState.value =
            _uiState.value.copy(
              isLoading = false,
              isEditMode = true,
              petId = pet.id,
              petType = pet.petType,
              name = pet.name,
              birthDate = pet.birthDate,
              sex = pet.sex,
              breed = pet.breed ?: "",
              color = pet.color ?: "",
              microchipNumber = pet.microchipNumber ?: "",
              passportNumber = pet.passportNumber ?: "",
              notes = pet.notes ?: "",
              photoUri = pet.photoUri,
            )
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false)
        _events.emit(PetFormEvent.Error(e.message ?: context.getString(R.string.pet_error_load)))
      }
    }
  }

  fun updateName(name: String) {
    _uiState.value =
      _uiState.value.copy(
        name = name,
        nameError =
          if (name.isBlank()) context.getString(R.string.pet_validation_name_required) else null,
      )
  }

  fun updateBirthDate(date: LocalDate?) {
    _uiState.value = _uiState.value.copy(birthDate = date, birthDateError = null)
  }

  fun updatePetType(petType: PetType) {
    _uiState.value = _uiState.value.copy(petType = petType, breed = "")
  }

  fun updateSex(sex: Sex) {
    _uiState.value = _uiState.value.copy(sex = sex)
  }

  fun updateBreed(breed: String) {
    _uiState.value = _uiState.value.copy(breed = breed, breedError = null)
  }

  fun updateColor(color: String) {
    _uiState.value = _uiState.value.copy(color = color, colorError = null)
  }

  fun updateMicrochipNumber(number: String) {
    _uiState.value = _uiState.value.copy(microchipNumber = number, microchipError = null)
  }

  fun updatePassportNumber(number: String) {
    _uiState.value = _uiState.value.copy(passportNumber = number, passportError = null)
  }

  fun updateNotes(notes: String) {
    _uiState.value = _uiState.value.copy(notes = notes, notesError = null)
  }

  fun updatePhotoUri(uri: String?) {
    _uiState.value = _uiState.value.copy(photoUri = uri)
  }

  fun importPhoto(uri: Uri) {
    viewModelScope.launch {
      withContext(ioDispatcher) { photoStorage.importFromPicker(uri) }
        .onSuccess { storedUri -> _uiState.value = _uiState.value.copy(photoUri = storedUri) }
        .onFailure { _events.emit(PetFormEvent.Error(context.getString(R.string.pet_photo_error))) }
    }
  }

  fun startCameraCapture() {
    viewModelScope.launch {
      withContext(ioDispatcher) { photoStorage.createCameraPhoto() }
        .onSuccess { pending ->
          pendingCameraPhoto = pending
          _events.emit(PetFormEvent.LaunchCamera(pending.uri))
        }
        .onFailure { _events.emit(PetFormEvent.Error(context.getString(R.string.pet_photo_error))) }
    }
  }

  fun completeCameraCapture(success: Boolean) {
    val pending = pendingCameraPhoto ?: return
    pendingCameraPhoto = null
    viewModelScope.launch {
      withContext(ioDispatcher) { photoStorage.completeCameraPhoto(pending, success) }
        .onSuccess { storedUri -> _uiState.value = _uiState.value.copy(photoUri = storedUri) }
        .onFailure {
          if (success) {
            _events.emit(PetFormEvent.Error(context.getString(R.string.pet_photo_error)))
          }
        }
    }
  }

  fun savePet() {
    val state = _uiState.value
    val alphanumericRegex = Regex("^[a-zA-Z0-9\\s\\-]*$")

    // Validate
    var hasError = false

    if (state.name.isBlank()) {
      _uiState.value =
        _uiState.value.copy(nameError = context.getString(R.string.pet_validation_name_required))
      hasError = true
    } else if (state.name.length > 50) {
      _uiState.value =
        _uiState.value.copy(nameError = context.getString(R.string.pet_validation_name_max_length))
      hasError = true
    }

    if (state.breed.length > 50) {
      _uiState.value =
        _uiState.value.copy(
          breedError = context.getString(R.string.pet_validation_field_max_length, 50)
        )
      hasError = true
    }

    if (state.color.length > 50) {
      _uiState.value =
        _uiState.value.copy(
          colorError = context.getString(R.string.pet_validation_field_max_length, 50)
        )
      hasError = true
    }

    if (state.microchipNumber.length > 50) {
      _uiState.value =
        _uiState.value.copy(
          microchipError = context.getString(R.string.pet_validation_field_max_length, 50)
        )
      hasError = true
    } else if (
      state.microchipNumber.isNotEmpty() && !alphanumericRegex.matches(state.microchipNumber)
    ) {
      _uiState.value =
        _uiState.value.copy(
          microchipError = context.getString(R.string.pet_validation_alphanumeric_only)
        )
      hasError = true
    }

    if (state.passportNumber.length > 50) {
      _uiState.value =
        _uiState.value.copy(
          passportError = context.getString(R.string.pet_validation_field_max_length, 50)
        )
      hasError = true
    } else if (
      state.passportNumber.isNotEmpty() && !alphanumericRegex.matches(state.passportNumber)
    ) {
      _uiState.value =
        _uiState.value.copy(
          passportError = context.getString(R.string.pet_validation_alphanumeric_only)
        )
      hasError = true
    }

    if (state.notes.length > 500) {
      _uiState.value =
        _uiState.value.copy(
          notesError = context.getString(R.string.pet_validation_notes_max_length)
        )
      hasError = true
    }

    if (state.birthDate != null && state.birthDate.isAfter(LocalDate.now(clock))) {
      _uiState.value =
        _uiState.value.copy(
          birthDateError = context.getString(R.string.pet_validation_birth_date_future)
        )
      hasError = true
    }

    if (hasError) return

    viewModelScope.launch {
      _uiState.value = state.copy(isSaving = true)

      try {
        val now = clock.millis()
        val petToSave =
          if (state.isEditMode && state.petId != null) {
            // Edit existing pet
            val existingPet = petRepository.getPetById(state.petId)
            existingPet?.copy(
              petType = state.petType,
              name = state.name.trim(),
              birthDate = state.birthDate,
              sex = state.sex,
              breed = state.breed.trim().ifBlank { null },
              color = state.color.trim().ifBlank { null },
              microchipNumber = state.microchipNumber.trim().ifBlank { null },
              passportNumber = state.passportNumber.trim().ifBlank { null },
              notes = state.notes.trim().ifBlank { null },
              photoUri = state.photoUri,
              updatedAt = now,
            ) ?: throw IllegalStateException("Pet not found")
          } else {
            // Create new pet
            Pet(
              id = UUID.randomUUID().toString(),
              petType = state.petType,
              name = state.name.trim(),
              birthDate = state.birthDate,
              sex = state.sex,
              breed = state.breed.trim().ifBlank { null },
              color = state.color.trim().ifBlank { null },
              microchipNumber = state.microchipNumber.trim().ifBlank { null },
              passportNumber = state.passportNumber.trim().ifBlank { null },
              notes = state.notes.trim().ifBlank { null },
              photoUri = state.photoUri,
              createdAt = now,
              updatedAt = now,
              syncStatus = SyncStatus.LOCAL_ONLY,
            )
          }

        petRepository.savePet(petToSave)
        _events.emit(PetFormEvent.PetSaved(petToSave.id))
      } catch (e: Exception) {
        _events.emit(PetFormEvent.Error(e.message ?: context.getString(R.string.pet_error_save)))
      } finally {
        _uiState.value = _uiState.value.copy(isSaving = false)
      }
    }
  }
}
