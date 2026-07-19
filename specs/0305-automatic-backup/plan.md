# Plan: Automatic Google Drive Backup

Spec: [spec.md](./spec.md)

## Status

This plan is **In Progress**. The provider-independent scheduler, worker, and
attempt history are implemented and use the Google Drive adapter. Physical-device
background validation remains open.

## Dependencies

- Spec 0301 archive, authorization, idempotency, and upload implementation.
- Existing Hilt/WorkManager application integration.

## Architecture

- `BackupScheduler` owns one unique periodic work name and cancel/update behavior.
- `AutomaticBackupWorker` checks settings and obtains authorization silently.
- The worker invokes the same `CreateBackupUseCase` as manual backup with an
  automatic trigger value.
- `BackupAttemptRepository` records scheduled, running, succeeded, failed,
  retrying, and authorization-required states without clinical data.

## Implementation sequence

1. Characterize existing WorkManager/Hilt patterns and define unique work names.
2. Implement schedule, update, cancel, and constraint mapping tests.
3. Implement the worker using the shared archive/upload use case.
4. Map transient, permanent, quota, cancellation, and authorization-required results.
5. Add exponential backoff and idempotent retry tests.
6. Expose observable status and foreground reconnect action.
7. Verify process death, network changes, and Doze on physical hardware.

## Progress

- Steps 1-5 are implemented and verified with WorkManager, deterministic provider
  tests, stable retry operation IDs, and silent Google authorization refresh.
- Step 6 records authorization-required without launching UI; foreground reconnect
  is completed by spec 0306.
- Step 7 remains pending for physical-device validation.

## Planned verification

- WorkManager test-driver coverage.
- Worker integration tests with fake authorization and Drive gateways.
- No-duplicate schedule and upload tests.
- Instrumented settings tests.
- Physical-device background runbook created during implementation.
- [Physical-device validation runbook](../../docs/test-runbooks/google-drive-physical-device-validation.md).
- `./gradlew spotlessCheck`
- `./gradlew test`

## Official references

- [Define WorkManager work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Manage unique work](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work)
- [Authorize access to Google user data on Android](https://developer.android.com/identity/authorization)
