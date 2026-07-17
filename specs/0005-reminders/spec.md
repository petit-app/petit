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

## Test strategy

Unit tests cover preferences, dates, and mappings; integration tests cover Room, AutoTaskService, and WorkManager; UI tests cover forms, filters, and completion.

## Out of scope

- Automatic task recurrence and notification snooze.

## Known limitation

The configured vaccination advance notice is used in task copy but is not applied to the task's scheduled date. Automatic care tasks therefore do not yet fully honor reminder preferences.
