---
spec: "0307"
title: "Backup Triggers"
family: backup-recovery
status: On Hold
owner: woliveiras
depends_on: ["0305", "0306"]
---

# Spec: Backup Triggers

## Context and motivation

> As a premium user,
> I want a backup to run automatically after I make important changes,
> So that my latest data is always protected.

This is a historical hypothesis that has not yet been implemented. Product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: Backup after creating a pet

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN automatic backup is enabled
AND I have an internet connection
WHEN I add a new pet
THEN after 5 minutes of inactivity
the backup runs automatically
AND includes the new pet
```

### Scenario 2: Debounce multiple changes

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I make several changes in succession:
  - I add the pet Luna
  - I add a 3.5kg weight record
  - I add the V3 vaccine
  - All in less than 5 minutes
THEN only ONE backup runs
(5 minutes after the last change)
AND it includes all changes
```

### Scenario 3: Backup after deletion

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN automatic backup is enabled
WHEN I delete a pet
THEN after 5 minutes without changes
the backup runs
AND reflects the deletion
```

### Scenario 4: Cancel pending backup

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I changed data and a backup is pending (in 3 min)
WHEN I make another change
THEN the timer is reset to 5 minutes again
AND only one backup will run
```

### Scenario 5: Do not duplicate periodic backup

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN a change-triggered backup is pending
AND the periodic backup is due to run now
THEN only one backup runs
AND the change-triggered backup timer is canceled
```

### Scenario 6: App closed after a change

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I made changes
AND I close the app immediately
THEN the pending backup will still run
(WorkManager persists the task)
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

### Backup Triggers

| Event | Trigger? | Debounce |
|--------|----------|----------|
| Create pet | ✅ | 5 min |
| Edit pet | ✅ | 5 min |
| Delete pet | ✅ | 5 min |
| Add weight record | ✅ | 5 min |
| Add vaccine | ✅ | 5 min |
| Add dewormer | ✅ | 5 min |
| Create reminder | ❌ | - |
| Edit settings | ❌ | - |

---

### UI Feedback (Optional)

### Subtle Indicator

Do not show anything visually. The change-triggered backup is "invisible" to the user; it only ensures that data is protected.

### For Debugging/Development

```kotlin
// Debug builds only
if (BuildConfig.DEBUG && hasPendingBackup()) {
    Snackbar.make(
        view,
        "Backup pending in ${getRemainingTime()} min",
        Snackbar.LENGTH_SHORT
    ).show()
}
```

---

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
