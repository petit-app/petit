---
spec: "0104"
title: Local network sync
family: local-sharing
status: Implemented
owner: woliveiras
depends_on: ["0101", "0103", "0105"]
---

# Spec: Local network sync

## Context and motivation

Devices in the same group should exchange changes automatically when they are
on the same Wi-Fi network, without a remote server or a high-power persistent
connection. The planned approach uses NSD for discovery and TCP over
infrastructure Wi-Fi for incremental changes.

## Current state

The automatable implementation is available: NSD discovery, a bounded TCP
transport, bilateral HMAC authentication, AES-GCM protected messages,
incremental transactional changesets, durable ACK/replay state, lifecycle
cleanup, periodic WorkManager attempts, and localized settings/state UI.
Physical two-device validation remains open.

## Requirements

### Functional

- [x] Register and discover `_petit._tcp` services while the app is in the foreground.
- [x] Authenticate each session with the group key and device identity.
- [x] Exchange bidirectional changesets since the last confirmed timestamp.
- [x] Apply the rules from spec 0105 and record the sync result.
- [x] Sync accumulated changes after reconnecting to the local network.
- [x] Schedule periodic background attempts with WorkManager.
- [x] Display syncing, synced, peer unavailable, and error states.
- [x] Allow automatic syncing to be disabled and a manual attempt to be triggered.

### Non-functional

- [x] Battery: never use Wi-Fi Direct as a persistent connection.
- [x] Lifecycle: stop advertising and discovery when the app leaves the foreground.
- [x] Security: reject an invalid key before exchanging data and protect TCP data in transit.
- [x] Reliability: apply changesets in batches, with acknowledgment and idempotent retries.
- [x] Performance: stop discovery after a timeout when no peer is available and use backoff.
- [x] Accessibility and i18n: states do not rely only on icons/color and are localized.

## Test strategy

Unit tests cover the protocol, authentication, batching, and states. Integration
tests use two processes for NSD/TCP, Room, and WorkManager. Final acceptance
requires two devices on the same Wi-Fi network, including network loss and
recovery. See the [protocol research](../../docs/local-sharing-protocols.md).

## Acceptance criteria

- [ ] Given two members on the same Wi-Fi network, when the app enters the foreground, then both advertise/discover `_petit._tcp` and start an authenticated session.
- [ ] Given changes on both devices, when they sync, then each side sends only its changeset, and both converge.
- [x] Given an invalid group key, when the client sends `HELLO`, then the server responds with an error and closes the connection before any health data is sent.
- [ ] Given changes made offline, when Wi-Fi returns, then they sync automatically without duplication.
- [ ] Given the app in the background, when the constraints are met, then WorkManager attempts to sync within Android's permitted interval.
- [x] Given automatic syncing is disabled, when the app opens, then it neither advertises nor discovers services and still allows manual transfer.
- [x] Given any state change, when the UI is observed, then it communicates the status without relying only on color.

## Edge cases

- Two instances initiate the connection simultaneously.
- NSD returns the device's own service or multiple members.
- The network changes during the handshake or changeset.
- The ACK is lost after successful application.
- Device clocks diverge.
- The key is revoked during a session.

## Decisions

| Decision             | Choice                                                   | Rationale                                                                  |
| -------------------- | -------------------------------------------------------- | -------------------------------------------------------------------------- |
| Discovery            | Android NSD / DNS-SD `_petit._tcp`                       | Works on the local network without Google Play Services.                   |
| Continuous transport | TCP over infrastructure Wi-Fi                            | Uses the already active radio and avoids the cost of a Wi-Fi Direct group. |
| Background           | Unique periodic work, minimum 15 minutes                 | Respects Android restrictions and optimizations.                           |
| Lifecycle            | NSD in the foreground; limited attempt in the background | Reduces battery use and resources retained by the process.                 |
| Transfer unit        | Batched changeset since the last ACK                     | Avoids syncing every write and allows retries.                             |
| Security             | Authenticated handshake and protected channel            | The local network alone is not a trust boundary.                           |

## Out of scope

- Initial pairing and key exchange.
- Persistent Wi-Fi Direct.
- Internet or cloud sync.
- Guaranteed continuous execution when Android terminates the process.
