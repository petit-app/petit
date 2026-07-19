# Tasks: Restore Google Drive Backup

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **In Progress**. Android implementation is complete; physical restore validation remains open.

## Tasks

- [x] **Download and validate an untrusted backup archive** (test-type: both)
  - blocked-by: spec 0301
  - desired behavior: the archive is streamed to staging and completely validated before local mutation.
  - acceptance criteria: unsafe paths, bombs, limits, schema, manifest, checksums, JSON, and references are covered along with cancellation cleanup.
  - verification: `./gradlew test`
  - evidence: bounded archive validation plus download checksum, cancellation, monotonic progress, and staging cleanup tests.

- [~] **Restore an exact snapshot with REPLACE** (test-type: both)
  - blocked-by: previous task
  - desired behavior: restorable Room data, preferences, reminders, and assets match the backup while device-bound identities remain local.
  - acceptance criteria: user-visible state is entirely old or entirely restored; staged/orphaned assets are recoverable and destructive confirmation is required.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
  - provider-independent evidence: exact Room/DataStore restore, asset rollback, durable recovery journal, reminder rebuild, and destructive confirmation are covered; real provider and device validation remain open.

- [x] **Merge a backup deterministically** (test-type: both)
  - blocked-by: first task
  - desired behavior: unique records are retained and conflicts use the shared resolver, with assets following the winner.
  - acceptance criteria: retries are idempotent and current preferences are preserved unless explicitly selected for import.
  - verification: `./gradlew test`
  - evidence: shared conflict resolver tests preserve local winners, install remote winner assets, retain unique records, and keep preferences unless selected.

- [ ] **Restore on a different physical device** (test-type: integration)
  - blocked-by: previous tasks
  - desired behavior: a backup created on device A restores complete portable data on device B.
  - acceptance criteria: photos, tasks, reminder scheduling, preferences, counts, and exclusions are verified without cloning device credentials.
  - verification: [physical-device validation runbook](../../docs/test-runbooks/google-drive-physical-device-validation.md)
