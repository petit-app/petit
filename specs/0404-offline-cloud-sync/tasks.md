# Tasks: Offline-First Sync

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Create Data Offline** (test-type: both)
  - blocked-by: spec 0401
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 1: Create Data Offline” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have no internet connection WHEN I register a new pet named "Mia" THEN Mia is saved in Room (syncStatus = PENDING_SYNC) AND Mia appears in the list as usual AND I see a "Pending sync" indicator on the item
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Automatic Sync on Reconnection** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 2: Automatic Sync on Reconnection” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have data pending sync AND I am offline WHEN the internet connection returns THEN sync starts automatically AND the pending data is uploaded AND syncStatus changes to SYNCED AND the pending indicator disappears
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Multiple Offline Edits** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 3: Multiple Offline Edits” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am offline WHEN I make several edits: - Add the pet Mia - Add a weigh-in for Mia - Change Luna's name to Lulu THEN all edits are saved locally AND all of them remain PENDING_SYNC AND upon reconnection, all of them are uploaded
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Conflict After Coming Back Online** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 4: Conflict After Coming Back Online” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I edited Luna offline (updatedAt = 1000) AND another device edited Luna online (updatedAt = 1500) WHEN I come back online and sync THEN conflict resolution occurs AND the newer version (1500) wins
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Sync Queue Persists After the App Is Closed** (test-type: both)
  - blocked-by: spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 5: Sync Queue Persists After the App Is Closed” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I made edits offline AND I close the app AND I reopen the app (still offline) THEN the edits are still PENDING_SYNC AND they will be synced upon reconnection ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
