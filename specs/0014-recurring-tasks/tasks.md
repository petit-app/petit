# Tasks: Recurring tasks

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Model a repeat rule and compute the next occurrence** (test-type: unit)
  - blocked-by: none
  - desired behavior: represent none, daily, every N days, weekly on weekdays, monthly, and yearly repeats with an end condition of never, until a date, or after N occurrences, and compute the next occurrence from an injectable clock.
  - acceptance criteria: every rule and end condition returns the expected next occurrence or no occurrence; month-end, February 29, and DST cases are deterministic.
  - test expectations: table-driven unit tests per rule, per end condition, and per edge case.
  - verification: `./gradlew test`
- [ ] **Persist repeat fields on tasks** (test-type: integration)
  - blocked-by: model a repeat rule and compute the next occurrence
  - desired behavior: store repeat rule, end condition, and completed-occurrence count on the task entity through an additive Room migration.
  - acceptance criteria: existing tasks migrate without data loss and default to no repeat; new fields round trip through the DAO.
  - test expectations: Room migration test plus DAO round-trip test.
  - verification: `./gradlew test`
- [ ] **Generate the next occurrence on completion** (test-type: both)
  - blocked-by: persist repeat fields on tasks
  - desired behavior: completing a recurring occurrence records the completion and creates the next pending occurrence, unless the end condition is met.
  - acceptance criteria: at most one pending occurrence exists per series; the finished series creates nothing; history keeps every completed occurrence.
  - test expectations: unit tests for the use case with a controlled clock; integration test through the repository and scheduling boundary.
  - verification: `./gradlew test`
- [ ] **Reconcile missed occurrences and reschedule** (test-type: both)
  - blocked-by: generate the next occurrence on completion
  - desired behavior: on app start, reboot, time change, and time zone change, resolve overdue series to a single pending occurrence and reschedule its work.
  - acceptance criteria: a long offline period produces one pending occurrence and no notification burst; the pending occurrence keeps its local wall-clock time.
  - test expectations: unit tests for reconciliation windows; integration test for scheduling after reconciliation.
  - verification: `./gradlew test`
- [ ] **Set a repeat rule in the task form** (test-type: both)
  - blocked-by: persist repeat fields on tasks
  - desired behavior: offer the repeat rule and end condition in the task form with a plain-language summary of the resulting schedule.
  - acceptance criteria: the summary matches the selected rule, invalid combinations are blocked, and edits apply to pending and future occurrences only.
  - test expectations: unit tests for summary text and validation; Compose tests for the control, summary, and accessibility semantics.
  - verification: `./gradlew test`
- [ ] **Show that a task repeats** (test-type: both)
  - blocked-by: set a repeat rule in the task form
  - desired behavior: display the repeat summary and next occurrence in the task list, task detail, home dashboard, and notification.
  - acceptance criteria: recurring tasks are labelled, non-recurring tasks are not, and the label is announced by the screen reader.
  - test expectations: Compose tests per surface, including TalkBack semantics.
  - verification: `./gradlew test`
- [ ] **Stop a recurring series** (test-type: both)
  - blocked-by: show that a task repeats
  - desired behavior: offer a stop action that cancels the pending occurrence and its scheduled work while preserving history.
  - acceptance criteria: the confirmation states that future occurrences stop; past occurrences remain visible; no further occurrence is generated.
  - test expectations: unit tests for the use case; Compose test for the confirmation flow.
  - verification: `./gradlew test`
- [ ] **Explain automatically generated health tasks** (test-type: both)
  - blocked-by: show that a task repeats
  - desired behavior: state on vaccination, deworming, and weigh-in tasks that the next task comes from the next dose or interval recorded in the health record.
  - acceptance criteria: automatic tasks never show a caregiver repeat rule and always show the cadence explanation.
  - test expectations: unit tests for the resolved text per kind; Compose test for the task detail.
  - verification: `./gradlew test`
- [ ] **Carry repeat fields through export, import, and sharing** (test-type: both)
  - blocked-by: persist repeat fields on tasks
  - desired behavior: include repeat rule, end condition, and occurrence count in the export bundle and local sharing payload, and restore them on import.
  - acceptance criteria: a round trip preserves the series; a backup without repeat fields imports as non-recurring.
  - test expectations: unit tests for serialization and legacy bundles; integration test for the import merge.
  - verification: `./gradlew test && ./gradlew spotlessCheck`
