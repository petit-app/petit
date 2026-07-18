---
spec: "0101"
title: Device pairing
family: local-sharing
status: Implemented
owner: woliveiras
depends_on: []
---

# Spec: Device pairing

## Context and motivation

Caregivers need to authorize two nearby devices to share health data for the
same pets without an account, remote server, or internet connection. Petit uses
Nearby Connections for discovery and transport, then persists a shared family
group key after pairing.

## Current state

Nearby Connections, permissions, screens, repositories, and persistence are
already in place. The sender displays the first four characters of its group
key, but the receiver neither enters nor validates that code: discovery passes
a blank expected key and automatically requests the first endpoint it finds.
The sender also persists its group key and local member before authorization,
so cancellation or failure can leave an incomplete group. The complete pairing
flow has not been validated on two physical devices.

## Requirements

### Functional

- [x] Start and cancel advertising or discovery through the pairing flow.
- [x] Request the Bluetooth and Wi-Fi permissions required by the Android version.
- [x] Persist the family group key and local identity only after authorization succeeds.
- [x] Show the sender a four-digit pairing code.
- [x] Let the receiver enter the code and reject invalid codes.
- [x] Exchange the same group key between both devices after validation.
- [ ] Confirm pairing without internet access.
- [x] Unpair without deleting local data.

### Non-functional

- [x] Security: accept the connection only after both sides validate the same code.
- [x] Privacy: do not send data to a remote server during pairing.
- [x] Accessibility: provide labels and touch targets of at least 48 dp.
- [x] Internationalization: keep visible text in `strings.xml` for pt-BR, en, and es.
- [x] Compatibility: provide a clear fallback when Google Play Services is unavailable.

## Test strategy

Unit tests cover code generation/validation and state transitions. Integration
tests cover DataStore, permissions, and Nearby callbacks. Final acceptance
requires two physical Android devices, with the scenario repeated without an
internet connection. See the [protocol research](../../docs/local-sharing-protocols.md).

## Acceptance criteria

- [x] Given a ready sender, when pairing starts, then it displays a four-digit code and advertising becomes active.
- [ ] Given a nearby receiver, when it enters the correct code, then the devices connect and persist the same group key.
- [x] Given an incorrect code, when the receiver tries to connect, then the connection is rejected and another attempt is allowed.
- [x] Given a waiting flow, when the person cancels, then advertising and discovery stop and no incomplete group remains.
- [ ] Given active Bluetooth or Wi-Fi and no internet connection, when the complete flow runs, then pairing succeeds.
- [x] Given a paired device, when it unpairs, then it loses the synchronization reference and retains its local data.

## Edge cases

- Permission denied or revoked during discovery.
- Bluetooth and Wi-Fi unavailable.
- Endpoint disappears before validation.
- Code expires, collides, or receives repeated attempts.
- Process is recreated during advertising or discovery.

## Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Transport | Nearby Connections with `P2P_STAR` | Matches the current implementation and abstracts BLE, Bluetooth, and Wi-Fi Direct. |
| Initial authorization | Four-digit code | Enables simple in-person confirmation before exchanging the key. |
| Group identity | Key persisted in DataStore | Keeps the flow local and enables authentication of later transfers. |
| Wi-Fi Direct usage | Only one-shot transport managed by Nearby | Avoids keeping it active for continuous synchronization. |

## Out of scope

- Transferring the dataset after pairing; see spec 0102.
- Managing already paired members; see spec 0103.
- Continuous synchronization over the local network; see spec 0104.
- Login, Firebase, or any cloud service.
