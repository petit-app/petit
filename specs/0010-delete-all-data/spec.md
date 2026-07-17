---
spec: "0010"
title: Delete all data
family: pet-care
status: In Progress
owner: woliveiras
depends_on: ["0001", "0002", "0003", "0004", "0005"]
---

# Spec: Delete all data

## Context and motivation

The caregiver needs a deliberate way to remove locally stored pet-care records from Petit and stop their scheduled reminders.

## Current state

The confirmation flow cancels task workers and transactionally deletes the
visible pet-care records. It does not yet purge local sharing data or
preferences, and deletion errors are not displayed by the screen.

## Functional requirements

- Expose a destructive Delete all data flow from Settings.
- Explain that pets, weights, vaccinations, deworming treatments, and tasks will be removed.
- Require the localized confirmation word before enabling deletion.
- Prevent navigation and repeated deletion while the operation is running.
- Cancel all task notification work before deleting pet-care records.
- Delete tasks, deworming treatments, vaccinations, weights, and pets in a Room transaction.
- Show success only after deletion finishes and provide an action back to Home.
- Surface a recoverable error when cancellation or deletion fails.

## Acceptance criteria

- Given the confirmation word does not match exactly, When the caregiver views the destructive action, Then it remains disabled.
- Given the confirmation word matches, When deletion starts, Then navigation and repeated submission are disabled until it finishes.
- Given scheduled task work and pet-care records exist, When deletion succeeds, Then task work is canceled and tasks, deworming treatments, vaccinations, weights, and pets are removed.
- Given deletion succeeds, Then a success state is displayed and the caregiver can return to an empty Home.
- Given deletion fails, Then the caregiver remains in the flow and sees a recoverable error without a false success state.

## Test strategy

Unit tests cover confirmation and ViewModel success/failure state; integration tests cover WorkManager cancellation, Room transaction behavior, deletion order, retained out-of-scope data, navigation, and failure handling.

## Edge cases

- A database failure after worker cancellation can leave records present while their notification jobs are canceled.
- An empty database should still produce a successful, idempotent result.
- Local family data and preferences may exist even when there are no pets.

## Known limitations

- The current use case does not delete family-group members or synchronization logs from Room.
- User preferences, onboarding completion, and reminder preferences remain in DataStore.
- Worker cancellation occurs before the Room transaction and cannot be rolled back if database deletion fails.
- The dedicated screen emits an error event, but does not currently collect and display it.

## Out of scope

- Deleting remote data from a cloud account or another device.
- Revoking operating-system backups or copies previously exported by the caregiver.
