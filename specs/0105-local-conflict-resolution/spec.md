---
spec: "0105"
title: Local conflict resolution
family: local-sharing
status: In Progress
owner: woliveiras
depends_on: ["0102"]
---

# Spec: Local conflict resolution

## Context and motivation

Two caregivers may edit or delete the same record before their devices
communicate again. Petit must converge without intervention and without
silently losing a change. The established rule is last-write-wins by
`updatedAt`, complemented by explicit soft-delete handling.

## Current state

The existing merge compares `updatedAt` and writes a `SyncLog`. There is no
dedicated `ConflictResolver`, history UI, or tests for all soft-delete cases.
Equal timestamps with different payloads have no tie-breaking rule, so symmetry
cannot yet be guaranteed.

## Requirements

### Functional

- [x] Insert a remote entity whose UUID does not exist locally.
- [x] Prefer the version with the most recent `updatedAt` in the current merge.
- [x] Record operations in `SyncLog`.
- [ ] Resolve a soft delete by comparing the deletion with the concurrent edit.
- [ ] Define a stable tie-breaker for equal timestamps and different payloads.
- [ ] Centralize the rule so one-off transfers and LAN produce the same result.
- [ ] Ensure determinism, idempotency, and symmetry with tests.
- [ ] Display sync history with sent, received, and resolved conflict counts.

### Non-functional

- [ ] Integrity: apply each batch in a transaction.
- [ ] Auditability: record the peer, type, time, and counts without clinical data in the log.
- [ ] Performance: process by UUID with appropriate queries/batches.
- [ ] Privacy: keep logs local and free of unnecessary sensitive content.

## Test strategy

Table-driven unit tests cover all local/remote combinations, including absence,
edits, deletion, equal timestamps, and retries. Integration tests cover Room,
transactions, `SyncLog`, and the same results through spec 0102. Spec 0104 must
then reuse exactly the same resolver.

## Acceptance criteria

- [ ] Given two versions with different timestamps, when they are resolved in either order, then the version with the most recent `updatedAt` prevails.
- [ ] Given a remote UUID that does not exist locally, when the batch is applied, then the record is inserted exactly once.
- [ ] Given a soft delete and a concurrent edit, when they are compared, then the event that is actually newer prevails.
- [ ] Given equal timestamps and different payloads, when they are resolved on both devices, then the documented tie-breaker produces the same result.
- [ ] Given the same changeset applied repeatedly, when the merge finishes, then the state and counts do not change after the first application.
- [ ] Given a completed sync, when the history is opened, then it shows the correct peer, time, type, and counts.
- [ ] Given a failure during the batch, when the transaction is rolled back, then the entities and log remain consistent.

## Edge cases

- Equal `updatedAt` with one side deleted.
- The local clock moves backward or diverges from the other device.
- A child entity arrives before its parent pet.
- The same soft delete is reapplied.
- A batch contains duplicate versions of the same UUID.
- Failure after the entities and before the log.

## Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Primary rule | Last-write-wins by `updatedAt` | Preserves the existing rule and is simple when timestamps differ. |
| Deletion | `deletedAt` participates as a concurrent event | A later edit can undo an earlier deletion. |
| Implementation | Single pure resolver | Prevents divergence between Nearby import and future LAN sync. |
| Tie | Decision pending before final implementation | Without an additional stable key, “keep local” breaks symmetry. |
| Audit | Local `SyncLog` with metadata and counts | Supports diagnosis without duplicating health content. |

## Out of scope

- Real-time collaborative editing.
- UI for manually choosing each conflict.
- Restoring historical versions of a record.
- Transport or discovery between devices.
