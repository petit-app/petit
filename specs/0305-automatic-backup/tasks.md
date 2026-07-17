# Tasks: Automatic Backup

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Automatic backup enabled by default (signed-in user)** (test-type: both)
  - blocked-by: spec 0301
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 1: Automatic backup enabled by default (signed-in user)” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am signed in with Google WHEN I enable automatic backup in settings THEN WorkManager schedules a daily backup at 2:00 a.m. AND I see "Automatic backup enabled — next backup at 2:00 a.m."
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Daily backup runs in the background** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 2: Daily backup runs in the background” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN automatic backup is enabled WHEN it is 2:00 a.m. THEN the backup runs automatically EVEN IF the app is closed AND I do not need to open the app AND the backup is saved to Google Drive
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Backup over Wi-Fi only** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 3: Backup over Wi-Fi only” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN "Back up over Wi-Fi only" is enabled AND I am connected to a mobile network (4G/5G) WHEN the automatic backup is due to run THEN the backup is postponed AND it runs when I connect to Wi-Fi
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Back up only when signed in** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 4: Back up only when signed in” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN an automatic backup is scheduled AND I am no longer signed in (signed out) WHEN it is 2:00 a.m. THEN the backup does NOT run AND I see the notification "Sign in to continue automatic backups"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Wi-Fi-only setting is respected** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 5: Wi-Fi-only setting is respected” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN "Back up over Wi-Fi only" is enabled AND I am connected to a mobile network (4G/5G) at 2:00 a.m. WHEN the automatic backup is due to run THEN the backup is postponed AND it runs when I connect to Wi-Fi AND I see the notification "Waiting for Wi-Fi to back up"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 6: No internet** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 6: No internet” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have no internet connection WHEN the automatic backup is due to run THEN the backup fails silently AND it will be attempted again next time AND I can see "Last backup: 2 days ago (failed)" in settings ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
