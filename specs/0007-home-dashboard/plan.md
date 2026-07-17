# Plan: Home dashboard

Spec: [spec.md](./spec.md)

## Sequence

1. Add unit tests for pet summaries, overall status propagation, healthy/alert states, ordering, and limits.
2. Carry `overallStatus` from `PetWithSummary` into every pet-card layout.
3. Derive and render the accessible “All good” state or severity-ordered alerts.
4. Add Compose/integration coverage without regressing tasks, timeline, refresh, or navigation.
5. Add a focused dashboard E2E journey backed by persisted health data.

## Architecture

- `HomeViewModel` combines `PetRepository`, `TaskRepository`, `TimelineRepository`, `WeightEntryRepository`, and the health summary.
- `HomeUiState` represents loading, refresh, pets, tasks, and events.
- `QuickAddScreen` provides five actions; `PetSelectionScreen` mediates actions that require a pet.
- `ActivityTimelineScreen` provides filters by period and pet.

## Dependencies and risks

- Depends on `0001`–`0005`; partial failures from one source must not hide the entire dashboard.
- Per-pet aggregations must avoid excessive queries and inconsistent states.
- Horizontal and compact pet-card layouts must expose the same health semantics.

## Verification

1. Run focused `HomeViewModel` tests after each aggregation change.
2. Run dashboard Compose/integration tests and the focused E2E journey.
3. Run `./gradlew test`, `./gradlew spotlessCheck`, then `./gradlew assembleDebug` followed by `./gradlew installDebug`.
