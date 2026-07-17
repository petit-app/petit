# Tasks: Pet management

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Register and edit a valid pet** (test-type: both)
  - blocked-by: none
  - desired behavior: validate the form, persist the pet, and update `updatedAt` when editing.
  - acceptance criteria: name and type are required; limits and a non-future birth date are enforced; data persists after restart.
  - verification: `./gradlew test`
- [x] **List and open active pets** (test-type: integration)
  - blocked-by: register and edit a valid pet
  - desired behavior: show only pets without `deletedAt` and navigate to the profile.
  - acceptance criteria: sorted list and profile with data and management actions.
  - verification: `./gradlew test`
- [x] **Soft-delete a pet** (test-type: both)
  - blocked-by: list and open active pets
  - desired behavior: confirm deletion, populate `deletedAt`, and hide the pet.
  - acceptance criteria: the record remains in the database and is excluded from active queries.
  - verification: `./gradlew test`
- [x] **Select and display the pet's local photo** (test-type: integration)
  - blocked-by: register and edit a valid pet
  - desired behavior: select an image from the gallery and display it in the form, list, and profile.
  - acceptance criteria: the selected URI remains associated after saving.
  - verification: `./gradlew test`
- [ ] **Complete photo capture and validation** (test-type: integration)
  - blocked-by: select and display the pet's local photo
  - desired behavior: provide camera capture and reject files larger than 5 MB or in an unsupported format.
  - acceptance criteria: the camera and picker accept only JPG/PNG files within the limit.
  - verification: `./gradlew test`
- [ ] **Cover the flow with automated tests** (test-type: both)
  - blocked-by: soft-delete a pet, complete photo capture and validation
  - desired behavior: protect validation, the DAO, soft delete, and navigation against regressions.
  - acceptance criteria: DAO unit tests and basic UI tests run successfully.
  - verification: `./gradlew test`

- [x] **Cover the pet CRUD E2E journey** (test-type: integration)
  - blocked-by: soft-delete a pet
  - desired behavior: exercise registration, editing, and deletion through the real Activity and local database.
  - acceptance criteria: a pet appears after creation, reflects its edited name, and disappears from Home after confirmed deletion.
  - verification: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woliveiras.petit.e2e.PetCrudJourneyTest`
