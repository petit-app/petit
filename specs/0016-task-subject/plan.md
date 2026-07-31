# Plan: Task subject

Spec: [spec.md](./spec.md)

## Status

This plan is **Draft**. It waits for spec approval before any implementation.

## Dependencies

- Spec 0005 tasks, task form, and `TaskNotificationWorker`.
- Spec 0003 vaccination records and `VaccineType`.
- Spec 0004 antiparasitic records and `DewormingType`.
- Spec 0011 species care catalog and `SpeciesCareCatalog`.
- `TaskEntity`, `TaskDao`, and the Room schema in `app/schemas`.
- Export and import bundle schema in `ExportBundle` and `ExportImportUseCase`.
- Local sharing payloads that carry task records.

## Architecture

- Add two nullable fields to `Task` and `TaskEntity`: the catalog code and the
  free text name. No new table.
- Resolve the options per kind in the domain layer through
  `SpeciesCareCatalog`, so the form only asks for a list and does not know the
  catalog rules.
- Keep display-name resolution in the presentation layer, reusing the existing
  localized text resolver, with the stored free text as fallback.
- Keep the medication suggestion source behind a repository query, so the form
  does not read the database directly.
- Keep the title prefill in the view model, so the rule is unit tested without
  Compose.

## Implementation sequence

1. [ ] Approve the spec, plan, tasks, and index row.
2. [ ] Add the subject fields to the domain model, entity, mapper, and Room
       migration, with migration and DAO tests.
3. [ ] Add option resolution per kind and subject validation to the view model,
       with unit tests including the kind change and the unknown code.
4. [ ] Add the per-kind subject control and the title prefill to the task form,
       with Compose tests and accessibility semantics.
5. [ ] Add the medication suggestion query and wire it into the form.
6. [ ] Show the subject in the task list, task detail, home dashboard, and
       notification.
7. [ ] Extend export, import, and local sharing with the new fields, keeping
       older backups importable.
8. [ ] Reconcile spec status, this plan, task checkboxes, and the specs index
       with fresh evidence.

## Commit boundaries

Intended boundaries, pending explicit authorization to commit:

1. approved spec, plan, tasks, and specs index;
2. domain model, entity, mapper, and migration;
3. view model option resolution, validation, and title prefill;
4. task form control and the surfaces that display the subject;
5. export, import, and sharing compatibility.

No push, amend, rebase, merge, force-push, or pull request is authorized.

## Risks and mitigations

- **Migration data loss:** keep the columns additive and nullable, and cover the
  migration with a Room migration test.
- **Backup incompatibility:** treat missing subject fields as no subject and
  test a legacy bundle explicitly.
- **Form getting longer and heavier:** show the subject control only for the
  kinds that need it, and keep custom tasks untouched.
- **Stale subject after a kind change:** clear the subject when the control
  changes and assert it in a unit test.
- **Unknown catalog code from a newer version:** fall back to the stored free
  text instead of showing an empty subject.

## Verification

1. `./gradlew spotlessApply`
2. `./gradlew :app:compileDebugKotlin`
3. `./gradlew :app:compileDebugAndroidTestKotlin`
4. `./gradlew test`
5. `./gradlew spotlessCheck`
6. `./gradlew assembleDebug && ./gradlew installDebug`
7. Verify each kind's subject control, the title prefill, and an export and
   import round trip on a device or emulator.
