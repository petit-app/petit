# Plan: Local network sync

Spec: [spec.md](./spec.md)

## Implementation status

Steps 1–7 and the two-process protected transport test are implemented and
verified locally. Step 8 remains open for two physical devices and real
Wi-Fi/background conditions. The implementation
depends on identity/key (0101), group membership (0103), and the single
deterministic resolver and transaction boundary from 0105.

## Implementation sequence

1. Define versioned `HELLO`, `HELLO_ACK`, `CHANGESET`, `ACK`, `ERROR`, and `CLOSE` messages.
2. Implement `NsdServiceManager` with register, discover, resolve, timeout, and filtering of its own service.
3. Implement a TCP server/client with authentication before the payload and size/time limits.
4. Create `LanSyncRepository` for changesets, ACK, and transactional integration with the resolver.
5. Integrate the lifecycle: start in `ON_START`, stop in `ON_STOP`, and clean up listeners/sockets.
6. Create unique periodic work with `NetworkType.CONNECTED`, backoff, and a minimum interval of 15 minutes.
7. Implement the on/off setting, manual action, and accessible global indicator.
8. Test the protected exchange in two processes, then validate full discovery and convergence on two physical devices on the same network.

## Protocol flow

1. The client sends `HELLO {version, deviceId, lastSyncTimestamp, nonce, scope, HMAC}`; the group key never crosses the socket.
2. The server validates version, HMAC, stable member, nonce replay, and scope before returning data.
3. `HELLO_ACK` provides the server identity, a second nonce, cursor, scope, and bilateral HMAC proof.
4. Both derive directional AES-256-GCM keys; all later messages carry strict sequence numbers.
5. Each side filters stable per-entity batch IDs through a per-group/per-peer durable ACK ledger, combines pending units into bounded `CHANGESET` batches, and advances its informational cursor only after the matching `ACK`. This does not lose a new write when a device clock moves backwards.
6. Room applies a new batch, its replay ledger, membership events, and `SyncLog` in one transaction; a lost ACK returns the durable result without reapplying.
7. `CLOSE` ends each outbound phase and then the socket. The lower stable UUID is the normal session initiator.

An offline `LEAVE` uses `MEMBERSHIP_ONLY`: it can carry only the caller's own
departure and is rejected if it contains clinical entities. Its restricted
credential is erased after the first protected ACK.

## Battery and lifecycle

| Context | Behavior |
| --- | --- |
| Foreground | Active NSD and on-demand TCP. |
| Background | Periodic WorkManager, limited by constraints. |
| Terminated process | No persistent service. |
| Peer unavailable | Discovery ends after a timeout and retries later with backoff. |

Wi-Fi Direct is prohibited for continuous syncing. Nearby remains reserved for
pairing and one-off transfers.

## Risks and mitigation

| Risk | Mitigation |
| --- | --- |
| Discovery is slow or blocked by the network | Timeout, backoff, explicit state, and manual action. |
| TCP exposes data on the LAN | Authenticate before the payload and use a protected channel. |
| Simultaneous sessions duplicate work | Select the direction by IDs and keep application idempotent. |
| Background power consumption | Unique periodic work, constraints, and batching. |
| Divergent clocks | Do not treat the timestamp as sufficient for tie-breaking; apply spec 0105. |

## Final verification

1. Run protocol tests with two local processes and injected failures.
2. Run `./gradlew spotlessCheck` and `./gradlew test`.
3. Run `./gradlew assembleDebug && ./gradlew installDebug`.
4. On two devices, validate foreground, background, Wi-Fi loss/recovery, and an invalid key.
5. Confirm that NSD and sockets are released when leaving the app and that Wi-Fi Direct is not kept active.
