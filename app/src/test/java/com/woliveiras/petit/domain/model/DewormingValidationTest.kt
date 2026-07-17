package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Test

class DewormingValidationTest {

  private val today = LocalDate.of(2026, 7, 17)
  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)

  @Test
  fun blankMedicationIsRejected() {
    assertThat(draft(medication = "   ").validate(clock))
      .containsExactly(DewormingValidationError.MEDICATION_REQUIRED)
  }

  @Test
  fun fieldLimitsAreValidated() {
    assertThat(draft(medication = "m".repeat(101), note = "n".repeat(501)).validate(clock))
      .containsExactly(
        DewormingValidationError.MEDICATION_TOO_LONG,
        DewormingValidationError.NOTE_TOO_LONG,
      )
      .inOrder()
  }

  @Test
  fun futureApplicationDateIsRejectedUsingControlledClock() {
    assertThat(draft(applicationDate = today.plusDays(1)).validate(clock))
      .containsExactly(DewormingValidationError.APPLICATION_DATE_IN_FUTURE)
  }

  @Test
  fun nextDoseMustBeStrictlyAfterApplicationDate() {
    assertThat(draft(nextDueDate = today).validate(clock))
      .containsExactly(DewormingValidationError.NEXT_DUE_DATE_NOT_AFTER_APPLICATION)
    assertThat(draft(nextDueDate = today.minusDays(1)).validate(clock))
      .containsExactly(DewormingValidationError.NEXT_DUE_DATE_NOT_AFTER_APPLICATION)
  }

  @Test
  fun validDraftAcceptsEveryTreatmentType() {
    DewormingType.entries.forEach { type ->
      assertThat(draft(type = type, nextDueDate = today.plusDays(1)).validate(clock)).isEmpty()
    }
  }

  private fun draft(
    type: DewormingType = DewormingType.INTERNAL,
    medication: String = "Milbemax",
    applicationDate: LocalDate = today,
    nextDueDate: LocalDate? = null,
    note: String = "",
  ) =
    DewormingDraft(
      type = type,
      medication = medication,
      applicationDate = applicationDate,
      nextDueDate = nextDueDate,
      note = note,
    )
}
