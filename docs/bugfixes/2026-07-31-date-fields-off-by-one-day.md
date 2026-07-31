# Bugfix: Date fields select the previous day

**Status:** fixed
**Date opened:** 2026-07-31
**Date fixed:** 2026-07-31
**Source:** user report (beta feedback)
**Outage:** no

## Summary

Every form that opens a date picker can commit the day before the one shown as
selected. A tester reported "the date thing is happening in every date field",
with a deworming form showing `Data de aplicação 30/07/2026` while the device
date was `31/07/2026`. The reporter's device is in a positive UTC offset zone
(Europe/Madrid, UTC+2 in July).

## Impact

- Affects the deworming, vaccination, weight, task, pet profile, and activity
  timeline forms, which are all the date entry points in the app.
- Affects users in zones with a positive UTC offset. Brazilian zones (negative
  offset) do not reproduce it, which is why it was not caught earlier.
- Corrupts stored health history: application dates, next due dates, weigh-in
  dates, and task schedules can be persisted one day earlier than chosen.
- Derived behavior inherits the error: status calculation (`OK`, `SCHEDULED`,
  `OVERDUE`), automatic task scheduling, notifications, and exported backups.

## Reproduction

**Environment:** device or emulator with time zone set to a positive UTC offset
(for example Europe/Madrid), Petit debug build.

**Steps:**

1. Set the device time zone to Europe/Madrid.
2. Open a pet profile and start a deworming record.
3. Tap the application date field.
4. Confirm the pre-selected date without changing it.

**Expected:** the field keeps the current date.

**Actual:** the field shows the previous day.

## Hypotheses

| Rank | Hypothesis                                                                                                                  | Prediction                                                                                                                                | Result                                 |
| ---- | --------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------- |
| 1    | `initialSelectedDateMillis` receives a system-zone instant while Material 3 interprets and normalizes the selection in UTC. | In UTC+N, `LocalDate.atStartOfDay(systemDefault())` lands on the previous UTC day, so the picker highlights and returns the previous day. | confirmed, covered by regression tests |
| 2    | The display formatter shifts the date.                                                                                      | `AppDisplayFormatter.localizedDate` round trips through the same zone it formats with, so it cannot shift.                                | falsified by code reading              |
| 3    | Room mapping shifts the date.                                                                                               | Storage happens after the picker already returned the wrong `LocalDate`.                                                                  | not the primary cause                  |

## Root cause analysis

All four pickers repeat the same conversion pair:

```kotlin
initialSelectedDateMillis =
  form.applicationDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
```

`androidx.compose.material3.DatePickerState` treats `selectedDateMillis` as UTC
midnight of the selected calendar day. In UTC+2, `2026-07-31T00:00+02:00` is
`2026-07-30T22:00Z`, so the picker renders July 30 and normalizes the selection
to `2026-07-30T00:00Z`. Converting that back with the system zone yields
`2026-07-30`.

Affected files:

- [DewormingFormScreen.kt](../../app/src/main/java/com/woliveiras/petit/presentation/feature/deworming/DewormingFormScreen.kt)
- [VaccinationFormScreen.kt](../../app/src/main/java/com/woliveiras/petit/presentation/feature/vaccination/VaccinationFormScreen.kt)
- [WeightEntryScreen.kt](../../app/src/main/java/com/woliveiras/petit/presentation/feature/weight/WeightEntryScreen.kt)
- [TaskFormScreen.kt](../../app/src/main/java/com/woliveiras/petit/presentation/feature/tasks/TaskFormScreen.kt)
- [PetFormScreen.kt](../../app/src/main/java/com/woliveiras/petit/presentation/feature/pets/PetFormScreen.kt)
- [ActivityTimelineScreen.kt](../../app/src/main/java/com/woliveiras/petit/presentation/feature/timeline/ActivityTimelineScreen.kt)

## Fix

1. Added [PickerDate.kt](../../app/src/main/java/com/woliveiras/petit/presentation/util/PickerDate.kt),
   the single conversion point between `LocalDate` and the picker's epoch
   millis, using `ZoneOffset.UTC` on both directions.
2. Added [PetitDatePickerDialog.kt](../../app/src/main/java/com/woliveiras/petit/presentation/components/PetitDatePickerDialog.kt)
   and replaced the six duplicated dialogs with it. Optional `minDate` and
   `maxDate` now disable out-of-range days in the picker instead of silently
   dropping the confirmed selection.
3. Added [PickerDateTest.kt](../../app/src/test/java/com/woliveiras/petit/presentation/util/PickerDateTest.kt),
   which round trips edge dates (year boundaries, leap day, DST transitions,
   pre-epoch) across negative, zero, and positive UTC offsets.
4. Added [PetitDatePickerDialogComposeTest.kt](../../app/src/androidTest/java/com/woliveiras/petit/presentation/components/PetitDatePickerDialogComposeTest.kt),
   which confirms the pre-selected date without changing it under both a
   positive and a negative offset zone.

## Follow-up

- Audit existing records created by affected users. Decide whether a data
  correction is possible or whether the release notes should ask users to
  review recent dates.
- Consider surfacing the selected date as text in the field label so a future
  shift is visible before saving.
- The storage mappers still round trip dates through `ZoneId.systemDefault()`.
  That is self-consistent inside one zone, so it is not this bug, but a user who
  travels or changes the device time zone can still see stored days shift.
- Add coverage for the `minDate` and `maxDate` bounds of
  `PetitDatePickerDialog`, including the inclusive boundary days and selecting a
  day other than the initial one.
