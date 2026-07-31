package com.woliveiras.petit.presentation.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.RecurrenceEnd
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.domain.model.TaskRecurrence
import java.time.format.TextStyle

/** Plain-language description of a repeat rule, such as "Every 8 hours between 07:00 and 23:00". */
fun recurrenceSummary(context: Context, recurrence: TaskRecurrence): String {
  val formatter = AppDisplayFormatter(context)
  val cadence =
    context.resources.getQuantityString(
      when (recurrence.unit) {
        RecurrenceUnit.HOURS -> R.plurals.repeat_summary_hours
        RecurrenceUnit.DAYS -> R.plurals.repeat_summary_days
        RecurrenceUnit.WEEKS -> R.plurals.repeat_summary_weeks
        RecurrenceUnit.MONTHS -> R.plurals.repeat_summary_months
        RecurrenceUnit.YEARS -> R.plurals.repeat_summary_years
      },
      recurrence.interval,
      recurrence.interval,
    )

  val locale = context.resources.configuration.locales[0]
  val withDetails =
    when {
      recurrence.weekdays.isNotEmpty() ->
        context.getString(
          R.string.repeat_summary_weekdays,
          cadence,
          recurrence.weekdays.sorted().joinToString(", ") {
            it.getDisplayName(TextStyle.SHORT, locale)
          },
        )
      recurrence.dailyWindow != null ->
        context.getString(
          R.string.repeat_summary_window,
          cadence,
          formatter.time(recurrence.dailyWindow.start),
          formatter.time(recurrence.dailyWindow.end),
        )
      else -> cadence
    }

  return when (val end = recurrence.end) {
    RecurrenceEnd.Never -> withDetails
    is RecurrenceEnd.OnDate ->
      context.getString(R.string.repeat_summary_until, withDetails, formatter.shortDate(end.date))
    is RecurrenceEnd.AfterOccurrences ->
      context.resources.getQuantityString(
        R.plurals.repeat_summary_times,
        end.count,
        withDetails,
        end.count,
      )
  }
}

@Composable
fun TaskRecurrence.summary(): String {
  val context = LocalContext.current
  val recurrence = this
  return remember(context, recurrence) { recurrenceSummary(context, recurrence) }
}
