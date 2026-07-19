# Tasks: Backup Settings

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **In Progress**. Android implementation is complete; physical authorization validation remains open.

## Tasks

- [x] **Persist free backup preferences** (test-type: both)
  - blocked-by: spec 0305
  - desired behavior: automatic, network, and notification preferences survive restart without credentials, gates, retention, or exact-time fields.
  - acceptance criteria: defaults and every update are covered in DataStore integration tests.
  - verification: `./gradlew test`

- [x] **Control one unique automatic schedule** (test-type: both)
  - blocked-by: previous task
  - desired behavior: enable, disable, and connected/unmetered changes update WorkManager without duplication.
  - acceptance criteria: UI describes execution as inexact and manual backup remains independent.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [x] **Expose manual backup, history, and notifications** (test-type: both)
  - blocked-by: previous task; spec 0303
  - desired behavior: Settings starts a manual backup, displays non-clinical history, and applies the success-notification preference.
  - acceptance criteria: success, retry, failure, and authorization-required states are localized and accessible.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
  - regression evidence: when disconnected, **Back up now** authorizes first, continues exactly once after success, and does not upload after cancellation or provider unavailability.

- [x] **Preview and browse backup history incrementally** (test-type: both)
  - blocked-by: previous task
  - desired behavior: Backup settings shows the three most recent attempts, while a dedicated history screen initially shows five and appends five more for each explicit load-more action.
  - acceptance criteria: bounded retrieval, stable newest-first ordering with an attempt-ID tie-breaker, no duplicates between pages, loading and end states, navigation, localization, and accessibility are covered without deleting or applying retention to stored attempts.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
  - evidence: repository and ViewModel JVM tests plus three focused Compose tests passed on Pixel_7_API_34 AVD.

- [~] **Disconnect Drive without deleting data** (test-type: both)
  - blocked-by: previous tasks
  - desired behavior: confirmation revokes Drive access and cancels work while preserving local and remote backups.
  - acceptance criteria: reconnect works without a Petit account and restores the remote list.
  - verification: `./gradlew test` and physical-device provider validation
  - automated evidence: disconnect disables automatic backup before revoking local authorization and invokes no remote deletion operation.
