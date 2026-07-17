# Plan: Local tasks and reminders

Spec: [spec.md](./spec.md)

## Sequence

1. Add unit tests for preference-based dates, overdue records, clock boundaries, and replacement/cancellation.
2. Extract deterministic scheduling calculation and apply advance notice to vaccination and deworming tasks.
3. Verify linked-task replacement, deletion, weight intervals, and WorkManager uniqueness at integration boundaries.
4. Close unit and Compose gaps for custom tasks, filters, completion, and settings.
5. Update the vaccination E2E journey to assert the configured scheduled instant.

## Architecture

- Tasks and notifications are local and one-shot.
- `referenceEntityId` links a task to the health record that created it.
- `PendingIntent` must be immutable; deletion and completion cancel the scheduled job.
- Routes: `tasks`, `tasks/form?taskId={taskId}`, `tasks/settings`, and `tasks/completed`.

## Dependencies and risks

- Depends on `0001` and receives events from `0002`, `0003`, and `0004`.
- Android restrictions on notifications and background execution require integration tests.
- Scheduling “now” must avoid flaky tests and negative WorkManager delays.

## Verification

1. Run focused `AutoTaskService` and scheduling tests after each slice.
2. Run `./gradlew test` and `./gradlew spotlessCheck`.
3. Run `VaccinationTaskJourneyTest` on an emulator.
4. Run `./gradlew assembleDebug` followed by `./gradlew installDebug`.
