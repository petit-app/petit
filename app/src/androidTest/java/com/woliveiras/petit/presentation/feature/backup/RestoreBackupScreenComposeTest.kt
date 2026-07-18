package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.restore.RestoreMode
import org.junit.Rule
import org.junit.Test

class RestoreBackupScreenComposeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun mergePreferencesAndReplaceConfirmationAreAccessible() {
    var selectedMode: RestoreMode? = null
    var applyPreferences = false
    var confirmed = false
    composeRule.setContent {
      RestoreBackupScreen(
        state = RestoreBackupUiState(mode = RestoreMode.MERGE, showReplaceConfirmation = true),
        copy = copy(),
        onNavigateBack = {},
        onModeSelected = { selectedMode = it },
        onApplyPreferencesChanged = { applyPreferences = it },
        onRestore = {},
        onConfirmReplace = { confirmed = true },
        onDismissReplace = {},
      )
    }

    composeRule.onNodeWithText("Replace local data").performClick()
    composeRule.onNodeWithText("Apply backup preferences").performClick()
    composeRule
      .onNode(hasText("Replace everything") and hasClickAction())
      .assertIsDisplayed()
      .performClick()

    assertThat(selectedMode).isEqualTo(RestoreMode.REPLACE)
    assertThat(applyPreferences).isTrue()
    assertThat(confirmed).isTrue()
  }

  private fun copy() =
    RestoreBackupCopy(
      title = "Restore backup",
      explanation = "Choose how this backup changes local data.",
      merge = "Merge",
      mergeDescription = "Keep unique local and backup data.",
      replace = "Replace local data",
      replaceDescription = "Make restorable data match the backup exactly.",
      applyPreferences = "Apply backup preferences",
      restore = "Restore",
      restoring = "Restoring backup",
      success = "Restore complete",
      authorizationRequired = "Authorization required",
      invalidBackup = "This backup is invalid or incompatible",
      failed = "Restore failed",
      replaceConfirmationTitle = "Replace everything",
      replaceConfirmationMessage = "Local restorable data absent from the backup will be removed.",
      confirmReplace = "Replace everything",
      cancel = "Cancel",
    )
}
