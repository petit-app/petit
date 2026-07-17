# Tasks: Local tasks and reminders

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Create automatic care tasks** (test-type: both)
  - blocked-by: 0001
  - desired behavior: create/cancel tasks linked to vaccination, deworming, and weight according to preferences.
  - acceptance criteria: correct type and reference; deleting the record cancels and removes the active task.
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
- [ ] **Add automatic recurrence** (test-type: both)
  - blocked-by: future product decision
  - desired behavior: remains out of scope for this specification.
  - acceptance criteria: requires new spec approval before implementation.
  - verification: `./gradlew test`
- [ ] **Add snooze** (test-type: both)
  - blocked-by: future product decision
  - desired behavior: remains out of scope for this specification.
  - acceptance criteria: requires new spec approval before implementation.
  - verification: `./gradlew test`
