package com.woliveiras.petit.presentation.feature.deworming

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.ui.theme.PetitTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class DewormingComposeTest {

  @get:Rule val composeRule = createComposeRule()

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

    composeRule.onNode(hasText("01/07/2026")).performScrollTo().performClick()

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
}
