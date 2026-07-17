---
spec: "0103"
title: Local family group
family: local-sharing
status: In Progress
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

## Current state

The member list, local removal, leaving the group, and DataStore preferences
already exist. There is still no evidence of device renaming or of propagating
removal or departure to the other device during the next sync.

## Requirements

### Functional

- [x] Display known members and identify the local device.
- [x] Remove a member from the local list after confirmation.
- [x] Leave the group by removing the local key while preserving pet data.
- [x] Manage the group key, ID, and local name in group preferences.
- [ ] Allow the local device to be renamed.
- [ ] Propagate renaming, removal, and departure to other members.
- [ ] Display the last sync time or “never synced” for each member.
- [ ] Ensure that a removed device loses authorization for future syncs.

### Non-functional

- [ ] Consistency: repeated removal and departure operations must be idempotent.
- [ ] Privacy: health data remains local when access is removed.
- [ ] Accessibility: confirmations and destructive actions have appropriate labels and targets.
- [ ] Internationalization: visible text remains in `strings.xml` for pt-BR, en, and es.

## Test strategy

Unit tests cover the ViewModel and the rename, remove, and leave use cases.
Integration tests cover Room, DataStore, and propagation between two devices.
Blocking a removed member must be tested in the transfer channel and, when
available, in the LAN channel.

## Acceptance criteria

- [ ] Given an existing group, when the screen opens, then it lists members, marks the local device, and shows each member's last sync.
- [ ] Given the local member, when its name changes, then the new name is persisted and appears on the other device after syncing.
- [ ] Given a remote member, when its removal is confirmed, then it leaves the list and cannot start a new sync with the previous key.
- [ ] Given a member that chooses to leave, when it confirms, then the key and local associations are removed, but pet data remains.
- [ ] Given an offline removal or departure, when the devices communicate again, then the membership change is propagated idempotently.

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
- Administrative roles or cloud-based group recovery.
