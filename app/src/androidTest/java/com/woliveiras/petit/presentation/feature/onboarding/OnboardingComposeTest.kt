package com.woliveiras.petit.presentation.feature.onboarding

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Rule
import org.junit.Test

class OnboardingComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun pagesAdvanceAndFinalActionCompletesOnboarding() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val next = context.getString(R.string.onboarding_next)
    val skip = context.getString(R.string.onboarding_skip)
    val getStarted = context.getString(R.string.onboarding_get_started)
    var completionCalls = 0

    composeRule.setContent {
      PetitTheme {
        OnboardingContent(uiState = OnboardingUiState(), onComplete = { completionCalls += 1 })
      }
    }

    composeRule
      .onNodeWithText(context.getString(R.string.onboarding_welcome_title))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.onboarding_welcome_description))
      .assertIsDisplayed()
    composeRule
      .onNode(hasStateDescription(context.getString(R.string.onboarding_page_indicator, 1, 3)))
      .assertIsDisplayed()

    composeRule.onNodeWithText(next).performClick()
    composeRule
      .onNodeWithText(context.getString(R.string.onboarding_features_title))
      .assertIsDisplayed()
    listOf(
        R.string.onboarding_feature_weight,
        R.string.onboarding_feature_vaccination,
        R.string.onboarding_feature_deworming,
        R.string.onboarding_feature_reminders,
      )
      .forEach { label -> composeRule.onNodeWithText(context.getString(label)).assertIsDisplayed() }
    composeRule
      .onNode(hasStateDescription(context.getString(R.string.onboarding_page_indicator, 2, 3)))
      .assertIsDisplayed()

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(context.getString(R.string.onboarding_cta_title)).assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.onboarding_cta_description))
      .assertIsDisplayed()
    composeRule.onAllNodesWithText(skip).assertCountEquals(0)
    composeRule
      .onNode(hasStateDescription(context.getString(R.string.onboarding_page_indicator, 3, 3)))
      .assertIsDisplayed()

    composeRule.onNodeWithText(getStarted).performClick()

    assertThat(completionCalls).isEqualTo(1)
  }

  @Test
  fun skipCompletesOnboardingFromFirstPage() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    var completionCalls = 0

    composeRule.setContent {
      PetitTheme {
        OnboardingContent(uiState = OnboardingUiState(), onComplete = { completionCalls += 1 })
      }
    }

    composeRule.onNodeWithText(context.getString(R.string.onboarding_skip)).performClick()

    assertThat(completionCalls).isEqualTo(1)
  }

  @Test
  fun completionInProgressDisablesCompletionActionsAndShowsProgress() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val next = context.getString(R.string.onboarding_next)

    composeRule.setContent {
      PetitTheme {
        OnboardingContent(uiState = OnboardingUiState(isCompleting = true), onComplete = {})
      }
    }

    composeRule.onNodeWithText(context.getString(R.string.onboarding_skip)).assertIsNotEnabled()
    composeRule
      .onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
      .assertCountEquals(1)

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(next).performClick()
    composeRule
      .onNodeWithText(context.getString(R.string.onboarding_get_started))
      .assertIsNotEnabled()
  }

  @Test
  fun completionErrorIsVisibleAndAllowsRetry() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    var completionCalls = 0

    composeRule.setContent {
      PetitTheme {
        OnboardingContent(
          uiState = OnboardingUiState(hasCompletionError = true),
          onComplete = { completionCalls += 1 },
        )
      }
    }

    composeRule
      .onNodeWithText(context.getString(R.string.onboarding_completion_error))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.onboarding_skip))
      .assertIsEnabled()
      .performClick()

    assertThat(completionCalls).isEqualTo(1)
  }
}
