package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Test

class VaccinationValidationTest {

  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)
  private val valid =
    VaccinationDraft(
      petType = PetType.CAT,
      vaccineType = VaccineType.V3,
      applicationDate = LocalDate.of(2026, 7, 17),
    )

  @Test
  fun validDraftHasNoErrors() {
    assertThat(valid.validate(clock)).isEmpty()
  }

  @Test
  fun otherRequiresTrimmedVisibleNameAndLimitsItToOneHundredCharacters() {
    assertThat(valid.copy(vaccineType = VaccineType.OTHER, customName = "  ").validate(clock))
      .containsExactly(VaccinationValidationError.CUSTOM_NAME_REQUIRED)
    assertThat(
        valid.copy(vaccineType = VaccineType.OTHER, customName = " x ".repeat(51)).validate(clock)
      )
      .containsExactly(VaccinationValidationError.CUSTOM_NAME_TOO_LONG)
    assertThat(
        valid.copy(vaccineType = VaccineType.OTHER, customName = "  Especial  ").validate(clock)
      )
      .isEmpty()
  }

  @Test
  fun newOrChangedTypeMustBeOfferedByTheSpeciesCatalog() {
    assertThat(valid.copy(vaccineType = VaccineType.DHPP).validate(clock))
      .containsExactly(VaccinationValidationError.VACCINE_TYPE_NOT_APPLICABLE)
    assertThat(
        valid.copy(petType = PetType.RABBIT, vaccineType = VaccineType.RABIES).validate(clock)
      )
      .containsExactly(VaccinationValidationError.VACCINE_TYPE_NOT_APPLICABLE)
    assertThat(valid.copy(vaccineType = VaccineType.OTHER, customName = "Especial").validate(clock))
      .isEmpty()
  }

  @Test
  fun validationUsesTheExactCatalogMatrixForAllSixSpecies() {
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
  }

  @Test
  fun unchangedHistoricalIncompatibleTypeIsValidButAReplacementIsRejected() {
    val historical =
      valid.copy(
        petType = PetType.RABBIT,
        vaccineType = VaccineType.DHPP,
        historicalVaccineType = VaccineType.DHPP,
      )

    assertThat(historical.validate(clock)).isEmpty()
    assertThat(historical.copy(vaccineType = VaccineType.RABIES).validate(clock))
      .containsExactly(VaccinationValidationError.VACCINE_TYPE_NOT_APPLICABLE)
  }

  @Test
  fun applicationCannotBeFutureAndNextDoseMustBeAfterApplication() {
    assertThat(valid.copy(applicationDate = LocalDate.of(2026, 7, 18)).validate(clock))
      .containsExactly(VaccinationValidationError.APPLICATION_DATE_IN_FUTURE)
    assertThat(valid.copy(nextDueDate = valid.applicationDate).validate(clock))
      .containsExactly(VaccinationValidationError.NEXT_DUE_DATE_NOT_AFTER_APPLICATION)
    assertThat(valid.copy(nextDueDate = valid.applicationDate.minusDays(1)).validate(clock))
      .containsExactly(VaccinationValidationError.NEXT_DUE_DATE_NOT_AFTER_APPLICATION)
  }

  @Test
  fun traceabilityFieldsEnforceDocumentedLimits() {
    assertThat(
        valid
          .copy(
            veterinarian = "v".repeat(101),
            clinic = "c".repeat(101),
            batchNumber = "b".repeat(51),
            note = "n".repeat(501),
          )
          .validate(clock)
      )
      .containsExactly(
        VaccinationValidationError.VETERINARIAN_TOO_LONG,
        VaccinationValidationError.CLINIC_TOO_LONG,
        VaccinationValidationError.BATCH_NUMBER_TOO_LONG,
        VaccinationValidationError.NOTE_TOO_LONG,
      )
  }
}
