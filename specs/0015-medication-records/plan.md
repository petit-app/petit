# Plan: Medication records

Spec: [spec.md](./spec.md)

## Status

This plan is **Draft**. It waits for spec approval before any implementation.

## Dependencies

- Spec 0001 pet management, profile shortcuts, and soft-delete conventions.
- Spec 0014 recurring tasks, for the reminder series linked to a treatment.
- Room schema, DAO, and mapper patterns used by deworming and vaccination.
- Export bundle, import merge, and local sharing payloads.

## Architecture

- Add a `Medication` domain model, `MedicationEntity`, DAO, mapper, and
  repository following the existing deworming vertical slice.
- Derive ongoing versus past status in a pure domain component with an
  injectable clock, not in the DAO.
- Reuse the existing form, list, and status indicator patterns from the
  deworming feature to avoid new UI concepts.
- Express the reminder offer as a use case that maps a treatment window and
  schedule to a recurring task, and keeps the link by reference id.
- Extend the export bundle additively so older backups keep importing.

## Implementation sequence

1. [ ] Approve the spec, plan, tasks, and index row.
2. [ ] Add the domain model, entity, DAO, mapper, repository, and migration.
3. [ ] Add validation and ongoing/past classification with unit tests.
4. [ ] Add the medication form and per-pet history screen.
5. [ ] Add the active medication section to the pet profile and health summary.
6. [ ] Add the medication entry point to the quick-add menu.
7. [ ] Add the linked recurring task offer and the stop propagation.
8. [ ] Extend export, import, and local sharing.
9. [ ] Reconcile spec status, this plan, task checkboxes, and the specs index
       with fresh evidence.

## Commit boundaries

Intended boundaries, pending explicit authorization to commit:

1. approved spec, plan, tasks, and specs index;
2. data layer, migration, and domain rules;
3. presentation surfaces and quick-add entry point;
4. recurring task link;
5. export, import, and sharing.

No push, amend, rebase, merge, force-push, or pull request is authorized.

## Risks and mitigations

- **Feature overlap with tasks:** keep the medication record as the source of
  truth and the task as a derived reminder.
- **Orphan links after import:** treat a missing linked task as no reminder and
  cover it with an import test.
- **Schema churn:** additive migration with a Room migration test.
- **Scope creep into dosing calculations:** first version records free text dose
  and does not compute dosages.
- **Clinical misinterpretation:** keep the existing disclaimer that Petit does
  not replace veterinary guidance.

## Verification

1. Run focused unit tests for validation, classification, and task mapping.
2. Run the Room migration and DAO tests.
3. Run focused Compose tests for the form, history, and profile section.
4. Run `./gradlew test`.
5. Run `./gradlew spotlessCheck`.
6. Run `./gradlew lintDebug` and report pre-existing failures separately.
7. Verify the create, remind, finish, and delete journey on a device or
   emulator, including TalkBack navigation.
