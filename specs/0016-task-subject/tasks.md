# Tasks: Task subject

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Persist the task subject** (test-type: integration)
  - blocked-by: none
  - desired behavior: store a nullable catalog code and a nullable free text name on the task entity through an additive Room migration.
  - acceptance criteria: existing tasks migrate without data loss and default to no subject; both fields round trip through the DAO and the mapper.
  - test expectations: Room migration test plus DAO and mapper round-trip tests.
  - verification: `./gradlew test`
- [ ] **Resolve subject options per kind** (test-type: unit)
  - blocked-by: persist the task subject
  - desired behavior: expose the vaccines applicable to the pet's species for vaccination, the treatment types for antiparasitic, free text for medication, and no options for weight and custom.
  - acceptance criteria: a species with no preset offers only the free text path; a task without a pet falls back to the unfiltered list; an unknown stored code resolves to the stored free text.
  - test expectations: unit tests per kind, per species, and for the unknown code fallback.
  - verification: `./gradlew test`
- [ ] **Validate the subject and prefill the title** (test-type: unit)
  - blocked-by: resolve subject options per kind
  - desired behavior: require the free text when "other" is selected, trim whitespace-only input, clear the subject when the kind changes, and prefill an empty title from the chosen subject without overwriting a title the caregiver typed.
  - acceptance criteria: saving with an empty "other" text is blocked; changing the kind clears the previous subject; a typed title is never replaced.
  - test expectations: unit tests for each validation rule and each prefill branch.
  - verification: `./gradlew test`
- [ ] **Choose the subject in the task form** (test-type: both)
  - blocked-by: validate the subject and prefill the title
  - desired behavior: show the per-kind subject control in the task form, with the free text field appearing for "other" and the optional product field for antiparasitic.
  - acceptance criteria: weight and custom show no control; the control is reachable and announced by the screen reader; the error message is announced when validation fails.
  - test expectations: Compose tests per kind, for the "other" path, and for the accessibility semantics.
  - verification: `./gradlew test`
- [ ] **Suggest medicines already used** (test-type: both)
  - blocked-by: choose the subject in the task form
  - desired behavior: suggest medicines already stored for the pet when the caregiver types a medication subject.
  - acceptance criteria: an empty history shows no suggestions and never blocks typing; suggestions ignore accent and case differences; picking a suggestion fills the field.
  - test expectations: unit tests for the suggestion query and matching; Compose test for picking a suggestion.
  - verification: `./gradlew test`
- [ ] **Show the subject where the task appears** (test-type: both)
  - blocked-by: choose the subject in the task form
  - desired behavior: display the resolved subject in the task list, task detail, home dashboard, and notification.
  - acceptance criteria: tasks without a subject render as today; the subject is announced by the screen reader; the notification text stays within its display limits.
  - test expectations: Compose tests per surface plus a unit test for the notification text.
  - verification: `./gradlew test`
- [ ] **Carry the subject through export, import, and sharing** (test-type: both)
  - blocked-by: persist the task subject
  - desired behavior: include the catalog code and the free text name in the export bundle and the local sharing payload, and restore them on import.
  - acceptance criteria: a round trip preserves the subject; a backup without subject fields imports with no subject and no error.
  - test expectations: unit tests for serialization and legacy bundles; integration test for the import merge.
  - verification: `./gradlew test && ./gradlew spotlessCheck`
