# Tasks: Backup Triggers

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **In Progress**. Android implementation is complete; physical trigger validation remains open.

## Tasks

- [x] **Classify restorable changes and revisions** (test-type: both)
  - blocked-by: specs 0305 and 0306
  - desired behavior: every committed restorable data or asset change advances a revision while backup bookkeeping cannot trigger itself.
  - acceptance criteria: all included and excluded trigger types in `spec.md` have regression coverage.
  - verification: `./gradlew test`

- [x] **Debounce one durable change-triggered backup** (test-type: both)
  - blocked-by: previous task
  - desired behavior: rapid changes replace one five-minute unique request that survives process death.
  - acceptance criteria: concurrent edits, app closure, settings disable, and network constraints preserve one authoritative request.
  - verification: `./gradlew test`

- [x] **Coalesce manual, periodic, and triggered runs** (test-type: both)
  - blocked-by: previous task
  - desired behavior: one successful backup covers its local revision and cancels redundant pending work.
  - acceptance criteria: equal/newer revisions do not create duplicate archives and older revisions remain eligible.
  - verification: `./gradlew test`

- [ ] **Validate trigger durability on hardware** (test-type: integration)
  - blocked-by: previous tasks
  - desired behavior: rapid edits, process death, network loss, retry, and authorization revocation follow the documented WorkManager behavior.
  - acceptance criteria: physical evidence confirms durability without exact timing promises or Petit gates.
  - verification: [physical-device validation runbook](../../docs/test-runbooks/google-drive-physical-device-validation.md)
