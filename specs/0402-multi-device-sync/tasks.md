# Tasks: Multi-Device Sync

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Second Device Receives Data** (test-type: both)
  - blocked-by: spec 0401
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 1: Second Device Receives Data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have data on device A AND I install the app on device B WHEN I sign in on device B AND enable sync THEN all my data is downloaded from Firestore AND I see the same pets as on device A
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Edits Appear in Real Time** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 2: Edits Appear in Real Time” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have the app open on devices A and B WHEN I change the pet's name to "Lulu" on device A THEN within a few seconds, device B shows "Lulu" without requiring a manual refresh
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Create on One Device, View on Another** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 3: Create on One Device, View on Another” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I add a new pet named "Simba" on device A WHEN sync completes THEN device B receives "Simba" automatically AND Simba appears in the pet list
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Delete on One Device, Reflect on Another** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 4: Delete on One Device, Reflect on Another” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I delete the pet "Simba" on device A WHEN sync completes THEN device B no longer shows "Simba" either
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Offline Device vs. Online Device** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 5: Offline Device vs. Online Device” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN device A is offline AND device B adds a pet named "Mia" WHEN device A comes back online THEN device A receives "Mia" automatically ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
