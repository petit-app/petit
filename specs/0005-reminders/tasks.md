# Tasks: Local tasks and reminders

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Create automatic care tasks** (test-type: both)
  - blocked-by: 0001
  - desired behavior: create/cancel tasks linked to vaccination, deworming, and weight according to preferences, including advance notice.
  - acceptance criteria: correct type, reference, and scheduled date; deleting the record cancels and removes the active task.
  - test expectations: unit tests cover vaccination, deworming, weight, disabled preferences, overdue dates, immediate fallback, replacement, and deletion; integration tests cover repository and scheduler calls.
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
  - test expectations: retain unit coverage for every changed behavior and add Room, WorkManager, or Compose tests only at their real seams.
  - verification: `./gradlew test && ./gradlew spotlessCheck`

- [x] **Cover automatic vaccination tasks with an E2E journey** (test-type: integration)
  - blocked-by: create automatic care tasks
  - desired behavior: save a vaccination with a next dose and persist its linked automatic task.
  - acceptance criteria: the vaccination appears in history and Room contains one pending vaccination task linked to that record.
  - verification: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woliveiras.petit.e2e.VaccinationTaskJourneyTest`
- [ ] **Verify advance notice in the vaccination E2E journey** (test-type: integration)
  - blocked-by: create automatic care tasks
  - desired behavior: exercise a non-default vaccination advance-notice preference through the existing journey.
  - acceptance criteria: the pending linked task is persisted at the preference-adjusted instant.
  - test expectations: update the existing E2E fixture/assertion without duplicating unit-level date cases.
  - verification: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woliveiras.petit.e2e.VaccinationTaskJourneyTest`
