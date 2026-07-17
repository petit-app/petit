# Tasks: Manage Backups

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: View the backup list** (test-type: both)
  - blocked-by: spec 0301
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 1: View the backup list” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am signed in with Google AND have multiple saved backups WHEN I open "Saved backups" THEN I see a list of all backups AND each item shows: - Backup date and time - Number of pets - File size - App version
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: View backup details** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 2: View backup details” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am on the backup list WHEN I tap a backup THEN I see full details: - Date and time - Contents (X pets, Y weigh-ins, Z vaccinations) - Size - Version of the app that created it AND I see the options: Restore, Delete
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Delete a specific backup** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 3: Delete a specific backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am viewing a backup's details WHEN I tap "Delete" AND confirm the deletion THEN the backup is removed from Google Drive AND no longer appears in the list
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Delete multiple backups** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 4: Delete multiple backups” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am on the backup list WHEN I enable selection mode (long press) AND select multiple backups AND tap "Delete selected" AND confirm THEN all selected backups are removed
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Manual backup limit** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 5: Manual backup limit” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have 10 saved manual backups (the limit) WHEN I create a new manual backup THEN the oldest manual backup is removed automatically AND the new backup is added AND I see the notification "Old backup removed to free up space"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 7: Backups after account deletion** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 7: Backups after account deletion” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have backups saved in Google Drive WHEN I delete my app account THEN the backups are retained for 90 days (grace period) AND I see the warning "Your account will be permanently deleted in X days." AND after 90 days without reactivation, the backups are purged automatically
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 8: Backups after account deletion** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 8: Backups after account deletion” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have saved backups WHEN I delete my account THEN the backups are scheduled for purging in 30 days AND after 30 days, all files in the user's bucket are permanently removed
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 6: Total space used** (test-type: both)
  - blocked-by: spec 0301; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 6: Total space used” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am on the backup screen WHEN I view the "Saved backups" section THEN I see the total number of backups AND the total space used (e.g., "3 backups • 45.2 KB") ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
