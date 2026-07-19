# Backup provider boundary

Petit backup behavior is split into a provider-independent archive/application layer and a replaceable remote-storage adapter.

## Implemented boundary

- `BackupArchiveCodec` creates and validates the versioned portable ZIP contract.
- `BackupArchivePreparer` captures the Room snapshot, preferences, reminder settings, and app-owned pet assets.
- `BackupAuthorizationGateway` exposes authorization state and a foreground authorization operation without provider SDK types.
- `BackupStorageGateway` exposes exact-ID upload, download, paginated list, metadata lookup, and deletion operations.
- `CreateBackupAction` is shared by manual UI and background workers and requires a stable backup ID for retry idempotency.
- ViewModels and workers consume only these provider-independent contracts.

The deterministic provider is located only in `app/src/test`. It exercises success, pagination, authorization loss, quota, interruption, retryable and permanent failures, lost upload responses, and idempotent retries. It cannot be injected into a production component.

## Google Drive adapter

The debug application binds these contracts to Google Identity Services and a
direct Google Drive API v3 adapter. Foreground authorization uses
`AuthorizationClient`, requests only `drive.appdata`, and keeps access tokens
in memory for the duration of requests. The storage adapter restricts listing
and creation to `appDataFolder`, recognizes Petit files through `appProperties`,
uses resumable uploads, and resolves exact remote IDs for download and deletion.

The adapter translates Google and HTTP failures into stable domain errors, so
Google SDK and REST types do not enter use cases, ViewModels, workers, or
Compose UI. Retry idempotency is based on the stable Petit backup operation ID;
a completed remote object is reused after a lost response.

## Operational validation boundary

Automated tests do not prove that an Android OAuth client, test-user allowlist,
consent screen, device certificate, or real Drive account is configured. Follow
[Google Drive physical-device validation](./test-runbooks/google-drive-physical-device-validation.md)
before marking provider-specific tasks complete. A resumable session URI is not
persisted across process death; a retry safely queries the operation ID and
starts a new session when no completed file exists.

Disconnect revokes local authorization and cancels automatic work without
deleting remote backups. Google documents that application access revocation
can revoke all scopes granted to the application, although Petit requests only
`drive.appdata`.
