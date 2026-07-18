# Plan: Backup Settings

Spec: [spec.md](./spec.md)

## Status

This plan is **In Progress**. Provider-neutral implementation and automated
verification are complete; real authorization and disconnect verification remain open.

## Dependencies

- Spec 0305 scheduler, worker, attempt history, and reauthorization state.
- Spec 0301 manual backup and Drive disconnect contracts.

## Implementation sequence

1. Extend DataStore with defaults for automatic, network, and notification settings.
2. Expose independent Drive connection and backup status in a dedicated ViewModel.
3. Implement atomic enable, disable, and network-constraint schedule updates.
4. Add manual backup without changing the periodic schedule.
5. Add success notification policy and actionable authorization/failure notifications.
6. Add a three-attempt history preview and inexact schedule presentation.
7. Add a dedicated history route backed by bounded five-attempt pages, stable
   newest-first ordering, and an accessible load-more action.
8. Add explicit disconnect confirmation that preserves all data.
9. Verify localization and accessibility in every supported locale.

## Planned verification

- DataStore repository integration tests.
- WorkManager unique update/cancel tests.
- ViewModel state and event tests.
- Bounded history repository and pagination tests.
- Compose UI and accessibility tests for the three-item preview, dedicated
  history screen, five-item pages, loading state, and end of history.
- Physical authorization/disconnect verification.
- `./gradlew spotlessCheck`
- `./gradlew test`
