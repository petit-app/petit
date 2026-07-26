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
import com.woliveiras.petit.domain.model.BreedCatalog
import com.woliveiras.petit.domain.model.BreedCatalogItem
import com.woliveiras.petit.domain.model.BreedIdentity
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.presentation.util.rethrowIfCancellation
import com.woliveiras.petit.presentation.util.uiFailureText
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
  val breedId: String? = null,
  val breedDisplayName: String? = null,
  val breedQuery: String = "",
  val breedResults: List<BreedCatalogItem> = emptyList(),
  val isBreedCatalogLoading: Boolean = false,
  val isBreedSearchLoading: Boolean = false,
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
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val photoStorage: PetPhotoStore,
  private val clock: Clock,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

  private val petId: String? = savedStateHandle["petId"]
  private var pendingCameraPhoto: PendingCameraPhoto? = null
  private var petLoadedForEdit: Pet? = null
  private var breedCatalog = BreedCatalog.fromJsonOrEmpty("{}")
  private var breedSearchRequest = 0

  private val _uiState =
    MutableStateFlow(
      PetFormUiState(
        petType =
          savedStateHandle.get<String>("breedPetType")?.let {
            runCatching { PetType.valueOf(it) }.getOrNull()
          } ?: PetType.OTHER,
        breed = savedStateHandle["breed"] ?: "",
        breedId = savedStateHandle["breedId"],
        breedDisplayName = savedStateHandle["breedDisplayName"],
        breedQuery = savedStateHandle["breedQuery"] ?: "",
      )
    )
  val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<PetFormEvent>()
  val events: SharedFlow<PetFormEvent> = _events.asSharedFlow()

  init {
    loadBreedCatalog()
    if (petId != null) {
      loadPet(petId)
    }
  }

  private fun loadBreedCatalog() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isBreedCatalogLoading = true)
      breedCatalog =
        withContext(ioDispatcher) {
          runCatching {
              context.assets.open("breed_catalog.json").bufferedReader().use { reader ->
                BreedCatalog.fromJsonOrEmpty(reader.readText())
              }
            }
            .getOrElse { BreedCatalog.fromJsonOrEmpty("{}") }
        }
      refreshBreedResults(_uiState.value.breedQuery)
      val state = _uiState.value
      _uiState.value =
        state.copy(
          isBreedCatalogLoading = false,
          breedDisplayName =
            state.breedId?.let { breedCatalog.resolve(it, localeTag())?.displayName }
              ?: state.breed.takeIf { it.isNotBlank() },
        )
    }
  }

  private fun loadPet(petId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val pet = petRepository.getPetById(petId)
        if (pet != null) {
          petLoadedForEdit = pet
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
              breedId = pet.breedId,
              breedDisplayName =
                pet.breedId?.let { breedCatalog.resolve(it, localeTag())?.displayName }
                  ?: pet.breed,
              color = pet.color ?: "",
              microchipNumber = pet.microchipNumber ?: "",
              passportNumber = pet.passportNumber ?: "",
              notes = pet.notes ?: "",
              photoUri = pet.photoUri,
            )
          persistBreedState(_uiState.value)
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false)
        _events.emit(PetFormEvent.Error(e.uiFailureText(context, R.string.pet_error_load)))
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
    val state = _uiState.value
    val selectedSinceLoad = state.breedId != null && state.breedId != petLoadedForEdit?.breedId
    _uiState.value =
      state.copy(
        petType = petType,
        breed = if (selectedSinceLoad) "" else state.breed,
        breedId = if (selectedSinceLoad) null else state.breedId,
        breedDisplayName = if (selectedSinceLoad) null else state.breedDisplayName,
      )
    persistBreedState(_uiState.value)
    refreshBreedResults("")
  }

  fun updateSex(sex: Sex) {
    _uiState.value = _uiState.value.copy(sex = sex)
  }

  fun updateBreed(breed: String) {
    _uiState.value =
      _uiState.value.copy(
        breed = breed,
        breedId = null,
        breedDisplayName = breed.takeIf { it.isNotBlank() },
        breedError = null,
      )
    persistBreedState(_uiState.value)
  }

  fun updateBreedSearch(query: String) {
    _uiState.value = _uiState.value.copy(breedQuery = query)
    savedStateHandle["breedQuery"] = query
    refreshBreedResults(query)
  }

  fun selectBreed(item: BreedCatalogItem) {
    _uiState.value =
      _uiState.value.copy(
        breed = item.canonicalName,
        breedId = item.id,
        breedDisplayName = item.displayName,
        breedQuery = "",
        breedError = null,
      )
    persistBreedState(_uiState.value)
  }

  fun selectMixedBreed() {
    _uiState.value =
      _uiState.value.copy(
        breed = "MIXED_BREED",
        breedId = BreedIdentity.MIXED_BREED_ID,
        breedDisplayName = context.getString(R.string.breed_mixed),
        breedQuery = "",
        breedError = null,
      )
    persistBreedState(_uiState.value)
  }

  fun selectUnknownBreed() {
    _uiState.value =
      _uiState.value.copy(
        breed = "Unknown breed",
        breedId = BreedIdentity.UNKNOWN_BREED_ID,
        breedDisplayName = context.getString(R.string.breed_unknown),
        breedQuery = "",
        breedError = null,
      )
    persistBreedState(_uiState.value)
  }

  fun clearBreed() {
    _uiState.value =
      _uiState.value.copy(
        breed = "",
        breedId = null,
        breedDisplayName = null,
        breedQuery = "",
        breedError = null,
      )
    persistBreedState(_uiState.value)
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
        .onFailure { failure ->
          failure.rethrowIfCancellation()
          _events.emit(PetFormEvent.Error(context.getString(R.string.pet_photo_error)))
        }
    }
  }

  fun startCameraCapture() {
    viewModelScope.launch {
      withContext(ioDispatcher) { photoStorage.createCameraPhoto() }
        .onSuccess { pending ->
          pendingCameraPhoto = pending
          _events.emit(PetFormEvent.LaunchCamera(pending.uri))
        }
        .onFailure { failure ->
          failure.rethrowIfCancellation()
          _events.emit(PetFormEvent.Error(context.getString(R.string.pet_photo_error)))
        }
    }
  }

  fun completeCameraCapture(success: Boolean) {
    val pending = pendingCameraPhoto ?: return
    pendingCameraPhoto = null
    viewModelScope.launch {
      withContext(ioDispatcher) { photoStorage.completeCameraPhoto(pending, success) }
        .onSuccess { storedUri -> _uiState.value = _uiState.value.copy(photoUri = storedUri) }
        .onFailure { failure ->
          failure.rethrowIfCancellation()
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
              breed = normalizeChangedText(state.breed, petLoadedForEdit?.breed),
              breedId = state.breedId,
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
              breedId = state.breedId,
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
        _events.emit(PetFormEvent.Error(e.uiFailureText(context, R.string.pet_error_save)))
      } finally {
        _uiState.value = _uiState.value.copy(isSaving = false)
      }
    }
  }

  private fun normalizeChangedText(current: String, original: String?): String? =
    if (current == original.orEmpty()) original else current.trim().ifBlank { null }

  private fun refreshBreedResults(query: String) {
    val state = _uiState.value
    val species = state.petType
    val request = ++breedSearchRequest
    _uiState.value = state.copy(breedQuery = query, isBreedSearchLoading = true)
    viewModelScope.launch {
      val results = withContext(ioDispatcher) { breedCatalog.search(species, localeTag(), query) }
      val current = _uiState.value
      if (
        request == breedSearchRequest && current.petType == species && current.breedQuery == query
      ) {
        _uiState.value = current.copy(breedResults = results, isBreedSearchLoading = false)
      }
    }
  }

  private fun localeTag(): String =
    context.resources.configuration.locales[0]?.toLanguageTag().orEmpty().ifBlank { "en" }

  private fun persistBreedState(state: PetFormUiState) {
    savedStateHandle["breedPetType"] = state.petType.name
    savedStateHandle["breed"] = state.breed
    savedStateHandle["breedId"] = state.breedId
    savedStateHandle["breedDisplayName"] = state.breedDisplayName
    savedStateHandle["breedQuery"] = state.breedQuery
  }
}
