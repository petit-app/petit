# Tasks: Restore Backup

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Successfully restore a backup** (test-type: both)
  - blocked-by: spec 0301
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 1: Successfully restore a backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am signed in with Google AND I have backups saved in Google Drive WHEN I open "Saved backups" AND select a backup to restore AND confirm the restore THEN I see the download progress AND the data is restored to the local database AND I see the message "Data restored successfully"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Restore on a new device** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 2: Restore on a new device” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I installed the app on a new phone AND signed in with my Google account WHEN I open "Restore from backup" THEN I see a list of available backups AND I can select which one to restore
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Restore replaces local data** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 3: Restore replaces local data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have local data AND restore a backup WHEN I confirm "Replace local data" THEN ALL local data is deleted AND the backup data is imported AND I see the backup data on the home screen
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Restore with merge** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 4: Restore with merge” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have local data AND restore a backup WHEN I choose "Merge with local data" THEN the data is merged (last-write-wins) AND unique data from both sources is retained
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Restore with no backups** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 5: Restore with no backups” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have no backups in Google Drive WHEN I open "Saved backups" THEN I see the message "No backups found" AND I see a suggestion to create my first backup
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 6: Download error** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 6: Download error” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I select a backup to restore WHEN the connection fails during the download THEN I see an error message AND the local data is not changed AND I can try again ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
