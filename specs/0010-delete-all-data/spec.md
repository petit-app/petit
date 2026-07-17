---
spec: "0010"
title: Delete all data
family: pet-care
status: Completed
owner: woliveiras
depends_on: ["0001", "0002", "0003", "0004", "0005"]
---

# Spec: Delete all data

## Context and motivation

The caregiver needs a deliberate way to remove locally stored pet-care records from Petit and stop their scheduled reminders.

## Current state

The confirmation flow cancels task workers, transactionally deletes pet-care,
family-member, and sync-log records, resets reminder and local-family
preferences, and preserves device-experience preferences. Failures remain
visible, prevent false success, and can be retried safely.

## Functional requirements

- Expose a destructive Delete all data flow from Settings.
- Explain that pets, weights, vaccinations, deworming treatments, and tasks will be removed.
- Require the localized confirmation word before enabling deletion.
- Prevent navigation and repeated deletion while the operation is running.
- Cancel all task notification work before deleting pet-care records.
- Delete tasks, deworming treatments, vaccinations, weights, and pets in a Room transaction.
- Delete family-group members and synchronization logs in the same Room transaction.
- Reset reminder and local-family preferences while preserving theme, language, and onboarding completion.
- Show success only after deletion finishes and provide an action back to Home.
- Surface a recoverable error when cancellation or deletion fails.

## Acceptance criteria

- Given the confirmation word does not match exactly, When the caregiver views the destructive action, Then it remains disabled.
- Given the confirmation word matches, When deletion starts, Then navigation and repeated submission are disabled until it finishes.
- Given scheduled task work and pet-care records exist, When deletion succeeds, Then task work is canceled and tasks, deworming treatments, vaccinations, weights, and pets are removed.
- Given deletion succeeds, Then a success state is displayed and the caregiver can return to an empty Home.
- Given deletion fails, Then the caregiver remains in the flow and sees a recoverable error without a false success state.
- Given deletion is retried after workers were already canceled, Then the operation remains idempotent and can complete safely.
- Given deletion succeeds, Then theme, language, and onboarding completion are retained while care and local-sharing preferences are reset.

## Test strategy

Every changed production behavior receives a unit test. Unit tests cover
confirmation, retained/reset preference policy, idempotence, and ViewModel
success/failure/retry state. Integration tests cover WorkManager cancellation,
all Room tables, transaction rollback, DataStore resets, navigation, and
failure handling. A focused E2E journey verifies deletion and safe recovery.

## Edge cases

- A database failure after worker cancellation can leave records present while their notification jobs are canceled.
- An empty database should still produce a successful, idempotent result.
- Local family data and preferences may exist even when there are no pets.

## Decisions

- “All data” means locally stored pet-care content, task history, local-sharing
  membership/sync logs, reminder settings, and family-group credentials.
- Theme, language, and onboarding completion are device-experience settings and are retained.
- Worker cancellation remains before the Room transaction. If Room deletion
  fails, the screen explains the failure and retry safely repeats cancellation
  before retrying the transaction.
- Exported files, OS backups, and remote copies remain out of scope.

## Out of scope

- Deleting remote data from a cloud account or another device.
- Revoking operating-system backups or copies previously exported by the caregiver.
