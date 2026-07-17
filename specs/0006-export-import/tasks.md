# Tasks: JSON export and import

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Export all domains to JSON** (test-type: both)
  - blocked-by: 0001, 0002, 0003, 0004, 0005
  - desired behavior: generate a versioned bundle and write it to the selected URI.
  - acceptance criteria: the date-named file contains metadata and complete history for all domains, including pending and completed tasks.
  - test expectations: unit tests cover serialization and pending/completed round-trip; Room tests prove soft-deleted tasks are excluded.
  - verification: `./gradlew test`
- [x] **Analyze and import a backup** (test-type: both)
  - blocked-by: export all domains to JSON
  - desired behavior: validate, summarize, and import with a conflict strategy.
  - acceptance criteria: an invalid file does not change data; merge uses `updatedAt`; the operation is atomic.
  - verification: `./gradlew test`
- [x] **Integrate document selection** (test-type: integration)
  - blocked-by: export all domains to JSON, analyze and import a backup
  - desired behavior: use the document picker to open and create a backup.
  - acceptance criteria: the user selects a source/destination without unsafe direct filesystem access.
  - verification: `./gradlew test`
- [ ] **Integrate export with the share sheet** (test-type: integration)
  - blocked-by: integrate document selection
  - desired behavior: allow sharing the backup through compatible apps.
  - acceptance criteria: the share sheet receives the URI with temporary read permission.
  - test expectations: unit tests cover intent construction; instrumentation verifies URI, MIME type, `ClipData`, and read grant.
  - verification: `./gradlew test`
- [ ] **Expose per-pet export in the profile** (test-type: integration)
  - blocked-by: export all domains to JSON
  - desired behavior: call `exportForPet(petId)` from the profile.
  - acceptance criteria: the bundle contains only the selected pet and its related records.
  - test expectations: unit tests cover every related domain and exclusion of other/global records; Compose/integration tests cover the profile entry point.
  - verification: `./gradlew test`
- [ ] **Convert supported legacy reminder backups** (test-type: both)
  - blocked-by: analyze and import a backup
  - desired behavior: convert a supported root `reminders` collection into current tasks before validation and analysis.
  - acceptance criteria: convertible backups import predictably; unsupported shapes fail without changing Room.
  - test expectations: unit tests cover conversion and rejection; Room tests prove rejection is atomic.
  - verification: `./gradlew test`
- [ ] **Add serialization and import regression tests** (test-type: both)
  - blocked-by: analyze and import a backup
  - desired behavior: cover round-trip, conflicts, corruption, atomicity, and older versions.
  - acceptance criteria: all acceptance criteria have automated coverage.
  - test expectations: cover current and legacy round-trip, conflicts, corruption, atomicity, task states, and per-pet filtering.
  - verification: `./gradlew test && ./gradlew spotlessCheck`

- [x] **Cover backup restoration with an Android E2E journey** (test-type: integration)
  - blocked-by: analyze and import a backup, integrate document selection, 0010
  - desired behavior: export through DocumentsUI, delete local pet-care data, and import the saved backup.
  - acceptance criteria: the pet disappears after deletion and returns after the user confirms import of the exported JSON file.
  - verification: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woliveiras.petit.e2e.BackupRestoreJourneyTest`
- [ ] **Preserve completed tasks in the backup E2E journey** (test-type: integration)
  - blocked-by: export all domains to JSON, cover backup restoration with an Android E2E journey
  - desired behavior: include a completed task in the existing export-delete-import journey.
  - acceptance criteria: the completed task disappears after deletion and returns with `COMPLETED` status after import.
  - test expectations: extend the existing journey while keeping serialization edge cases in unit tests.
  - verification: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woliveiras.petit.e2e.BackupRestoreJourneyTest`
