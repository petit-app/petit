package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpeciesCareCatalogTest {

  @Test
  fun everySupportedSpeciesHasAnExplicitDeterministicBreedMatrix() {
    assertThat(PetType.entries)
      .containsExactly(
        PetType.CAT,
        PetType.DOG,
        PetType.RABBIT,
        PetType.BIRD,
        PetType.HAMSTER,
        PetType.OTHER,
      )
      .inOrder()

    assertThat(SpeciesCareCatalog.breedPresets(PetType.CAT).map { it.storedValue })
      .containsExactly(
        "MIXED_BREED",
        "PERSIAN",
        "SIAMESE",
        "MAINE_COON",
        "RAGDOLL",
        "BRITISH_SHORTHAIR",
        "BENGAL",
        "ABYSSINIAN",
        "SPHYNX",
        "SCOTTISH_FOLD",
        "BURMESE",
        "RUSSIAN_BLUE",
        "NORWEGIAN_FOREST",
        "TURKISH_ANGORA",
        null,
      )
      .inOrder()
    assertThat(SpeciesCareCatalog.breedPresets(PetType.DOG).map { it.storedValue })
      .containsExactly(
        "MIXED_BREED",
        "LABRADOR",
        "GOLDEN_RETRIEVER",
        "GERMAN_SHEPHERD",
        "POODLE",
        "BULLDOG",
        "BEAGLE",
        "SHIH_TZU",
        "YORKSHIRE",
        null,
      )
      .inOrder()

    listOf(PetType.RABBIT, PetType.BIRD, PetType.HAMSTER, PetType.OTHER).forEach { petType ->
      assertThat(SpeciesCareCatalog.breedPresets(petType))
        .containsExactly(
          BreedPreset(storedValue = null, labelKey = "option_other", isManualEntry = true)
        )
    }
  }

  @Test
  fun everySupportedSpeciesHasTheApprovedVaccineMatrixWithoutSuggestedIntervals() {
    assertThat(SpeciesCareCatalog.vaccinePresets(PetType.CAT).map { it.vaccineType })
      .containsExactly(
        VaccineType.RABIES,
        VaccineType.V3,
        VaccineType.V4,
        VaccineType.V5,
        VaccineType.FELV,
        VaccineType.FIV,
        VaccineType.OTHER,
      )
      .inOrder()
    assertThat(SpeciesCareCatalog.vaccinePresets(PetType.DOG).map { it.vaccineType })
      .containsExactly(
        VaccineType.RABIES,
        VaccineType.DHPP,
        VaccineType.BORDETELLA,
        VaccineType.LEPTOSPIROSIS,
        VaccineType.LEISHMANIA,
        VaccineType.GRIPE_CANINA,
        VaccineType.OTHER,
      )
      .inOrder()
    assertThat(SpeciesCareCatalog.vaccinePresets(PetType.RABBIT).map { it.vaccineType })
      .containsExactly(VaccineType.RHDV, VaccineType.MYXOMATOSIS, VaccineType.OTHER)
      .inOrder()
    assertThat(SpeciesCareCatalog.vaccinePresets(PetType.BIRD).map { it.vaccineType })
      .containsExactly(VaccineType.POLYOMAVIRUS, VaccineType.OTHER)
      .inOrder()
    listOf(PetType.HAMSTER, PetType.OTHER).forEach { petType ->
      assertThat(SpeciesCareCatalog.vaccinePresets(petType).map { it.vaccineType })
        .containsExactly(VaccineType.OTHER)
    }

    PetType.entries.forEach { petType ->
      SpeciesCareCatalog.vaccinePresets(petType).forEach { preset ->
        assertThat(preset.suggestedIntervalMonths).isNull()
        assertThat(preset.labelKey).startsWith("vaccine_")
      }
    }
  }

  @Test
  fun antiparasiticRecordingIsManualOnlyForEverySpeciesAndCategory() {
    PetType.entries.forEach { petType ->
      DewormingType.entries.forEach { type ->
        assertThat(SpeciesCareCatalog.antiparasiticOptions(petType, type))
          .isEqualTo(
            AntiparasiticOptions(
              manualEntryLabelKey = "deworming_field_medication_custom",
              suggestedIntervalMonths = null,
            )
          )
      }
    }
  }

  @Test
  fun catalogLabelsAreResourceKeysAndNeverPersistedDisplayLabels() {
    PetType.entries.forEach { petType ->
      SpeciesCareCatalog.breedPresets(petType).forEach { preset ->
        assertThat(preset.labelKey).matches("[a-z0-9_]+")
        assertThat(preset.storedValue).isNotEqualTo(preset.labelKey)
      }
      SpeciesCareCatalog.vaccinePresets(petType).forEach { preset ->
        assertThat(preset.labelKey).matches("[a-z0-9_]+")
      }
    }
  }
}
