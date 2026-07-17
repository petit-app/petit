package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Test

class DewormingEntryTest {

  private val today = LocalDate.of(2026, 7, 17)
  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)

  @Test
  fun statusUsesControlledClockAtEveryBoundary() {
    assertThat(entry(nextDueDate = null).status(clock)).isEqualTo(HealthStatus.OK)
    assertThat(entry(nextDueDate = today.minusDays(1)).status(clock))
      .isEqualTo(HealthStatus.OVERDUE)
    assertThat(entry(nextDueDate = today).status(clock)).isEqualTo(HealthStatus.SCHEDULED)
    assertThat(entry(nextDueDate = today.plusDays(30)).status(clock))
      .isEqualTo(HealthStatus.SCHEDULED)
    assertThat(entry(nextDueDate = today.plusDays(31)).status(clock)).isEqualTo(HealthStatus.OK)
  }

  @Test
  fun overdueUsesControlledClock() {
    assertThat(entry(nextDueDate = today.minusDays(1)).isOverdue(clock)).isTrue()
    assertThat(entry(nextDueDate = today).isOverdue(clock)).isFalse()
    assertThat(entry(nextDueDate = null).isOverdue(clock)).isFalse()
  }

  private fun entry(nextDueDate: LocalDate?) =
    DewormingEntry(
      id = "entry-1",
      petId = "pet-1",
      type = DewormingType.INTERNAL,
      medication = "Milbemax",
      applicationDate = today.minusMonths(1),
      nextDueDate = nextDueDate,
      createdAt = 1L,
      updatedAt = 1L,
    )
}
