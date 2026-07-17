# Tasks: Petit Cloud Gate

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **Draft**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Keep Petit Local available without an account** (test-type: both)
  - blocked-by: spec approval
  - summary: classify and expose all on-device capabilities as free without requiring identity or entitlement.
  - desired behavior: local pet care, JSON/PDF export, and local sharing remain available without a Petit account.
  - acceptance criteria: Scenario 1 in `spec.md`.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Connect Google Drive independently from Petit Cloud** (test-type: both)
  - blocked-by: previous task; specs 0301–0307
  - summary: model Drive authorization separately and keep backup, restore, management, and automation free.
  - desired behavior: connecting Drive never requests a purchase or activates a Petit Cloud entitlement.
  - acceptance criteria: Scenario 2 in `spec.md`.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Gate only Petit-managed cloud capabilities** (test-type: both)
  - blocked-by: first task; specs 0401–0405
  - summary: show a Petit Cloud gate only where Petit operates the hosted infrastructure.
  - desired behavior: real-time sync, automatic multi-device sync, and remote family collaboration are gated while local and Drive capabilities remain available.
  - acceptance criteria: Scenarios 3, 4, and 7 in `spec.md`.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Resolve and display Petit Cloud entitlement** (test-type: both)
  - blocked-by: spec 0201; previous task
  - summary: resolve hosted-service access from persistent Petit identity and an independent active entitlement.
  - desired behavior: settings show the correct Petit Cloud state without inferring it from Drive authorization.
  - acceptance criteria: Scenario 5 in `spec.md`.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Handle Petit Cloud expiration without local data loss** (test-type: both)
  - blocked-by: previous task; approved hosted-data retention policy
  - summary: stop paid hosted operations while preserving all local and user-owned-cloud capabilities.
  - desired behavior: expiration never deletes or hides Room data and does not disable an authorized Google Drive connection.
  - acceptance criteria: Scenario 6 in `spec.md`.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Verify independent service states end to end** (test-type: integration)
  - blocked-by: all previous tasks
  - summary: cover combinations of local access, Drive authorization, Petit identity, entitlement, expiration, and provider failures.
  - desired behavior: every service-model boundary remains correct across restarts, offline use, revoked authorization, and expired entitlement.
  - acceptance criteria: all scenarios and edge cases in `spec.md` have traceable automated coverage.
  - verification: `./gradlew test`, `./gradlew connectedDebugAndroidTest`, and `./gradlew spotlessCheck`
