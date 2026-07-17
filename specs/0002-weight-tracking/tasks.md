# Tasks: Weight tracking

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Record weight in grams or kilograms** (test-type: both)
  - blocked-by: 0001
  - desired behavior: validate, convert to grams, and persist a weight entry.
  - acceptance criteria: only values between 0 and 50 kg and non-future dates are accepted.
  - verification: `./gradlew test`
- [x] **Keep one weight entry per pet per day** (test-type: integration)
  - blocked-by: record weight in grams or kilograms
  - desired behavior: replace the active entry when the pet and date match.
  - acceptance criteria: the query returns a single entry for the day.
  - verification: `./gradlew test`
- [x] **Display history and weight-change chart** (test-type: integration)
  - blocked-by: record weight in grams or kilograms
  - desired behavior: sort entries and render a bar chart in kg.
  - acceptance criteria: the descending history and chart update after changes.
  - verification: `./gradlew test`
- [x] **Edit and delete a weight entry** (test-type: both)
  - blocked-by: display history and weight-change chart
  - desired behavior: update the value/`updatedAt` and perform a soft delete.
  - acceptance criteria: the list and chart reflect edits and deletions.
  - verification: `./gradlew test`
- [ ] **Add automated weight regression tests** (test-type: both)
  - blocked-by: edit and delete a weight entry
  - desired behavior: cover conversion, limits, upsert, and queries.
  - acceptance criteria: the automated suite protects all spec criteria.
  - verification: `./gradlew test`

- [x] **Cover weight history with an E2E journey** (test-type: integration)
  - blocked-by: display history and weight-change chart
  - desired behavior: register a pet, add a weight through the UI, and read it back from history.
  - acceptance criteria: the saved kilogram value appears in the current-weight card and the dated history entry.
  - verification: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woliveiras.petit.e2e.WeightHistoryJourneyTest`
