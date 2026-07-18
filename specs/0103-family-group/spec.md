---
spec: "0103"
title: Local family group
family: local-sharing
status: Implemented
owner: woliveiras
depends_on: ["0101"]
---

# Spec: Local family group

## Context and motivation

After pairing devices, caregivers need to know who participates in sharing and
control access without a central account. The group is maintained locally, and
leaving it never deletes the pets' health history.

This spec consolidates the canonical `us-103-family-group` story; the
`us-103-household-group` variant contained only outdated navigation and a
wireframe, with no additional requirements.

## Baseline before implementation

The member list, local removal, leaving the group, and DataStore preferences
already exist, but group creation persists state before pairing authorization
completes. Removal and departure are local database/preference operations; they
do not revoke the old key on another device. There is still no device renaming
or propagation of membership changes during a later transfer.

## Requirements

### Functional

- [x] Display known members and identify the local device.
- [x] Remove a member from the local list after confirmation.
- [x] Leave the group by removing the local key while preserving pet data.
- [x] Manage the group key, ID, and local name in group preferences.
- [x] Allow the local device to be renamed.
- [x] Propagate renaming, removal, and departure to other members.
- [x] Display the last sync time or “never synced” for each member.
- [x] Ensure that a removed device loses authorization for future syncs.

### Non-functional

- [x] Consistency: repeated removal and departure operations must be idempotent.
- [x] Privacy: health data remains local when access is removed.
- [x] Accessibility: confirmations and destructive actions have appropriate labels and targets.
- [x] Internationalization: visible text remains in `strings.xml` for pt-BR, en, and es.

## Test strategy

Unit tests cover the ViewModel and the rename, remove, and leave use cases.
Integration tests cover Room, DataStore, and propagation between two devices.
Blocking a removed member must be tested in the transfer channel and, when
available, in the LAN channel.

## Acceptance criteria

- [x] Given an existing group, when the screen opens, then it lists members, marks the local device, and shows each member's last sync.
- [ ] Given the local member, when its name changes, then the new name is persisted and appears on the other device after syncing.
- [ ] Given a remote member, when its removal is confirmed, then it leaves the list and cannot start a new sync with the previous key.
- [x] Given a member that chooses to leave, when it confirms, then the key and local associations are removed, but pet data remains.
- [ ] Given an offline removal or departure, when the devices communicate again, then the membership change is propagated idempotently.

## Implemented propagation rule

Rename, remove, and leave operations produce a minimal non-clinical
`MembershipChange` keyed by a one-way SHA-256 group identifier and stable member UUID, so a
departure can be queued after the visible association is cleared. A local
`LEAVE` retains the group credential only in a Room outbox until a protected
membership-only session receives an ACK; that scope cannot carry clinical
entities. Peers keep
only the deterministic final change per member (timestamp, destructive-event
priority, then name), apply it idempotently, and forward it in one-shot and LAN
bundles only within the matching group. A removal tombstone is loaded into the
pairing authorization session so the old stable identity cannot reuse the
previous group access.

## Edge cases

- Group with no remote members or with duplicate names.
- Simultaneous removal on two devices.
- Removed device attempts a transfer using an old key.
- App closes between confirmation and persistence.
- Unknown last sync time.

## Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Authority | Local state propagated between peers | Keeps the product free of a central server. |
| Leaving the group | Preserve all pet data | Stopping sharing does not mean deleting health history. |
| Identity | Stable UUID per device | Avoids using the editable name as a key. |
| Removal | Revoke the sync reference and propagate a marker | Prevents a merely visual deletion from being reversed at the next contact. |

## Out of scope

- Initial pairing, defined in spec 0101.
- Full history transfer, defined in spec 0102.
- Detailed sync history UI, covered in spec 0105.
- Administrative roles, remote invitations, and cloud-based group recovery; see spec 0405.
