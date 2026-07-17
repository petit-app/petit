# Tasks: Device-to-Device Transfer

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: Send data to another device** (test-type: both)
  - blocked-by: spec 0101
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 1: Send data to another device” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I have local data (pets, weights, etc.) AND another device with Petit is nearby WHEN I open Settings > "Share data" AND I tap "Send data" THEN I see a 4-digit code to share AND I wait for the receiver to connect WHEN the receiver enters the code THEN the data is sent via Nearby Connections AND I see "Data sent successfully"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Receive data from another device** (test-type: both)
  - blocked-by: spec 0101; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 2: Receive data from another device” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I am in the app WHEN I open Settings > "Receive data" AND I tap "Receive from another phone" THEN I see a field for entering a code WHEN I enter the sender's 4-digit code THEN a connection is established AND I see the transfer progress WHEN the transfer completes THEN I see an option to "Replace" or "Merge" data
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: Transfer without internet** (test-type: both)
  - blocked-by: spec 0101; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 3: Transfer without internet” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN both devices are offline BUT they are on the same Wi-Fi network OR have Bluetooth enabled WHEN I start the transfer THEN it works normally (Nearby Connections uses Wi-Fi Direct or Bluetooth)
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Merge received data** (test-type: both)
  - blocked-by: spec 0101; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 4: Merge received data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I received data from another device AND I have local data WHEN I choose "Merge" THEN the data is combined AND duplicates are resolved by ID (unique UUIDs) AND I see a summary: "2 pets added, 10 weight records merged"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Replace local data** (test-type: both)
  - blocked-by: spec 0101; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 5: Replace local data” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN I received data from another device WHEN I choose "Replace" THEN I see the confirmation "Your local data will be deleted. Continue?" WHEN I confirm THEN all local data is deleted AND the received data is imported AND I see "Data restored successfully"
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 6: Cancel transfer** (test-type: both)
  - blocked-by: spec 0101; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 6: Cancel transfer” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN a transfer is in progress WHEN I tap "Cancel" THEN the transfer is interrupted AND partial data is discarded AND both devices return to their initial state
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 7: Connection error** (test-type: both)
  - blocked-by: spec 0101; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including the domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 7: Connection error” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN the devices are too far apart OR Bluetooth/Wi-Fi is disabled WHEN I try to start a transfer THEN I see the message "Could not connect. Move the devices closer together and enable Wi-Fi or Bluetooth." ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
