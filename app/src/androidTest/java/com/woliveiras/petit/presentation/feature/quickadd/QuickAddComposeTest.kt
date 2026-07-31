package com.woliveiras.petit.presentation.feature.quickadd

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickAddComposeTest {

  @get:Rule val composeRule = createComposeRule()

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun offersInternalDewormerAndFleaAndTickAsSeparateEntries() {
    composeRule.setContent { PetitTheme { QuickAdd() } }

    composeRule.onNodeWithText(context.getString(R.string.quick_add_deworming)).assertIsDisplayed()
    composeRule.onNodeWithText(context.getString(R.string.quick_add_flea_tick)).assertIsDisplayed()
  }

  @Test
  fun selectingFleaAndTickTriggersItsOwnAction() {
    var selected: String? = null
    composeRule.setContent {
      PetitTheme {
        QuickAdd(
          onSelectDeworming = { selected = "deworming" },
          onSelectFleaAndTick = { selected = "flea-tick" },
        )
      }
    }

    composeRule.onNodeWithText(context.getString(R.string.quick_add_flea_tick)).performClick()

    assertThat(selected).isEqualTo("flea-tick")
  }

  @Composable
  private fun QuickAdd(onSelectDeworming: () -> Unit = {}, onSelectFleaAndTick: () -> Unit = {}) {
    QuickAddScreen(
      onNavigateBack = {},
      onSelectWeight = {},
      onSelectVaccination = {},
      onSelectDeworming = onSelectDeworming,
      onSelectFleaAndTick = onSelectFleaAndTick,
      onSelectReminder = {},
      onSelectNewPet = {},
    )
  }
}
