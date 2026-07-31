package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TaskSubjectOptionsTest {

  @Test
  fun eachTaskKindMapsToItsSubjectControl() {
    assertThat(TaskSubjectOptions.controlFor(TaskKind.VACCINATION))
      .isEqualTo(TaskSubjectControl.VACCINE)
    assertThat(TaskSubjectOptions.controlFor(TaskKind.DEWORMING))
      .isEqualTo(TaskSubjectControl.ANTIPARASITIC)
    assertThat(TaskSubjectOptions.controlFor(TaskKind.MEDICATION))
      .isEqualTo(TaskSubjectControl.MEDICATION)
    assertThat(TaskSubjectOptions.controlFor(TaskKind.WEIGHT)).isEqualTo(TaskSubjectControl.NONE)
    assertThat(TaskSubjectOptions.controlFor(TaskKind.CUSTOM)).isEqualTo(TaskSubjectControl.NONE)
  }

  @Test
  fun vaccineOptionsFollowTheSelectedSpecies() {
    val catOptions = TaskSubjectOptions.vaccineOptions(PetType.CAT)

    assertThat(catOptions).contains(VaccineType.FELV)
    assertThat(catOptions).doesNotContain(VaccineType.DHPP)
  }

  @Test
  fun vaccineOptionsWithoutPetListEveryTypeAndKeepOtherLast() {
    val options = TaskSubjectOptions.vaccineOptions(null)

    assertThat(options).containsExactlyElementsIn(VaccineType.entries)
    assertThat(options.last()).isEqualTo(VaccineType.OTHER)
  }

  @Test
  fun onlyTheOtherVaccineRequiresFreeText() {
    assertThat(
        TaskSubjectOptions.requiresFreeText(TaskSubjectControl.VACCINE, VaccineType.OTHER.name)
      )
      .isTrue()
    assertThat(
        TaskSubjectOptions.requiresFreeText(TaskSubjectControl.VACCINE, VaccineType.RABIES.name)
      )
      .isFalse()
    assertThat(TaskSubjectOptions.requiresFreeText(TaskSubjectControl.MEDICATION, null)).isFalse()
    assertThat(TaskSubjectOptions.requiresFreeText(TaskSubjectControl.NONE, null)).isFalse()
  }

  @Test
  fun suggestionsIgnoreAccentsAndCase() {
    val used = listOf("Antibiótico", "Apoquel")

    assertThat(TaskSubjectOptions.matchingSuggestions("antibio", used))
      .containsExactly("Antibiótico")
  }

  @Test
  fun suggestionsSkipWhatTheCaregiverAlreadyTyped() {
    val used = listOf("Apoquel")

    assertThat(TaskSubjectOptions.matchingSuggestions("Apoquel", used)).isEmpty()
  }

  @Test
  fun emptyHistoryProducesNoSuggestions() {
    assertThat(TaskSubjectOptions.matchingSuggestions("apo", emptyList())).isEmpty()
  }

  @Test
  fun blankQueryOffersTheMostRecentNames() {
    val used = listOf("Apoquel", "Antibiótico")

    assertThat(TaskSubjectOptions.matchingSuggestions("", used)).isEqualTo(used)
  }
}
