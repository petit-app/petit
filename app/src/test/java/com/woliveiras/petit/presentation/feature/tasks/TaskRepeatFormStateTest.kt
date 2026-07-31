package com.woliveiras.petit.presentation.feature.tasks

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.DailyWindow
import com.woliveiras.petit.domain.model.RecurrenceEnd
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.domain.model.TaskRecurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Test

class TaskRepeatFormStateTest {

  @Test
  fun theDefaultStateDoesNotRepeat() {
    val state = TaskRepeatFormState()

    assertThat(state.repeats).isFalse()
    assertThat(state.recurrence).isNull()
  }

  @Test
  fun everyPresetProducesItsCadence() {
    val expected =
      mapOf(
        RepeatPreset.DAILY to (1 to RecurrenceUnit.DAYS),
        RepeatPreset.WEEKLY to (1 to RecurrenceUnit.WEEKS),
        RepeatPreset.MONTHLY to (1 to RecurrenceUnit.MONTHS),
        RepeatPreset.QUARTERLY to (3 to RecurrenceUnit.MONTHS),
        RepeatPreset.SEMIANNUAL to (6 to RecurrenceUnit.MONTHS),
        RepeatPreset.YEARLY to (1 to RecurrenceUnit.YEARS),
      )

    expected.forEach { (preset, cadence) ->
      val recurrence = TaskRepeatFormState(preset = preset).recurrence
      assertThat(recurrence?.interval).isEqualTo(cadence.first)
      assertThat(recurrence?.unit).isEqualTo(cadence.second)
    }
  }

  @Test
  fun theCustomPresetUsesTheTypedIntervalAndUnit() {
    val state =
      TaskRepeatFormState(
        preset = RepeatPreset.CUSTOM,
        interval = "8",
        unit = RecurrenceUnit.HOURS,
        useDailyWindow = true,
        windowStart = LocalTime.of(8, 0),
        windowEnd = LocalTime.of(22, 0),
      )

    val recurrence = state.recurrence
    assertThat(recurrence?.interval).isEqualTo(8)
    assertThat(recurrence?.unit).isEqualTo(RecurrenceUnit.HOURS)
    assertThat(recurrence?.dailyWindow)
      .isEqualTo(DailyWindow(LocalTime.of(8, 0), LocalTime.of(22, 0)))
  }

  @Test
  fun weekdaysOnlyReachTheRuleOnAWeeklyCadence() {
    val weekly =
      TaskRepeatFormState(preset = RepeatPreset.WEEKLY, weekdays = setOf(DayOfWeek.MONDAY))
    val monthly =
      TaskRepeatFormState(preset = RepeatPreset.MONTHLY, weekdays = setOf(DayOfWeek.MONDAY))

    assertThat(weekly.showsWeekdays).isTrue()
    assertThat(weekly.recurrence?.weekdays).containsExactly(DayOfWeek.MONDAY)
    assertThat(monthly.showsWeekdays).isFalse()
    assertThat(monthly.recurrence?.weekdays).isEmpty()
  }

  @Test
  fun anUnusableIntervalOrEndConditionYieldsNoRule() {
    assertThat(TaskRepeatFormState(preset = RepeatPreset.CUSTOM, interval = "").recurrence).isNull()
    assertThat(TaskRepeatFormState(preset = RepeatPreset.CUSTOM, interval = "0").recurrence)
      .isNull()
    assertThat(
        TaskRepeatFormState(
            preset = RepeatPreset.CUSTOM,
            interval = "48",
            unit = RecurrenceUnit.HOURS,
          )
          .recurrence
      )
      .isNull()
    assertThat(
        TaskRepeatFormState(preset = RepeatPreset.DAILY, endMode = RepeatEndMode.ON_DATE).recurrence
      )
      .isNull()
    assertThat(
        TaskRepeatFormState(
            preset = RepeatPreset.DAILY,
            endMode = RepeatEndMode.AFTER_OCCURRENCES,
            endCount = "0",
          )
          .recurrence
      )
      .isNull()
  }

  @Test
  fun endConditionsReachTheRule() {
    val onDate =
      TaskRepeatFormState(
          preset = RepeatPreset.DAILY,
          endMode = RepeatEndMode.ON_DATE,
          endDate = LocalDate.of(2026, 12, 31),
        )
        .recurrence
    val afterCount =
      TaskRepeatFormState(
          preset = RepeatPreset.DAILY,
          endMode = RepeatEndMode.AFTER_OCCURRENCES,
          endCount = "5",
        )
        .recurrence

    assertThat(onDate?.end).isEqualTo(RecurrenceEnd.OnDate(LocalDate.of(2026, 12, 31)))
    assertThat(afterCount?.end).isEqualTo(RecurrenceEnd.AfterOccurrences(5))
  }

  @Test
  fun editingATaskReopensTheControlsItWasSavedWith() {
    val recurrence =
      TaskRecurrence(
        interval = 6,
        unit = RecurrenceUnit.HOURS,
        dailyWindow = DailyWindow(LocalTime.of(6, 0), LocalTime.of(23, 0)),
        end = RecurrenceEnd.AfterOccurrences(12),
      )

    val state = TaskRepeatFormState.of(recurrence)

    assertThat(state.preset).isEqualTo(RepeatPreset.CUSTOM)
    assertThat(state.interval).isEqualTo("6")
    assertThat(state.unit).isEqualTo(RecurrenceUnit.HOURS)
    assertThat(state.useDailyWindow).isTrue()
    assertThat(state.endMode).isEqualTo(RepeatEndMode.AFTER_OCCURRENCES)
    assertThat(state.endCount).isEqualTo("12")
    assertThat(state.recurrence).isEqualTo(recurrence)
  }

  @Test
  fun aPlainCadenceReopensOnItsPresetInsteadOfCustom() {
    val quarterly = TaskRecurrence(interval = 3, unit = RecurrenceUnit.MONTHS)

    assertThat(TaskRepeatFormState.of(quarterly).preset).isEqualTo(RepeatPreset.QUARTERLY)
    assertThat(TaskRepeatFormState.of(null).preset).isEqualTo(RepeatPreset.NONE)
  }
}
