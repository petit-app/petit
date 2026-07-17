---
spec: "0404"
title: "Offline-First Sync"
family: cloud-sync
phase: 5
status: On Hold
owner: ""
depends_on: ["0401"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Offline-First Sync

## Context and Motivation

> As a premium user,
> I want the app to work normally even when offline,
> So that I can record data without a connection and have it sync later.

This is a historical hypothesis that has not been implemented. The product, external provider, availability, and monetization must be revalidated before approval.

## Functional Requirements

### Scenario 1: Create Data Offline

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have no internet connection
WHEN I register a new pet named "Mia"
THEN Mia is saved in Room (syncStatus = PENDING_SYNC)
AND Mia appears in the list as usual
AND I see a "Pending sync" indicator on the item
```

### Scenario 2: Automatic Sync on Reconnection

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have data pending sync
AND I am offline
WHEN the internet connection returns
THEN sync starts automatically
AND the pending data is uploaded
AND syncStatus changes to SYNCED
AND the pending indicator disappears
```

### Scenario 3: Multiple Offline Edits

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am offline
WHEN I make several edits:
  - Add the pet Mia
  - Add a weigh-in for Mia
  - Change Luna's name to Lulu
THEN all edits are saved locally
AND all of them remain PENDING_SYNC
AND upon reconnection, all of them are uploaded
```

### Scenario 4: Conflict After Coming Back Online

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I edited Luna offline (updatedAt = 1000)
AND another device edited Luna online (updatedAt = 1500)
WHEN I come back online and sync
THEN conflict resolution occurs
AND the newer version (1500) wins
```

### Scenario 5: Sync Queue Persists After the App Is Closed

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I made edits offline
AND I close the app
AND I reopen the app (still offline)
THEN the edits are still PENDING_SYNC
AND they will be synced upon reconnection
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

### Pending Item Indicator

```
┌────────────────────────────────┐
│ ← My Pets                      │
├────────────────────────────────┤
│ ┌──────────────────────────────┐
│ │ ┌────┐  Luna            ☁️✓  │  ← Synced
│ │ │ 📷 │  3.5 kg               │
│ │ └────┘                       │
│ └──────────────────────────────┘
│ ┌──────────────────────────────┐
│ │ ┌────┐  Mia             ☁️⏳  │  ← Pending
│ │ │ 📷 │  New                  │
│ │ └────┘                       │
│ └──────────────────────────────┘
└────────────────────────────────┘
```

### Offline Status Banner

```
┌────────────────────────────────┐
│ ⚠️ No connection              │
│ Changes will be synced when    │
│ the internet connection returns.│
└────────────────────────────────┘
┌────────────────────────────────┐
│ 🐱 Petit                    ⚙️    │
├────────────────────────────────┤
│ ...                            │
```

### Detailed Sync Status

```
┌────────────────────────────────┐
│ ← Sync                         │
├────────────────────────────────┤
│                                │
│ 📊 SYNC STATUS                 │
│ ┌────────────────────────────┐ │
│ │ ⚠️ 3 pending items        │ │
│ │                            │ │
│ │ • 1 new pet              │ │
│ │ • 1 weigh-in             │ │
│ │ • 1 edited vaccination   │ │
│ │                            │ │
│ │ Waiting for connection... │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
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
