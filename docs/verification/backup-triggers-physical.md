# Backup Trigger Physical-Device Runbook

This runbook validates behavior that JVM tests and an emulator cannot prove. It
requires a physical Android device and a real storage adapter. Do not use the
provider-unavailable production boundary or the deterministic test provider as
evidence for cloud behavior.

## Preconditions

- Install a build configured with the real provider adapter and a test account.
- Confirm automatic backup is explicitly enabled.
- Record the configured network requirement and the current restorable revision.
- Keep provider files visible through an independent provider client.

## Rapid edits and process death

1. Change a pet, weight, vaccination, deworming record, task, reminder setting,
   and pet photo within five minutes.
2. Confirm WorkManager contains one unique `petit_change_triggered_backup`
   request whose target is the newest revision.
3. Force-stop the app without clearing app data.
4. Reopen the app before the delay expires and confirm the existing request was
   preserved rather than duplicated.
5. Let constraints become eligible and confirm exactly one complete archive is
   produced for the settled changes.

## Constraints, retry, and newer revisions

1. Remove the required network before the request becomes eligible and confirm
   no upload starts.
2. Restore the network, interrupt the provider transfer with a retryable error,
   then make another restorable edit before retry.
3. Confirm the retry keeps its stable attempt ID and does not mark the newer
   revision complete.
4. Confirm a later unique request covers the newer revision.

## Manual and periodic interaction

1. Leave one change-triggered request pending.
2. Complete a manual backup that captured the same or a newer revision.
3. Confirm the pending triggered request is canceled and no duplicate archive
   is created for that revision.
4. Repeat with the periodic worker.

## Authorization and loop prevention

1. Revoke provider authorization while triggered work is pending.
2. Confirm no interactive authorization UI opens in the background and attempt
   history reports authorization required.
3. Reauthorize in the foreground and confirm pending restorable changes remain
   eligible.
4. Observe the app for at least ten minutes after a successful backup and
   confirm attempt history, progress, provider state, and WorkManager updates do
   not enqueue another backup by themselves.

Record device model, Android version, app commit, provider adapter commit,
timestamps, WorkManager state, and resulting provider file IDs with the test
evidence. Never record tokens, pet names, clinical values, or archive contents.
