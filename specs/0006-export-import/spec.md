---
spec: "0006"
title: JSON export and import
family: pet-care
status: In Progress
owner: woliveiras
depends_on: ["0001", "0002", "0003", "0004", "0005"]
---

# Spec: JSON export and import

## Context and motivation

The caregiver needs to create a manual backup and restore their data on another device or after reinstalling the app.

## Current state

Versioned JSON export/import, document selection, conflict analysis, and merge strategies are implemented. The export currently includes only pending tasks, so it does not yet preserve completed-task history as required for a complete backup.

## Functional requirements

- Export pets, weights, vaccinations, deworming treatments, and the complete task history to JSON through a user-selected URI.
- Include the app version, export date, and schema version; name the file `petit_backup_YYYY-MM-DD.json`.
- Read and validate the backup, then present counts and conflicts before confirmation.
- Resolve conflicts by replacing, keeping, or merging based on the latest `updatedAt`.
- Apply the import atomically and reject an invalid file without changing data.
- Allow exporting only one pet and its related records.
- Share a generated backup URI through compatible apps with temporary read permission.
- Convert supported legacy backups that use the `reminders` key before validation.

## Acceptance criteria

- Given local records, When all data is exported, Then the JSON contains all domains and metadata.
- Given a valid backup, When it is selected, Then the user sees counts and conflicts and can confirm or cancel.
- Given an ID conflict, When a strategy is selected, Then the result honors `REPLACE`, `KEEP`, or `MERGE`.
- Given an invalid or corrupted file, Then no local data is changed.
- Given a pet, When its profile is exported, Then only that pet and its related data are included.
- Given pending and completed active tasks, When data is exported and restored, Then both states and their references are preserved.
- Given a supported legacy backup with `reminders`, When it is selected, Then it is converted to the current task representation or rejected without mutation if conversion is impossible.

## Test strategy

Every changed production behavior receives a unit test. Unit tests cover
serialization, parsing, complete task history, per-pet filtering, legacy
conversion, and merge. Integration tests cover `ContentResolver`, share intent
permissions, Room transactions, and selection flows; Compose tests cover
analysis and confirmation. The existing backup/restore E2E journey is expanded
to preserve a completed task.

## Decisions

- “Complete task history” includes non-deleted pending and completed tasks;
  logically deleted tasks remain excluded.
- A per-pet export includes only the selected pet and records/tasks carrying
  that `petId`; global custom tasks are excluded.
- Share intents include the URI in `ClipData` and grant temporary read permission.
- Unsupported or non-convertible schema versions fail validation before any write.

## Known limitations

- Exported JSON is not encrypted; the user chooses its destination and sharing target.
