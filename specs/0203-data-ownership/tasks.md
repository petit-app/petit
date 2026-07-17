# Tasks: Data Ownership

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: First login links existing data** (test-type: both)
  - blocked-by: spec 0201
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 1: First login links existing data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I have 2 pets registered locally AND I have never logged in before WHEN I log in with "pessoa-a@example.com" THEN my 2 pets are marked with ownerId = my userId AND I can continue using the app normally
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Data created after login is linked automatically** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 2: Data created after login is linked automatically” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am logged in as "pessoa-a@example.com" WHEN I register a new pet named "Luna" THEN Luna is created with ownerId = my userId
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Data created in anonymous mode has no owner** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 3: Data created in anonymous mode has no owner” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am using the app without being logged in WHEN I register a pet named "Simba" THEN Simba is created with ownerId = null
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Logging out does not remove ownership** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 4: Logging out does not remove ownership” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I have pets linked to my userId WHEN I log out THEN the pets retain their ownerId AND remain visible in the app
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Log in with a different account** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 5: Log in with a different account” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I have pets belonging to "user-a" on the device AND I log in as "pessoa-b@example.com" (user-b) THEN I see Person A's pets (local data) BUT they retain ownerId = user-a AND new data will have ownerId = user-b (management of multiple owners is handled in a future sync) ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
