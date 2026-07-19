# Tasks: Manual Google Drive Backup

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **In Progress**. Android implementation is complete; real-provider validation remains open.

## Tasks

- [x] **Create a complete portable backup archive** (test-type: both)
  - blocked-by: none
  - desired behavior: one Room snapshot produces a versioned ZIP containing all restorable data and app-owned pet assets.
  - acceptance criteria: manifest payload paths, sizes, counts, SHA-256 checksums, JSON data, assets, exclusions, final archive metadata, and cleanup match the archive contract.
  - verification: `./gradlew test`

- [~] **Authorize Google Drive independently** (test-type: both)
  - blocked-by: previous task; completed Cloud Console runbook
  - desired behavior: a user without a Petit account can grant, cancel, retry, disconnect, and recover Drive app-data authorization.
  - acceptance criteria: only `drive.appdata` is requested; no purchase, Firebase session, or Petit account is created.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
  - automated evidence: Google Identity Services requests only `drive.appdata`; cancellation, denial, silent refresh, revocation, no-token-persistence, Activity lifecycle replay, and Activity-scoped foreground authorization boundaries are covered.
  - device evidence: the configured test user completed Google consent on Android 17 hardware with only `drive.appdata`; Petit then reported `Connected`.
  - remaining evidence: exercise cancellation, revocation, and reconnect on the physical device.

- [~] **Upload one backup idempotently** (test-type: both)
  - blocked-by: previous tasks
  - desired behavior: a resumable upload publishes one completed archive per backup ID and reports monotonic byte progress.
  - acceptance criteria: interruption, timeout, retry, cancellation, token expiry, and quota errors cannot produce duplicate completed backups or leak staging files.
  - verification: `./gradlew test`
  - automated evidence: the Drive v3 adapter restricts operations to `appDataFolder`, uses resumable upload, and reuses a completed file for a stable backup ID after a lost response.
  - device evidence: a real manual upload completed in the test user's `appDataFolder` with status `Succeeded`, `1.12 kB`, one pet, and zero tasks.
  - remaining evidence: execute interruption and retry against the test account on a physical device.

- [~] **Expose manual backup states in Settings** (test-type: both)
  - blocked-by: previous task
  - desired behavior: Settings explains authorization, starts backup, shows progress and completion, and offers actionable recovery.
  - acceptance criteria: all scenarios in `spec.md` have localized, accessible UI coverage.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
  - automated evidence: localized authorization, progress, completion, quota, retry, failure, silent-refresh, and disconnected authorization-then-backup states are wired to the real adapter behind domain contracts.
  - device evidence: the disconnected authorization flow and a subsequent manual backup completed on the physical Android 17 device.
  - remaining evidence: validate cancellation, retry, quota, and offline UI states on the physical device.

- [ ] **Validate the real provider integration** (test-type: integration)
  - blocked-by: previous tasks
  - desired behavior: debug and release identities can authorize and upload to their own appDataFolder configuration.
  - acceptance criteria: consent, cancellation, revocation, offline behavior, retry, and quota handling are evidenced on a physical device.
  - verification: [Google Cloud Console setup runbook](../../docs/test-runbooks/google-drive-cloud-console-setup.md) and [physical-device validation runbook](../../docs/test-runbooks/google-drive-physical-device-validation.md)
