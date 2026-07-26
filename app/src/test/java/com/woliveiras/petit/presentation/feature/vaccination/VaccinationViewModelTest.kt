package com.woliveiras.petit.presentation.feature.vaccination

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.worker.AutoTaskService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VaccinationViewModelTest {

  private val dispatcher: TestDispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)
  private lateinit var repository: FakeVaccinationRepository
  private lateinit var viewModel: VaccinationViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repository = FakeVaccinationRepository()
    viewModel =
      VaccinationViewModel(
        savedStateHandle = SavedStateHandle(mapOf("petId" to "pet-1")),
        context = context,
        petRepository = FakePetRepository(PetType.CAT),
        vaccinationRepository = repository,
        autoTaskService = NoOpAutoTaskService(),
        clock = clock,
      )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun otherMustHaveVisibleCustomNameBeforeSaving() =
    runTest(dispatcher) {
      advanceUntilIdle()
      viewModel.updateVaccineType(VaccineType.OTHER)
      viewModel.updateCustomName("   ")

      viewModel.saveVaccination()
      advanceUntilIdle()

      assertThat(repository.saved).isEmpty()
      assertThat(viewModel.uiState.value.form.customNameError).isNotNull()
    }

  @Test
  fun incompatibleNewSelectionIsRejectedBeforeItChangesTheForm() =
    runTest(dispatcher) {
      advanceUntilIdle()
      viewModel.updateVaccineType(VaccineType.DHPP)

      assertThat(viewModel.uiState.value.form.vaccineType).isNotEqualTo(VaccineType.DHPP)
    }

  @Test
  fun validOtherIsTrimmedAndTraceabilityDetailsArePreserved() =
    runTest(dispatcher) {
      advanceUntilIdle()
      viewModel.updateVaccineType(VaccineType.OTHER)
      viewModel.updateCustomName("  Especial  ")
      viewModel.updateVeterinarian("  Dra. Ana  ")
      viewModel.updateClinic("  Petit Vet  ")
      viewModel.updateBatchNumber("  lote-7  ")
      viewModel.updateNote("  reforço  ")

      viewModel.saveVaccination()
      advanceUntilIdle()

      assertThat(repository.saved.single().customVaccineTypeName).isEqualTo("Especial")
      assertThat(repository.saved.single().veterinarian).isEqualTo("Dra. Ana")
      assertThat(repository.saved.single().clinic).isEqualTo("Petit Vet")
      assertThat(repository.saved.single().batchNumber).isEqualTo("lote-7")
      assertThat(repository.saved.single().note).isEqualTo("reforço")
    }

  @Test
  fun selectingCatalogTypeClearsOtherName() =
    runTest(dispatcher) {
      advanceUntilIdle()
      viewModel.updateVaccineType(VaccineType.OTHER)
      viewModel.updateCustomName("Especial")

      viewModel.updateVaccineType(VaccineType.V3)

      assertThat(viewModel.uiState.value.form.customName).isEmpty()
    }

  @Test
  fun selectionAndApplicationDateNeverInferOrReplaceTheNextDueDate() =
    runTest(dispatcher) {
      advanceUntilIdle()
      val manuallyChosenDate = LocalDate.of(2026, 8, 1)
      viewModel.updateNextDueDate(manuallyChosenDate)

      viewModel.updateVaccineType(VaccineType.RABIES)
      viewModel.updateApplicationDate(LocalDate.of(2026, 7, 2))

      assertThat(viewModel.uiState.value.form.nextDueDate).isEqualTo(manuallyChosenDate)
    }

  @Test
  fun catalogExclusivelyDrivesChoicesAndResetUsesAValidSpeciesDefault() =
    runTest(dispatcher) {
      val rabbitViewModel = viewModel(PetType.RABBIT)
      advanceUntilIdle()

      assertThat(rabbitViewModel.uiState.value.availableVaccineTypes)
        .containsExactly(VaccineType.RHDV, VaccineType.MYXOMATOSIS, VaccineType.OTHER)
        .inOrder()
      assertThat(rabbitViewModel.uiState.value.form.vaccineType)
        .isIn(rabbitViewModel.uiState.value.availableVaccineTypes)

      rabbitViewModel.resetForm()

      assertThat(rabbitViewModel.uiState.value.form.vaccineType).isEqualTo(VaccineType.RHDV)
    }

  @Test
  fun saveResetsToTheSameSpeciesAwareDefaultAsResetForm() =
    runTest(dispatcher) {
      advanceUntilIdle()
      viewModel.updateVaccineType(VaccineType.V3)

      viewModel.saveVaccination()
      advanceUntilIdle()

      assertThat(repository.saved.single().vaccineType).isEqualTo(VaccineType.V3)
      assertThat(viewModel.uiState.value.form.vaccineType).isEqualTo(VaccineType.RABIES)
      assertThat(viewModel.uiState.value.form.customName).isEmpty()
    }

  @Test
  fun historicalIncompatibleEntryStaysEditableAndCannotBeReplacedWithAnotherIncompatibleType() =
    runTest(dispatcher) {
      val rabbitViewModel = viewModel(PetType.RABBIT)
      val original =
        entry(id = "entry-1", createdAt = 10L, updatedAt = 20L, type = VaccineType.DHPP)
      repository.entries.value = listOf(original)
      advanceUntilIdle()
      rabbitViewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()

      assertThat(rabbitViewModel.uiState.value.form.vaccineType).isEqualTo(VaccineType.DHPP)
      assertThat(rabbitViewModel.uiState.value.availableVaccineTypes).contains(VaccineType.DHPP)

      rabbitViewModel.updateVaccineType(VaccineType.RABIES)
      assertThat(rabbitViewModel.uiState.value.form.vaccineType).isEqualTo(VaccineType.DHPP)

      rabbitViewModel.updateVaccineType(VaccineType.RHDV)
      assertThat(rabbitViewModel.uiState.value.availableVaccineTypes)
        .doesNotContain(VaccineType.DHPP)
      rabbitViewModel.updateVaccineType(VaccineType.DHPP)
      assertThat(rabbitViewModel.uiState.value.form.vaccineType).isEqualTo(VaccineType.RHDV)

      rabbitViewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()

      rabbitViewModel.saveVaccination()
      advanceUntilIdle()
      assertThat(repository.saved.single().vaccineType).isEqualTo(VaccineType.DHPP)
    }

  @Test
  fun delayedPetInfoCannotOverwriteAnAlreadyLoadedHistoricalIncompatibleType() =
    runTest(dispatcher) {
      val petInfoGate = CompletableDeferred<Unit>()
      val delayedPetRepository = DelayedPetRepository(PetType.RABBIT, petInfoGate)
      val delayedViewModel =
        VaccinationViewModel(
          savedStateHandle = SavedStateHandle(mapOf("petId" to "pet-1")),
          context = context,
          petRepository = delayedPetRepository,
          vaccinationRepository = repository,
          autoTaskService = NoOpAutoTaskService(),
          clock = clock,
        )
      val original =
        entry(id = "entry-delayed", createdAt = 10L, updatedAt = 20L, type = VaccineType.DHPP)
      repository.entries.value = listOf(original)

      delayedViewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()
      assertThat(delayedViewModel.uiState.value.form.vaccineType).isEqualTo(VaccineType.DHPP)

      petInfoGate.complete(Unit)
      advanceUntilIdle()

      assertThat(delayedViewModel.uiState.value.petType).isEqualTo(PetType.RABBIT)
      assertThat(delayedViewModel.uiState.value.form.vaccineType).isEqualTo(VaccineType.DHPP)
    }

  @Test
  fun editingPreservesCreatedAtAndUpdatesUpdatedAtFromClock() =
    runTest(dispatcher) {
      val original = entry(id = "entry-1", createdAt = 10L, updatedAt = 20L)
      repository.entries.value = listOf(original)
      advanceUntilIdle()
      viewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()

      viewModel.updateNote("updated")
      viewModel.saveVaccination()
      advanceUntilIdle()

      assertThat(repository.saved.single().createdAt).isEqualTo(10L)
      assertThat(repository.saved.single().updatedAt).isEqualTo(clock.millis())
    }

  @Test
  fun unchangedCustomVaccineNameSurvivesOpenAndSaveByteExact() =
    runTest(dispatcher) {
      val rawCustomName = " Vacina X® "
      val original =
        entry(
          id = "entry-custom",
          createdAt = 10L,
          updatedAt = 20L,
          type = VaccineType.OTHER,
          customName = rawCustomName,
        )
      repository.entries.value = listOf(original)
      advanceUntilIdle()
      viewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()

      viewModel.saveVaccination()
      advanceUntilIdle()

      assertThat(repository.saved.single().customVaccineTypeName).isEqualTo(rawCustomName)
    }

  @Test
  fun changedCustomVaccineNameKeepsExistingTrimNormalization() =
    runTest(dispatcher) {
      val original =
        entry(
          id = "entry-custom-changed",
          createdAt = 10L,
          updatedAt = 20L,
          type = VaccineType.OTHER,
          customName = " Original ",
        )
      repository.entries.value = listOf(original)
      advanceUntilIdle()
      viewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()

      viewModel.updateCustomName(" Changed X® ")
      viewModel.saveVaccination()
      advanceUntilIdle()

      assertThat(repository.saved.single().customVaccineTypeName).isEqualTo("Changed X®")
    }

  private fun entry(
    id: String,
    createdAt: Long,
    updatedAt: Long,
    type: VaccineType = VaccineType.V3,
    customName: String? = null,
  ) =
    VaccinationEntry(
      id = id,
      petId = "pet-1",
      vaccineType = type,
      customVaccineTypeName = customName,
      applicationDate = LocalDate.of(2026, 7, 1),
      createdAt = createdAt,
      updatedAt = updatedAt,
      syncStatus = SyncStatus.LOCAL_ONLY,
    )

  private fun viewModel(petType: PetType = PetType.CAT) =
    VaccinationViewModel(
      savedStateHandle = SavedStateHandle(mapOf("petId" to "pet-1")),
      context = context,
      petRepository = FakePetRepository(petType),
      vaccinationRepository = repository,
      autoTaskService = NoOpAutoTaskService(),
      clock = clock,
    )

  private class FakePetRepository(petType: PetType) : PetRepository {
    private val pet =
      Pet(
        id = "pet-1",
        name = "Mimi",
        petType = petType,
        sex = Sex.UNKNOWN,
        createdAt = 1L,
        updatedAt = 1L,
      )

    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(listOf(pet))

    override suspend fun getPetById(id: String): Pet? = pet.takeIf { it.id == id }

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(pet)

    override fun getPetCount(): Flow<Int> = MutableStateFlow(1)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class DelayedPetRepository(
    petType: PetType,
    private val gate: CompletableDeferred<Unit>,
  ) : PetRepository {
    private val pet =
      Pet(
        id = "pet-1",
        name = "Mimi",
        petType = petType,
        sex = Sex.UNKNOWN,
        createdAt = 1L,
        updatedAt = 1L,
      )

    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(listOf(pet))

    override suspend fun getPetById(id: String): Pet? {
      gate.await()
      return pet.takeIf { it.id == id }
    }

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(pet)

    override fun getPetCount(): Flow<Int> = MutableStateFlow(1)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class FakeVaccinationRepository : VaccinationEntryRepository {
    val entries = MutableStateFlow<List<VaccinationEntry>>(emptyList())
    val saved = mutableListOf<VaccinationEntry>()

    override fun getVaccinationEntriesForPet(petId: String): Flow<List<VaccinationEntry>> = entries

    override fun getLatestVaccinationsForPet(petId: String): Flow<List<VaccinationEntry>> = entries

    override suspend fun getVaccinationEntryById(id: String) = entries.value.find { it.id == id }

    override fun getOverdueVaccinations(): Flow<List<VaccinationEntry>> =
      MutableStateFlow(emptyList())

    override fun getUpcomingVaccinations(days: Int): Flow<List<VaccinationEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun saveVaccinationEntry(entry: VaccinationEntry) {
      saved += entry
    }

    override suspend fun deleteVaccinationEntry(id: String) = Unit

    override suspend fun countEntriesForPet(petId: String): Int = entries.value.size
  }

  private class NoOpAutoTaskService : AutoTaskService {
    override suspend fun handleVaccinationSaved(entry: VaccinationEntry) = Unit

    override suspend fun handleVaccinationDeleted(entryId: String) = Unit

    override suspend fun handleDewormingSaved(
      entry: com.woliveiras.petit.domain.model.DewormingEntry
    ) = Unit

    override suspend fun handleDewormingDeleted(entryId: String) = Unit

    override suspend fun handleWeightSaved(petId: String, petName: String) = Unit

    override suspend fun cancelWeightTask(petId: String) = Unit
  }
}
