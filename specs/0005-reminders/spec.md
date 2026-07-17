---
spec: "0005"
title: Local tasks and reminders
family: pet-care
status: In Progress
owner: woliveiras
depends_on: ["0001"]
---

# Spec: Local tasks and reminders

## Context and motivation

The caregiver needs reminders about pending care even while offline. The original reminder concept evolved into one-shot tasks.

## Current state

Custom and health-linked tasks, WorkManager scheduling, completion, deletion, filters, and reminder preferences are present. Automatic vaccination and deworming tasks are currently scheduled for the next-dose date instead of applying the configured advance-notice interval.

## Functional requirements

- Create automatic tasks for upcoming vaccinations, deworming treatments, and enabled periodic weigh-ins, applying the configured advance notice and interval preferences.
- Create and edit a custom task, optionally associated with a pet.
- Schedule a local notification with WorkManager and complete or delete tasks.
- Cancel the schedule and soft-delete the linked task when the health record is deleted.
- Configure categories, advance notice, weigh-in interval, and default time.
- Filter active tasks and display completed-task history.

## Acceptance criteria

- Given a next dose, When the record is saved, Then a linked task is created and scheduled according to preferences.
- Given weigh-in reminders are enabled, When a weigh-in is saved, Then the next task is created at the configured interval.
- Given a future custom task, When it is saved, Then it becomes `PENDING` and receives a local notification at the scheduled time.
- Given a pending task, When it is completed, Then it becomes `COMPLETED`, leaves the active list, and its schedule is canceled.
- Given a deleted linked record, Then its tasks are soft-deleted and its jobs are canceled.
- Given the device is offline, Then the local notification continues to work.
- Given an advance-notice date that has already passed while the care due date
  is still current or future, Then the task is scheduled immediately instead of
  being discarded or scheduled in the past.

## Test strategy

Every changed production behavior receives a unit test. Unit tests cover
preference-based date calculation, clock boundaries, replacement/cancellation,
and mappings. Integration tests cover Room, `AutoTaskService`, and WorkManager;
Compose tests cover forms, filters, and completion. The existing vaccination
E2E journey is updated to assert the configured advance notice.

## Edge cases

- Vaccination/deworming task time is the due date minus configured advance days
  at the configured notification time.
- If that instant is already past but the due date is not past, the persisted
  task and WorkManager request use the current instant.
- Overdue health records do not create a new upcoming-care task.
- Editing a linked record replaces its task and unique work; deletion cancels both idempotently.

## Decisions

- Date and time calculations use an injectable clock.
- The task description and actual scheduled instant are derived from the same preferences snapshot.

## Out of scope

- Automatic task recurrence and notification snooze.

## Known limitation

The configured vaccination and deworming advance notice is used in task copy
but is not applied to the task's scheduled date. Automatic care tasks therefore
do not yet fully honor reminder preferences.
