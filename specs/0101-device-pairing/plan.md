# Plan: Device pairing

Spec: [spec.md](./spec.md)

## Starting point

The project already includes Nearby integration, permissions, presentation,
repositories, and group persistence. The implementation must preserve these
parts and complete the explicit code protocol and inter-device validation.

## Implementation sequence

1. Map the current `PairingState` transitions and separate discovery from authorization.
2. Implement generation, expiration, and validation of the four-digit code.
3. Connect code entry on the receiver to the Nearby connection request.
4. Ensure atomic key exchange and persistence only after authorization.
5. Stop advertising, discovery, and connections on cancellation, error, and success.
6. Validate permissions by API level and fallback messages.
7. Cover logic and integration; finish with a manual test on two devices.

## Dependencies and integration

- Nearby Connections and Google Play Services for pairing mode.
- DataStore for the group key and device identity.
- Spec 0102 consumes the resulting authorized connection.
- Technical reference: [local sharing protocols](../../docs/local-sharing-protocols.md).

## Risks and mitigation

| Risk | Mitigation |
| --- | --- |
| Wrong endpoint accepted through automatic discovery | Require code validation before exchanging the key. |
| Residual state after interruption | Centralize idempotent cleanup for all terminal states. |
| Permission differences between Android versions | Test matrices below and above APIs 31 and 33. |
| Device without Google Play Services | Detect unavailability and explain the limitation without losing data. |

## Final verification

Automated implementation is complete. The JVM suite covers the authorization
protocol, code expiry, retries, permission matrices, state transitions, and
cleanup. Room/DataStore instrumentation covers idempotent unpairing and local
data preservation. The two-device matrix below remains pending.

1. Run `./gradlew spotlessCheck` and `./gradlew test`.
2. Run `./gradlew assembleDebug && ./gradlew installDebug` on the first device.
3. Install the same APK on the second device.
4. Validate correct code, incorrect code, cancellation, and no internet connection.
5. Confirm that both persist the same key and that no data is deleted on exit.
