package com.woliveiras.petit.presentation.util

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone
import org.junit.After
import org.junit.Test

class PickerDateTest {

  private val originalTimeZone: TimeZone = TimeZone.getDefault()

  @After
  fun restoreTimeZone() {
    TimeZone.setDefault(originalTimeZone)
  }

  @Test
  fun convertsToUtcMidnightRegardlessOfDeviceTimeZone() {
    val date = LocalDate.of(2026, 7, 31)
    val expected = Instant.parse("2026-07-31T00:00:00Z").toEpochMilli()

    zones.forEach { zone ->
      TimeZone.setDefault(TimeZone.getTimeZone(zone))

      assertThat(PickerDate.toEpochMillis(date)).isEqualTo(expected)
      assertThat(PickerDate.toLocalDate(expected)).isEqualTo(date)
    }
  }

  @Test
  fun roundTripsEveryEdgeDateInEveryTimeZone() {
    val dates =
      listOf(
        LocalDate.of(2026, 7, 31),
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        LocalDate.of(2024, 2, 29),
        LocalDate.of(2026, 3, 29),
        LocalDate.of(2026, 10, 25),
        LocalDate.of(2026, 2, 15),
        LocalDate.of(1969, 12, 31),
      )

    zones.forEach { zone ->
      TimeZone.setDefault(TimeZone.getTimeZone(zone))

      dates.forEach { date ->
        assertThat(PickerDate.toLocalDate(PickerDate.toEpochMillis(date))).isEqualTo(date)
      }
    }
  }

  private companion object {
    val zones =
      listOf(
        "UTC",
        "Europe/Madrid",
        "Europe/Lisbon",
        "America/Sao_Paulo",
        "America/Los_Angeles",
        "Asia/Tokyo",
        "Pacific/Kiritimati",
        "Pacific/Niue",
        "Asia/Kathmandu",
      )
  }
}
