---
spec: "0016"
title: Task subject
family: pet-care
status: Draft
owner: woliveiras
depends_on: ["0003", "0004", "0005", "0011"]
---

# Spec: Task subject

## Context and motivation

Beta feedback showed that a task kind alone does not say what the caregiver has
to do. Choosing "vaccination" does not record which vaccine, choosing
"antiparasitic" does not record which product, and choosing "medication" does
not record which medicine. The caregiver has to repeat that information by hand
in the title, and the app cannot connect the task to the health record it refers
to.

The health forms already solve this. The vaccination form has a vaccine type
dropdown with a free-text fallback, and the antiparasitic form has a treatment
type dropdown plus a product field. The task form is the odd one out.

## Current state

- [Task](../../app/src/main/java/com/woliveiras/petit/domain/model/Task.kt)
  stores `kind`, `title`, `description`, and `referenceEntityId`, with no field
  for what the task is about.
- [TaskFormScreen](../../app/src/main/java/com/woliveiras/petit/presentation/feature/tasks/TaskFormScreen.kt)
  exposes title, description, pet, kind, date, and time only.
- [SpeciesCareCatalog](../../app/src/main/java/com/woliveiras/petit/domain/model/SpeciesCareCatalog.kt)
  already provides vaccine presets per species, and `VaccineType` and
  `DewormingType` already carry display names.
- `referenceEntityId` is only set by `AutoTaskServiceImpl` for automatically
  generated health tasks; caregiver-created tasks always leave it null.

## Functional requirements

- Store the subject of a task as two nullable fields: a catalog code and a free
  text name. The code identifies a known catalog entry, the name carries what
  the caregiver typed or the resolved display name.
- Offer a subject control in the task form that depends on the selected kind:
  - vaccination: a dropdown of the vaccines available for the pet's species,
    with an "other" option that opens a free text field;
  - antiparasitic: a dropdown of the treatment types, with an optional free text
    field for the product name;
  - medication: a free text field with suggestions from medicines already used
    for that pet;
  - weight: no subject control;
  - custom: no subject control, because the title already answers what it is.
- Prefill the title from the selected subject and keep it editable, so the
  caregiver never has to type the same thing twice.
- Show the subject in the task list, task detail, home dashboard, and
  notification, so the caregiver knows which vaccine or medicine is due.
- Resolve catalog codes through the localized display names, and fall back to
  the stored free text when the code is unknown.
- Carry the subject fields through JSON export, import, and local sharing.
- Keep existing tasks valid: a task without a subject behaves exactly as today.

## Acceptance criteria

- Given a task with kind vaccination and a dog selected, When the caregiver
  opens the subject control, Then only the vaccines applicable to dogs are
  listed, plus an "other" option.
- Given the vaccination subject "other", When the caregiver selects it, Then a
  free text field appears and is required before saving.
- Given a vaccine chosen from the list and an empty title, When the caregiver
  saves, Then the title is filled with the vaccine name.
- Given a vaccine chosen from the list and a title already typed by the
  caregiver, When the caregiver saves, Then the typed title is kept.
- Given a task with kind antiparasitic, When the caregiver opens the subject
  control, Then the treatment types are listed and the product name field is
  optional.
- Given a task with kind medication, When the caregiver types in the subject
  field, Then medicines already used for that pet are suggested and can be
  picked.
- Given a task with kind weight or custom, When the caregiver opens the form,
  Then no subject control is shown.
- Given the kind is changed after a subject was chosen, When the new kind uses a
  different control, Then the previous subject is cleared instead of leaking
  into the new kind.
- Given a task with a subject, When the caregiver opens the task list or the
  task detail, Then the subject is shown next to the title and announced by the
  screen reader.
- Given a task created before this change, When it is opened, Then it renders
  with no subject and stays editable.
- Given a backup that contains task subjects, When it is imported, Then the
  catalog code and the free text name are restored.
- Given a backup produced before this change, When it is imported, Then tasks
  import with no subject and no error.

## Test strategy

- Unit tests for subject validation, title prefill, kind change clearing, and
  display-name resolution with an unknown code.
- Unit tests for the medication suggestion source, including an empty history.
- Room migration test for the additive columns and a DAO round-trip test.
- Unit tests for export, import, and legacy bundles without subject fields.
- Compose tests for each per-kind control, the "other" free text path, the
  prefilled title, and the accessibility semantics of the subject control.

## Edge cases

- Pet species without any vaccine preset, where only "other" is available.
- Task without a pet selected, where no species filter can be applied.
- Catalog code stored by a newer version of the app and unknown to this one.
- Free text longer than the title limit.
- Subject text that is only whitespace.
- Medication suggestions containing near-duplicates that differ by accent or
  case.

## Decisions

- The subject is stored as a code plus a name instead of a foreign key to a
  health record, because the caregiver often plans a task before any record
  exists.
- Custom tasks get no subject control. Adding one would duplicate the title and
  make the shortest flow longer.
- The title is prefilled, not locked, so the caregiver can still write "Lino's
  second dose" over the catalog name.
- Subject columns are additive and nullable so older backups keep importing and
  the migration cannot lose data.
- Medication suggestions come from tasks and records already stored on the
  device. No remote medicine catalog is introduced.

## Open questions

- Should picking a vaccine subject also preselect the vaccine type when the
  caregiver later records the dose from the task?
- Should the antiparasitic product field reuse the product name search planned
  in spec 0004?
- Should medication subjects link to the medication records planned in spec
  0015 once those exist?
