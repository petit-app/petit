# Tasks: Home dashboard

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Display the initial state without pets** (test-type: both)
  - blocked-by: 0001
  - desired behavior: display a welcome message and an add action.
  - acceptance criteria: the action opens the form for the first pet.
  - verification: `./gradlew test`
- [ ] **Summarize health and upcoming care by pet** (test-type: both)
  - blocked-by: 0001, 0002, 0003, 0004
  - desired behavior: combine photo, name, latest weight, status, and next event.
  - acceptance criteria: cards display the calculated overall health status, update with active data, and open the profile.
  - test expectations: unit tests cover aggregation and active-data updates; Compose tests cover compact and horizontal cards plus accessibility semantics.
  - verification: `./gradlew test`
- [x] **Display tasks and timeline** (test-type: integration)
  - blocked-by: 0005
  - desired behavior: display up to five upcoming tasks and recent activity with “View all”.
  - acceptance criteria: actions open the full lists and the correct events.
  - verification: `./gradlew test`
- [x] **Provide quick actions and refresh** (test-type: integration)
  - blocked-by: summarize health and upcoming care by pet
  - desired behavior: navigate through Quick Add and settings, and support pull-to-refresh.
  - acceptance criteria: all five actions work, and pet selection appears when necessary.
  - verification: `./gradlew test`
- [ ] **Display the overall healthy state and separate alerts** (test-type: both)
  - blocked-by: summarize health and upcoming care by pet
  - desired behavior: add an “All good” banner and a section for items requiring attention.
  - acceptance criteria: healthy and critical states are clear and accessible.
  - test expectations: unit tests cover all-OK, scheduled, overdue, mixed, and ordering states; Compose tests cover visible and accessibility semantics.
  - verification: `./gradlew test`
- [ ] **Add automated dashboard regression tests** (test-type: both)
  - blocked-by: summarize health and upcoming care by pet, display tasks and timeline, provide quick actions and refresh
  - desired behavior: cover aggregation, empty state, pet-card health, task and activity limits, navigation, and Quick Add.
  - acceptance criteria: all acceptance criteria have automated coverage at unit, integration, or UI boundaries.
  - test expectations: retain unit coverage for every changed behavior and add a persisted-data E2E dashboard journey.
  - verification: `./gradlew test && ./gradlew spotlessCheck`
