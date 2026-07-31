# Tasks: Medication records

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Persist a medication record** (test-type: both)
  - blocked-by: 0001
  - desired behavior: store name, active ingredient, dose, start date, optional end date, optional daily schedule, notes, and audit and sync fields, with soft delete.
  - acceptance criteria: name and start date are required; an end date before the start date is rejected; deleted records leave the row in place and disappear from active queries.
  - test expectations: unit tests for validation; Room migration, ordering, and soft-delete tests.
  - verification: `./gradlew test`
- [ ] **Classify ongoing and past treatments** (test-type: unit)
  - blocked-by: persist a medication record
  - desired behavior: derive ongoing versus past from the treatment window and an explicit finish action, using an injectable clock.
  - acceptance criteria: open-ended treatments stay ongoing; past end dates and manual finishes move the record to history; boundaries are deterministic.
  - test expectations: table-driven unit tests across window boundaries.
  - verification: `./gradlew test`
- [ ] **Record and edit medication from a form** (test-type: both)
  - blocked-by: classify ongoing and past treatments
  - desired behavior: create, edit, finish, and delete a medication from a pet-scoped form reusing existing form patterns.
  - acceptance criteria: field-level validation messages appear; edits update `updatedAt`; deletion asks for confirmation.
  - test expectations: unit tests for the view model; Compose tests for the form, validation, and accessibility semantics.
  - verification: `./gradlew test`
- [ ] **List medications per pet** (test-type: both)
  - blocked-by: record and edit medication from a form
  - desired behavior: show ongoing and past sections in deterministic order with an empty state.
  - acceptance criteria: ordering is stable for equal dates; the empty state offers the create action.
  - test expectations: Room ordering test; Compose tests for both sections and the empty state.
  - verification: `./gradlew test`
- [ ] **Surface active medications in the pet profile** (test-type: both)
  - blocked-by: list medications per pet
  - desired behavior: show active medications with name and dose in the profile and health summary, linking to the medication history.
  - acceptance criteria: pets without active medications show no section; the shortcut opens the history.
  - test expectations: unit tests for the summary projection; Compose tests for the profile section.
  - verification: `./gradlew test`
- [ ] **Add a medication entry point to quick add** (test-type: integration)
  - blocked-by: record and edit medication from a form
  - desired behavior: add a medication option to the quick-add menu that opens the medication form for the selected pet.
  - acceptance criteria: the option appears with a label and description consistent with the other quick-add entries and reaches the form.
  - test expectations: Compose test for the menu entry and navigation.
  - verification: `./gradlew test`
- [ ] **Create a reminder series from a treatment** (test-type: both)
  - blocked-by: 0014, list medications per pet
  - desired behavior: offer to create a recurring task from the treatment window and daily schedule, and link it to the medication.
  - acceptance criteria: accepting creates a series matching the schedule and window; declining creates nothing; finishing or deleting the treatment stops the series.
  - test expectations: unit tests for the mapping and stop propagation; integration test for the full journey.
  - verification: `./gradlew test`
- [ ] **Carry medications through export, import, and sharing** (test-type: both)
  - blocked-by: persist a medication record
  - desired behavior: include medication records and their task link in the export bundle and local sharing payload.
  - acceptance criteria: a round trip preserves records and links; bundles without medications import unchanged; a missing linked task imports as no reminder.
  - test expectations: unit tests for serialization and legacy bundles; integration test for the import merge.
  - verification: `./gradlew test && ./gradlew spotlessCheck`
