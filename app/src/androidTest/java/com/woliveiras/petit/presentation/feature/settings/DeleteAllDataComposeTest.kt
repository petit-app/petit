package com.woliveiras.petit.presentation.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Rule
import org.junit.Test

class DeleteAllDataComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun mismatchedConfirmationKeepsDestructiveActionDisabled() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val delete = context.getString(R.string.pet_delete_confirm_button)

    composeRule.setContent {
      PetitTheme {
        DeleteConfirmationContent(
          uiState = DeleteAllDataUiState(confirmText = "delete"),
          onNavigateBack = {},
          onConfirmTextChanged = {},
          onDelete = {},
        )
      }
    }

    composeRule.onNodeWithText(delete).assertIsNotEnabled()
  }

  @Test
  fun errorIsVisibleAndRetryRunsTheSameConfirmedAction() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val confirmWord = context.getString(R.string.delete_all_data_confirm_word)
    val retry = context.getString(R.string.action_retry)
    var calls = 0

    composeRule.setContent {
      PetitTheme {
        DeleteConfirmationContent(
          uiState =
            DeleteAllDataUiState(confirmText = confirmWord, errorMessage = "Room unavailable"),
          onNavigateBack = {},
          onConfirmTextChanged = {},
          onDelete = { calls += 1 },
        )
      }
    }

    composeRule.onNodeWithText("Room unavailable").assertIsDisplayed()
    composeRule.onNodeWithText(retry).assertIsEnabled().performClick()

    assertThat(calls).isEqualTo(1)
  }

  @Test
  fun runningOperationBlocksNavigation() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val cancel = context.getString(R.string.action_cancel)

    composeRule.setContent {
      PetitTheme {
        DeleteConfirmationContent(
          uiState =
            DeleteAllDataUiState(
              confirmText = context.getString(R.string.delete_all_data_confirm_word),
              isDeleting = true,
              errorMessage = "Room unavailable",
            ),
          onNavigateBack = {},
          onConfirmTextChanged = {},
          onDelete = {},
        )
      }
    }

    composeRule.onNodeWithText(cancel).assertIsNotEnabled()
  }
}
