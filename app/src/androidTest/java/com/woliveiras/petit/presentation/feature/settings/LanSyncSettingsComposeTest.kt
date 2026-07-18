package com.woliveiras.petit.presentation.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.data.lan.LanSyncState
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Rule
import org.junit.Test

class LanSyncSettingsComposeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun localizedStateToggleAndManualActionAreAccessibleWithoutColor() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    var toggledTo: Boolean? = null
    var attempts = 0
    composeRule.setContent {
      PetitTheme {
        LanSyncSettingsCard(
          enabled = true,
          state = LanSyncState.Syncing("Kitchen"),
          onEnabledChange = { toggledTo = it },
          onAttemptNow = { attempts++ },
        )
      }
    }

    composeRule.onNodeWithText(resources.getString(R.string.lan_sync_title)).assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.lan_sync_syncing, "Kitchen"))
      .assertIsDisplayed()
    composeRule
      .onNode(hasClickAction() and androidx.compose.ui.test.isToggleable())
      .assertIsOn()
      .performClick()
    composeRule.onNodeWithText(resources.getString(R.string.lan_sync_try_now)).performClick()

    assertThat(toggledTo).isFalse()
    assertThat(attempts).isEqualTo(1)
  }
}
