# Tasks: Deworming records

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Record, edit, and delete deworming treatments** (test-type: both)
  - blocked-by: 0001
  - desired behavior: validate and persist `INTERNAL`, `EXTERNAL`, or `BOTH`, with soft delete.
  - acceptance criteria: required medication, valid dates, and descending history.
  - verification: `./gradlew test`
- [ ] **Calculate and display per-record status** (test-type: both)
  - blocked-by: record, edit, and delete deworming treatments
  - desired behavior: classify each next dose and display its indicator.
  - acceptance criteria: states `OK`, `SCHEDULED`, and `OVERDUE` follow the dates and each has a visible indicator.
  - test expectations: unit tests cover every boundary with a controlled clock; Compose tests cover text and visual indicators.
  - verification: `./gradlew test`
- [ ] **Separate health status by category** (test-type: both)
  - blocked-by: calculate and display per-record status
  - desired behavior: display internal and external sections using the latest applicable record.
  - acceptance criteria: `BOTH` counts in both categories, and each section has its own status.
  - test expectations: unit tests cover competition between specific and `BOTH` records plus tie-breaking; Room/Compose tests cover the rendered summaries.
  - verification: `./gradlew test`
- [ ] **Add automated deworming regression tests** (test-type: both)
  - blocked-by: separate health status by category
  - desired behavior: cover validation, status, categories, Room, and UI.
  - acceptance criteria: all acceptance criteria have automated coverage.
  - test expectations: close remaining unit, Room, and Compose gaps; add E2E only if a cross-boundary journey remains uncovered.
  - verification: `./gradlew test && ./gradlew spotlessCheck`
