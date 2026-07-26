package com.woliveiras.petit.presentation.util

import android.content.Context
import android.content.res.Configuration
import android.icu.text.DateFormat as IcuDateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.woliveiras.petit.R
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** App-owned, configuration-aware formatting for user-visible values. */
class AppDisplayFormatter(private val context: Context) {

  private val locale: Locale
    get() = context.resources.configuration.locales[0]

  fun shortDate(value: LocalDate): String = localizedDate(value, "yMd")

  fun dayMonth(value: LocalDate): String = localizedDate(value, "MMMd")

  fun monthYear(value: LocalDate): String = localizedDate(value, "MMMMy")

  fun time(value: LocalTime): String =
    value.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

  fun dateTime(value: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
      .apply { timeZone = TimeZone.getTimeZone(zoneId) }
      .format(Date.from(value))

  fun dateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    dateTime(Instant.ofEpochMilli(epochMillis), zoneId)

  fun decimal(value: Number, fractionDigits: Int): String =
    NumberFormat.getNumberInstance(locale)
      .apply {
        isGroupingUsed = false
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
      }
      .format(value)

  fun weight(weightGrams: Int): String =
    if (weightGrams < 1_000) {
      context.getString(R.string.weight_value_grams, decimal(weightGrams, fractionDigits = 0))
    } else {
      kilograms(weightGrams / 1_000.0)
    }

  fun kilograms(value: Double, fractionDigits: Int = 1): String =
    context.getString(R.string.weight_value_kilograms, decimal(value, fractionDigits))

  fun signedKilograms(value: Double): String =
    if (value > 0) "+${kilograms(value)}" else kilograms(value)

  fun parseDecimal(value: String): Double? {
    val normalized = value.trim().replace(',', '.')
    if (!DECIMAL_INPUT.matches(normalized)) return null
    return normalized.toDoubleOrNull()
  }

  private fun localizedDate(value: LocalDate, skeleton: String): String {
    val instant = value.atStartOfDay(ZoneId.systemDefault()).toInstant()
    return IcuDateFormat.getInstanceForSkeleton(skeleton, locale).format(Date.from(instant))
  }

  private companion object {
    val DECIMAL_INPUT = Regex("""[+-]?(?:\d+(?:\.\d+)?|\.\d+)""")
  }
}

@Composable
fun rememberAppDisplayFormatter(): AppDisplayFormatter {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  return remember(context, configuration) {
    AppDisplayFormatter(context.createConfigurationContext(Configuration(configuration)))
  }
}
