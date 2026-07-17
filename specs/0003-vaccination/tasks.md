# Tasks: Vaccination records

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Record and edit a vaccination** (test-type: both)
  - blocked-by: 0001
  - desired behavior: validate dates, type by species, `OTHER`, and traceability details.
  - acceptance criteria: a valid record is persisted, `OTHER` requires a custom name, and editing updates `updatedAt`.
  - test expectations: unit tests cover every validation branch and timestamp behavior; Room tests cover create/edit persistence.
  - verification: `./gradlew test`
- [ ] **Calculate and display the dose status** (test-type: both)
  - blocked-by: record and edit a vaccination
  - desired behavior: classify the next dose as `OK`, `SCHEDULED`, or `OVERDUE` and render every state in the history.
  - acceptance criteria: the 30-day thresholds and absence of a next dose follow the spec, and each state has a visible indicator.
  - test expectations: unit tests cover past, today, 30 days, 31 days, and no due date with a controlled clock; Compose tests cover all badges.
  - verification: `./gradlew test`
- [x] **Display and delete vaccination history** (test-type: both)
  - blocked-by: calculate and display the dose status
  - desired behavior: display doses by month, highlight types/statuses, and allow soft deletion.
  - acceptance criteria: the history and visual state update after deletion.
  - verification: `./gradlew test`
- [ ] **Provide history grouped by type** (test-type: integration)
  - blocked-by: display and delete vaccination history
  - desired behavior: group doses of each type without losing the full chronology.
  - acceptance criteria: each type shows its current status and provides access to all its doses.
  - test expectations: unit tests cover grouping and deterministic latest selection; Room/Compose tests prove older doses remain accessible.
  - verification: `./gradlew test`
- [ ] **Add automated vaccination regression tests** (test-type: both)
  - blocked-by: provide history grouped by type
  - desired behavior: cover calculation, validation, catalog, persistence, and UI.
  - acceptance criteria: all acceptance criteria have automated coverage.
  - test expectations: close any remaining unit, Room, and Compose gaps; add E2E only if a cross-boundary journey remains uncovered.
  - verification: `./gradlew test && ./gradlew spotlessCheck`
