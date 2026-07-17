package com.woliveiras.petit.presentation.feature.deworming

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.worker.AutoTaskService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
class DewormingViewModelTest {

  private val dispatcher: TestDispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)
  private lateinit var repository: FakeDewormingEntryRepository
  private lateinit var autoTaskService: RecordingAutoTaskService
  private lateinit var viewModel: DewormingViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repository = FakeDewormingEntryRepository()
    autoTaskService = RecordingAutoTaskService()
    viewModel =
      DewormingViewModel(
        savedStateHandle = SavedStateHandle(mapOf("petId" to "pet-1")),
        context = context,
        petRepository = FakePetRepository(),
        dewormingRepository = repository,
        autoTaskService = autoTaskService,
        clock = clock,
      )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun formStartsAtControlledToday() {
    assertThat(viewModel.uiState.value.today).isEqualTo(LocalDate.of(2026, 7, 17))
    assertThat(viewModel.uiState.value.form.applicationDate).isEqualTo(LocalDate.of(2026, 7, 17))
  }

  @Test
  fun updateApplicationDateKeepsSelectedIntervalOffset() =
    runTest(dispatcher) {
      val initialApplicationDate = LocalDate.of(2026, 7, 1)
      val newApplicationDate = LocalDate.of(2026, 7, 10)

      viewModel.updateApplicationDate(initialApplicationDate)
      viewModel.updateNextDueDate(initialApplicationDate.plusMonths(2))
      viewModel.updateApplicationDate(newApplicationDate)

      assertThat(viewModel.uiState.value.form.nextDueDate)
        .isEqualTo(newApplicationDate.plusMonths(2))
    }

  @Test
  fun selectCustomIntervalClearsStaleNextDueDate() =
    runTest(dispatcher) {
      val applicationDate = LocalDate.of(2026, 7, 1)

      viewModel.updateApplicationDate(applicationDate)
      viewModel.updateMonthlyInterval(2)
      viewModel.selectCustomInterval()

      assertThat(viewModel.uiState.value.form.customIntervalValue).isEmpty()
      assertThat(viewModel.uiState.value.form.nextDueDate).isNull()
    }

  @Test
  fun updateApplicationDateRecalculatesCustomInterval() =
    runTest(dispatcher) {
      val initialApplicationDate = LocalDate.of(2026, 7, 1)
      val newApplicationDate = LocalDate.of(2026, 7, 10)

      viewModel.updateApplicationDate(initialApplicationDate)
      viewModel.selectCustomInterval()
      viewModel.updateCustomIntervalUnit(DewormingIntervalUnit.DAILY)
      viewModel.updateCustomIntervalValue("15")
      viewModel.updateApplicationDate(newApplicationDate)

      assertThat(viewModel.uiState.value.form.nextDueDate)
        .isEqualTo(newApplicationDate.plusDays(15))
    }

  @Test
  fun invalidDraftDoesNotSaveAndShowsAllRelevantErrors() =
    runTest(dispatcher) {
      viewModel.updateMedication("   ")
      viewModel.updateApplicationDate(LocalDate.of(2026, 7, 18))
      viewModel.updateNextDueDate(LocalDate.of(2026, 7, 18))

      viewModel.saveDeworming()
      advanceUntilIdle()

      assertThat(repository.saved).isEmpty()
      assertThat(viewModel.uiState.value.form.medicationError).isNotNull()
      assertThat(viewModel.uiState.value.form.applicationDateError).isNotNull()
    }

  @Test
  fun everyTypeCanBeSavedWithTrimmedFieldsAndClockTimestamps() =
    runTest(dispatcher) {
      DewormingType.entries.forEach { type ->
        viewModel.updateDewormingType(type)
        viewModel.updateMedication("  Medication $type  ")
        viewModel.updateNote("  note  ")
        viewModel.saveDeworming()
        advanceUntilIdle()
      }

      assertThat(repository.saved.map { it.type }).containsExactlyElementsIn(DewormingType.entries)
      repository.saved.forEach { entry ->
        assertThat(entry.medication).isEqualTo("Medication ${entry.type}")
        assertThat(entry.note).isEqualTo("note")
        assertThat(entry.createdAt).isEqualTo(clock.millis())
        assertThat(entry.updatedAt).isEqualTo(clock.millis())
      }
    }

  @Test
  fun editingPreservesCreatedAtAndPersistsUpdatedValues() =
    runTest(dispatcher) {
      val original = entry(id = "entry-1", createdAt = 10L, updatedAt = 20L)
      repository.entries.value = listOf(original)
      advanceUntilIdle()
      viewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()

      viewModel.updateDewormingType(DewormingType.BOTH)
      viewModel.updateMedication("  Updated  ")
      viewModel.saveDeworming()
      advanceUntilIdle()

      val saved = repository.saved.single()
      assertThat(saved.id).isEqualTo(original.id)
      assertThat(saved.type).isEqualTo(DewormingType.BOTH)
      assertThat(saved.medication).isEqualTo("Updated")
      assertThat(saved.createdAt).isEqualTo(10L)
      assertThat(saved.updatedAt).isEqualTo(clock.millis())
    }

  @Test
  fun deleteCurrentEntrySoftDeletesAndCancelsAutomaticTask() =
    runTest(dispatcher) {
      val original = entry(id = "entry-1", createdAt = 10L, updatedAt = 20L)
      repository.entries.value = listOf(original)
      advanceUntilIdle()
      viewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()

      viewModel.deleteCurrentEntry()
      advanceUntilIdle()

      assertThat(repository.deleted).containsExactly(original.id)
      assertThat(autoTaskService.deleted).containsExactly(original.id)
    }

  private fun entry(id: String, createdAt: Long, updatedAt: Long) =
    DewormingEntry(
      id = id,
      petId = "pet-1",
      type = DewormingType.INTERNAL,
      medication = "Milbemax",
      applicationDate = LocalDate.of(2026, 7, 1),
      createdAt = createdAt,
      updatedAt = updatedAt,
    )

  private class FakePetRepository : PetRepository {
    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(emptyList())

    override suspend fun getPetById(id: String): Pet? = null

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(null)

    override fun getPetCount(): Flow<Int> = MutableStateFlow(0)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class FakeDewormingEntryRepository : DewormingEntryRepository {
    val entries = MutableStateFlow<List<DewormingEntry>>(emptyList())
    val saved = mutableListOf<DewormingEntry>()
    val deleted = mutableListOf<String>()

    override fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntry>> = entries

    override fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntry>> = entries

    override suspend fun getDewormingEntryById(id: String): DewormingEntry? =
      entries.value.find { it.id == id }

    override fun getOverdueDewormings(): Flow<List<DewormingEntry>> = MutableStateFlow(emptyList())

    override fun getUpcomingDewormings(days: Int): Flow<List<DewormingEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun saveDewormingEntry(entry: DewormingEntry) {
      saved += entry
    }

    override suspend fun deleteDewormingEntry(id: String) {
      deleted += id
    }

    override suspend fun countEntriesForPet(petId: String): Int = entries.value.size
  }

  private class RecordingAutoTaskService : AutoTaskService {
    val deleted = mutableListOf<String>()

    override suspend fun handleVaccinationSaved(
      entry: com.woliveiras.petit.domain.model.VaccinationEntry
    ) = Unit

    override suspend fun handleVaccinationDeleted(entryId: String) = Unit

    override suspend fun handleDewormingSaved(entry: DewormingEntry) = Unit

    override suspend fun handleDewormingDeleted(entryId: String) {
      deleted += entryId
    }

    override suspend fun handleWeightSaved(petId: String, petName: String) = Unit

    override suspend fun cancelWeightTask(petId: String) = Unit
  }
}
