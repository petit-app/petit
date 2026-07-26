package com.woliveiras.petit.presentation.feature.pets

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.data.media.PendingCameraPhoto
import com.woliveiras.petit.data.media.PetPhotoStore
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.ui.theme.PetitTheme
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class PetFormComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun createFormShowsRequiredFieldsAndPhotoSourceChoices() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resources = InstrumentationRegistry.getInstrumentation().targetContext
    val viewModel =
      PetFormViewModel(
        SavedStateHandle(),
        context,
        EmptyPetRepository(),
        EmptyPhotoStore(),
        Clock.systemUTC(),
        Dispatchers.IO,
      )

    composeRule.setContent {
      PetitTheme {
        PetFormScreen(petId = null, onNavigateBack = {}, onPetSaved = {}, viewModel = viewModel)
      }
    }

    composeRule.onNodeWithText(resources.getString(R.string.pet_form_title_new)).assertIsDisplayed()
    composeRule.onNodeWithText(resources.getString(R.string.pet_form_name)).assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription(resources.getString(R.string.pet_form_add_photo))
      .performClick()

    composeRule.onNodeWithText(resources.getString(R.string.pet_photo_gallery)).assertIsDisplayed()
    composeRule.onNodeWithText(resources.getString(R.string.pet_photo_camera)).assertIsDisplayed()
  }

  @Test
  fun catBreedSelectorSearchesCatalogAndExposesNonBreedChoices() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext

    val viewModel = showFormFor(PetType.CAT)
    openBreedMenu(resources)

    composeRule.onNodeWithText(resources.getString(R.string.breed_mixed)).assertIsDisplayed()
    composeRule.onNodeWithText(resources.getString(R.string.breed_unknown)).assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.breed_catalog_search_label))
      .performTextReplacement("Siamês")
    assertResultCount(viewModel, 2)
    composeRule
      .onNodeWithText(resources.getString(R.string.breed_catalog_manual))
      .assertIsDisplayed()
  }

  @Test
  fun dogBreedSelectorSearchesCatalogAndExposesNonBreedChoices() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext

    val viewModel = showFormFor(PetType.DOG)
    openBreedMenu(resources)

    composeRule.onNodeWithText(resources.getString(R.string.breed_mixed)).assertIsDisplayed()
    composeRule.onNodeWithText(resources.getString(R.string.breed_unknown)).assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.breed_catalog_search_label))
      .performTextReplacement("German Shepherd Dog")
    assertResultCount(viewModel, 1)
    composeRule
      .onNodeWithText(resources.getString(R.string.breed_catalog_manual))
      .assertIsDisplayed()
  }

  @Test
  fun rabbitBirdHamsterAndOtherBreedMenusExposeOnlyManualOther() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext
    val presetLabels =
      listOf(
        resources.getString(R.string.breed_mixed),
        resources.getString(R.string.breed_persian),
        resources.getString(R.string.breed_labrador),
      )

    val viewModel = formViewModel()
    viewModel.updatePetType(PetType.RABBIT)
    composeRule.setContent {
      PetitTheme {
        PetFormScreen(petId = null, onNavigateBack = {}, onPetSaved = {}, viewModel = viewModel)
      }
    }

    listOf(PetType.RABBIT, PetType.BIRD, PetType.HAMSTER, PetType.OTHER).forEach { petType ->
      viewModel.updatePetType(petType)
      composeRule.waitForIdle()
      if (petType == PetType.RABBIT) openLegacyBreedMenu(resources)
      assertManualOnlyBreedMenu(resources, presetLabels)
    }
  }

  @Test
  fun legacyBreedRemainsVisibleAndEditableAfterSpeciesChange() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext
    val legacyBreed = "Legacy rescue breed"
    val replacement = "Custom rabbit breed"
    val viewModel = formViewModel()
    viewModel.updatePetType(PetType.CAT)
    viewModel.updateBreed(legacyBreed)
    viewModel.updatePetType(PetType.RABBIT)

    composeRule.setContent {
      PetitTheme {
        PetFormScreen(petId = null, onNavigateBack = {}, onPetSaved = {}, viewModel = viewModel)
      }
    }

    composeRule
      .onNodeWithText(legacyBreed)
      .performScrollTo()
      .assertIsDisplayed()
      .performTextReplacement(replacement)
    composeRule.onNodeWithText(replacement).assertIsDisplayed()
  }

  @Test
  fun manualTextThatLooksLikeLegacyKeyRemainsExact() {
    val viewModel = formViewModel()
    viewModel.updatePetType(PetType.CAT)
    viewModel.updateBreed("PERSIAN")

    composeRule.setContent {
      PetitTheme {
        PetFormScreen(petId = null, onNavigateBack = {}, onPetSaved = {}, viewModel = viewModel)
      }
    }

    composeRule
      .onNode(hasText("PERSIAN") and hasSetTextAction())
      .performScrollTo()
      .assertIsDisplayed()
  }

  private fun showFormFor(petType: PetType): PetFormViewModel {
    val viewModel = formViewModel()
    viewModel.updatePetType(petType)

    composeRule.setContent {
      PetitTheme {
        PetFormScreen(petId = null, onNavigateBack = {}, onPetSaved = {}, viewModel = viewModel)
      }
    }
    return viewModel
  }

  private fun openBreedMenu(context: Context) {
    composeRule
      .onNodeWithContentDescription(context.getString(R.string.pet_form_breed))
      .performScrollTo()
      .performClick()
  }

  private fun openLegacyBreedMenu(context: Context) {
    composeRule
      .onNodeWithText(context.getString(R.string.pet_form_breed_select))
      .performScrollTo()
      .performClick()
  }

  private fun assertManualOnlyBreedMenu(context: Context, presetLabels: List<String>) {
    assertBreedMenuItem(context.getString(R.string.option_other))
    presetLabels.forEach { label -> composeRule.onAllNodesWithText(label).assertCountEquals(0) }
  }

  private fun assertResultCount(viewModel: PetFormViewModel, count: Int) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      !viewModel.uiState.value.isBreedCatalogLoading &&
        !viewModel.uiState.value.isBreedSearchLoading &&
        viewModel.uiState.value.breedQuery.isNotEmpty()
    }
    assertThat(viewModel.uiState.value.breedResults).hasSize(count)
  }

  private fun assertBreedMenuItem(label: String) {
    composeRule
      .onNode(hasText(label) and hasAnyAncestor(isPopup()))
      .performScrollTo()
      .assertIsDisplayed()
  }

  private fun formViewModel() =
    PetFormViewModel(
      SavedStateHandle(),
      ApplicationProvider.getApplicationContext(),
      EmptyPetRepository(),
      EmptyPhotoStore(),
      Clock.systemUTC(),
      Dispatchers.IO,
    )

  private class EmptyPetRepository : PetRepository {
    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(emptyList())

    override suspend fun getPetById(id: String): Pet? = null

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(null)

    override fun getPetCount(): Flow<Int> = MutableStateFlow(0)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class EmptyPhotoStore : PetPhotoStore {
    override fun importFromPicker(source: Uri): Result<String> = Result.failure(Exception())

    override fun createCameraPhoto(): Result<PendingCameraPhoto> = Result.failure(Exception())

    override fun completeCameraPhoto(
      pending: PendingCameraPhoto,
      success: Boolean,
    ): Result<String> = Result.failure(Exception())
  }
}
