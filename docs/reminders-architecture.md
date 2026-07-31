# Reminder and Push Notification Architecture

## Overview

Petit reminders use WorkManager for reliable scheduled execution, even when the app is not in the foreground.

Main flow:

1. User creates or edits a reminder in UI.
2. ViewModel persists data through repository.
3. Repository stores reminder in Room.
4. Scheduler registers work in WorkManager.
5. Worker executes at scheduled time and shows notification.

## Main Components

### ReminderScheduler

Location: `app/src/main/java/com/woliveiras/petit/worker/ReminderScheduler.kt`

Responsibilities:

- Schedule one-time reminder work
- Cancel work by reminder id
- Reschedule for snooze/repeat changes

### ReminderWorker

Location: `app/src/main/java/com/woliveiras/petit/worker/ReminderWorker.kt`

Responsibilities:

- Load reminder by id
- Skip cancelled/completed reminders
- Show notification
- Mark reminder as triggered
- Schedule next occurrence when repeat is enabled

### Notification Channel

Location: `app/src/main/java/com/woliveiras/petit/PetitApplication.kt`

Responsibilities:

- Register reminders channel at app startup
- Configure importance/vibration/description

### Hilt + WorkManager Integration

`PetitApplication` provides WorkManager configuration with `HiltWorkerFactory` to support dependency injection in workers.

## Reminder Lifecycle

### Create Reminder

1. Save reminder data.
2. Enqueue unique work keyed by reminder id.

### Complete or Cancel Reminder

1. Update status in repository.
2. Cancel pending work.

### Snooze Reminder

1. Compute new scheduled time.
2. Save updated reminder.
3. Reschedule work.

### Repeating Reminder

Repeating tasks are stored as a series: one pending row plus the occurrences already completed.
`TaskSeriesCoordinator` owns the whole lifecycle.

Task columns that describe the series:

- `repeatRule`: the encoded `TaskRecurrence` (`v1|interval|unit|weekdays|windowStart|windowEnd|endType|endValue`).
- `seriesId`: shared by every occurrence of the same series.
- `occurrenceIndex`: position of the pending occurrence inside the series.

Flow when the notification fires:

1. The worker loads the pending row and calls `onNotificationDelivered`.
2. The coordinator advances the pending row in place when the delivery is late, so a caregiver who
   never opens the app still sees a single actionable occurrence instead of a backlog.
3. The worker posts the notification.
4. Only then the worker calls `scheduleFollowUp`, which enqueues the following occurrence. The order
   matters: the follow-up reuses the same unique work name, so enqueueing it before posting would
   cancel the worker that is running.

Flow when the caregiver marks an occurrence as done:

1. `completeOccurrence` cancels the pending work and marks the row as completed.
2. A new pending row is created for the next occurrence, inheriting `seriesId` and the repeat rule.
3. Stopping a series (`stopSeries`) only removes the pending occurrence; the completed history stays.

Recovery:

- `reconcilePendingSeries` runs on app start and collapses missed occurrences, then reschedules.
  This replaces `BOOT_COMPLETED` and `TIMEZONE_CHANGED` receivers: WorkManager already survives
  reboots, and running the reconciliation on start keeps the app free of extra broadcast permissions
  while still recovering from time and time zone changes.

## Permission Model

Android 13+ requires runtime permission:

- `POST_NOTIFICATIONS`

If denied, reminder data can still be saved, but notifications are not shown.

## Known Limitations

- Delivery may be delayed by system power optimizations.
- Reinstall removes WorkManager internal jobs.
- Notification actions may be intentionally limited in MVP versions.

## Reliability Notes

- Prefer unique work names per reminder id.
- Keep reminder status authoritative in Room.
- Always cancel scheduled work when deleting reminders.
