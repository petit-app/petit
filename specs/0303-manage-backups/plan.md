# Plan: Manage Google Drive Backups

Spec: [spec.md](./spec.md)

## Status

This plan is **In Progress**. Steps 1–6 are implemented through provider-neutral
contracts and the Drive adapter; real `appDataFolder` verification in step 7 remains open.

## Dependencies

- Spec 0301 authorization, archive metadata, and Drive storage repository.
- Spec 0302 restore entry point for the Restore action.

## Implementation sequence

1. Define a minimal remote metadata model and recognized-file filter.
2. Implement complete paginated listing, compatibility mapping, sorting, and totals.
3. Add details and restore navigation.
4. Implement idempotent single deletion with confirmation.
5. Implement explicit multi-select and delete-all with bounded requests and partial-failure reporting.
6. Add disconnect/reconnect, empty, unavailable, and authorization-required states.
7. Verify permanent deletion semantics against a real appDataFolder.

## Safety rules

- Resolve exact Drive file IDs before deletion.
- Never delete using a broad name-only query.
- Treat Drive appDataFolder deletion as permanent, not trash.
- Do not delete remote files when the user disconnects Drive or Petit Cloud.
- Do not infer backup compatibility solely from filename.

## Planned verification

- Repository contract tests for pagination and permanent deletion.
- ViewModel tests for loading, partial failure, and retry.
- Instrumented list/details/selection tests.
- Physical-device provider validation.
- [Physical-device validation runbook](../../docs/test-runbooks/google-drive-physical-device-validation.md).
- `./gradlew spotlessCheck`
- `./gradlew test`
