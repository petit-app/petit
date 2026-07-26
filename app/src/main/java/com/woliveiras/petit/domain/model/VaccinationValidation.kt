package com.woliveiras.petit.domain.model

import java.time.Clock
import java.time.LocalDate

data class VaccinationDraft(
  val petType: PetType,
  val vaccineType: VaccineType,
  /**
   * The type originally saved on an edited record, retained only to preserve incompatible history.
   */
  val historicalVaccineType: VaccineType? = null,
  val customName: String = "",
  val applicationDate: LocalDate,
  val nextDueDate: LocalDate? = null,
  val veterinarian: String = "",
  val clinic: String = "",
  val batchNumber: String = "",
  val note: String = "",
)

enum class VaccinationValidationError {
  VACCINE_TYPE_NOT_APPLICABLE,
  CUSTOM_NAME_REQUIRED,
  CUSTOM_NAME_TOO_LONG,
  APPLICATION_DATE_IN_FUTURE,
  NEXT_DUE_DATE_NOT_AFTER_APPLICATION,
  VETERINARIAN_TOO_LONG,
  CLINIC_TOO_LONG,
  BATCH_NUMBER_TOO_LONG,
  NOTE_TOO_LONG,
}

fun VaccinationDraft.validate(clock: Clock): List<VaccinationValidationError> = buildList {
  val isHistoricalUnchangedType = vaccineType == historicalVaccineType
  val isCatalogType =
    SpeciesCareCatalog.vaccinePresets(petType).any { it.vaccineType == vaccineType }
  if (!isCatalogType && !isHistoricalUnchangedType) {
    add(VaccinationValidationError.VACCINE_TYPE_NOT_APPLICABLE)
  }
  if (vaccineType == VaccineType.OTHER) {
    when {
      customName.isBlank() -> add(VaccinationValidationError.CUSTOM_NAME_REQUIRED)
      customName.trim().length > 100 -> add(VaccinationValidationError.CUSTOM_NAME_TOO_LONG)
    }
  }
  if (applicationDate.isAfter(LocalDate.now(clock))) {
    add(VaccinationValidationError.APPLICATION_DATE_IN_FUTURE)
  }
  if (nextDueDate != null && !nextDueDate.isAfter(applicationDate)) {
    add(VaccinationValidationError.NEXT_DUE_DATE_NOT_AFTER_APPLICATION)
  }
  if (veterinarian.length > 100) add(VaccinationValidationError.VETERINARIAN_TOO_LONG)
  if (clinic.length > 100) add(VaccinationValidationError.CLINIC_TOO_LONG)
  if (batchNumber.length > 50) add(VaccinationValidationError.BATCH_NUMBER_TOO_LONG)
  if (note.length > 500) add(VaccinationValidationError.NOTE_TOO_LONG)
}
