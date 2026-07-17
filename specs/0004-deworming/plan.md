# Plan: Deworming records

Spec: [spec.md](./spec.md)

## Sequence

1. Add unit tests for validation, status boundaries, and category projection.
2. Make date-dependent logic deterministic and complete all per-record indicators.
3. Select the latest applicable record for internal and external categories, including `BOTH` in each.
4. Display the two category summaries while preserving descending history, editing, and soft delete.
5. Add Room and Compose coverage for ordering, categories, status, and deletion.

## Architecture

- Room stores `type`, medication, dates, and synchronization metadata.
- `DewormingViewModel` accesses the repository and exposes state to the form and records screens.
- The category view requires selecting the latest applicable records for each category.
- Saving or deleting may trigger automatic tasks from spec `0005`.

## Dependencies and risks

- Depends on `0001`; optionally integrates with `0005`.
- `BOTH` may compete with category-specific records; the rule must choose the latest applicable event.
- Equal administration dates require a deterministic tie-breaker.

## Verification

1. Run focused deworming unit and Room tests after each slice.
2. Run `./gradlew test` and `./gradlew spotlessCheck`.
3. For UI changes, run the focused Android test, then `./gradlew assembleDebug` followed by `./gradlew installDebug`.
