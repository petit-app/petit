package com.woliveiras.petit.e2e

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DeleteAllDataJourneyTest {

  private val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule val rules: RuleChain = RuleChain.outerRule(ClearAppStateRule()).around(composeRule)

  @Test
  fun confirmedDeletionReturnsToAnEmptyHome() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val next = context.getString(R.string.onboarding_next)
    val getStarted = context.getString(R.string.onboarding_get_started)
    val registerPet = context.getString(R.string.home_register_pet)
    val namePlaceholder = context.getString(R.string.pet_form_name_placeholder)
    val save = context.getString(R.string.action_save)
    val edit = context.getString(R.string.action_edit)
    val profile = context.getString(R.string.nav_profile)
    val deleteAllData = context.getString(R.string.settings_delete_all_data)
    val confirmWord = context.getString(R.string.delete_all_data_confirm_word)
    val confirmDelete = context.getString(R.string.pet_delete_confirm_button)
    val deletionSuccess = context.getString(R.string.delete_all_data_success)
    val goHome = context.getString(R.string.pet_delete_go_home)
    val petName = "Delete journey pet"

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(getStarted).performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithText(registerPet, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeRule.onNodeWithText(registerPet, useUnmergedTree = true).performClick()
    composeRule.onNodeWithText(namePlaceholder).performTextInput(petName)
    closeSoftKeyboard()
    composeRule.onNodeWithText(save).performScrollTo().performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithContentDescription(edit).fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.onNodeWithText(profile).performClick()
    composeRule.onNodeWithText(deleteAllData).performScrollTo().performClick()
    composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(confirmWord)
    closeSoftKeyboard()
    composeRule.onNodeWithText(confirmDelete).performScrollTo().performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText(deletionSuccess).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(deletionSuccess).assertIsDisplayed()
    composeRule.onNodeWithText(goHome).performClick()
    composeRule.onNodeWithText(registerPet, useUnmergedTree = true).assertIsDisplayed()
    composeRule.onAllNodesWithText(petName).assertCountEquals(0)
  }
}
