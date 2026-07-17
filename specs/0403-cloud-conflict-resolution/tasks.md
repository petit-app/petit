# Tasks: Cloud Conflict Resolution

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Basic Last-Write-Wins** (test-type: both)
  - blocked-by: spec 0401
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 1: Basic Last-Write-Wins” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN the pet "Luna" has updatedAt = 1000 on device A AND device B edits Luna (updatedAt = 1500) WHEN device A receives the change from the Firestore snapshot listener THEN device B's version (the newer one) is retained AND device A shows B's changes
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Older Offline Edit** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 2: Older Offline Edit” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN device A is offline and edits Luna (updatedAt = 1000) AND device B edits Luna online (updatedAt = 1500) WHEN device A comes back online and attempts to sync THEN device B's version wins (higher updatedAt) AND device A's changes are discarded AND device A updates to B's version
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Newer Offline Edit** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 3: Newer Offline Edit” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN device A is offline and edits Luna (updatedAt = 2000) AND the Firestore version has updatedAt = 1500 WHEN device A comes back online and syncs THEN device A's version wins (higher updatedAt) AND Firestore is updated with A's version AND other devices receive A's version
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Delete vs. Edit** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 4: Delete vs. Edit” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN device A deletes Luna (deletedAt = 1500) AND device B edited Luna (updatedAt = 1600) before receiving the deletion WHEN sync occurs THEN if the edit is newer than the deletion, Luna is restored OR if the deletion is newer, Luna remains deleted
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Different Fields Edited** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 5: Different Fields Edited” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN device A changes Luna's name to "Lulu" AND device B edits Luna's weight at the same time WHEN sync occurs THEN both changes are retained (if the strategy is field-level) OR the newer version wins completely (if it is document-level) ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
