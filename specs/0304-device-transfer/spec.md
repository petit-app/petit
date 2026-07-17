---
spec: "0304"
title: "Device-to-Device Transfer"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0101"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Device-to-Device Transfer

## Context and motivation

> As an app user,
> I want to transfer my data to another nearby phone,
> So that I can share data with another device without using the cloud.

This is a historical hypothesis that has not yet been implemented. Product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: Send data to another device

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have local data (pets, weights, etc.)
AND another device with Petit is nearby
WHEN I open Settings > "Share data"
AND I tap "Send data"
THEN I see a 4-digit code to share
AND I wait for the receiver to connect
WHEN the receiver enters the code
THEN the data is sent via Nearby Connections
AND I see "Data sent successfully"
```

### Scenario 2: Receive data from another device

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am in the app
WHEN I open Settings > "Receive data"
AND I tap "Receive from another phone"
THEN I see a field for entering a code
WHEN I enter the sender's 4-digit code
THEN a connection is established
AND I see the transfer progress
WHEN the transfer completes
THEN I see an option to "Replace" or "Merge" data
```

### Scenario 3: Transfer without internet

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN both devices are offline
BUT they are on the same Wi-Fi network OR have Bluetooth enabled
WHEN I start the transfer
THEN it works normally
(Nearby Connections uses Wi-Fi Direct or Bluetooth)
```

### Scenario 4: Merge received data

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I received data from another device
AND I have local data
WHEN I choose "Merge"
THEN the data is combined
AND duplicates are resolved by ID (unique UUIDs)
AND I see a summary: "2 pets added, 10 weight records merged"
```

### Scenario 5: Replace local data

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I received data from another device
WHEN I choose "Replace"
THEN I see the confirmation "Your local data will be deleted. Continue?"
WHEN I confirm
THEN all local data is deleted
AND the received data is imported
AND I see "Data restored successfully"
```

### Scenario 6: Cancel transfer

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN a transfer is in progress
WHEN I tap "Cancel"
THEN the transfer is interrupted
AND partial data is discarded
AND both devices return to their initial state
```

### Scenario 7: Connection error

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN the devices are too far apart
OR Bluetooth/Wi-Fi is disabled
WHEN I try to start a transfer
THEN I see the message "Could not connect. Move the devices closer together and enable Wi-Fi or Bluetooth."
```

---

## Non-functional requirements

- [ ] Preserve Petit's local operation when authentication, the network, or an external service is unavailable.
- [ ] Protect personal and pet health data during storage, transfer, and deletion.
- [ ] Provide accessible and understandable loading, success, empty, and error states.
- [ ] Prevent silent data loss or duplication during interrupted operations.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Eligibility, validation, state, conflict, and data transformation rules. |
| Integration | Flows that cross the interface, repositories, local database, and external providers. |
| Both | Each vertical task uses unit tests for rules and integration tests for I/O boundaries. |

## Acceptance criteria

The scenarios in **Functional requirements** are this spec's testable criteria and must have traceable coverage before the status advances to `Implemented`.

## Preserved product notes

### UI/UX

### Screen: Share Data (Sender)

```
┌────────────────────────────────┐
│ ← Share Data                   │
├────────────────────────────────┤
│                                │
│        📱 ➡️ 📱                │
│                                │
│  Share your data with another  │
│  nearby phone                  │
│                                │
│ ┌────────────────────────────┐ │
│ │    SEND DATA               │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Works without internet,     │
│ using Wi-Fi Direct or          │
│ Bluetooth.                     │
│                                │
└────────────────────────────────┘
```

### Screen: Waiting for Connection (Sender)

```
┌────────────────────────────────┐
│ ← Waiting for connection...    │
├────────────────────────────────┤
│                                │
│         🔒                     │
│                                │
│    Security code:              │
│                                │
│        ┌──────────┐            │
│        │   4729   │            │
│        └──────────┘            │
│                                │
│  Ask the other phone to enter  │
│  this code.                    │
│                                │
│ ┌────────────────────────────┐ │
│ │       CANCEL               │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Screen: Receive Data (Receiver)

```
┌────────────────────────────────┐
│ ← Receive Data                 │
├────────────────────────────────┤
│                                │
│        📱 ⬅️ 📱                │
│                                │
│  Enter the code shown on the   │
│  other phone:                  │
│                                │
│  ┌────┬────┬────┬────┐         │
│  │  4 │  7 │  2 │  9 │         │
│  └────┴────┴────┴────┘         │
│                                │
│ ┌────────────────────────────┐ │
│ │      CONNECT               │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Make sure Wi-Fi or          │
│ Bluetooth is enabled.          │
│                                │
└────────────────────────────────┘
```

### Screen: Transferring

```
┌────────────────────────────────┐
│ Transferring...                │
├────────────────────────────────┤
│                                │
│         ████████░░             │
│            80%                 │
│                                │
│  Sending data...               │
│  2 pets • 25 records          │
│                                │
│  Do not close the app          │
│                                │
└────────────────────────────────┘
```

### Dialog: Choose Action (Receiver)

```
┌────────────────────────────────┐
│                                │
│         ✅                     │
│                                │
│   Data received!               │
│                                │
│   2 pets                      │
│   15 weight records            │
│   8 vaccinations               │
│                                │
│ ┌────────────────────────────┐ │
│ │    MERGE WITH LOCAL DATA   │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    REPLACE LOCAL DATA      │ │
│ └────────────────────────────┘ │
│                                │
│       Cancel                   │
│                                │
└────────────────────────────────┘
```

---

### Security

- **4-digit code**: Prevents unauthorized connections
- **Proximity-based**: Works only with nearby devices (< 10 meters)
- **One-shot transfer**: The connection is closed after the transfer
- **No cloud storage**: Data travels directly between devices
- **Encryption**: Nearby Connections uses automatic encryption

---


### References

- [Google Nearby Connections API](https://developers.google.com/nearby/connections/overview)
- [Android Strategy.P2P_POINT_TO_POINT](https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Strategy#P2P_POINT_TO_POINT)

## Edge cases

- The device loses connectivity or the process is interrupted midway through the operation.
- The session expires, switches accounts, or lacks sufficient authorization.
- Local and remote data diverge, are incomplete, or were created by different app versions.
- The external provider is unavailable, enforces a quota, or changes its API.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Proposal status | On Hold | Demand and the product model still need to be validated. |
| External technology | Undecided | Firebase, Google Drive, and the cited APIs are historical options, not current commitments. |
| Local source of truth | Preserve Room as the offline foundation | Keeps Petit useful without an account or connectivity. |

## Out of scope

- Implementing this proposal before review, explicit approval, and an index update.
- Treating historical examples of pricing, tiers, providers, or schedules as current decisions.
- Features covered by the specs declared in `depends_on`.
