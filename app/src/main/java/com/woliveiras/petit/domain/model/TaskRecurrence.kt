package com.woliveiras.petit.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** How often a series repeats. */
enum class RecurrenceUnit {
  HOURS,
  DAYS,
  WEEKS,
  MONTHS,
  YEARS,
}

/** Hours of the day an hourly series is allowed to run, so a treatment does not fire overnight. */
data class DailyWindow(val start: LocalTime, val end: LocalTime) {
  val isUsable: Boolean
    get() = start < end
}

/** When a series stops producing occurrences. */
sealed interface RecurrenceEnd {
  data object Never : RecurrenceEnd

  data class OnDate(val date: LocalDate) : RecurrenceEnd

  data class AfterOccurrences(val count: Int) : RecurrenceEnd
}

/**
 * A repeat rule owned by the caregiver.
 *
 * Occurrences are computed on local dates so a series keeps its wall-clock time across daylight
 * saving changes.
 */
data class TaskRecurrence(
  val interval: Int,
  val unit: RecurrenceUnit,
  val weekdays: Set<DayOfWeek> = emptySet(),
  val dailyWindow: DailyWindow? = null,
  val end: RecurrenceEnd = RecurrenceEnd.Never,
) {

  /** Rules outside these bounds come from a corrupt payload and are dropped instead of applied. */
  val isSupported: Boolean
    get() =
      interval >= 1 &&
        interval <= MAX_INTERVAL &&
        (unit != RecurrenceUnit.HOURS || interval <= MAX_HOURLY_INTERVAL) &&
        (unit == RecurrenceUnit.WEEKS || weekdays.isEmpty()) &&
        (unit == RecurrenceUnit.HOURS || dailyWindow == null) &&
        (dailyWindow?.isUsable != false) &&
        (end !is RecurrenceEnd.AfterOccurrences || end.count >= 1)

  /**
   * The occurrence following [current], or null once the end condition is reached.
   *
   * [occurrenceIndex] is how many occurrences of the series came before [current].
   */
  fun nextOccurrence(current: LocalDateTime, occurrenceIndex: Int = 0): LocalDateTime? {
    if (!isSupported) return null
    val candidate =
      when (unit) {
        RecurrenceUnit.HOURS -> nextHourly(current)
        RecurrenceUnit.DAYS -> current.plusDays(interval.toLong())
        RecurrenceUnit.WEEKS -> nextWeekly(current)
        RecurrenceUnit.MONTHS -> current.plusMonths(interval.toLong())
        RecurrenceUnit.YEARS -> current.plusYears(interval.toLong())
      }
    return candidate.takeIf { isWithinEnd(it, occurrenceIndex) }
  }

  /**
   * The occurrence a pending task should sit on at [now].
   *
   * An occurrence is only replaced once the following one has arrived, so the caregiver keeps a
   * full interval to mark a dose as done. Returns the unchanged position when nothing is due yet.
   */
  fun occurrenceDueAt(
    current: LocalDateTime,
    occurrenceIndex: Int,
    now: LocalDateTime,
  ): Occurrence {
    var scheduledFor = current
    var index = occurrenceIndex
    repeat(MAX_CATCH_UP_STEPS) {
      val next = nextOccurrence(scheduledFor, index)
      if (next == null || next.isAfter(now)) return Occurrence(scheduledFor, index)
      scheduledFor = next
      index += 1
    }
    return Occurrence(scheduledFor, index)
  }

  /**
   * The first occurrence strictly after [now], for a series that ran out of date while the app was
   * closed. Collapses every missed occurrence into one so reopening the app cannot burst
   * notifications.
   */
  fun advanceBeyond(current: LocalDateTime, occurrenceIndex: Int, now: LocalDateTime): Occurrence? {
    var scheduledFor = current
    var index = occurrenceIndex
    repeat(MAX_CATCH_UP_STEPS) {
      val next = nextOccurrence(scheduledFor, index) ?: return null
      scheduledFor = next
      index += 1
      if (scheduledFor.isAfter(now)) return Occurrence(scheduledFor, index)
    }
    return null
  }

  private fun nextHourly(current: LocalDateTime): LocalDateTime {
    val candidate = current.plusHours(interval.toLong())
    val window = dailyWindow ?: return candidate
    val time = candidate.toLocalTime()
    return when {
      time < window.start -> LocalDateTime.of(candidate.toLocalDate(), window.start)
      time > window.end -> LocalDateTime.of(candidate.toLocalDate().plusDays(1), window.start)
      else -> candidate
    }
  }

  private fun nextWeekly(current: LocalDateTime): LocalDateTime {
    if (weekdays.isEmpty()) return current.plusWeeks(interval.toLong())
    val time = current.toLocalTime()
    val date = current.toLocalDate()
    val weekStart = date.with(DayOfWeek.MONDAY)
    val laterThisWeek = weekdays.map(weekStart::with).filter { it.isAfter(date) }.minOrNull()
    if (laterThisWeek != null) return LocalDateTime.of(laterThisWeek, time)
    val nextWeekStart = weekStart.plusWeeks(interval.toLong())
    return LocalDateTime.of(weekdays.map(nextWeekStart::with).min(), time)
  }

  private fun isWithinEnd(candidate: LocalDateTime, occurrenceIndex: Int): Boolean =
    when (end) {
      RecurrenceEnd.Never -> true
      is RecurrenceEnd.OnDate -> !candidate.toLocalDate().isAfter(end.date)
      is RecurrenceEnd.AfterOccurrences -> occurrenceIndex + 2 <= end.count
    }

  /**
   * Compact representation stored in the database and carried in export bundles, so both use a
   * single codec.
   *
   * Shape: `v1|interval|unit|weekdays|windowStart|windowEnd|endType|endValue`.
   */
  fun encode(): String =
    listOf(
        VERSION,
        interval.toString(),
        unit.name,
        weekdays.sorted().joinToString(WEEKDAY_SEPARATOR) { it.name },
        dailyWindow?.start?.toString().orEmpty(),
        dailyWindow?.end?.toString().orEmpty(),
        when (end) {
          RecurrenceEnd.Never -> END_NEVER
          is RecurrenceEnd.OnDate -> END_ON_DATE
          is RecurrenceEnd.AfterOccurrences -> END_AFTER_OCCURRENCES
        },
        when (end) {
          RecurrenceEnd.Never -> ""
          is RecurrenceEnd.OnDate -> end.date.toString()
          is RecurrenceEnd.AfterOccurrences -> end.count.toString()
        },
      )
      .joinToString(FIELD_SEPARATOR)

  /** A generated occurrence and its position in the series. */
  data class Occurrence(val scheduledFor: LocalDateTime, val occurrenceIndex: Int)

  companion object {
    const val MAX_INTERVAL = 999
    const val MAX_HOURLY_INTERVAL = 23

    /** Bounds catch-up work: an hourly series left alone for years must still resolve. */
    private const val MAX_CATCH_UP_STEPS = 100_000

    private const val VERSION = "v1"
    private const val FIELD_SEPARATOR = "|"
    private const val WEEKDAY_SEPARATOR = ","
    private const val FIELD_COUNT = 8
    private const val END_NEVER = "NEVER"
    private const val END_ON_DATE = "ON_DATE"
    private const val END_AFTER_OCCURRENCES = "AFTER_OCCURRENCES"

    /** Reads a stored rule. Anything unreadable or unsupported becomes a non-repeating task. */
    fun decode(value: String?): TaskRecurrence? {
      val fields = value?.split(FIELD_SEPARATOR) ?: return null
      if (fields.size != FIELD_COUNT || fields[0] != VERSION) return null
      val interval = fields[1].toIntOrNull() ?: return null
      val unit = RecurrenceUnit.entries.firstOrNull { it.name == fields[2] } ?: return null
      val weekdays =
        fields[3]
          .split(WEEKDAY_SEPARATOR)
          .filter { it.isNotBlank() }
          .map { name -> DayOfWeek.entries.firstOrNull { it.name == name } ?: return null }
          .toSet()
      val window = decodeWindow(fields[4], fields[5]) ?: return null
      val end = decodeEnd(fields[6], fields[7]) ?: return null
      return TaskRecurrence(
          interval = interval,
          unit = unit,
          weekdays = weekdays,
          dailyWindow = window.value,
          end = end,
        )
        .takeIf { it.isSupported }
    }

    /** Null means malformed; a boxed null value means the rule simply has no window. */
    private fun decodeWindow(start: String, end: String): OptionalWindow? =
      when {
        start.isEmpty() && end.isEmpty() -> OptionalWindow(null)
        start.isEmpty() || end.isEmpty() -> null
        else ->
          runCatching { DailyWindow(LocalTime.parse(start), LocalTime.parse(end)) }
            .getOrNull()
            ?.let(::OptionalWindow)
      }

    private fun decodeEnd(type: String, value: String): RecurrenceEnd? =
      when (type) {
        END_NEVER -> RecurrenceEnd.Never
        END_ON_DATE -> runCatching { RecurrenceEnd.OnDate(LocalDate.parse(value)) }.getOrNull()
        END_AFTER_OCCURRENCES -> value.toIntOrNull()?.let(RecurrenceEnd::AfterOccurrences)
        else -> null
      }

    private class OptionalWindow(val value: DailyWindow?)
  }
}
