---
spec: "0102"
title: One-shot data transfer
family: local-sharing
status: In Progress
owner: woliveiras
depends_on: ["0101"]
---

# Spec: One-shot data transfer

## Context and motivation

After pairing, a caregiver needs to send the pets' complete history to another
device without a server or internet connection. The receiver chooses whether
to merge the received data or replace its local database.

## Current state

`NearbyTransferRepository`, `SendDataUseCase`, `MergeDataUseCase`, and the
transfer interface already exist. `REPLACE` mode overwrites matching entities
but does not remove local records missing from the bundle. Progress, failures,
and the complete flow have not yet been validated on two devices.

## Requirements

### Functional

- [x] Serialize shareable data into an `ExportBundle`.
- [x] Send and receive the bundle over the authorized Nearby connection.
- [x] Offer the receiver merge and replace options.
- [x] Merge entities by UUID and `updatedAt`.
- [ ] Make `REPLACE` remove missing local data before import.
- [ ] Display actual progress and a summary by entity type.
- [ ] Discard a partial payload when the transfer fails.
- [ ] Confirm the destructive mode before replacing data.

### Non-functional

- [ ] Integrity: apply the import atomically.
- [ ] Security: accept payloads only from the paired endpoint.
- [ ] Performance: choose BYTES or FILE based on the payload limit without truncating the bundle.
- [ ] Accessibility and i18n: announce progress and keep text in pt-BR, en, and es.

## Test strategy

Unit tests cover serialization, merge, replace, counters, and incomplete
payload handling. Integration tests cover Room, Nearby, and the flow between
two devices, including without an internet connection. See the
[protocol research](../../docs/local-sharing-protocols.md).

## Acceptance criteria

- [ ] Given a paired device with local data, when it sends the data, then the receiver gets a complete `ExportBundle` and sees progress and completion.
- [ ] Given a receiver with existing data, when it chooses merge, then UUIDs are matched and the version with the latest `updatedAt` prevails.
- [ ] Given a receiver with records missing from the bundle, when it confirms replacement, then the shareable database is cleared and reflects only the bundle.
- [ ] Given an interrupted connection, when only part of the payload arrives, then no changes are persisted and another attempt is offered.
- [ ] Given two devices without an internet connection, when they transfer through Nearby, then the flow succeeds.
- [ ] Given a completed import, when the summary is displayed, then the counters match the entities actually added, updated, and removed.

## Edge cases

- Empty or incompatible bundle, or one larger than the BYTES limit.
- Insufficient space or transactional failure on the receiver.
- Child entities whose pet does not exist in the bundle.
- Same bundle received more than once.
- Soft delete newer than the active copy.

## Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Format | Serialized `ExportBundle` | Reuses the existing local export/import boundary. |
| Merge | UUID + `updatedAt` | Keeps the result deterministic for versions with different timestamps. |
| Replacement | Transactional cleanup followed by import | Makes the behavior match the meaning presented to the user. |
| Transport | Nearby after spec 0101 | Works locally and reuses the authorized channel. |

## Out of scope

- Discovering or pairing devices.
- Automatic or incremental synchronization.
- Resolving ties between identical timestamps; see spec 0105.
- Cloud backup.
