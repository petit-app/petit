# Tasks: JSON export and import

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Export all domains to JSON** (test-type: both)
  - blocked-by: 0001, 0002, 0003, 0004, 0005
  - desired behavior: generate a versioned bundle and write it to the selected URI.
  - acceptance criteria: the date-named file contains metadata and complete history for all domains, including pending and completed tasks.
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
  - verification: `./gradlew test`
- [ ] **Expose per-pet export in the profile** (test-type: integration)
  - blocked-by: export all domains to JSON
  - desired behavior: call `exportForPet(petId)` from the profile.
  - acceptance criteria: the bundle contains only the selected pet and its related records.
  - verification: `./gradlew test`
- [ ] **Add serialization and import regression tests** (test-type: both)
  - blocked-by: analyze and import a backup
  - desired behavior: cover round-trip, conflicts, corruption, atomicity, and older versions.
  - acceptance criteria: all acceptance criteria have automated coverage.
  - verification: `./gradlew test`
