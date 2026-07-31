package com.woliveiras.petit.presentation.feature.tasks

import com.woliveiras.petit.domain.model.DailyWindow
import com.woliveiras.petit.domain.model.RecurrenceEnd
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.domain.model.TaskRecurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Cadences offered in the task form; each one maps onto an interval and a unit. */
enum class RepeatPreset(val interval: Int, val unit: RecurrenceUnit?) {
  NONE(0, null),
  DAILY(1, RecurrenceUnit.DAYS),
  WEEKLY(1, RecurrenceUnit.WEEKS),
  MONTHLY(1, RecurrenceUnit.MONTHS),
  QUARTERLY(3, RecurrenceUnit.MONTHS),
  SEMIANNUAL(6, RecurrenceUnit.MONTHS),
  YEARLY(1, RecurrenceUnit.YEARS),
  CUSTOM(1, null);

  companion object {
    /**
     * The preset that produced [recurrence], so editing a task reopens the control it was set on.
     */
    fun of(recurrence: TaskRecurrence?): RepeatPreset {
      if (recurrence == null) return NONE
      val plain = recurrence.weekdays.isEmpty() && recurrence.dailyWindow == null
      if (!plain) return CUSTOM
      return entries.firstOrNull {
        it != NONE &&
          it != CUSTOM &&
          it.interval == recurrence.interval &&
          it.unit == recurrence.unit
      } ?: CUSTOM
    }
  }
}

/** How a series stops. */
enum class RepeatEndMode {
  NEVER,
  ON_DATE,
  AFTER_OCCURRENCES,
}

/** Repeat controls of the task form, kept separate so the form state stays readable. */
data class TaskRepeatFormState(
  val preset: RepeatPreset = RepeatPreset.NONE,
  val interval: String = "1",
  val unit: RecurrenceUnit = RecurrenceUnit.DAYS,
  val weekdays: Set<DayOfWeek> = emptySet(),
  val useDailyWindow: Boolean = false,
  val windowStart: LocalTime = DEFAULT_WINDOW_START,
  val windowEnd: LocalTime = DEFAULT_WINDOW_END,
  val endMode: RepeatEndMode = RepeatEndMode.NEVER,
  val endDate: LocalDate? = null,
  val endCount: String = DEFAULT_END_COUNT,
) {

  val repeats: Boolean
    get() = preset != RepeatPreset.NONE

  /** The unit the caregiver is effectively repeating on, which drives the extra controls. */
  val effectiveUnit: RecurrenceUnit?
    get() = if (preset == RepeatPreset.CUSTOM) unit else preset.unit

  val showsWeekdays: Boolean
    get() = effectiveUnit == RecurrenceUnit.WEEKS

  val showsDailyWindow: Boolean
    get() = effectiveUnit == RecurrenceUnit.HOURS

  /** The rule the controls describe, or null when the caregiver typed something unusable. */
  val recurrence: TaskRecurrence?
    get() {
      val unit = effectiveUnit ?: return null
      val interval =
        if (preset == RepeatPreset.CUSTOM) this.interval.trim().toIntOrNull() ?: return null
        else preset.interval
      val end =
        when (endMode) {
          RepeatEndMode.NEVER -> RecurrenceEnd.Never
          RepeatEndMode.ON_DATE -> RecurrenceEnd.OnDate(endDate ?: return null)
          RepeatEndMode.AFTER_OCCURRENCES ->
            RecurrenceEnd.AfterOccurrences(endCount.trim().toIntOrNull() ?: return null)
        }
      return TaskRecurrence(
          interval = interval,
          unit = unit,
          weekdays = if (unit == RecurrenceUnit.WEEKS) weekdays else emptySet(),
          dailyWindow =
            if (unit == RecurrenceUnit.HOURS && useDailyWindow) {
              DailyWindow(windowStart, windowEnd)
            } else null,
          end = end,
        )
        .takeIf { it.isSupported }
    }

  /**
   * Whether the controls describe a rule that can actually repeat from [scheduledFor]. An end that
   * cuts the series before its first follow-up would silently produce a task that never repeats.
   */
  fun isValidFor(scheduledFor: LocalDateTime): Boolean {
    val rule = recurrence ?: return false
    return rule.nextOccurrence(scheduledFor, occurrenceIndex = 0) != null
  }

  companion object {
    val DEFAULT_WINDOW_START: LocalTime = LocalTime.of(7, 0)
    val DEFAULT_WINDOW_END: LocalTime = LocalTime.of(23, 0)
    const val DEFAULT_END_COUNT = "10"

    /** Reopens the controls a stored rule was built from. */
    fun of(recurrence: TaskRecurrence?): TaskRepeatFormState {
      if (recurrence == null) return TaskRepeatFormState()
      val end = recurrence.end
      return TaskRepeatFormState(
        preset = RepeatPreset.of(recurrence),
        interval = recurrence.interval.toString(),
        unit = recurrence.unit,
        weekdays = recurrence.weekdays,
        useDailyWindow = recurrence.dailyWindow != null,
        windowStart = recurrence.dailyWindow?.start ?: DEFAULT_WINDOW_START,
        windowEnd = recurrence.dailyWindow?.end ?: DEFAULT_WINDOW_END,
        endMode =
          when (end) {
            RecurrenceEnd.Never -> RepeatEndMode.NEVER
            is RecurrenceEnd.OnDate -> RepeatEndMode.ON_DATE
            is RecurrenceEnd.AfterOccurrences -> RepeatEndMode.AFTER_OCCURRENCES
          },
        endDate = (end as? RecurrenceEnd.OnDate)?.date,
        endCount = (end as? RecurrenceEnd.AfterOccurrences)?.count?.toString() ?: DEFAULT_END_COUNT,
      )
    }
  }
}
