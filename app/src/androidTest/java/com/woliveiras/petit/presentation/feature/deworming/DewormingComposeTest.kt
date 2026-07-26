package com.woliveiras.petit.presentation.feature.deworming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.presentation.util.AppDisplayFormatter
import com.woliveiras.petit.ui.theme.PetitTheme
import com.woliveiras.petit.worker.AutoTaskService
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class DewormingComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun medicationIsADirectManualFieldWithoutCommercialPresetLabels() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    var medication by mutableStateOf("")
    composeRule.setContent {
      PetitTheme {
        DewormingMedicationField(
          medication = medication,
          error = null,
          onValueChange = { medication = it },
        )
      }
    }

    composeRule
      .onNodeWithText(context.getString(R.string.deworming_field_medication_custom))
      .assertIsDisplayed()

    composeRule.runOnIdle { medication = "Legacy Brand" }

    composeRule.onNodeWithText("Legacy Brand").assertIsDisplayed()
    listOf("Milbemax", "Frontline", "Bravecto").forEach { brand ->
      composeRule.onAllNodesWithText(brand).assertCountEquals(0)
    }
  }

  @Test
  fun formDisplaysVeterinaryAdvisoryWithoutSuggestedIntervalControls() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    composeRule.setContent {
      PetitTheme {
        DewormingFormScreen(petId = "pet-1", onNavigateBack = {}, viewModel = formViewModel())
      }
    }
    composeRule.waitForIdle()

    composeRule
      .onNodeWithText(context.getString(R.string.care_presets_veterinary_advisory))
      .performScrollTo()
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.deworming_field_next_due_date))
      .performScrollTo()
      .assertIsDisplayed()
    composeRule
      .onAllNodesWithText(context.getString(R.string.deworming_field_monthly_interval))
      .assertCountEquals(0)
  }

  @Test
  fun categorySummariesCountBothAsInternalAndExternal() {
    val today = LocalDate.of(2026, 7, 17)
    val combined = entry("both", DewormingType.BOTH, day = 10, nextDueDate = today.plusDays(5))

    composeRule.setContent {
      PetitTheme {
        DewormingTimeline(dewormings = listOf(combined), today = today, onEditEntry = {})
      }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    composeRule.onNodeWithText(context.getString(R.string.deworming_internal)).assertIsDisplayed()
    composeRule.onNodeWithText(context.getString(R.string.deworming_external)).assertIsDisplayed()
  }

  @Test
  fun categorySummariesAssociateLatestApplicableMedicationWithItsStatus() {
    val today = LocalDate.of(2026, 7, 17)
    val entries =
      listOf(
        entry("both", DewormingType.BOTH, day = 10, nextDueDate = today.minusDays(1)),
        entry("external", DewormingType.EXTERNAL, day = 12, nextDueDate = today.plusDays(5)),
      )
    composeRule.setContent {
      PetitTheme { DewormingTimeline(dewormings = entries, today = today, onEditEntry = {}) }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val internalDescription =
      listOf(
          context.getString(R.string.deworming_internal),
          "Medication both",
          context.getString(R.string.health_status_overdue),
        )
        .joinToString(", ")
    val externalDescription =
      listOf(
          context.getString(R.string.deworming_external),
          "Medication external",
          context.getString(R.string.health_status_scheduled),
        )
        .joinToString(", ")

    composeRule.onNode(hasContentDescription(internalDescription)).assertIsDisplayed()
    composeRule.onNode(hasContentDescription(externalDescription)).assertIsDisplayed()
  }

  @Test
  fun historyRendersOkScheduledAndOverdueAccessibleIndicators() {
    val today = LocalDate.of(2026, 7, 17)
    val entries =
      listOf(
        entry("ok", DewormingType.INTERNAL, day = 12, nextDueDate = null),
        entry("scheduled", DewormingType.EXTERNAL, day = 11, nextDueDate = today.plusDays(5)),
        entry("overdue", DewormingType.BOTH, day = 10, nextDueDate = today.minusDays(1)),
      )
    composeRule.setContent {
      PetitTheme { DewormingTimeline(dewormings = entries, today = today, onEditEntry = {}) }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    listOf(
        context.getString(R.string.health_status_ok),
        context.getString(R.string.health_status_scheduled),
        context.getString(R.string.health_status_overdue),
      )
      .forEach { label -> composeRule.onAllNodesWithText(label)[0].assertIsDisplayed() }

    listOf(
        context.getString(R.string.health_status_ok),
        context.getString(R.string.health_status_scheduled),
        context.getString(R.string.health_status_overdue),
      )
      .forEach { label ->
        composeRule.onAllNodesWithContentDescription(label)[0].assertIsDisplayed()
      }
  }

  @Test
  fun chronologicalHistoryKeepsOlderEntryAccessibleAndClickable() {
    var clickedId: String? = null
    val today = LocalDate.of(2026, 7, 17)
    val entries =
      listOf(
        entry("latest", DewormingType.INTERNAL, day = 10),
        entry("older", DewormingType.INTERNAL, day = 1),
      )
    composeRule.setContent {
      PetitTheme {
        DewormingTimeline(dewormings = entries, today = today, onEditEntry = { clickedId = it.id })
      }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val olderDate = AppDisplayFormatter(context).shortDate(LocalDate.of(2026, 7, 1))
    composeRule.onNode(hasText(olderDate)).performScrollTo().performClick()

    assertThat(clickedId).isEqualTo("older")
  }

  private fun entry(id: String, type: DewormingType, day: Int, nextDueDate: LocalDate? = null) =
    DewormingEntry(
      id = id,
      petId = "pet-1",
      type = type,
      medication = "Medication $id",
      applicationDate = LocalDate.of(2026, 7, day),
      nextDueDate = nextDueDate,
      createdAt = 1L,
      updatedAt = day.toLong(),
    )

  private fun formViewModel() =
    DewormingViewModel(
      savedStateHandle = SavedStateHandle(mapOf("petId" to "pet-1")),
      context = ApplicationProvider.getApplicationContext(),
      petRepository = FormPetRepository(),
      dewormingRepository = FormDewormingRepository(),
      autoTaskService = FormAutoTaskService(),
      clock = Clock.systemUTC(),
    )

  private class FormPetRepository : PetRepository {
    private val pet =
      Pet(
        id = "pet-1",
        name = "Mimi",
        petType = PetType.CAT,
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

  private class FormDewormingRepository : DewormingEntryRepository {
    private val entries = MutableStateFlow<List<DewormingEntry>>(emptyList())

    override fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntry>> = entries

    override fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntry>> = entries

    override suspend fun getDewormingEntryById(id: String): DewormingEntry? = null

    override fun getOverdueDewormings(): Flow<List<DewormingEntry>> = MutableStateFlow(emptyList())

    override fun getUpcomingDewormings(days: Int): Flow<List<DewormingEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun saveDewormingEntry(entry: DewormingEntry) = Unit

    override suspend fun deleteDewormingEntry(id: String) = Unit

    override suspend fun countEntriesForPet(petId: String) = 0
  }

  private class FormAutoTaskService : AutoTaskService {
    override suspend fun handleVaccinationSaved(
      entry: com.woliveiras.petit.domain.model.VaccinationEntry
    ) = Unit

    override suspend fun handleVaccinationDeleted(entryId: String) = Unit

    override suspend fun handleDewormingSaved(entry: DewormingEntry) = Unit

    override suspend fun handleDewormingDeleted(entryId: String) = Unit

    override suspend fun handleWeightSaved(petId: String, petName: String) = Unit

    override suspend fun cancelWeightTask(petId: String) = Unit
  }
}
