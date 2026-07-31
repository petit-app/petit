package com.woliveiras.petit.presentation.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material 3 date pickers read and normalize their selection as UTC midnight, so the conversion
 * must ignore the device time zone to keep the selected calendar day.
 */
object PickerDate {

  fun toEpochMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

  fun toLocalDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
}
