# Tasks: Backup Settings

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Enable/disable automatic backup** (test-type: both)
  - blocked-by: spec 0305
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 1: Enable/disable automatic backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am signed in with Google WHEN I open Settings > Automatic Backup AND I enable the "Automatic backup" toggle THEN the daily backup at 2:00 a.m. is scheduled AND I see "Next backup: today/tomorrow at 2:00 a.m." WHEN I disable the toggle THEN the schedule is canceled AND I see "Automatic backup disabled"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Configure Wi-Fi only** (test-type: both)
  - blocked-by: spec 0305; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 2: Configure Wi-Fi only” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN automatic backup is enabled AND "Wi-Fi only" is disabled WHEN I enable "Wi-Fi only" THEN future backups run only over Wi-Fi AND the current schedule is adjusted GIVEN I am on a mobile network at 2:00 a.m. AND "Wi-Fi only" is enabled WHEN the backup is due to run THEN it is postponed until I connect to Wi-Fi
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: View backup history** (test-type: both)
  - blocked-by: spec 0305; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 3: View backup history” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have completed automatic backups WHEN I open "View history" THEN I see a list of the latest backups AND each item shows: - Date/time - Whether it was automatic or manual - Status (success/failure)
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Backup notification** (test-type: both)
  - blocked-by: spec 0305; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 5: Backup notification” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN "Notify after backup" is enabled WHEN an automatic backup completes successfully THEN I receive a silent notification "Backup completed: 2 pets, 15 KB" GIVEN "Notify after backup" is disabled WHEN a backup is completed THEN I do NOT receive a notification
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 6: Back up now** (test-type: both)
  - blocked-by: spec 0305; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 6: Back up now” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am on the backup settings screen WHEN I tap "Back up now" THEN a backup runs immediately AND the timer for the next automatic backup is reset ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
