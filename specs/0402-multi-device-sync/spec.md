---
spec: "0402"
title: "Multi-Device Sync"
family: cloud-sync
status: On Hold
owner: woliveiras
depends_on: ["0401"]
---

# Spec: Multi-Device Sync

## Context and Motivation

> As a premium user with multiple devices,
> I want my data to be available on all of them,
> So that I can access and edit it from anywhere.

This is a historical hypothesis that has not been implemented. The product, external provider, availability, and monetization must be revalidated before approval.

## Functional Requirements

### Scenario 1: Second Device Receives Data

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have data on device A
AND I install the app on device B
WHEN I sign in on device B
AND enable sync
THEN all my data is downloaded from Firestore
AND I see the same pets as on device A
```

### Scenario 2: Edits Appear in Real Time

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have the app open on devices A and B
WHEN I change the pet's name to "Lulu" on device A
THEN within a few seconds, device B shows "Lulu"
Without requiring a manual refresh
```

### Scenario 3: Create on One Device, View on Another

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I add a new pet named "Simba" on device A
WHEN sync completes
THEN device B receives "Simba" automatically
AND Simba appears in the pet list
```

### Scenario 4: Delete on One Device, Reflect on Another

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I delete the pet "Simba" on device A
WHEN sync completes
THEN device B no longer shows "Simba" either
```

### Scenario 5: Offline Device vs. Online Device

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN device A is offline
AND device B adds a pet named "Mia"
WHEN device A comes back online
THEN device A receives "Mia" automatically
```

---

## Non-Functional Requirements

- [ ] Preserve Petit’s local operation when authentication, the network, or an external service is unavailable.
- [ ] Protect personal and pet health data during storage, transmission, and deletion.
- [ ] Provide accessible, understandable loading, success, empty, and error states.
- [ ] Prevent silent data loss or duplication during interrupted operations.

## Test Strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Eligibility, validation, state, conflict, and data transformation rules. |
| Integration | Flows that cross the UI, repositories, local database, and external providers. |
| Both | Each vertical task uses unit tests for rules and integration tests for I/O boundaries. |

## Acceptance Criteria

The scenarios in **Functional Requirements** are this spec’s testable acceptance criteria and must have traceable coverage before the status advances to `Implemented`.

## Preserved Product Notes

### UI/UX

### Device Indicator

```
┌────────────────────────────────┐
│ ← Sync                         │
├────────────────────────────────┤
│                                │
│ 📱 CONNECTED DEVICES           │
│ ┌────────────────────────────┐ │
│ │ 📱 This device            │ │
│ │    Galaxy S24 • Online     │ │
│ │                            │ │
│ │ 📱 Another device         │ │
│ │    Pixel 8 • 5 min ago    │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Your data is synced         │
│ automatically across all      │
│ signed-in devices.            │
│                                │
└────────────────────────────────┘
```

### First Sync on a New Device

```
┌────────────────────────────────┐
│                                │
│         ☁️ ↓                   │
│                                │
│   Syncing your data...         │
│                                │
│   ████████░░░░░░  60%          │
│                                │
│   Downloading: 2 pets          │
│                15 weigh-ins    │
│                8 vaccinations │
│                                │
│   Keep the connection active   │
│                                │
└────────────────────────────────┘
```

---

### Multi-Device Conflict Resolution

When two devices edit the same data simultaneously:

```kotlin
suspend fun handleIncomingChange(remote: PetFirestoreModel) {
    val local = petDao.getPetById(remote.id)

    when {
        // New remote record
        local == null -> {
            petDao.insertPet(remote.toEntity())
        }
        // Remote record is newer: accept it
        remote.updatedAt > local.updatedAt -> {
            petDao.updatePet(remote.toEntity())
        }
        // Local record is newer: keep it and upload it again
        local.updatedAt > remote.updatedAt && local.syncStatus == "SYNCED" -> {
            // The local record is newer but has already been marked as synced
            // This means the local change has not been uploaded yet
            // Keep the local record and upload it to the cloud
            uploadToFirestore(local)
        }
        // Same timestamp: treat as a tie and keep the local record
        else -> {
            // Do nothing; the local record is already correct
        }
    }
}
```

---

## Edge Cases

- The device loses connectivity or the process is interrupted midway through the operation.
- The session expires, switches accounts, or lacks sufficient authorization.
- Local and remote data diverge, are incomplete, or were created by different app versions.
- The external provider is unavailable, enforces quota limits, or changes its API.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Proposal status | On Hold | Demand and the product model still need validation. |
| External technology | Not decided | Firebase, Google Drive, and the cited APIs are historical options, not current commitments. |
| Local source of truth | Preserve Room as the offline foundation | Keeps Petit useful without an account or connectivity. |

## Out of Scope

- Implementing this proposal before review, explicit approval, and an index update.
- Treating historical examples of pricing, tiers, providers, or schedules as current decisions.
- Capabilities covered by the specs declared in `depends_on`.
