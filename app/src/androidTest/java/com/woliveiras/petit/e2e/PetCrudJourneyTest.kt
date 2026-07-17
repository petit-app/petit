package com.woliveiras.petit.e2e

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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
class PetCrudJourneyTest {
  private val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule val rules: RuleChain = RuleChain.outerRule(ClearAppStateRule()).around(composeRule)

  @Test
  fun createEditAndDeletePet_updatesHomeAndRemovesPet() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val next = context.getString(R.string.onboarding_next)
    val getStarted = context.getString(R.string.onboarding_get_started)
    val registerPet = context.getString(R.string.home_register_pet)
    val newPet = context.getString(R.string.pet_form_title_new)
    val editPet = context.getString(R.string.pet_form_title_edit)
    val namePlaceholder = context.getString(R.string.pet_form_name_placeholder)
    val save = context.getString(R.string.action_save)
    val edit = context.getString(R.string.action_edit)
    val moreOptions = context.getString(R.string.action_more_options)
    val delete = context.getString(R.string.action_delete)
    val confirmDelete = context.getString(R.string.pet_delete_confirm_button)
    val goHome = context.getString(R.string.pet_delete_go_home)
    val originalName = "Luna"
    val editedName = "Milo"

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(getStarted).performClick()

    composeRule.onNodeWithText(registerPet).performClick()
    composeRule.onNodeWithText(newPet).assertIsDisplayed()
    composeRule.onNodeWithText(namePlaceholder).performTextInput(originalName)
    closeSoftKeyboard()
    composeRule.onNodeWithText(save).performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithContentDescription(edit).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onAllNodesWithText(originalName)[0].assertIsDisplayed()
    composeRule.onNodeWithContentDescription(edit).performClick()
    composeRule.onNodeWithText(editPet).assertIsDisplayed()
    composeRule.onNodeWithText(originalName).performTextReplacement(editedName)
    closeSoftKeyboard()
    composeRule.onNodeWithText(save).performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithContentDescription(edit).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onAllNodesWithText(editedName)[0].assertIsDisplayed()
    composeRule.onAllNodesWithText(originalName).assertCountEquals(0)
    composeRule.onNodeWithContentDescription(moreOptions).performClick()
    composeRule.onNodeWithText(delete).performClick()
    composeRule.onNodeWithText(confirmDelete).performScrollTo().performClick()

    val deletedTitle = context.getString(R.string.pet_delete_success_title, editedName)
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText(deletedTitle).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(deletedTitle).assertIsDisplayed()
    composeRule.onNodeWithText(goHome).performClick()
    composeRule.onNodeWithText(registerPet).assertIsDisplayed()
    composeRule.onAllNodesWithText(editedName).assertCountEquals(0)
  }
}
