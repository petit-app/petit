# Tasks: Google Login

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Successful login** (test-type: both)
  - blocked-by: none
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 1: Successful login” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am using the app without being logged in WHEN I tap "Sign in with Google" THEN I see the system Google account picker WHEN I select my account AND authorize the app THEN I am successfully authenticated AND I see my name/photo on the settings screen AND the state changes to "Authenticated"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: First login associates existing data** (test-type: both)
  - blocked-by: none; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 2: First login associates existing data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I have local data (pets, weights, etc.) AND I have never logged in before WHEN I sign in with Google for the first time THEN my local data is associated with my userId AND I can continue using the app normally
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Login canceled** (test-type: both)
  - blocked-by: none; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 3: Login canceled” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I start the login process WHEN I cancel the account picker OR close the dialog THEN I return to the previous state (anonymous) AND I do not see an error message AND I can try again
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Network error during login** (test-type: both)
  - blocked-by: none; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 4: Network error during login” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I have no internet connection WHEN I try to log in THEN I see the message "No connection. Try again." AND I remain in anonymous mode AND the app continues to work normally offline
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Login triggered when attempting a backup** (test-type: both)
  - blocked-by: none; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 5: Login triggered when attempting a backup” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am using the app without being logged in AND I have local data (pets, weights, etc.) WHEN I try to "Back up to Google Drive" THEN I see a dialog explaining that login is required AND I have a "Sign in with Google" option WHEN I log in successfully THEN the backup starts automatically ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
