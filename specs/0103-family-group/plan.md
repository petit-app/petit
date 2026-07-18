# Plan: Local family group

Spec: [spec.md](./spec.md)

## Implementation status

Steps 1–5 are implemented and verified locally. Step 6 remains open only for
physical two-device propagation and revocation validation.

## Starting point

The original app listed and removed members only locally. It now keeps the
stable UUID separate from the editable name, queues deterministic membership
changes, forwards them through the sharing bundle, and rejects revoked stable
identities during authorization.

## Implementation sequence

1. Cover the existing list, removal, and departure behavior with regression tests.
2. Implement renaming by stable local ID and persist the name in DataStore/Room.
3. Model renaming, removal, and departure as syncable, idempotent changes.
4. Reject connections from removed identities or identities with a revoked key.
5. Display the last known sync and empty states on the group screen.
6. Validate propagation and data preservation on two devices.

## Dependencies and integration

- Depends on the identity and key established by spec 0101.
- Manual propagation can use spec 0102.
- Automatic propagation and the restricted offline-departure outbox use spec 0104.
- Idempotent resolution follows spec 0105.

## Risks and mitigation

| Risk | Mitigation |
| --- | --- |
| Removed device reappears | Propagate a removal marker and validate authorization before syncing. |
| Name used as identity | Keep the immutable UUID separate from the editable name. |
| Leaving deletes health data | Isolate membership cleanup from pet tables and test this boundary. |
| Offline changes diverge | Apply membership events deterministically and idempotently. |

## Final verification

1. Run `./gradlew spotlessCheck` and `./gradlew test`.
2. Run `./gradlew assembleDebug && ./gradlew installDebug`.
3. On two devices, validate renaming, removing, leaving, and reconnecting with the peer.
4. Confirm that the old key is rejected and that pet data remains.
