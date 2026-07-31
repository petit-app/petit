package com.woliveiras.petit.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.ui.theme.PetitTheme
import java.time.LocalDate
import java.util.TimeZone
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetitDatePickerDialogComposeTest {

  @get:Rule val composeRule = createComposeRule()

  private val originalTimeZone: TimeZone = TimeZone.getDefault()

  private val confirmLabel: String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.action_ok)

  @After
  fun restoreTimeZone() {
    TimeZone.setDefault(originalTimeZone)
  }

  @Test
  fun confirmingWithoutChangingSelectionKeepsTheSameDayInAPositiveOffsetTimeZone() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
    val selectedDate = LocalDate.of(2026, 7, 31)
    var confirmedDate: LocalDate? = null

    composeRule.setContent {
      PetitTheme {
        PetitDatePickerDialog(
          selectedDate = selectedDate,
          onDateSelected = { confirmedDate = it },
          onDismissRequest = {},
        )
      }
    }

    composeRule.onNodeWithText(confirmLabel).performClick()
    composeRule.waitForIdle()

    assertThat(confirmedDate).isEqualTo(selectedDate)
  }

  @Test
  fun confirmingWithoutChangingSelectionKeepsTheSameDayInANegativeOffsetTimeZone() {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    val selectedDate = LocalDate.of(2026, 7, 31)
    var confirmedDate: LocalDate? = null

    composeRule.setContent {
      PetitTheme {
        PetitDatePickerDialog(
          selectedDate = selectedDate,
          onDateSelected = { confirmedDate = it },
          onDismissRequest = {},
        )
      }
    }

    composeRule.onNodeWithText(confirmLabel).performClick()
    composeRule.waitForIdle()

    assertThat(confirmedDate).isEqualTo(selectedDate)
  }
}
