# Plan: Delete all data

Spec: [spec.md](./spec.md)

## Sequence

1. Present the destructive scope and require the localized confirmation word.
2. Cancel all scheduled task notification work.
3. Delete dependent pet-care records and pets in one Room transaction.
4. Show a success state and return to Home after completion.
5. Add unit tests for the reset policy, failure state, idempotence, and retry.
6. Extend the Room transaction to family-group members and sync logs.
7. Reset reminder and family-group preferences while retaining theme, language, and onboarding completion.
8. Display cancellation/deletion failures and provide a safe retry path.
9. Add Room, DataStore, WorkManager, Compose, and focused E2E coverage.

## Architecture

- `DeleteAllDataViewModel` owns confirmation and operation state.
- `DeleteAllDataUseCase` coordinates `TaskScheduler` and `PetitDatabase` on an I/O dispatcher.
- Room deletes dependent pet-care tables before pets inside a transaction.
- The dedicated Compose screen separates confirmation and success states.

## Dependencies and risks

- Depends on the local data from specs `0001`–`0005`.
- Worker cancellation and the Room transaction do not share an atomic boundary.
- `PetitDatabase` also contains family-group members and sync logs that the current purge omits.
- DataStore preferences live outside Room and require an explicit product decision before reset.
- Preference reset can fail after the Room transaction, so the operation and retry path must remain idempotent and report partial failure accurately.

## Verification

1. Run focused use-case and ViewModel unit tests for success, each failure boundary, and retry.
2. Run Room/DataStore/WorkManager integration tests and the focused deletion E2E journey.
3. Run `./gradlew test`, `./gradlew spotlessCheck`, then `./gradlew assembleDebug` followed by `./gradlew installDebug`.
