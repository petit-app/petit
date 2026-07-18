# Tasks: Local family group

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **View and manage the local group** (test-type: integration)
  - blocked-by: spec 0101
  - summary: list members, remove them locally, leave, and persist preferences.
  - desired behavior: the user controls their associations without affecting pet data.
  - acceptance criteria: the list, local removal, departure, and DataStore exist in the current app.
  - verification: `./gradlew test`

- [x] **Rename the device by stable identity** (test-type: both)
  - blocked-by: local group view
  - summary: edit the name without changing the member UUID.
  - desired behavior: the new name persists after restart and is available for propagation.
  - acceptance criteria: the edited name appears locally; the identity and key do not change.
  - verification: `./gradlew test`

- [x] **Propagate membership changes** (test-type: both)
  - blocked-by: spec 0102; rename the device
  - summary: sync renaming, removal, and departure as idempotent events.
  - desired behavior: peers converge, and a revoked key cannot regain access.
  - acceptance criteria: events cross one-shot/LAN bundles idempotently and transport authorization rejects the old key/identity; physical observation remains in the final task.
  - verification: focused repository, pairing, import, and LAN changeset tests.

- [x] **Display the last sync and group states** (test-type: unit)
  - blocked-by: local group view
  - summary: show the known time, “never synced,” and an empty group.
  - desired behavior: the user understands how current each association is.
  - acceptance criteria: all states have accessible, localized content.
  - verification: `./gradlew test`

- [ ] **Validate removal and departure on two devices** (test-type: integration)
  - blocked-by: membership propagation; group states
  - summary: perform renaming, removal, departure, and an attempt with the old key.
  - desired behavior: both devices converge without deleting pet data.
  - acceptance criteria: the association is revoked on both sides, and the local history remains.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
