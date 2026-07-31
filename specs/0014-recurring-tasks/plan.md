# Plan: Recurring tasks

Spec: [spec.md](./spec.md)

## Status

This plan is **Draft**. It waits for spec approval before any implementation.

## Dependencies

- Spec 0005 tasks, reminder preferences, and `TaskNotificationWorker`.
- `TaskEntity`, `TaskDao`, and the Room schema in `app/schemas`.
- `AutoTaskServiceImpl` for automatically generated health tasks.
- Export and import bundle schema in `ExportBundle` and `ExportImportUseCase`.
- Local sharing payloads that carry task records.

## Architecture

- Model recurrence as a value object in `domain/model` (repeat rule plus end
  condition), persisted as denormalized columns on `TaskEntity`.
- Put occurrence computation in a pure domain component that receives a clock,
  so every rule is testable without Android or WorkManager.
- Keep the scheduling boundary unchanged in shape: the repository asks the
  scheduler to schedule or cancel the single pending occurrence.
- Generate the next occurrence in a use case triggered by completion, by app
  start reconciliation, and by the notification worker.
- Keep display text (repeat summary, next occurrence) in the presentation
  layer, resolved through the existing localized text resolver.
- Keep automatic health tasks on their current path and add explanatory copy
  instead of a repeat rule.

## Implementation sequence

1. [ ] Approve the spec, plan, tasks, and index row.
2. [ ] Add the recurrence value object and occurrence computation with unit
       tests for every rule, end condition, month-end, and DST case.
3. [ ] Add the Room migration and repeat columns, with a migration test.
4. [ ] Add next-occurrence generation on completion and a start-up
       reconciliation for missed occurrences.
5. [ ] Add the repeat control and plain-language summary to the task form.
6. [ ] Add the recurring label and next-occurrence text to the task list, task
       detail, home dashboard, and notification.
7. [ ] Add the stop-series action with confirmation, and scope edits to pending
       and future occurrences.
8. [ ] Add explanatory copy for automatically generated health tasks.
9. [ ] Extend export, import, and local sharing with the new fields, keeping
       older backups importable.
10. [ ] Reconcile spec status, this plan, task checkboxes, and the specs index
        with fresh evidence.

## Commit boundaries

Intended boundaries, pending explicit authorization to commit:

1. approved spec, plan, tasks, and specs index;
2. domain recurrence model, occurrence computation, and tests;
3. persistence, migration, and scheduling changes;
4. presentation changes and copy;
5. export, import, and sharing compatibility.

No push, amend, rebase, merge, force-push, or pull request is authorized.

## Risks and mitigations

- **Notification burst after a long offline period:** resolve missed
  occurrences to a single pending occurrence and cover it with unit tests.
- **Drifting schedules across DST:** compute occurrences from local dates and
  re-resolve the instant at scheduling time.
- **Migration data loss:** additive nullable columns with defaults, plus a Room
  migration test.
- **Backup incompatibility:** treat repeat fields as optional on import and
  keep the current schema version behavior for older bundles.
- **User confusion between automatic and caregiver-owned repeats:** distinct
  labels and copy, verified in Compose tests.
- **Scheduling reliability under battery restrictions:** reuse the existing
  WorkManager path and reconcile pending occurrences on app start.

## Verification

1. Run focused unit tests for occurrence computation and reconciliation.
2. Run the Room migration test.
3. Run focused Compose tests for the form control, labels, and stop-series flow.
4. Run `./gradlew test`.
5. Run `./gradlew spotlessCheck`.
6. Run `./gradlew lintDebug` and report pre-existing failures separately.
7. Verify a daily series, an every-N-days series, completion, stop, reboot, and
   time zone change on a device or emulator.
