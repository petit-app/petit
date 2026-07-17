---
spec: "0006"
title: JSON export and import
family: pet-care
status: Implemented
owner: woliveiras
depends_on: ["0001", "0002", "0003", "0004", "0005"]
---

# Spec: JSON export and import

## Context and motivation

The caregiver needs to create a manual backup and restore their data on another device or after reinstalling the app.

## Functional requirements

- Export pets, weights, vaccinations, deworming treatments, and tasks to JSON through a user-selected URI.
- Include the app version, export date, and schema version; name the file `petit_backup_YYYY-MM-DD.json`.
- Read and validate the backup, then present counts and conflicts before confirmation.
- Resolve conflicts by replacing, keeping, or merging based on the latest `updatedAt`.
- Apply the import atomically and reject an invalid file without changing data.
- Allow exporting only one pet and its related records.

## Acceptance criteria

- Given local records, When all data is exported, Then the JSON contains all domains and metadata.
- Given a valid backup, When it is selected, Then the user sees counts and conflicts and can confirm or cancel.
- Given an ID conflict, When a strategy is selected, Then the result honors `REPLACE`, `KEEP`, or `MERGE`.
- Given an invalid or corrupted file, Then no local data is changed.
- Given a pet, When its profile is exported, Then only that pet and its related data are included.

## Test strategy

Unit tests cover serialization, parsing, and merge; integration tests cover ContentResolver, the Room transaction, and selection flows; UI tests cover analysis and confirmation.

## Known limitations

- `exportForPet(petId)` exists, but there is no corresponding entry point in the pet profile.
- The current format uses the `tasks` key; legacy backups containing only `reminders` require explicit conversion.
