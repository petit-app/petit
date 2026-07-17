# Plan: Vaccination records

Spec: [spec.md](./spec.md)

## Sequence

1. Add unit tests for date/status boundaries, species validation, `OTHER`, and edit metadata.
2. Centralize validation and make date-dependent behavior use a controlled clock.
3. Complete the form behavior for species-specific types and the required custom name.
4. Render every health state and provide a latest-dose summary grouped by type.
5. Add Room and Compose coverage for persistence, grouping, history, and soft delete.

## Architecture

- Room stores the doses; the domain calculates status from `nextDueDate` and an injected clock.
- `VaccinationViewModel` accesses the repository and exposes state to `VaccinationFormScreen` and `VaccinationRecordsScreen`.
- Saving or deleting a dose may trigger automated tasks from spec `0005`.

## Dependencies and risks

- Depends on `0001`; optionally integrates with `0005`.
- Day calculation must be deterministic and testable with a controlled clock.
- Same-day doses need deterministic ordering when selecting the latest record.

## Verification

1. Run focused vaccination unit and integration tests after each vertical slice.
2. Run `./gradlew test` and `./gradlew spotlessCheck`.
3. For UI changes, run the focused Android test, then `./gradlew assembleDebug` followed by `./gradlew installDebug`.
