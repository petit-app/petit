---
spec: "0015"
title: Medication records
family: pet-care
status: Draft
owner: woliveiras
depends_on: ["0001", "0014"]
---

# Spec: Medication records

## Context and motivation

Beta feedback asked for recurring medication reminders. The underlying need is
broader than a reminder: caregivers keep a list of medications the pet uses or
has used, with the active ingredient, the dose, the treatment window, and the
daily schedule. The reference workflow is a spreadsheet with a dedicated
medication tab (name, description, active ingredient) that goes stale because it
is separate from the reminders kept in a calendar.

Petit already has `TaskKind.MEDICATION`, but it only changes the icon and the
notification emoji. There is no medication entity, no history, and no way to see
what a pet is taking right now.

## Current state

- `TaskKind.MEDICATION` is selectable in the task form and used for the list
  icon, the localized label, and the notification emoji.
- No medication entity, DAO, repository, or screen exists.
- Medication is not represented in the pet profile, the health summary, the
  export bundle, or local sharing.

## Functional requirements

- Record a medication for a pet with name, optional active ingredient, optional
  dose text, start date, optional end date, optional daily schedule, and notes.
- Mark a treatment as ongoing or finished, derived from the treatment window and
  an explicit finish action.
- List active medications and past medications per pet, in deterministic order.
- Show active medications in the pet profile and the health summary.
- Edit and soft-delete a medication record.
- Offer to create a recurring task from a medication treatment window and
  schedule, and keep the two linked so stopping the treatment stops the series.
- Provide a medication entry point in the quick-add menu.
- Let the caregiver find a record by the product or brand name they know, so
  searching for a flea and tick product, a vaccine, or a medication leads to the
  matching entry point even when the app names the category differently.
- Include medication records in JSON export, import, and local sharing.

## Acceptance criteria

- Given a medication with a start date and no end date, When it is saved, Then
  it is listed as ongoing.
- Given a medication with an end date in the past, When the list is opened, Then
  it is listed under past medications.
- Given a medication marked as finished today, When the list is opened, Then it
  moves out of the ongoing section without deleting history.
- Given an end date before the start date, When saving, Then the form blocks the
  submission with a field-level message.
- Given a medication with a daily schedule, When the caregiver accepts the
  reminder offer, Then a recurring task is created with that schedule and
  treatment window.
- Given a linked recurring task, When the treatment is finished or deleted, Then
  the series stops and no further occurrence is generated.
- Given a pet with active medications, When the profile is opened, Then the
  active medications are listed with name and dose.
- Given a medication record, When it is soft-deleted, Then it is excluded from
  active queries and the row remains in the database.
- Given a backup with medication records, When it is imported, Then records and
  their task links are restored, and older backups import unchanged.
- Given a caregiver searching for a product name that belongs to the
  antiparasitic catalog, When the results are shown, Then the antiparasitic
  entry point is offered with the matching type preselected.
- Given a search term that matches no catalog entry, When the results are shown,
  Then the caregiver is offered the medication form with the term prefilled as
  the medication name.

## Test strategy

Every changed production behavior receives a unit test. Unit tests cover
validation, ongoing versus past classification with an injectable clock, and the
medication-to-task mapping. Room tests cover the migration, deterministic
ordering, and soft delete. Compose tests cover the form, the profile section,
the empty state, and accessibility semantics. An integration test covers the
create-treatment-then-create-reminder journey and the stop propagation.

## Edge cases

- Medication with a start date in the future.
- Treatment that ends the same day it starts.
- Medication finished manually before the recorded end date.
- Deleting a pet that has active medications and linked series.
- Importing a medication whose linked task is absent from the bundle.
- Very long medication names or dose text in list and notification surfaces.

## Decisions

- Medication is its own record, not a task variant, so history survives task
  completion and deletion.
- Reminder creation is an explicit offer, not an automatic side effect.
- Ongoing versus past is derived from dates and an explicit finish action, using
  an injectable clock.
- The medication list is per pet and reachable from the pet profile.
- Search matches the product names caregivers use, not the app's category
  names, and routes to the existing record type instead of duplicating
  antiparasitic or vaccine records as medications.

## Open questions

- Should medication schedules support more than one time per day in the first
  version?
- Should the medication list carry dose units as structured data or free text?
- Should medication history appear in the PDF export?
