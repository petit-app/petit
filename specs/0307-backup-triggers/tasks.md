# Tasks: Backup Triggers

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Backup after creating a pet** (test-type: both)
  - blocked-by: spec 0305, spec 0306
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 1: Backup after creating a pet” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN automatic backup is enabled AND I have an internet connection WHEN I add a new pet THEN after 5 minutes of inactivity the backup runs automatically AND includes the new pet
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Debounce multiple changes** (test-type: both)
  - blocked-by: spec 0305, spec 0306; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 2: Debounce multiple changes” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I make several changes in succession: - I add the pet Luna - I add a 3.5kg weight record - I add the V3 vaccine - All in less than 5 minutes THEN only ONE backup runs (5 minutes after the last change) AND it includes all changes
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Backup after deletion** (test-type: both)
  - blocked-by: spec 0305, spec 0306; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 3: Backup after deletion” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN automatic backup is enabled WHEN I delete a pet THEN after 5 minutes without changes the backup runs AND reflects the deletion
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Cancel pending backup** (test-type: both)
  - blocked-by: spec 0305, spec 0306; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 4: Cancel pending backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I changed data and a backup is pending (in 3 min) WHEN I make another change THEN the timer is reset to 5 minutes again AND only one backup will run
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Do not duplicate periodic backup** (test-type: both)
  - blocked-by: spec 0305, spec 0306; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 5: Do not duplicate periodic backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN a change-triggered backup is pending AND the periodic backup is due to run now THEN only one backup runs AND the change-triggered backup timer is canceled
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 6: App closed after a change** (test-type: both)
  - blocked-by: spec 0305, spec 0306; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 6: App closed after a change” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I made changes AND I close the app immediately THEN the pending backup will still run (WorkManager persists the task) ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
