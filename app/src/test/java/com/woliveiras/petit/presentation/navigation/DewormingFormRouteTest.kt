package com.woliveiras.petit.presentation.navigation

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.DewormingType
import org.junit.Test

class DewormingFormRouteTest {

  @Test
  fun routeWithoutOptionalParamsKeepsTheOriginalPath() {
    assertThat(Screen.DewormingForm.createRoute("pet-1")).isEqualTo("pets/pet-1/deworming/form")
  }

  @Test
  fun typedEntryPointCarriesTheTreatmentType() {
    val route =
      Screen.DewormingForm.createRoute("pet-1", dewormingType = DewormingType.EXTERNAL.name)

    assertThat(route).isEqualTo("pets/pet-1/deworming/form?dewormingType=EXTERNAL")
  }

  @Test
  fun editRouteCombinesEntryIdAndTreatmentType() {
    val route =
      Screen.DewormingForm.createRoute(
        petId = "pet-1",
        entryId = "entry-1",
        dewormingType = DewormingType.INTERNAL.name,
      )

    assertThat(route).isEqualTo("pets/pet-1/deworming/form?entryId=entry-1&dewormingType=INTERNAL")
  }
}
