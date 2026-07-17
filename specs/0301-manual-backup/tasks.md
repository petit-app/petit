# Tasks: Manual Backup

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Successfully create a backup (user already signed in)** (test-type: both)
  - blocked-by: spec 0201
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 1: Successfully create a backup (user already signed in)” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am signed in with Google AND I have an internet connection WHEN I open Settings > "Google Drive Backup" AND tap "Back up now" THEN I see a progress indicator AND the backup is uploaded to Google Drive (appDataFolder) AND I see the message "Backup completed successfully" AND I see the date/time of the latest backup
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Backup without internet access** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 2: Backup without internet access” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have no internet connection WHEN I try to create a backup THEN I see the message "No connection. Connect to the internet to create a backup." AND the backup does not start
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Backup while signed out (triggers sign-in)** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 3: Backup while signed out (triggers sign-in)” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am not signed in WHEN I try to create a backup THEN I see a dialog explaining that Google sign-in is required AND I have a "Sign in with Google" option WHEN I sign in successfully THEN the backup starts automatically
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: First backup** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 4: First backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have never created a backup before WHEN I create my first backup THEN the file is created in the Google Drive appDataFolder AND the metadata is initialized AND I see "Backup completed successfully"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Subsequent backup** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 5: Subsequent backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I already have previous backups WHEN I create a new backup THEN a new file is created (it does not replace the previous one) AND the metadata is updated AND old backups are retained (up to the limit)
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 6: Error during backup** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 6: Error during backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I start a backup WHEN an error occurs (network drops, quota exceeded, etc.) THEN I see a specific error message AND the partial backup is discarded AND I can try again ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
