package com.woliveiras.petit.domain.model

import java.time.Clock
import java.time.LocalDate

data class DewormingDraft(
  val type: DewormingType,
  val medication: String,
  val applicationDate: LocalDate,
  val nextDueDate: LocalDate? = null,
  val note: String = "",
)

enum class DewormingValidationError {
  MEDICATION_REQUIRED,
  MEDICATION_TOO_LONG,
  APPLICATION_DATE_IN_FUTURE,
  NEXT_DUE_DATE_NOT_AFTER_APPLICATION,
  NOTE_TOO_LONG,
}

fun DewormingDraft.validate(clock: Clock): List<DewormingValidationError> = buildList {
  when {
    medication.isBlank() -> add(DewormingValidationError.MEDICATION_REQUIRED)
    medication.trim().length > 100 -> add(DewormingValidationError.MEDICATION_TOO_LONG)
  }
  if (applicationDate.isAfter(LocalDate.now(clock))) {
    add(DewormingValidationError.APPLICATION_DATE_IN_FUTURE)
  }
  if (nextDueDate != null && !nextDueDate.isAfter(applicationDate)) {
    add(DewormingValidationError.NEXT_DUE_DATE_NOT_AFTER_APPLICATION)
  }
  if (note.length > 500) add(DewormingValidationError.NOTE_TOO_LONG)
}
