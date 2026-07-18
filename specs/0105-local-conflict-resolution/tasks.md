# Tasks: Local conflict resolution

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Merge by UUID and most recent update** (test-type: unit)
  - blocked-by: spec 0102
  - summary: insert a new UUID and use `updatedAt` for different versions.
  - desired behavior: the current merge keeps the chronologically most recent version.
  - acceptance criteria: the behavior exists in the current import flow.
  - verification: `./gradlew test`

- [x] **Record sync metadata** (test-type: integration)
  - blocked-by: existing merge
  - summary: write `SyncLog` when importing data.
  - desired behavior: the operation leaves a local audit record.
  - acceptance criteria: the current flow persists `SyncLog`.
  - verification: `./gradlew test`

- [x] **Decide the tie-breaker for equal timestamps** (test-type: unit)
  - blocked-by: characterization tests
  - summary: select a stable key and document the total order.
  - desired behavior: both devices choose the same version when payloads differ.
  - acceptance criteria: the decision covers active/deleted versions and passes with both input orders.
  - verification: `./gradlew test`

- [x] **Extract and cover a single resolver** (test-type: unit)
  - blocked-by: tie-breaker decided
  - summary: centralize insertion, editing, soft delete, and tie-breaking in a pure function.
  - desired behavior: the resolver is deterministic, idempotent, and symmetric.
  - acceptance criteria: the complete matrix, retries, and reverse order pass.
  - verification: `./gradlew test`

- [x] **Apply the batch and log in the same transaction** (test-type: integration)
  - blocked-by: single resolver
  - summary: integrate the resolver into spec 0102 with consistent rollback.
  - desired behavior: a failure leaves neither applied entities without a log nor a log without entities.
  - acceptance criteria: a test with an injected failure rolls back the entire batch.
  - verification: `./gradlew test`

- [x] **Display local sync history** (test-type: both)
  - blocked-by: transaction with log
  - summary: list the peer, time, type, and counts without clinical content.
  - desired behavior: the user can confirm and diagnose recent syncs.
  - acceptance criteria: the screen displays data, empty states, and errors correctly and accessibly.
  - verification: `./gradlew test`

- [ ] **Validate convergence on two devices** (test-type: integration)
  - blocked-by: single resolver; transaction with log
  - summary: exercise edit/edit, edit/delete, delete/delete, and tie cases.
  - desired behavior: both databases end in the same state after transfers in both directions.
  - acceptance criteria: all scenarios converge without silent data loss.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
