package com.woliveiras.petit.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.MainActivity
import com.woliveiras.petit.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class WeightHistoryJourneyTest {
  private val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule val rules: RuleChain = RuleChain.outerRule(ClearAppStateRule()).around(composeRule)

  @Test
  fun addWeight_displaysCurrentValueAndDatedHistory() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val next = context.getString(R.string.onboarding_next)
    val getStarted = context.getString(R.string.onboarding_get_started)
    val registerPet = context.getString(R.string.home_register_pet)
    val namePlaceholder = context.getString(R.string.pet_form_name_placeholder)
    val save = context.getString(R.string.action_save)
    val edit = context.getString(R.string.action_edit)
    val weightSection = context.getString(R.string.pet_detail_section_weight)
    val addWeight = context.getString(R.string.weight_add)
    val addWeightTitle = context.getString(R.string.weight_add_title)
    val historyTitle = context.getString(R.string.weight_history_title)
    val petName = "Luna"
    val weightValue = "4.5"
    val expectedHistoryDescription =
      "4.5 kg, ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(getStarted).performClick()
    composeRule.onNodeWithText(registerPet).performClick()
    composeRule.onNodeWithText(namePlaceholder).performTextInput(petName)
    closeSoftKeyboard()
    composeRule.onNodeWithText(save).performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithContentDescription(edit).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(weightSection).performScrollTo().performClick()
    composeRule.onNodeWithContentDescription(addWeight).performClick()
    composeRule.onNodeWithText(addWeightTitle).assertIsDisplayed()

    composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(weightValue)
    closeSoftKeyboard()
    composeRule.onNodeWithText(save).performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodes(hasContentDescription(expectedHistoryDescription, substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeRule.onNodeWithText(historyTitle).assertIsDisplayed()
    composeRule
      .onNode(hasContentDescription(expectedHistoryDescription, substring = true))
      .assertIsDisplayed()
  }
}
