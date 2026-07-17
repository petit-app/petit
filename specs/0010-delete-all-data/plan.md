# Plan: Delete all data

Spec: [spec.md](./spec.md)

## Sequence

1. Present the destructive scope and require the localized confirmation word.
2. Cancel all scheduled task notification work.
3. Delete dependent pet-care records and pets in one Room transaction.
4. Show a success state and return to Home after completion.
5. Extend the purge to family-group/synchronization data and define preference-reset behavior.
6. Make partial-failure behavior visible and recoverable.
7. Add automated coverage for confirmation, cancellation, transactions, retained data, and navigation.

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
