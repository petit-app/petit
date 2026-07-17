# Tasks: Account Management

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: View account information** (test-type: both)
  - blocked-by: spec 0201
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 1: View account information” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am logged in as "pessoa-a@example.com" WHEN I go to Settings > My Account THEN I see: - My Google profile photo - My name "Person A" - My email "pessoa-a@example.com" - My plan status (Free/Premium) - Date of last login
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Log out** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 2: Log out” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am logged in WHEN I tap "Log out" AND confirm the action THEN I am logged out of Firebase Auth AND I return to the "Anonymous" state AND my local data remains on the device AND I can continue using the app without being logged in
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Logging out preserves local data** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 3: Logging out preserves local data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I have 2 registered pets AND I am logged in WHEN I log out THEN I can still see my 2 pets AND I can add new data AND the data is not deleted
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Switch accounts** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 4: Switch accounts” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am logged in as "pessoa-a@example.com" WHEN I log out AND log in with "pessoa-b@example.com" THEN I am authenticated as Person B AND the local data remains (Person A's data) (data association by account is handled during sync — future phases)
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Delete account** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 5: Delete account” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am logged in WHEN I tap "Delete my account" THEN I see a warning explaining the consequences: - "Your account will be removed from Firebase" - "Local data will remain on the device" - "Cloud data will be removed within 30 days" WHEN I confirm by typing "DELETE" THEN my account is removed from Firebase AND the cloud data is scheduled to be purged in 30 days AND I am logged out AND I return to anonymous mode ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
