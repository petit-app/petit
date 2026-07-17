# Tasks: Delete all data

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Require destructive-action confirmation** (test-type: both)
  - blocked-by: none
  - desired behavior: describe the deleted pet-care domains and enable deletion only for the exact localized confirmation word.
  - acceptance criteria: mismatched input cannot start deletion; matching input can.
  - verification: source evidence in `DeleteAllDataScreen` and `DeleteAllDataViewModel`.
- [x] **Cancel workers and delete pet-care records** (test-type: integration)
  - blocked-by: require destructive-action confirmation
  - desired behavior: cancel scheduled task work and delete tasks, health records, weights, and pets transactionally.
  - acceptance criteria: the five visible pet-care domains are removed and no scheduled task notification remains active.
  - verification: source evidence in `DeleteAllDataUseCase` and the Room DAOs.
- [x] **Show completion and return to Home** (test-type: integration)
  - blocked-by: cancel workers and delete pet-care records
  - desired behavior: display success only after the operation completes and navigate back to Home on request.
  - acceptance criteria: successful deletion does not leave the destructive form active.
  - verification: source evidence in `DeleteAllDataViewModel`, `DeleteAllDataScreen`, and `PetitNavGraph`.
- [ ] **Complete the local-data purge scope** (test-type: integration)
  - blocked-by: cancel workers and delete pet-care records
  - desired behavior: delete family-group members and sync logs, and decide whether user/reminder preferences and onboarding completion are reset.
  - acceptance criteria: the product definition of all local data matches every Room table and DataStore intentionally retained or removed.
  - verification: `./gradlew test`
- [ ] **Make deletion failures recoverable** (test-type: both)
  - blocked-by: cancel workers and delete pet-care records
  - desired behavior: display errors and define recovery when workers were canceled but the Room transaction failed.
  - acceptance criteria: failure never shows success, is visible to the caregiver, and offers a safe retry path.
  - verification: `./gradlew test`
- [ ] **Add automated delete-all-data regression tests** (test-type: both)
  - blocked-by: complete the local-data purge scope, make deletion failures recoverable
  - desired behavior: cover confirmation, idempotence, WorkManager cancellation, transaction rollback, complete scope, errors, and navigation.
  - acceptance criteria: every acceptance criterion and documented retention decision has automated coverage.
  - verification: `./gradlew test`
