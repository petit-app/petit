# Plan: JSON export and import

Spec: [spec.md](./spec.md)

## Sequence

1. Add unit tests for pending/completed task round-trip, per-pet filtering, corruption, legacy conversion, and merge.
2. Export all active tasks and convert supported legacy `reminders` before validation.
3. Preserve atomic analysis/import behavior and close Room transaction coverage.
4. Expose per-pet export from the profile and share generated URIs with temporary read permission.
5. Add ContentResolver/Compose coverage and expand the backup/restore E2E journey with completed-task history.

## Architecture

- `ExportImportUseCase` coordinates repositories, parsing, analysis, and merge.
- `ExportBundle` uses the keys `pets`, `weightEntries`, `vaccinationEntries`, `dewormingEntries`, and `tasks`.
- The import is persisted only after validation and user confirmation.

## Dependencies and risks

- Depends on `0001`–`0005` and must preserve IDs and references.
- Schema changes require explicit migration and tests with older backups.
- I/O or parsing failures must not produce a partial import.
- Document-provider URIs vary in share support; permission and `ClipData` behavior require instrumentation coverage.

## Verification

1. Run focused serialization, filtering, conversion, and merge unit tests after each slice.
2. Run Room/ContentResolver/share-intent integration tests and `BackupRestoreJourneyTest`.
3. Run `./gradlew test`, `./gradlew spotlessCheck`, then `./gradlew assembleDebug` followed by `./gradlew installDebug`.
