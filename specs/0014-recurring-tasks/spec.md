---
spec: "0014"
title: Recurring tasks
family: pet-care
status: Draft
owner: woliveiras
depends_on: ["0005"]
---

# Spec: Recurring tasks

## Context and motivation

Beta feedback showed two related problems. A tester asked for recurring
medication reminders ("an alarm every day, or every X days at time Y"), and
another user assumed the tasks the app already creates would repeat by
themselves. Today every task is one-shot: `Task` has no repeat field, the form
has no repeat control, and nothing in the list, detail, or notification tells
the caregiver whether the task will come back.

Caregivers currently solve this outside the app. The reference workflow is a
spreadsheet plus Google Calendar, where recurring events (annual vaccines,
monthly antiparasitics, daily medication) are the normal way to remember care.
Medication also needs repetition inside the same day, because a prescription of
one dose every eight hours is common.

## Current state

- [Task](../../app/src/main/java/com/woliveiras/petit/domain/model/Task.kt) and
  `TaskEntity` store `scheduledFor` and `status` only.
- [TaskFormScreen](../../app/src/main/java/com/woliveiras/petit/presentation/feature/tasks/TaskFormScreen.kt)
  exposes title, description, pet, kind, date, and time.
- [AutoTaskServiceImpl](../../app/src/main/java/com/woliveiras/petit/worker/AutoTaskServiceImpl.kt)
  creates a single forward-looking task per health record and replaces it when
  the record changes.
- `TaskNotificationWorker` schedules one-shot work; no worker regenerates tasks.

## Functional requirements

- Store a repeat rule on a task as an interval and a unit: every N hours, days,
  weeks, months, or years, with no repeat as the absence of a rule.
- Offer the common cadences as presets that map onto that rule: daily, weekly,
  monthly, quarterly, every six months, yearly, and custom.
- Let a weekly rule select the weekdays it applies to.
- Let an hourly rule carry an optional daily window, so a treatment every eight
  hours can stay between 07:00 and 23:00 instead of firing overnight.
- Store an end condition: never, until a date, or after N occurrences.
- Let the caregiver set the repeat rule and end condition in the task form,
  with a plain-language summary of the resulting schedule.
- Keep at most one pending occurrence per series; generate the next occurrence
  when the current one is completed, when its notification fires, or when it
  expires, whichever happens first.
- Label recurring tasks in the task list, task detail, home dashboard, and
  notification with the repeat summary and the next occurrence date.
- Explain the cadence of automatically generated health tasks so the caregiver
  can tell them apart from a repeating series they own.
- Let the caregiver stop a series, edit future occurrences, or complete a
  single occurrence without deleting the series.
- Reschedule pending occurrences after reboot, time change, and time zone
  change.
- Include repeat fields in JSON export, import, and local sharing payloads.

## Acceptance criteria

- Given a task with a daily repeat at 20:00, When the caregiver completes the
  occurrence for today, Then a pending occurrence exists for tomorrow at 20:00
  and today's completion stays in history.
- Given a repeat of every 8 hours with a daily window from 07:00 to 23:00, When
  the occurrence at 23:00 is reached, Then the next occurrence is the first slot
  on or after 07:00 of the following day.
- Given a repeat of every 8 hours with no window, When an occurrence is reached,
  Then the next occurrence is exactly 8 hours later, including overnight.
- Given an hourly series, When the caregiver never completes an occurrence, Then
  the next occurrence is still generated when the notification fires, so the
  series does not stall.
- Given the quarterly preset, When the caregiver saves the task, Then the stored
  rule is every 3 months and the summary states every 3 months.
- Given a repeat of every 3 days ending after 5 occurrences, When the fifth
  occurrence is completed, Then no new occurrence is generated and the series is
  marked finished.
- Given a repeat until a date, When the next computed occurrence is after that
  date, Then no occurrence is generated.
- Given a recurring task, When the caregiver opens the task list, Then the item
  shows the repeat summary and the next occurrence date.
- Given a non-recurring task, When the caregiver opens the task list, Then no
  repeat label is shown.
- Given an automatically generated vaccination or deworming task, When the
  caregiver opens it, Then the screen states that the next task is created from
  the next dose recorded in the health record, not from a repeat rule.
- Given a recurring task, When the caregiver stops the series, Then the pending
  occurrence and its scheduled work are cancelled and past occurrences remain in
  history.
- Given a recurring task, When the caregiver edits the time or repeat rule,
  Then only the pending and future occurrences change.
- Given several missed occurrences, When the app is opened, Then a single
  pending occurrence is shown and the missed ones are resolved deterministically
  without a notification burst.
- Given a device restart or time zone change, When scheduling is re-evaluated,
  Then the pending occurrence keeps its local wall-clock time.
- Given a backup that contains recurring tasks, When it is imported, Then repeat
  rule, end condition, and occurrence count are restored.

## Test strategy

Every changed production behavior receives a unit test. Unit tests cover
occurrence computation with an injectable clock, including every unit and
interval, the hourly daily window, both end conditions, month-end dates, DST
transitions, and missed-occurrence resolution. Room tests cover the migration
and repeat-field persistence. Compose tests cover the preset list, the custom
interval editor, the plain-language summary, the recurring label, the
stop-series confirmation, and accessibility semantics of the new controls. An
integration test covers complete, regenerate, and reschedule through the
repository, the notification worker, and the scheduling boundary.

## Edge cases

- Monthly repeat on day 29, 30, or 31 in shorter months.
- Yearly repeat on February 29.
- DST transitions where the chosen local time does not exist or occurs twice.
- Hourly window shorter than the interval, so at most one slot fits per day.
- Hourly rule whose first occurrence starts outside its own daily window.
- Long offline periods that skip many occurrences.
- Series whose end date is in the past when it is first evaluated.
- Repeat rules imported from a backup produced by a newer schema version.
- Notification permission revoked or exact alarm restricted while a series is
  active.

## Decisions

- One repeat rule shape covers every cadence: an interval plus a unit of hours,
  days, weeks, months, or years. Quarterly and every six months are presets for
  every 3 and every 6 months, so no extra concept is stored.
- Intraday repetition is expressed as an hourly interval with an optional daily
  window, not as a list of fixed times of day. A list of times can be added
  later as a second rule shape if beta feedback asks for it.
- Only one pending occurrence exists per series; the next occurrence is created
  on completion, when the notification fires, or on expiry, not batched in
  advance. Generating on notification keeps hourly series alive when the
  caregiver does not mark a dose as done.
- Scheduling keeps using WorkManager, which is inexact by design. Exact alarms
  are deliberately deferred because they require the Android 12 exact alarm
  permission and a store justification; revisit if beta reports drift that
  matters for medication.
- Occurrence computation uses local dates and an injectable clock, never epoch
  arithmetic on days.
- Exporting a task to the device calendar is a separate capability and is
  tracked in its own spec, so this one stays about the repeat engine.
- Automatically generated health tasks stay driven by the health record's next
  due date, and are explained rather than converted into repeat rules.
- Repeat fields are additive in the export schema so older backups keep
  importing.

## Open questions

- Should the caregiver be able to convert an automatic health task into a
  caregiver-owned recurring task?
- Should a stopped series be archived separately from completed tasks?
- Should an hourly series without a window warn the caregiver that it will
  notify overnight?
