package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test

class TaskRecurrenceTest {

  @Test
  fun everyUnitAdvancesToTheExpectedNextOccurrence() {
    val start = LocalDateTime.of(2026, 3, 10, 8, 0)

    assertThat(recurrence(8, RecurrenceUnit.HOURS).nextOccurrence(start))
      .isEqualTo(LocalDateTime.of(2026, 3, 10, 16, 0))
    assertThat(recurrence(1, RecurrenceUnit.DAYS).nextOccurrence(start))
      .isEqualTo(LocalDateTime.of(2026, 3, 11, 8, 0))
    assertThat(recurrence(2, RecurrenceUnit.WEEKS).nextOccurrence(start))
      .isEqualTo(LocalDateTime.of(2026, 3, 24, 8, 0))
    assertThat(recurrence(3, RecurrenceUnit.MONTHS).nextOccurrence(start))
      .isEqualTo(LocalDateTime.of(2026, 6, 10, 8, 0))
    assertThat(recurrence(1, RecurrenceUnit.YEARS).nextOccurrence(start))
      .isEqualTo(LocalDateTime.of(2027, 3, 10, 8, 0))
  }

  @Test
  fun aWeeklyRuleWithChosenWeekdaysWalksThroughThemBeforeJumpingWeeks() {
    val rule =
      recurrence(1, RecurrenceUnit.WEEKS)
        .copy(weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
    val monday = LocalDateTime.of(2026, 3, 9, 7, 30)

    val wednesday = rule.nextOccurrence(monday)
    val friday = wednesday?.let { rule.nextOccurrence(it) }
    val nextMonday = friday?.let { rule.nextOccurrence(it) }

    assertThat(wednesday).isEqualTo(LocalDateTime.of(2026, 3, 11, 7, 30))
    assertThat(friday).isEqualTo(LocalDateTime.of(2026, 3, 13, 7, 30))
    assertThat(nextMonday).isEqualTo(LocalDateTime.of(2026, 3, 16, 7, 30))
  }

  @Test
  fun aWeeklyRuleWithAnIntervalSkipsWholeWeeksAfterTheLastChosenWeekday() {
    val rule =
      recurrence(2, RecurrenceUnit.WEEKS)
        .copy(weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
    val thursday = LocalDateTime.of(2026, 3, 12, 9, 0)

    assertThat(rule.nextOccurrence(thursday)).isEqualTo(LocalDateTime.of(2026, 3, 24, 9, 0))
  }

  @Test
  fun anHourlyRuleRollsOverToTheFirstSlotOfTheNextDayWhenItLeavesTheWindow() {
    val rule =
      recurrence(8, RecurrenceUnit.HOURS)
        .copy(dailyWindow = DailyWindow(LocalTime.of(7, 0), LocalTime.of(23, 0)))

    assertThat(rule.nextOccurrence(LocalDateTime.of(2026, 3, 10, 7, 0)))
      .isEqualTo(LocalDateTime.of(2026, 3, 10, 15, 0))
    assertThat(rule.nextOccurrence(LocalDateTime.of(2026, 3, 10, 23, 0)))
      .isEqualTo(LocalDateTime.of(2026, 3, 11, 7, 0))
  }

  @Test
  fun anHourlyOccurrenceBeforeTheWindowMovesToTheSameDayOpening() {
    val rule =
      recurrence(1, RecurrenceUnit.HOURS)
        .copy(dailyWindow = DailyWindow(LocalTime.of(7, 0), LocalTime.of(23, 0)))

    assertThat(rule.nextOccurrence(LocalDateTime.of(2026, 3, 10, 5, 0)))
      .isEqualTo(LocalDateTime.of(2026, 3, 10, 7, 0))
  }

  @Test
  fun anHourlyWindowShorterThanTheIntervalStillProducesOneOccurrencePerDay() {
    val rule =
      recurrence(12, RecurrenceUnit.HOURS)
        .copy(dailyWindow = DailyWindow(LocalTime.of(8, 0), LocalTime.of(12, 0)))

    val second = rule.nextOccurrence(LocalDateTime.of(2026, 3, 10, 8, 0))
    val third = second?.let { rule.nextOccurrence(it) }

    assertThat(second).isEqualTo(LocalDateTime.of(2026, 3, 11, 8, 0))
    assertThat(third).isEqualTo(LocalDateTime.of(2026, 3, 12, 8, 0))
  }

  @Test
  fun monthlyRulesClampToTheLastDayOfShorterMonths() {
    val rule = recurrence(1, RecurrenceUnit.MONTHS)

    assertThat(rule.nextOccurrence(LocalDateTime.of(2026, 1, 31, 9, 0)))
      .isEqualTo(LocalDateTime.of(2026, 2, 28, 9, 0))
    assertThat(rule.nextOccurrence(LocalDateTime.of(2028, 1, 31, 9, 0)))
      .isEqualTo(LocalDateTime.of(2028, 2, 29, 9, 0))
    assertThat(rule.nextOccurrence(LocalDateTime.of(2026, 3, 31, 9, 0)))
      .isEqualTo(LocalDateTime.of(2026, 4, 30, 9, 0))
  }

  @Test
  fun aYearlyRuleOnFebruaryTwentyNineFallsBackToTheTwentyEighth() {
    val rule = recurrence(1, RecurrenceUnit.YEARS)

    assertThat(rule.nextOccurrence(LocalDateTime.of(2028, 2, 29, 9, 0)))
      .isEqualTo(LocalDateTime.of(2029, 2, 28, 9, 0))
  }

  @Test
  fun anOccurrenceLandingInADaylightSavingGapStillResolvesToARealInstant() {
    val zone = ZoneId.of("America/Sao_Paulo")
    val rule = recurrence(1, RecurrenceUnit.DAYS)
    val beforeGap = LocalDateTime.of(2018, 11, 3, 0, 30)

    val next = rule.nextOccurrence(beforeGap)

    assertThat(next).isEqualTo(LocalDateTime.of(2018, 11, 4, 0, 30))
    assertThat(next!!.atZone(zone).toLocalTime()).isEqualTo(LocalTime.of(1, 30))
  }

  @Test
  fun aSeriesEndingOnADateStopsAfterThatDate() {
    val rule =
      recurrence(1, RecurrenceUnit.DAYS).copy(end = RecurrenceEnd.OnDate(LocalDate.of(2026, 3, 12)))

    assertThat(rule.nextOccurrence(LocalDateTime.of(2026, 3, 11, 9, 0)))
      .isEqualTo(LocalDateTime.of(2026, 3, 12, 9, 0))
    assertThat(rule.nextOccurrence(LocalDateTime.of(2026, 3, 12, 9, 0))).isNull()
  }

  @Test
  fun aSeriesEndingAfterAFixedNumberOfOccurrencesStopsAtTheLastOne() {
    val rule =
      recurrence(1, RecurrenceUnit.DAYS).copy(end = RecurrenceEnd.AfterOccurrences(count = 3))
    val first = LocalDateTime.of(2026, 3, 10, 9, 0)

    val second = rule.nextOccurrence(first, occurrenceIndex = 0)
    val third = rule.nextOccurrence(second!!, occurrenceIndex = 1)
    val fourth = rule.nextOccurrence(third!!, occurrenceIndex = 2)

    assertThat(second).isEqualTo(LocalDateTime.of(2026, 3, 11, 9, 0))
    assertThat(third).isEqualTo(LocalDateTime.of(2026, 3, 12, 9, 0))
    assertThat(fourth).isNull()
  }

  @Test
  fun catchingUpCollapsesEveryMissedOccurrenceIntoASinglePendingOne() {
    val rule = recurrence(1, RecurrenceUnit.DAYS)
    val missed = LocalDateTime.of(2026, 3, 1, 9, 0)
    val now = LocalDateTime.of(2026, 3, 10, 12, 0)

    val occurrence = rule.advanceBeyond(missed, occurrenceIndex = 0, now = now)

    assertThat(occurrence?.scheduledFor).isEqualTo(LocalDateTime.of(2026, 3, 11, 9, 0))
    assertThat(occurrence?.occurrenceIndex).isEqualTo(10)
  }

  @Test
  fun catchingUpReturnsNothingWhenTheSeriesAlreadyEnded() {
    val rule =
      recurrence(1, RecurrenceUnit.DAYS).copy(end = RecurrenceEnd.OnDate(LocalDate.of(2026, 3, 5)))

    val occurrence =
      rule.advanceBeyond(
        current = LocalDateTime.of(2026, 3, 1, 9, 0),
        occurrenceIndex = 0,
        now = LocalDateTime.of(2026, 3, 10, 12, 0),
      )

    assertThat(occurrence).isNull()
  }

  @Test
  fun unsupportedRulesNeverProduceOccurrences() {
    assertThat(recurrence(0, RecurrenceUnit.DAYS).isSupported).isFalse()
    assertThat(recurrence(24, RecurrenceUnit.HOURS).isSupported).isFalse()
    assertThat(recurrence(1000, RecurrenceUnit.DAYS).isSupported).isFalse()
    assertThat(
        recurrence(1, RecurrenceUnit.DAYS).copy(weekdays = setOf(DayOfWeek.MONDAY)).isSupported
      )
      .isFalse()
    assertThat(
        recurrence(1, RecurrenceUnit.HOURS)
          .copy(dailyWindow = DailyWindow(LocalTime.of(22, 0), LocalTime.of(8, 0)))
          .isSupported
      )
      .isFalse()
    assertThat(
        recurrence(1, RecurrenceUnit.DAYS).copy(end = RecurrenceEnd.AfterOccurrences(0)).isSupported
      )
      .isFalse()
    assertThat(
        recurrence(0, RecurrenceUnit.DAYS).nextOccurrence(LocalDateTime.of(2026, 3, 10, 9, 0))
      )
      .isNull()
  }

  @Test
  fun everyRuleShapeSurvivesAnEncodeDecodeRoundTrip() {
    val rules =
      listOf(
        recurrence(1, RecurrenceUnit.DAYS),
        recurrence(3, RecurrenceUnit.MONTHS).copy(end = RecurrenceEnd.AfterOccurrences(4)),
        recurrence(1, RecurrenceUnit.YEARS)
          .copy(end = RecurrenceEnd.OnDate(LocalDate.of(2030, 12, 31))),
        recurrence(1, RecurrenceUnit.WEEKS)
          .copy(weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
        recurrence(8, RecurrenceUnit.HOURS)
          .copy(dailyWindow = DailyWindow(LocalTime.of(7, 0), LocalTime.of(23, 0))),
      )

    rules.forEach { rule -> assertThat(TaskRecurrence.decode(rule.encode())).isEqualTo(rule) }
  }

  @Test
  fun unreadableOrUnsupportedStoredRulesDecodeAsNoRepeat() {
    assertThat(TaskRecurrence.decode(null)).isNull()
    assertThat(TaskRecurrence.decode("")).isNull()
    assertThat(TaskRecurrence.decode("1|DAYS")).isNull()
    assertThat(TaskRecurrence.decode("v2|1|DAYS||||NEVER|")).isNull()
    assertThat(TaskRecurrence.decode("v1|1|DECADES||||NEVER|")).isNull()
    assertThat(TaskRecurrence.decode("v1|1|WEEKS|FUNDAY|||NEVER|")).isNull()
    assertThat(TaskRecurrence.decode("v1|1|HOURS||07:00||NEVER|")).isNull()
    assertThat(TaskRecurrence.decode("v1|0|DAYS||||NEVER|")).isNull()
    assertThat(TaskRecurrence.decode("v1|1|DAYS||||FOREVER|")).isNull()
  }

  private fun recurrence(interval: Int, unit: RecurrenceUnit) =
    TaskRecurrence(interval = interval, unit = unit)
}
