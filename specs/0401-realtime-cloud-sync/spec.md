---
spec: "0401"
title: "Real-Time Sync"
family: cloud-sync
status: On Hold
owner: woliveiras
depends_on: ["0201"]
---

# Spec: Real-Time Sync

## Context and motivation

> As a premium user,
> I want my data to sync automatically with the cloud,
> So that it is always up to date and available on any device.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: Sync after creating data

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am a premium user with sync enabled
AND I have an internet connection
WHEN I add a new pet named "Luna"
THEN Luna is saved to Room immediately (syncStatus = PENDING)
AND after a few seconds, Luna is sent to Firestore
AND syncStatus changes to SYNCED
AND I see the sync indicator ✓
```

### Scenario 2: Real-time sync receiving data

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have the app open
AND someone (or another device) adds data to Firestore
WHEN the change is detected by the Firestore snapshot listener
THEN the new data is downloaded automatically
AND saved to the local Room database
AND appears in the UI without requiring a manual refresh
```

### Scenario 3: Sync without internet access (queue)

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have no internet connection
WHEN I add a new pet
THEN the pet is saved to Room (syncStatus = PENDING)
AND the pet appears in the UI as usual
AND when the internet connection is restored, sync occurs automatically
```

### Scenario 4: Enable sync for the first time

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have local data
AND I have never synced before
WHEN I enable "Cloud sync" in settings
THEN all local data is sent to Firestore
AND I see the progress message "Syncing X of Y items..."
AND when the operation is complete, all items have syncStatus = SYNCED
```

### Scenario 5: Premium expires

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN my premium subscription expires
WHEN this happens
THEN the Firestore snapshot listener is disconnected
AND new data is saved locally only (syncStatus = LOCAL_ONLY)
AND previously synced data remains on the device
AND I see the warning "Sync paused - Renew your premium subscription"
```

---

## Non-functional requirements

- [ ] Preserve Petit’s local operation when authentication, the network, or an external service is unavailable.
- [ ] Protect personal and pet health data during storage, transmission, and deletion.
- [ ] Provide accessible and understandable loading, success, empty, and error states.
- [ ] Prevent silent data loss or duplication when operations are interrupted.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Eligibility, validation, state, conflict, and data transformation rules. |
| Integration | Flows that cross the UI, repositories, local database, and external providers. |
| Both | Each vertical task uses unit tests for rules and integration tests for I/O boundaries. |

## Acceptance criteria

The scenarios under **Functional requirements** are this spec’s testable criteria and must have traceable coverage before the status advances to `Implemented`.

## Preserved product notes

### UI/UX

### Sync indicator in the toolbar

```
┌────────────────────────────────┐
│ 🐱 Petit                 ☁️✓  ⚙️  │  ← Sync OK
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘

┌────────────────────────────────┐
│ 🐱 Petit                 ☁️⟳  ⚙️  │  ← Syncing
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘

┌────────────────────────────────┐
│ 🐱 Petit                 ☁️!  ⚙️  │  ← Pending (no internet)
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘
```

### Sync settings

```
┌────────────────────────────────┐
│ ← Sync                         │
├────────────────────────────────┤
│                                │
│ ☁️ CLOUD SYNC                  │
│ ┌────────────────────────────┐ │
│ │ Enable                [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Your data is synced         │
│ automatically across all      │
│ your devices.                 │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ ✅ Synced                  │ │
│ │ Last sync: 2 min ago       │ │
│ │                            │ │
│ │ 2 pets • 15 weigh-ins      │ │
│ │ 8 vaccines • 6 dewormings  │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ⚙️ OPTIONS                     │
│ ┌────────────────────────────┐ │
│ │ Wi-Fi-only sync      [OFF] │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │      FORCE FULL SYNC       │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

---

## Edge cases

- The device loses connectivity or the process is interrupted mid-operation.
- The session expires, switches accounts, or lacks sufficient authorization.
- Local and remote data diverges, is incomplete, or was created by different app versions.
- The external provider is unavailable, enforces quota limits, or changes its API.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Proposal status | On Hold | Demand and the product model still need to be validated. |
| External technology | Undecided | The cited Firebase, Google Drive, and APIs are historical options, not current commitments. |
| Local source of truth | Preserve Room as the offline foundation | Keeps Petit useful without an account or connectivity. |

## Out of scope

- Implementing this proposal before review, explicit approval, and an index update.
- Treating historical pricing, tier, provider, or timeline examples as current decisions.
- Capabilities covered by the specs declared in `depends_on`.
