package com.woliveiras.petit.presentation.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.DailyWindow
import com.woliveiras.petit.domain.model.RecurrenceEnd
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.domain.model.TaskRecurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecurrenceSummaryTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun aPlainCadenceReadsAsTheQuantityString() {
    val summary = recurrenceSummary(context, TaskRecurrence(3, RecurrenceUnit.MONTHS))

    assertThat(summary)
      .isEqualTo(context.resources.getQuantityString(R.plurals.repeat_summary_months, 3, 3))
  }

  @Test
  fun aSingleIntervalUsesTheSingularForm() {
    val summary = recurrenceSummary(context, TaskRecurrence(1, RecurrenceUnit.DAYS))

    assertThat(summary)
      .isEqualTo(context.resources.getQuantityString(R.plurals.repeat_summary_days, 1, 1))
  }

  @Test
  fun theChosenWeekdaysAreListed() {
    val summary =
      recurrenceSummary(
        context,
        TaskRecurrence(
          interval = 1,
          unit = RecurrenceUnit.WEEKS,
          weekdays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY),
        ),
      )

    val monday = DayOfWeek.MONDAY.getDisplayName(java.time.format.TextStyle.SHORT, locale())
    val wednesday = DayOfWeek.WEDNESDAY.getDisplayName(java.time.format.TextStyle.SHORT, locale())
    assertThat(summary).contains("$monday, $wednesday")
  }

  @Test
  fun anHourlyWindowIsSpelledOut() {
    val summary =
      recurrenceSummary(
        context,
        TaskRecurrence(
          interval = 8,
          unit = RecurrenceUnit.HOURS,
          dailyWindow = DailyWindow(LocalTime.of(7, 0), LocalTime.of(23, 0)),
        ),
      )

    val formatter = AppDisplayFormatter(context)
    assertThat(summary).contains(formatter.time(LocalTime.of(7, 0)))
    assertThat(summary).contains(formatter.time(LocalTime.of(23, 0)))
  }

  @Test
  fun anEndDateIsAppended() {
    val end = LocalDate.of(2026, 12, 31)
    val summary =
      recurrenceSummary(
        context,
        TaskRecurrence(1, RecurrenceUnit.DAYS, end = RecurrenceEnd.OnDate(end)),
      )

    assertThat(summary).contains(AppDisplayFormatter(context).shortDate(end))
  }

  @Test
  fun anOccurrenceCountIsAppended() {
    val summary =
      recurrenceSummary(
        context,
        TaskRecurrence(1, RecurrenceUnit.DAYS, end = RecurrenceEnd.AfterOccurrences(5)),
      )
    val cadence = context.resources.getQuantityString(R.plurals.repeat_summary_days, 1, 1)

    assertThat(summary)
      .isEqualTo(context.resources.getQuantityString(R.plurals.repeat_summary_times, 5, cadence, 5))
  }

  private fun locale() = context.resources.configuration.locales[0]
}
