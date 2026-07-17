---
spec: "0306"
title: "Backup Settings"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0305"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Backup Settings

## Context and motivation

> As a signed-in user,
> I want to configure how automatic backup works,
> So that I can optimize data and battery usage.

This is a historical hypothesis that has not yet been implemented. Product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: Enable/disable automatic backup

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am signed in with Google
WHEN I open Settings > Automatic Backup
AND I enable the "Automatic backup" toggle
THEN the daily backup at 2:00 a.m. is scheduled
AND I see "Next backup: today/tomorrow at 2:00 a.m."

WHEN I disable the toggle
THEN the schedule is canceled
AND I see "Automatic backup disabled"
```

### Scenario 2: Configure Wi-Fi only

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN automatic backup is enabled
AND "Wi-Fi only" is disabled
WHEN I enable "Wi-Fi only"
THEN future backups run only over Wi-Fi
AND the current schedule is adjusted

GIVEN I am on a mobile network at 2:00 a.m.
AND "Wi-Fi only" is enabled
WHEN the backup is due to run
THEN it is postponed until I connect to Wi-Fi
```

### Scenario 3: View backup history

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have completed automatic backups
WHEN I open "View history"
THEN I see a list of the latest backups
AND each item shows:
  - Date/time
  - Whether it was automatic or manual
  - Status (success/failure)
```

### Scenario 5: Backup notification

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN "Notify after backup" is enabled
WHEN an automatic backup completes successfully
THEN I receive a silent notification
"Backup completed: 2 pets, 15 KB"

GIVEN "Notify after backup" is disabled
WHEN a backup is completed
THEN I do NOT receive a notification
```

### Scenario 6: Back up now

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am on the backup settings screen
WHEN I tap "Back up now"
THEN a backup runs immediately
AND the timer for the next automatic backup is reset
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

### Screen: Automatic Backup Settings

```
┌────────────────────────────────┐
│ ← Automatic Backup             │
├────────────────────────────────┤
│                                │
│ ☁️ AUTOMATIC BACKUP            │
│ ┌────────────────────────────┐ │
│ │ Enable                [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Your data is saved          │
│ automatically to Firebase     │
│ Storage, even when the app    │
│ is closed.                    │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ ✅ Last: Today 10:30       │ │
│ │ ⏰ Next: Tomorrow 10:30    │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ⚙️ SETTINGS                    │
│                                │
│ ┌────────────────────────────┐ │
│ │ Frequency                 │ │
│ │ Every 24 hours          ▶ │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │ Wi-Fi only           [ON]  │ │
│ │ Saves mobile data          │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │ Notify on success   [OFF]  │ │
│ │ Shows notification after   │ │
│ │ each backup                │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │    BACK UP NOW             │ │
│ └────────────────────────────┘ │
│                                │
│ View backup history         ▶  │
│                                │
└────────────────────────────────┘
```

### Bottom Sheet: Frequency

```
┌────────────────────────────────┐
│                    ─────       │
│                                │
│ Backup frequency               │
│                                │
│ ○ Every 6 hours                │
│   More protection, more data   │
│                                │
│ ● Every 24 hours               │
│   Recommended                  │
│                                │
│ ○ Once a week                  │
│   Lower usage                  │
│                                │
└────────────────────────────────┘
```

### Screen: Backup History

```
┌────────────────────────────────┐
│ ← Backup History               │
├────────────────────────────────┤
│                                │
│ March 2026                     │
│ ┌────────────────────────────┐ │
│ │ ✅ 18/03 10:30  Automatic  │ │
│ │    2 pets • 15.4 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ✅ 17/03 10:30  Automatic  │ │
│ │    2 pets • 15.2 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ✅ 16/03 14:00  Manual     │ │
│ │    2 pets • 15.1 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ❌ 15/03 10:30  Automatic  │ │
│ │    Failed: No connection   │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
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
