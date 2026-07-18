# Tasks: Device pairing

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Provide Nearby modes and display the sender code** (test-type: integration)
  - blocked-by: none
  - summary: expose advertising/discovery, request permissions, and show the sender the first four characters of the generated group key.
  - desired behavior: the sender can start a local pairing attempt and communicate the displayed code in person.
  - acceptance criteria: the current flow starts the Nearby modes and displays a non-empty four-digit sender code without a cloud dependency.
  - verification: `./gradlew test`

- [x] **Authorize pairing with a four-digit code** (test-type: both)
  - blocked-by: discovery and group persistence
  - summary: generate, display, receive, validate, and expire the code.
  - desired behavior: only the receiver with the correct code completes pairing.
  - acceptance criteria: the correct code connects; an incorrect code is rejected; another attempt is allowed.
  - verification: `./gradlew test`

- [x] **Persist pairing atomically and clean up failures** (test-type: both)
  - blocked-by: code authorization
  - summary: persist the key and members only after authorization, then clean up advertising, discovery, the connection, and incomplete state.
  - desired behavior: interruptions never leave a partially persisted key or member.
  - acceptance criteria: cancelling or losing the endpoint returns to the initial state without residual data.
  - verification: `./gradlew test`

- [ ] **Validate end-to-end pairing on two devices** (test-type: integration)
  - blocked-by: code authorization; atomic cancellation and failures
  - summary: run the acceptance matrix on physical hardware.
  - desired behavior: pairing works with and without an internet connection and respects permissions.
  - acceptance criteria: two devices persist the same key; an invalid code and cancellation do not pair them.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
