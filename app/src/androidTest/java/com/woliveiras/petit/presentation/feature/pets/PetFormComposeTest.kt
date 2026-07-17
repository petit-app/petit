package com.woliveiras.petit.presentation.feature.pets

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.R
import com.woliveiras.petit.data.media.PendingCameraPhoto
import com.woliveiras.petit.data.media.PetPhotoStore
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.Pet
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
