# Tasks: Cloud Family Sharing

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Create a family group** (test-type: both)
  - blocked-by: spec 0201, spec 0401
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 1: Create a family group” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am a premium user WHEN I open Settings > "Family" AND I tap "Create family group" THEN a group is created AND I become the administrator AND I receive an invitation code
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Invite a member** (test-type: both)
  - blocked-by: spec 0201, spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 2: Invite a member” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am the admin of a family group WHEN I share the invitation code "PETIT-ABC123" AND another person enters the code in their app THEN they join the family group AND can see all pets in the group
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Everyone can view and edit** (test-type: both)
  - blocked-by: spec 0201, spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 3: Everyone can view and edit” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN Person A and Person B are in the same family group WHEN Person B adds a weight measurement for a pet THEN Person A sees the weight measurement automatically AND Person A can also add or edit data
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Admin permissions** (test-type: both)
  - blocked-by: spec 0201, spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 4: Admin permissions” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am the group admin WHEN I open the member list THEN I can: - Remove members - Generate a new invitation code - Delete the group
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: A member leaves the group** (test-type: both)
  - blocked-by: spec 0201, spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 5: A member leaves the group” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am a member of a family group WHEN I select "Leave group" THEN I lose access to the shared data AND the data remains available to the other members AND my personal data (not shared) remains with me
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 6: Private vs. shared pets** (test-type: both)
  - blocked-by: spec 0201, spec 0401; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and UI where applicable.
  - desired behavior: the “Scenario 6: Private vs. shared pets” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am a member of a family group WHEN I add a new pet THEN I can choose: - "Share with family" (everyone can see it) - "Keep private" (only I can see it) ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
