package com.woliveiras.petit.presentation.feature.pets

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.di.IoDispatcher
import com.woliveiras.petit.domain.model.BreedCatalog
import com.woliveiras.petit.domain.model.BreedCatalogItem
import com.woliveiras.petit.domain.model.BreedIdentity
import com.woliveiras.petit.domain.model.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BreedSelectionValue(val breedId: String?, val breed: String, val displayName: String?) {
  companion object {
    val EMPTY = BreedSelectionValue(breedId = null, breed = "", displayName = null)
  }
}

enum class BreedSelectionMode {
  EMPTY,
  CATALOG,
  MIXED,
  UNKNOWN,
  MANUAL,
}

data class BreedSelectionUiState(
  val species: PetType? = null,
  val initialSelection: BreedSelectionValue = BreedSelectionValue.EMPTY,
  val draftSelection: BreedSelectionValue = BreedSelectionValue.EMPTY,
  val selectionMode: BreedSelectionMode = BreedSelectionMode.EMPTY,
  val query: String = "",
  val manualBreed: String = "",
  val results: List<BreedCatalogItem> = emptyList(),
  val isCatalogLoading: Boolean = false,
  val isSearchLoading: Boolean = false,
  val manualError: String? = null,
) {
  val canConfirm: Boolean
    get() = selectionMode != BreedSelectionMode.MANUAL || manualError == null
}

@HiltViewModel
class BreedSelectionViewModel
@Inject
constructor(
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

  private var breedCatalog = BreedCatalog.fromJsonOrEmpty("{}")
  private var searchRequest = 0

  private val _uiState = MutableStateFlow(restoredState())
  val uiState: StateFlow<BreedSelectionUiState> = _uiState.asStateFlow()

  init {
    loadBreedCatalog()
  }

  fun initialize(species: PetType, initialSelection: BreedSelectionValue) {
    if (_uiState.value.species != null) return
    val mode = modeFor(initialSelection)
    _uiState.value =
      _uiState.value.copy(
        species = species,
        initialSelection = initialSelection,
        draftSelection = initialSelection,
        selectionMode = mode,
        manualBreed = if (mode == BreedSelectionMode.MANUAL) initialSelection.breed else "",
      )
    persist()
    refreshResults(_uiState.value.query)
  }

  fun updateQuery(query: String) {
    _uiState.value = _uiState.value.copy(query = query)
    persist()
    refreshResults(query)
  }

  fun selectCatalogBreed(item: BreedCatalogItem) {
    updateDraft(
      value =
        BreedSelectionValue(
          breedId = item.id,
          breed = item.canonicalName,
          displayName = item.displayName,
        ),
      mode = BreedSelectionMode.CATALOG,
    )
  }

  fun selectMixedBreed() {
    updateDraft(
      BreedSelectionValue(
        breedId = BreedIdentity.MIXED_BREED_ID,
        breed = "MIXED_BREED",
        displayName = context.getString(R.string.breed_mixed),
      ),
      BreedSelectionMode.MIXED,
    )
  }

  fun selectUnknownBreed() {
    updateDraft(
      BreedSelectionValue(
        breedId = BreedIdentity.UNKNOWN_BREED_ID,
        breed = "Unknown breed",
        displayName = context.getString(R.string.breed_unknown),
      ),
      BreedSelectionMode.UNKNOWN,
    )
  }

  fun selectNoBreed() {
    updateDraft(BreedSelectionValue.EMPTY, BreedSelectionMode.EMPTY)
  }

  fun selectInitialSelection() {
    val initial = _uiState.value.initialSelection
    updateDraft(initial, modeFor(initial))
  }

  fun selectManualEntry() {
    val existingManual =
      _uiState.value.draftSelection.takeIf { it.breedId == null }?.breed.orEmpty()
    _uiState.value =
      _uiState.value.copy(
        draftSelection =
          BreedSelectionValue(
            breedId = null,
            breed = existingManual,
            displayName = existingManual.takeIf { it.isNotBlank() },
          ),
        selectionMode = BreedSelectionMode.MANUAL,
        manualBreed = existingManual,
        manualError = validateManual(existingManual),
      )
    persist()
  }

  fun updateManualBreed(breed: String) {
    _uiState.value =
      _uiState.value.copy(
        manualBreed = breed,
        draftSelection =
          BreedSelectionValue(
            breedId = null,
            breed = breed,
            displayName = breed.takeIf { it.isNotBlank() },
          ),
        manualError = validateManual(breed),
      )
    persist()
  }

  fun confirmSelection(): BreedSelectionValue? {
    val state = _uiState.value
    if (!state.canConfirm) return null
    return if (state.selectionMode == BreedSelectionMode.MANUAL) {
      val breed = state.manualBreed
      BreedSelectionValue(
        breedId = null,
        breed = breed,
        displayName = breed.takeUnless { it.isBlank() },
      )
    } else {
      state.draftSelection
    }
  }

  private fun updateDraft(value: BreedSelectionValue, mode: BreedSelectionMode) {
    _uiState.value =
      _uiState.value.copy(draftSelection = value, selectionMode = mode, manualError = null)
    persist()
  }

  private fun loadBreedCatalog() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isCatalogLoading = true)
      breedCatalog =
        withContext(ioDispatcher) {
          runCatching {
              context.assets.open("breed_catalog.json").bufferedReader().use { reader ->
                BreedCatalog.fromJsonOrEmpty(reader.readText())
              }
            }
            .getOrElse { BreedCatalog.fromJsonOrEmpty("{}") }
        }
      _uiState.value = _uiState.value.copy(isCatalogLoading = false)
      refreshResults(_uiState.value.query)
    }
  }

  private fun refreshResults(query: String) {
    val species = _uiState.value.species ?: return
    val request = ++searchRequest
    _uiState.value = _uiState.value.copy(isSearchLoading = true)
    viewModelScope.launch {
      val results = withContext(ioDispatcher) { breedCatalog.search(species, localeTag(), query) }
      val current = _uiState.value
      if (request == searchRequest && current.species == species && current.query == query) {
        _uiState.value = current.copy(results = results, isSearchLoading = false)
      }
    }
  }

  private fun validateManual(breed: String): String? =
    if (breed.length > MAX_BREED_LENGTH) {
      context.getString(R.string.pet_validation_field_max_length, MAX_BREED_LENGTH)
    } else {
      null
    }

  private fun modeFor(value: BreedSelectionValue): BreedSelectionMode =
    when (value.breedId) {
      BreedIdentity.MIXED_BREED_ID -> BreedSelectionMode.MIXED
      BreedIdentity.UNKNOWN_BREED_ID -> BreedSelectionMode.UNKNOWN
      null -> if (value.breed.isBlank()) BreedSelectionMode.EMPTY else BreedSelectionMode.MANUAL
      else -> BreedSelectionMode.CATALOG
    }

  private fun localeTag(): String =
    context.resources.configuration.locales[0]?.toLanguageTag().orEmpty().ifBlank { "en" }

  private fun persist() {
    val state = _uiState.value
    savedStateHandle[KEY_SPECIES] = state.species?.name
    savedStateHandle[KEY_QUERY] = state.query
    savedStateHandle[KEY_MODE] = state.selectionMode.name
    savedStateHandle[KEY_MANUAL] = state.manualBreed
    saveValue(KEY_INITIAL, state.initialSelection)
    saveValue(KEY_DRAFT, state.draftSelection)
  }

  private fun saveValue(prefix: String, value: BreedSelectionValue) {
    savedStateHandle["${prefix}Id"] = value.breedId
    savedStateHandle["${prefix}Breed"] = value.breed
    savedStateHandle["${prefix}DisplayName"] = value.displayName
  }

  private fun restoredState(): BreedSelectionUiState {
    val species =
      savedStateHandle.get<String>(KEY_SPECIES)?.let {
        runCatching { PetType.valueOf(it) }.getOrNull()
      }
    val initial = restoreValue(KEY_INITIAL)
    val draft = restoreValue(KEY_DRAFT)
    val mode =
      savedStateHandle.get<String>(KEY_MODE)?.let {
        runCatching { BreedSelectionMode.valueOf(it) }.getOrNull()
      } ?: modeFor(draft)
    val manual = savedStateHandle[KEY_MANUAL] ?: ""
    return BreedSelectionUiState(
      species = species,
      initialSelection = initial,
      draftSelection = draft,
      selectionMode = mode,
      query = savedStateHandle[KEY_QUERY] ?: "",
      manualBreed = manual,
      manualError = if (mode == BreedSelectionMode.MANUAL) validateManual(manual) else null,
    )
  }

  private fun restoreValue(prefix: String): BreedSelectionValue =
    BreedSelectionValue(
      breedId = savedStateHandle["${prefix}Id"],
      breed = savedStateHandle["${prefix}Breed"] ?: "",
      displayName = savedStateHandle["${prefix}DisplayName"],
    )

  private companion object {
    const val MAX_BREED_LENGTH = 50
    const val KEY_SPECIES = "breedSelectionSpecies"
    const val KEY_QUERY = "breedSelectionQuery"
    const val KEY_MODE = "breedSelectionMode"
    const val KEY_MANUAL = "breedSelectionManual"
    const val KEY_INITIAL = "breedSelectionInitial"
    const val KEY_DRAFT = "breedSelectionDraft"
  }
}
