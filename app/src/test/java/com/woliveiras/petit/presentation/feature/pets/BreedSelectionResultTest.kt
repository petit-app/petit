package com.woliveiras.petit.presentation.feature.pets

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BreedSelectionResultTest {

  @Test
  fun navigationResultPreservesKnownAndExactManualValues() {
    val known = BreedSelectionValue("VBO:0100221", "Siamese", "Siamês")
    val manual = BreedSelectionValue(null, "  PERSIAN  ", "  PERSIAN  ")

    assertThat(known.toNavigationResult().toBreedSelectionValue()).isEqualTo(known)
    assertThat(manual.toNavigationResult().toBreedSelectionValue()).isEqualTo(manual)
    assertThat(BreedSelectionValue.EMPTY.toNavigationResult().toBreedSelectionValue())
      .isEqualTo(BreedSelectionValue.EMPTY)
  }
}
