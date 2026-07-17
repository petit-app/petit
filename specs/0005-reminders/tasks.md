# Tasks: Local tasks and reminders

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Create automatic care tasks** (test-type: both)
  - blocked-by: 0001
  - desired behavior: create/cancel tasks linked to vaccination, deworming, and weight according to preferences, including advance notice.
  - acceptance criteria: correct type, reference, and scheduled date; deleting the record cancels and removes the active task.
  - verification: `./gradlew test`
- [x] **Manage a custom task** (test-type: both)
  - blocked-by: 0001
  - desired behavior: create, edit, complete, and list custom tasks.
  - acceptance criteria: a future task starts as `PENDING`; completion moves it to history.
  - verification: `./gradlew test`
- [x] **Schedule a one-shot local notification** (test-type: integration)
  - blocked-by: create automatic care tasks, manage a custom task
  - desired behavior: schedule via WorkManager and notify even while offline.
  - acceptance criteria: execution uses the current task and an immutable `PendingIntent`.
  - verification: `./gradlew test`
- [x] **Configure and filter tasks** (test-type: integration)
  - blocked-by: manage a custom task
  - desired behavior: persist preferences and filter by today, week, and month.
  - acceptance criteria: settings persist across restarts, and filters display the expected tasks.
  - verification: `./gradlew test`
- [ ] **Add automated reminder regression tests** (test-type: both)
  - blocked-by: create automatic care tasks, manage a custom task, schedule a one-shot local notification
  - desired behavior: cover preference-based dates, linked-task cancellation, custom tasks, filters, completion, and WorkManager scheduling.
  - acceptance criteria: all acceptance criteria have automated coverage, including offline one-shot notification behavior at the appropriate boundary.
  - verification: `./gradlew test`
