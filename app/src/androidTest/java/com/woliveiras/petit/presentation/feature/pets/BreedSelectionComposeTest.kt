package com.woliveiras.petit.presentation.feature.pets

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.BreedCatalogItem
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Rule
import org.junit.Test

class BreedSelectionComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun focusedScreenKeepsSearchAndConfirmationVisible() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext
    val queries = mutableListOf<String>()
    var confirmed = false
    setScreen(onQueryChange = queries::add, onConfirm = { confirmed = true })

    composeRule
      .onNodeWithText(resources.getString(R.string.breed_catalog_title))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.breed_catalog_search_label))
      .performTextReplacement("Siamês")
    composeRule.onNodeWithText(resources.getString(R.string.action_confirm)).performClick()

    composeRule.runOnIdle {
      assertThat(queries).contains("Siamês")
      assertThat(confirmed).isTrue()
    }
  }

  @Test
  fun systemBackLeavesWithoutPublishingConfirmation() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext
    var navigatedBack = false
    var confirmed = false
    setScreen(onNavigateBack = { navigatedBack = true }, onConfirm = { confirmed = true })

    composeRule
      .onNodeWithContentDescription(resources.getString(R.string.action_back))
      .performClick()

    composeRule.runOnIdle {
      assertThat(navigatedBack).isTrue()
      assertThat(confirmed).isFalse()
    }
  }

  @Test
  fun screenExposesCatalogAndNonCatalogChoices() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext
    setScreen()

    composeRule.onNodeWithText(resources.getString(R.string.breed_mixed)).assertIsDisplayed()
    composeRule.onNodeWithText(resources.getString(R.string.breed_unknown)).assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.breed_catalog_manual))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.pet_form_breed_none_selected))
      .assertIsDisplayed()
    composeRule.onNodeWithText("Siamês").assertIsDisplayed()
  }

  @Test
  fun futureIdentityRemainsVisibleSelectedAndAccessible() {
    val future =
      BreedSelectionValue(breedId = "VBO:9999999", breed = "Future fallback", displayName = null)
    setScreen(
      uiState =
        BreedSelectionUiState(
          species = com.woliveiras.petit.domain.model.PetType.CAT,
          initialSelection = future,
          draftSelection = future,
          selectionMode = BreedSelectionMode.CATALOG,
          results = emptyList(),
        )
    )

    composeRule
      .onNodeWithText("Future fallback")
      .assertIsDisplayed()
      .assertIsSelected()
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
  }

  private fun setScreen(
    uiState: BreedSelectionUiState =
      BreedSelectionUiState(
        species = com.woliveiras.petit.domain.model.PetType.CAT,
        results =
          listOf(
            BreedCatalogItem(id = "VBO:0100221", displayName = "Siamês", canonicalName = "Siamese")
          ),
      ),
    onNavigateBack: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onConfirm: () -> Unit = {},
  ) {
    composeRule.setContent {
      PetitTheme {
        BreedSelectionScreen(
          uiState = uiState,
          onNavigateBack = onNavigateBack,
          onQueryChange = onQueryChange,
          onCatalogSelected = {},
          onMixedSelected = {},
          onUnknownSelected = {},
          onManualSelected = {},
          onManualBreedChange = {},
          onNoBreedSelected = {},
          onInitialSelectionSelected = {},
          onConfirm = onConfirm,
        )
      }
    }
  }
}
