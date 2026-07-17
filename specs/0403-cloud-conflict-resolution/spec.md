---
spec: "0403"
title: "Cloud Conflict Resolution"
family: cloud-sync
phase: 5
status: On Hold
owner: ""
depends_on: ["0401"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Cloud Conflict Resolution

## Context and Motivation

> As a user with multiple devices,
> I want edit conflicts to be resolved automatically,
> So that I do not lose data or have to resolve conflicts manually.

This is a historical hypothesis that has not been implemented. The product, external provider, availability, and monetization must be revalidated before approval.

## Functional Requirements

### Scenario 1: Basic Last-Write-Wins

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN the pet "Luna" has updatedAt = 1000 on device A
AND device B edits Luna (updatedAt = 1500)
WHEN device A receives the change from the Firestore snapshot listener
THEN device B's version (the newer one) is retained
AND device A shows B's changes
```

### Scenario 2: Older Offline Edit

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN device A is offline and edits Luna (updatedAt = 1000)
AND device B edits Luna online (updatedAt = 1500)
WHEN device A comes back online and attempts to sync
THEN device B's version wins (higher updatedAt)
AND device A's changes are discarded
AND device A updates to B's version
```

### Scenario 3: Newer Offline Edit

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN device A is offline and edits Luna (updatedAt = 2000)
AND the Firestore version has updatedAt = 1500
WHEN device A comes back online and syncs
THEN device A's version wins (higher updatedAt)
AND Firestore is updated with A's version
AND other devices receive A's version
```

### Scenario 4: Delete vs. Edit

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN device A deletes Luna (deletedAt = 1500)
AND device B edited Luna (updatedAt = 1600) before receiving the deletion
WHEN sync occurs
THEN if the edit is newer than the deletion, Luna is restored
OR if the deletion is newer, Luna remains deleted
```

### Scenario 5: Different Fields Edited

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN device A changes Luna's name to "Lulu"
AND device B edits Luna's weight at the same time
WHEN sync occurs
THEN both changes are retained (if the strategy is field-level)
OR the newer version wins completely (if it is document-level)
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

### Resolution Strategy

### Document-Level (Current Implementation)

```
Rule: Last-Write-Wins based on updatedAt

Local:  { id: "1", name: "Luna",    updatedAt: 1000 }
Remote: { id: "1", name: "Lulu", updatedAt: 1500 }

Result: Remote wins (1500 > 1000)
        Local is replaced by Remote
```

### Field-Level (Future Improvement)

```
Local:  { id: "1", name: "Luna",    weight: 3.5, updatedAt: 1000, weightUpdatedAt: 1000 }
Remote: { id: "1", name: "Lulu", weight: 3.4, updatedAt: 1500, weightUpdatedAt: 900 }

Result: Merge
        name: "Lulu" (remote is newer)
        weight: 3.5 (local is newer)
```

---

### Edge Cases

### 1. Equal Timestamps
```kotlin
// If timestamps are exactly equal (rare), prefer remote
// This prevents sync loops
if (remote.updatedAt == local.updatedAt && local.syncStatus == "SYNCED") {
    return Resolution.KeepLocal  // Already synced
}
```

### 2. Large Clock Drift
```kotlin
// If the timestamp difference is unreasonable (> 1 year), something is wrong
val MAX_REASONABLE_DIFF = 365L * 24 * 60 * 60 * 1000  // 1 year in ms

if (abs(remote.updatedAt - local.updatedAt) > MAX_REASONABLE_DIFF) {
    Log.w("Sync", "Suspicious timestamp difference, preferring local")
    return Resolution.KeepLocal
}
```

### 3. Corrupted Data
```kotlin
// Validate data before accepting it
if (!remote.isValid()) {
    Log.e("Sync", "Invalid remote data, keeping local")
    return Resolution.KeepLocal
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
