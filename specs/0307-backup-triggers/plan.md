# Plan: Backup Triggers

Spec: [spec.md](./spec.md)

## Status

This plan is **In Progress**. Steps 1–6 are implemented and covered through the
shared Drive adapter; physical process-death and authorization validation in step 7 remains open.

## Dependencies

- Spec 0305 shared automatic worker, revision tracking, and constraints.
- Spec 0306 persisted enable/network settings.

## Architecture

- Repositories emit a lightweight `RestorableDataChanged(revision)` signal only
  after a successful local commit.
- `BackupTriggerCoordinator` classifies the change and enqueues unique one-time
  work with `ExistingWorkPolicy.REPLACE` and a five-minute initial delay.
- Manual, periodic, and triggered backup use the same archive/upload use case
  and update one successful local-revision watermark.
- Successful coverage of a pending revision cancels redundant triggered work.

## Implementation sequence

1. Define restorable versus bookkeeping change classification.
2. Add a monotonic local revision at the transaction boundary.
3. Implement unique delayed work and debounce replacement tests.
4. Wire each restorable repository and asset mutation after commit.
5. Coalesce manual, periodic, and triggered completion by revision.
6. Add constraint, retry, disabled, and authorization-required behavior.
7. Verify process death and rapid edits on physical hardware.

## Planned verification

- Trigger-classification unit tests for every restorable data type.
- WorkManager debounce and process-recreation integration tests.
- Concurrent-write and no-self-trigger regression tests.
- Manual/periodic/triggered coalescing tests.
- Physical-device durability validation.
- [Physical-device validation runbook](../../docs/test-runbooks/google-drive-physical-device-validation.md).
- `./gradlew spotlessCheck`
- `./gradlew test`
