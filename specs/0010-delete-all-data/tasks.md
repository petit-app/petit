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
- [x] **Complete the local-data purge scope** (test-type: integration)
  - blocked-by: cancel workers and delete pet-care records
  - desired behavior: delete family-group members and sync logs, reset reminder/family preferences, and retain theme, language, and onboarding completion.
  - acceptance criteria: every Room table and DataStore is intentionally deleted, reset, or retained according to the spec.
  - test expectations: unit tests cover the reset policy; Room and DataStore tests verify every store explicitly.
  - verification: `./gradlew test`
- [x] **Make deletion failures recoverable** (test-type: both)
  - blocked-by: cancel workers and delete pet-care records
  - desired behavior: display errors and define recovery when workers were canceled but the Room transaction failed.
  - acceptance criteria: failure never shows success, is visible to the caregiver, and offers a safe retry path.
  - test expectations: unit tests cover cancellation, Room, and preference-reset failures plus repeated submission/retry; Compose tests cover error and retry UI.
  - verification: `./gradlew test`
- [x] **Add automated delete-all-data regression tests** (test-type: both)
  - blocked-by: complete the local-data purge scope, make deletion failures recoverable
  - desired behavior: cover confirmation, idempotence, WorkManager cancellation, transaction rollback, complete scope, errors, and navigation.
  - acceptance criteria: every acceptance criterion and documented retention decision has automated coverage.
  - test expectations: add a focused E2E journey for complete deletion and safe navigation; keep rollback/idempotence at integration boundaries.
  - verification: `./gradlew test && ./gradlew spotlessCheck`
