# Runbook: Google Drive physical-device validation

## Purpose

Validate Petit's Google Drive backup, restore, management, automatic backup,
settings, and change-trigger behavior against a real user's `appDataFolder`.
Deterministic fake-provider tests remain the primary regression suite; this
runbook supplies provider and device evidence that fakes cannot provide.

This runbook covers the test environment only. It does not authorize production
Google Cloud, OAuth, signing, Terraform, release, or publication changes.

## Required configuration gate

Do not start real-device authorization until every value below is confirmed in
Google Auth Platform and the installed APK matches the recorded identity.

```text
Google Cloud project ID: petit-test-env
Google Cloud project number: 479651435689
OAuth publication status: Testing
Application ID: com.woliveiras.petit.debug
Debug SHA-1: 84:95:40:39:FB:11:05:A1:21:E7:93:FE:7B:79:D3:9C:BF:F5:FF:2D
Requested scope: https://www.googleapis.com/auth/drive.appdata
Test-user account: <record outside the repository>
Expected Android OAuth client: package and SHA-1 above
Drive API v3 enabled: yes
```

The operator must confirm that Branding, Audience, the test-user allowlist,
Data Access, and the Android OAuth client are complete by following
[Google Drive Google Cloud Console setup](./google-drive-cloud-console-setup.md).
Account names, screenshots containing personal information, and OAuth client
IDs should be stored with the test evidence outside this repository.

Stop if the consent screen requests `drive`, `drive.file`, or any Drive scope
other than `drive.appdata`. Do not work around `DEVELOPER_ERROR`, a missing test
user, or an incorrect package/SHA-1 by creating credentials in another project.

## Device and data preparation

1. Use a physical Android device with current Google Play services.
2. Install the debug APK signed by the certificate recorded above.
3. Use a dedicated test Google account; do not use a maintainer's personal
   Drive or production data.
4. Create non-sensitive fixtures that exercise every restorable entity type,
   reminder state, app preferences, and an app-owned pet image.
5. Record app version, Android version, device model, network type, battery
   state, and the test start time without recording clinical values.
6. Keep a second physical device available for the cross-device restore.

## Authorization and manual backup

1. Open **Settings > Backup settings** and verify Drive is disconnected.
2. Choose **Connect Google Drive**.
3. Confirm the consent UI identifies Petit and requests only app-data access.
4. Cancel once. Verify no backup, Petit account, purchase, or remote deletion
   occurs and retry remains available.
5. Retry and authorize the dedicated test account.
6. Choose **Back up now** and observe monotonic progress through completion.
7. Record only the backup ID, timestamps, trigger, archive size, app/schema
   versions, and non-clinical counts shown by Petit.
8. Repeat after disabling network access during transfer. Verify the failed or
   retryable state is accurate, local staging is cleaned, and retry publishes
   no duplicate completed backup for the same operation.

Pass criteria:

- authorization succeeds without Petit authentication or payment;
- only `drive.appdata` is requested;
- one completed archive is created in the authorizing user's
  `appDataFolder`;
- cancellation and interruption leave local data unchanged and expose no
  usable partial backup;
- retries do not create duplicate completed backups for one backup ID.

## List, inspect, and delete

1. Create at least six backups using manual and automatic/data-change triggers.
2. Open **Saved backups** and verify pagination returns all recognized Petit
   backups in newest-first order.
3. Verify details show compatibility, versions, trigger, time, size, and
   non-clinical counts without pet names or clinical values.
4. Delete one selected backup and confirm it no longer appears.
5. Repeat deletion for the same exact remote ID and verify the operation is
   treated as already deleted.
6. Exercise multi-select deletion and record any partial failure accurately.
7. Create two disposable backups, choose **Delete all backups**, confirm, and
   verify only recognized Petit files resolved to exact IDs are deleted.

Deletion in `appDataFolder` is permanent because Drive does not support moving
these files to trash. Never use a broad name-only deletion query.

## Restore and rollback

1. Create a backup on device A and record non-sensitive fixture counts.
2. On device A, change local data and execute **MERGE**. Verify deterministic
   conflict results, winning assets, preference selection, and reminder
   rescheduling.
3. Change local data again and execute **REPLACE** after the destructive
   confirmation. Verify the backup is the exact restorable state while Google
   authorization, device identity, pairing keys, and family authorization
   remain device-local.
4. During a separate restore, disable connectivity while downloading. Verify
   no partial database or asset state becomes visible and retry is safe.
5. Attempt restore with incompatible and intentionally corrupted test fixtures
   produced by deterministic test tooling, not by modifying real user data.
   Verify validation finishes before local mutation.
6. Install Petit on device B, authorize the same dedicated test account, and
   restore the device-A backup. Compare portable entity, preference, reminder,
   and asset counts while confirming device-bound identities were not cloned.

Pass criteria require the complete old state or complete restored state after
every interruption. Any mixed database/assets state is a failure.

## Disconnect, revocation, and reconnect

1. With remote backups present, disconnect Drive in Petit and confirm.
2. Verify periodic and change-triggered work is canceled and no remote backup
   is deleted.
3. Reconnect the same test account and verify its backups are listed again.
4. Revoke Petit access from the Google account outside the app.
5. Run a foreground Drive action and verify Petit requires authorization.
6. Allow background work to become eligible and verify it records
   **Authorization required** without opening UI.
7. Reauthorize in the foreground and verify normal access resumes.

## Automatic backup and triggers

1. Enable automatic backup and verify one unique periodic request is active.
2. Toggle between connected and unmetered-only networks and verify eligibility
   follows the selected constraint without duplicate work.
3. Close Petit, allow eligible work to run, and verify one complete backup and
   one non-sensitive attempt record are produced.
4. Force-stop or terminate the app process, reopen it, and verify the schedule
   and previously granted authorization recover without a stored token.
5. Make rapid restorable edits and verify one five-minute change-triggered
   request is debounced from the last edit.
6. Change backup history or provider state and verify no change-triggered loop
   is scheduled.
7. Let a manual or periodic backup cover the same or newer local revision and
   verify redundant triggered work is canceled.
8. Exercise transient failure and verify exponential backoff reuses one
   operation ID; after success, the next periodic interval creates a new backup.
9. Exercise Doze and unmet constraints. Verify the UI describes scheduling as
   inexact and never promises a clock time.

## Security inspection

After testing, inspect device logs and local app storage with approved debug
tools. Fail the run if any access token, refresh token, backup content, pet
name, clinical value, or archive bytes appear in logs, DataStore, Room,
analytics, notifications, or persistent operation metadata.

Confirm no `google-services.json`, client secret, API key, service account, or
credential export was added to the repository or APK workflow.

## Evidence record

```text
Date/time:
Tester:
Project ID: petit-test-env
Package: com.woliveiras.petit.debug
Debug SHA-1: 84:95:40:39:FB:11:05:A1:21:E7:93:FE:7B:79:D3:9C:BF:F5:FF:2D
Test-user evidence location:
Android OAuth client confirmed: Pass / Fail / Blocked
Scope shown by consent: drive.appdata / Other / Blocked
Authorization cancellation: Pass / Fail / Blocked
Authorization success: Pass / Fail / Blocked
Manual upload and idempotent retry: Pass / Fail / Blocked
List/details/pagination: Pass / Fail / Blocked
Exact and bulk deletion: Pass / Fail / Blocked
MERGE restore: Pass / Fail / Blocked
REPLACE restore and rollback: Pass / Fail / Blocked
Second-device restore: Pass / Fail / Blocked
Disconnect/reconnect: Pass / Fail / Blocked
External revocation: Pass / Fail / Blocked
Periodic background execution: Pass / Fail / Blocked
Change-trigger debounce/coalescing: Pass / Fail / Blocked
Network, retry, process-death, and Doze behavior: Pass / Fail / Blocked
No sensitive logs or persisted tokens: Pass / Fail / Blocked
Overall result: Pass / Fail / Blocked
Evidence location:
Notes:
```

Do not mark any provider or physical-device task complete when its evidence is
`Blocked` or has not been executed.
