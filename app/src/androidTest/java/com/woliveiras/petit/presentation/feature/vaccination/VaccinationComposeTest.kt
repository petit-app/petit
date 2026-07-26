package com.woliveiras.petit.presentation.feature.vaccination

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.presentation.util.AppDisplayFormatter
import com.woliveiras.petit.ui.theme.PetitTheme
import com.woliveiras.petit.worker.AutoTaskService
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class VaccinationComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun otherBranchShowsCustomNameAndValidationError() {
    val error = "Vaccine name is required"
    composeRule.setContent {
      PetitTheme {
        VaccinationCustomNameField(
          vaccineType = VaccineType.OTHER,
          customName = "",
          error = error,
          onValueChange = {},
        )
      }
    }

    composeRule.onNodeWithText(error).assertIsDisplayed()
  }

  @Test
  fun catalogTypeDoesNotShowCustomNameField() {
    composeRule.setContent {
      PetitTheme {
        VaccinationCustomNameField(
          vaccineType = VaccineType.V3,
          customName = "",
          error = null,
          onValueChange = {},
        )
      }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    composeRule
      .onAllNodesWithText(context.getString(R.string.vaccination_field_custom_name))
      .assertCountEquals(0)
  }

  @Test
  fun formDisplaysVeterinaryAdvisory() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    showForm(PetType.CAT)

    composeRule
      .onNodeWithText(context.getString(R.string.care_presets_veterinary_advisory))
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun catVaccineMenuOffersCatCatalogChoicesPlusOther() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    showForm(PetType.CAT)
    openVaccineMenu(context, context.getString(R.string.vaccine_rabies))
    assertVaccineMenuItem(context.getString(R.string.vaccine_rabies))
    assertVaccineMenuItem(context.getString(R.string.vaccine_v3))
    assertVaccineMenuItem(context.getString(R.string.vaccine_other))
  }

  @Test
  fun dogVaccineMenuOffersDogCatalogChoicesPlusOther() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    showForm(PetType.DOG)
    openVaccineMenu(context, context.getString(R.string.vaccine_rabies))
    assertVaccineMenuItem(context.getString(R.string.vaccine_rabies))
    assertVaccineMenuItem(context.getString(R.string.vaccine_dhpp))
    assertVaccineMenuItem(context.getString(R.string.vaccine_other))
  }

  @Test
  fun rabbitVaccineMenuOffersRabbitCatalogWithoutUniversalRabies() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    showForm(PetType.RABBIT)
    openVaccineMenu(context, context.getString(R.string.vaccine_rhdv))
    assertVaccineMenuItem(context.getString(R.string.vaccine_rhdv))
    assertVaccineMenuItem(context.getString(R.string.vaccine_other))
    composeRule.onAllNodesWithText(context.getString(R.string.vaccine_rabies)).assertCountEquals(0)
  }

  @Test
  fun birdVaccineMenuOffersBirdCatalogWithoutUniversalRabies() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    showForm(PetType.BIRD)
    openVaccineMenu(context, context.getString(R.string.vaccine_polyomavirus))
    assertVaccineMenuItem(context.getString(R.string.vaccine_polyomavirus))
    assertVaccineMenuItem(context.getString(R.string.vaccine_other))
    composeRule.onAllNodesWithText(context.getString(R.string.vaccine_rabies)).assertCountEquals(0)
  }

  @Test
  fun hamsterVaccineMenuOffersOnlyManualOtherWithoutUniversalRabies() {
    assertManualOtherVaccineMenuFor(PetType.HAMSTER)
  }

  @Test
  fun otherSpeciesVaccineMenuOffersOnlyManualOtherWithoutUniversalRabies() {
    assertManualOtherVaccineMenuFor(PetType.OTHER)
  }

  @Test
  fun historicalIncompatibleTypeStaysVisibleWithoutOfferingAnotherIncompatibleChoice() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val entry = entry("historical", VaccineType.DHPP)
    val viewModel = formViewModel(PetType.RABBIT, listOf(entry))
    composeRule.setContent {
      PetitTheme {
        VaccinationFormScreen(
          petId = "pet-1",
          entryId = null,
          onNavigateBack = {},
          viewModel = viewModel,
        )
      }
    }
    viewModel.loadEntryForEdit(entry.id)
    composeRule.waitForIdle()

    composeRule.onNodeWithText(context.getString(R.string.vaccine_dhpp)).assertIsDisplayed()
    openVaccineMenu(context, context.getString(R.string.vaccine_dhpp))
    composeRule.onAllNodesWithText(context.getString(R.string.vaccine_rabies)).assertCountEquals(0)
  }

  @Test
  fun historyRendersOkScheduledAndOverdueStates() {
    val today = LocalDate.of(2026, 7, 17)
    val entries =
      listOf(
        entry("ok", VaccineType.RABIES, nextDueDate = null),
        entry("scheduled", VaccineType.V3, nextDueDate = today.plusDays(5)),
        entry("overdue", VaccineType.OTHER, nextDueDate = today.minusDays(1)),
      )
    composeRule.setContent {
      PetitTheme { VaccinationTimeline(vaccinations = entries, today = today, onEditEntry = {}) }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    listOf(
        context.getString(R.string.health_status_ok),
        context.getString(R.string.health_status_scheduled),
        context.getString(R.string.health_status_overdue),
      )
      .forEach { label -> composeRule.onAllNodesWithText(label)[0].assertIsDisplayed() }
  }

  @Test
  fun groupedHistoryKeepsOlderDoseAccessible() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val today = LocalDate.of(2026, 7, 17)
    val entries =
      listOf(
        entry("latest", VaccineType.RABIES, LocalDate.of(2026, 7, 1)),
        entry("older", VaccineType.RABIES, LocalDate.of(2025, 7, 1)),
      )
    composeRule.setContent {
      PetitTheme { VaccinationTimeline(vaccinations = entries, today = today, onEditEntry = {}) }
    }

    val olderDate = AppDisplayFormatter(context).shortDate(LocalDate.of(2025, 7, 1))
    composeRule.onNode(hasText(olderDate)).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun historyDisplaysSavedTraceabilityDetails() {
    val today = LocalDate.of(2026, 7, 17)
    val detailed =
      entry("detailed", VaccineType.RABIES)
        .copy(
          veterinarian = "Dra. Ana",
          clinic = "Petit Vet",
          batchNumber = "lote-7",
          note = "reforço anual",
        )
    composeRule.setContent {
      PetitTheme {
        VaccinationTimeline(vaccinations = listOf(detailed), today = today, onEditEntry = {})
      }
    }

    listOf("Dra. Ana", "Petit Vet", "lote-7", "reforço anual").forEach { value ->
      composeRule.onNodeWithText(value).performScrollTo().assertIsDisplayed()
    }
  }

  @Test
  fun otherUsesCustomNameInAccessibleCardDescription() {
    val today = LocalDate.of(2026, 7, 17)
    val custom = entry("custom", VaccineType.OTHER)
    composeRule.setContent {
      PetitTheme {
        VaccinationTimeline(vaccinations = listOf(custom), today = today, onEditEntry = {})
      }
    }

    composeRule.onNode(hasContentDescription("Especial", substring = true)).assertIsDisplayed()
  }

  private fun entry(
    id: String,
    type: VaccineType,
    applicationDate: LocalDate = LocalDate.of(2026, 7, 1),
    nextDueDate: LocalDate? = null,
  ) =
    VaccinationEntry(
      id = id,
      petId = "pet-1",
      vaccineType = type,
      customVaccineTypeName = if (type == VaccineType.OTHER) "Especial" else null,
      applicationDate = applicationDate,
      nextDueDate = nextDueDate,
      createdAt = 1L,
      updatedAt = 1L,
    )

  private fun showForm(petType: PetType) {
    val viewModel = formViewModel(petType)
    composeRule.setContent {
      PetitTheme {
        VaccinationFormScreen(
          petId = "pet-1",
          entryId = null,
          onNavigateBack = {},
          viewModel = viewModel,
        )
      }
    }
    composeRule.waitForIdle()
  }

  private fun openVaccineMenu(
    context: Context,
    selectedLabel: String = context.getString(R.string.vaccine_other),
  ) {
    composeRule.onNodeWithText(selectedLabel).performScrollTo().performClick()
  }

  private fun assertVaccineMenuItem(label: String) {
    composeRule
      .onNode(hasText(label) and hasAnyAncestor(isPopup()))
      .performScrollTo()
      .assertIsDisplayed()
  }

  private fun assertManualOtherVaccineMenuFor(petType: PetType) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val otherLabel = context.getString(R.string.vaccine_other)

    showForm(petType)
    openVaccineMenu(context, otherLabel)
    assertVaccineMenuItem(otherLabel)
    composeRule.onAllNodesWithText(context.getString(R.string.vaccine_rabies)).assertCountEquals(0)
  }

  private fun formViewModel(petType: PetType, entries: List<VaccinationEntry> = emptyList()) =
    VaccinationViewModel(
      savedStateHandle = SavedStateHandle(mapOf("petId" to "pet-1")),
      context = ApplicationProvider.getApplicationContext(),
      petRepository = FormPetRepository(petType),
      vaccinationRepository = FormVaccinationRepository(entries),
      autoTaskService = FormAutoTaskService(),
      clock = Clock.systemUTC(),
    )

  private class FormPetRepository(petType: PetType) : PetRepository {
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

  private class FormVaccinationRepository(entries: List<VaccinationEntry>) :
    VaccinationEntryRepository {
    private val entries = MutableStateFlow(entries)

    override fun getVaccinationEntriesForPet(petId: String): Flow<List<VaccinationEntry>> = entries

    override fun getLatestVaccinationsForPet(petId: String): Flow<List<VaccinationEntry>> = entries

    override suspend fun getVaccinationEntryById(id: String) = entries.value.find { it.id == id }

    override fun getOverdueVaccinations(): Flow<List<VaccinationEntry>> =
      MutableStateFlow(emptyList())

    override fun getUpcomingVaccinations(days: Int): Flow<List<VaccinationEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun saveVaccinationEntry(entry: VaccinationEntry) = Unit

    override suspend fun deleteVaccinationEntry(id: String) = Unit

    override suspend fun countEntriesForPet(petId: String) = entries.value.size
  }

  private class FormAutoTaskService : AutoTaskService {
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
