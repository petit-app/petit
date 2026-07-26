package com.woliveiras.petit.presentation.feature.pets

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.BreedCatalogItem
import com.woliveiras.petit.domain.model.BreedIdentity
import com.woliveiras.petit.domain.model.PetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BreedSelectionViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun draftSelectionDoesNotReplaceInitialValueUntilCallerConfirms() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      val initial = BreedSelectionValue("VBO:0100221", "Siamese", "Siamês")
      viewModel.initialize(PetType.CAT, initial)
      advanceUntilIdle()

      viewModel.selectCatalogBreed(BreedCatalogItem("VBO:0100154", "Maine Coon", "Maine Coon"))

      assertThat(viewModel.uiState.value.initialSelection).isEqualTo(initial)
      assertThat(viewModel.uiState.value.draftSelection)
        .isEqualTo(BreedSelectionValue("VBO:0100154", "Maine Coon", "Maine Coon"))
      assertThat(viewModel.confirmSelection()).isEqualTo(viewModel.uiState.value.draftSelection)
    }

  @Test
  fun manualMixedUnknownAndEmptySelectionsProduceTheirDocumentedPairs() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      viewModel.initialize(PetType.DOG, BreedSelectionValue.EMPTY)
      advanceUntilIdle()

      viewModel.selectManualEntry()
      viewModel.updateManualBreed("  PERSIAN  ")
      assertThat(viewModel.confirmSelection())
        .isEqualTo(
          BreedSelectionValue(breedId = null, breed = "  PERSIAN  ", displayName = "  PERSIAN  ")
        )

      viewModel.selectMixedBreed()
      assertThat(viewModel.confirmSelection()?.breedId).isEqualTo(BreedIdentity.MIXED_BREED_ID)

      viewModel.selectUnknownBreed()
      assertThat(viewModel.confirmSelection()?.breedId).isEqualTo(BreedIdentity.UNKNOWN_BREED_ID)

      viewModel.selectNoBreed()
      assertThat(viewModel.confirmSelection()).isEqualTo(BreedSelectionValue.EMPTY)
    }

  @Test
  fun draftAndQueryRestoreWithoutPublishingAResult() =
    runTest(dispatcher) {
      val handle = SavedStateHandle()
      val original = viewModel(handle)
      original.initialize(PetType.CAT, BreedSelectionValue.EMPTY)
      advanceUntilIdle()
      original.updateQuery("siam")
      original.selectCatalogBreed(BreedCatalogItem("VBO:0100221", "Siamese", "Siamese"))
      advanceUntilIdle()

      val restored = viewModel(handle)
      advanceUntilIdle()

      assertThat(restored.uiState.value.species).isEqualTo(PetType.CAT)
      assertThat(restored.uiState.value.query).isEqualTo("siam")
      assertThat(restored.uiState.value.draftSelection)
        .isEqualTo(BreedSelectionValue("VBO:0100221", "Siamese", "Siamese"))
      assertThat(restored.uiState.value.initialSelection).isEqualTo(BreedSelectionValue.EMPTY)
    }

  @Test
  fun manualSelectionLongerThanFormLimitCannotBeConfirmed() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      viewModel.initialize(PetType.DOG, BreedSelectionValue.EMPTY)
      advanceUntilIdle()

      viewModel.selectManualEntry()
      viewModel.updateManualBreed("a".repeat(51))

      assertThat(viewModel.uiState.value.manualError).isNotNull()
      assertThat(viewModel.uiState.value.canConfirm).isFalse()
      assertThat(viewModel.confirmSelection()).isNull()
    }

  @Test
  fun manualLimitCountsPreservedWhitespace() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      viewModel.initialize(PetType.DOG, BreedSelectionValue.EMPTY)
      advanceUntilIdle()

      viewModel.selectManualEntry()
      viewModel.updateManualBreed(" ".repeat(2) + "a".repeat(49))

      assertThat(viewModel.uiState.value.canConfirm).isFalse()
      assertThat(viewModel.confirmSelection()).isNull()
    }

  private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
    BreedSelectionViewModel(handle, context, dispatcher)
}
