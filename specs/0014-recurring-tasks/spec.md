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

- Store a repeat rule on a task: none, daily, every N days, weekly on selected
  weekdays, monthly on the same day, or yearly.
- Store an end condition: never, until a date, or after N occurrences.
- Let the caregiver set the repeat rule and end condition in the task form,
  with a plain-language summary of the resulting schedule.
- Keep at most one pending occurrence per series; generate the next occurrence
  when the current one is completed or expires.
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
occurrence computation with an injectable clock, including every repeat rule,
both end conditions, month-end dates, DST transitions, and missed-occurrence
resolution. Room tests cover the migration and repeat-field persistence.
Compose tests cover the repeat control, the plain-language summary, the
recurring label, the stop-series confirmation, and accessibility semantics of
the new controls. An integration test covers complete, regenerate, and
reschedule through the repository and the scheduling boundary.

## Edge cases

- Monthly repeat on day 29, 30, or 31 in shorter months.
- Yearly repeat on February 29.
- DST transitions where the chosen local time does not exist or occurs twice.
- Long offline periods that skip many occurrences.
- Series whose end date is in the past when it is first evaluated.
- Repeat rules imported from a backup produced by a newer schema version.
- Notification permission revoked or exact alarm restricted while a series is
  active.

## Decisions

- Only one pending occurrence exists per series; the next occurrence is created
  on completion or expiry, not batched in advance.
- Occurrence computation uses local dates and an injectable clock, never epoch
  arithmetic on days.
- Automatically generated health tasks stay driven by the health record's next
  due date, and are explained rather than converted into repeat rules.
- Repeat fields are additive in the export schema so older backups keep
  importing.

## Open questions

- Should the caregiver be able to convert an automatic health task into a
  caregiver-owned recurring task?
- Should Petit offer an "add to calendar" or ICS export as a bridge for people
  who already keep pet care in Google Calendar?
- Should a stopped series be archived separately from completed tasks?
