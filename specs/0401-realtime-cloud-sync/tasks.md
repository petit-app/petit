# Tasks: Real-Time Sync

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Sync after creating data** (test-type: both)
  - blocked-by: spec 0201
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI when applicable.
  - desired behavior: the “Scenario 1: Sync after creating data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am a premium user with sync enabled AND I have an internet connection WHEN I add a new pet named "Luna" THEN Luna is saved to Room immediately (syncStatus = PENDING) AND after a few seconds, Luna is sent to Firestore AND syncStatus changes to SYNCED AND I see the sync indicator ✓
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Real-time sync receiving data** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI when applicable.
  - desired behavior: the “Scenario 2: Real-time sync receiving data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have the app open AND someone (or another device) adds data to Firestore WHEN the change is detected by the Firestore snapshot listener THEN the new data is downloaded automatically AND saved to the local Room database AND appears in the UI without requiring a manual refresh
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Sync without internet access (queue)** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI when applicable.
  - desired behavior: the “Scenario 3: Sync without internet access (queue)” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have no internet connection WHEN I add a new pet THEN the pet is saved to Room (syncStatus = PENDING) AND the pet appears in the UI as usual AND when the internet connection is restored, sync occurs automatically
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Enable sync for the first time** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI when applicable.
  - desired behavior: the “Scenario 4: Enable sync for the first time” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have local data AND I have never synced before WHEN I enable "Cloud sync" in settings THEN all local data is sent to Firestore AND I see the progress message "Syncing X of Y items..." AND when the operation is complete, all items have syncStatus = SYNCED
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Premium expires** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI when applicable.
  - desired behavior: the “Scenario 5: Premium expires” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN my premium subscription expires WHEN this happens THEN the Firestore snapshot listener is disconnected AND new data is saved locally only (syncStatus = LOCAL_ONLY) AND previously synced data remains on the device AND I see the warning "Sync paused - Renew your premium subscription" ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
