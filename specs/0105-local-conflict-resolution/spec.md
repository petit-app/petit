---
spec: "0105"
title: Local conflict resolution
family: local-sharing
status: Implemented
owner: woliveiras
depends_on: ["0102"]
---

# Spec: Local conflict resolution

## Context and motivation

Two caregivers may edit or delete the same record before their devices
communicate again. Petit must converge without intervention and without
silently losing a change. The established rule is last-write-wins by
`updatedAt`, complemented by explicit soft-delete handling.

## Baseline before implementation

The existing one-shot import compares `updatedAt` and `MergeDataUseCase` writes
a `SyncLog` after that import returns. The entity import and log are not part of
one transaction. There is no dedicated `ConflictResolver`, history UI, or tests
for all soft-delete cases.
Equal timestamps with different payloads have no tie-breaking rule, so symmetry
cannot yet be guaranteed.

## Requirements

### Functional

- [x] Insert a remote entity whose UUID does not exist locally.
- [x] Prefer the version with the most recent `updatedAt` in the current merge.
- [x] Record operations in `SyncLog`.
- [x] Resolve a soft delete by comparing the deletion with the concurrent edit.
- [x] Define a stable tie-breaker for equal timestamps and different payloads.
- [x] Centralize the rule so one-off transfers and LAN produce the same result.
- [x] Ensure determinism, idempotency, and symmetry with tests.
- [x] Display sync history with sent, received, and resolved conflict counts.

### Non-functional

- [x] Integrity: apply each batch in a transaction.
- [x] Auditability: record the peer, type, time, and counts without clinical data in the log.
- [x] Performance: process by UUID with appropriate queries/batches.
- [x] Privacy: keep logs local and free of unnecessary sensitive content.

## Test strategy

Table-driven unit tests cover all local/remote combinations, including absence,
edits, deletion, equal timestamps, and retries. Integration tests cover Room,
transactions, `SyncLog`, and the same results through spec 0102. Spec 0104 must
then reuse exactly the same resolver.

## Acceptance criteria

- [x] Given two versions with different timestamps, when they are resolved in either order, then the version with the most recent `updatedAt` prevails.
- [x] Given a remote UUID that does not exist locally, when the batch is applied, then the record is inserted exactly once.
- [x] Given a soft delete and a concurrent edit, when they are compared, then the event that is actually newer prevails.
- [x] Given equal timestamps and different payloads, when they are resolved on both devices, then the documented tie-breaker produces the same result.
- [x] Given the same changeset applied repeatedly, when the merge finishes, then the state and counts do not change after the first application.
- [x] Given a completed sync, when the history is opened, then it shows the correct peer, time, type, and counts.
- [x] Given a failure during the batch, when the transaction is rolled back, then the entities and log remain consistent.

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
| Tie | Effective timestamp, then tombstone, then canonical domain payload | The later event wins; at equal time deletion prevents ambiguous resurrection, and a lexicographic, type-aware, length-prefixed encoding of fixed fields makes active/active ties symmetric. Transport-only `syncStatus` is excluded. |
| Audit | Local `SyncLog` with metadata and transferred-item counts | `entitiesSent` and `entitiesReceived` count bundle items successfully sent or received, while conflict and mutation outcomes remain separate. This supports diagnosis without duplicating health content. |

## Out of scope

- Real-time collaborative editing.
- UI for manually choosing each conflict.
- Restoring historical versions of a record.
- Transport or discovery between devices.
- Resolving conflicts delivered through cloud sync; see spec 0403, which must reuse the local resolver once completed.

## Resolver total order

1. Compare the effective event time: `max(updatedAt, deletedAt)`.
2. The greater effective time wins, so a later active edit can restore an older deletion.
3. At equal time, a tombstone wins over an active version.
4. If both have the same deletion state, compare a fixed-field payload encoded with
   explicit value types and length-prefixed text. This is unambiguous even when user
   text contains separators. Transport-only `syncStatus` is not part of domain
   conflict identity.

This order is deterministic, symmetric, and idempotent. It still inherits the
documented wall-clock limitation when devices generate different timestamps.
