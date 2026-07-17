---
spec: "0303"
title: "Manage Backups"
family: backup-recovery
status: On Hold
owner: woliveiras
depends_on: ["0301"]
---

# Spec: Manage Backups

## Context and motivation

> As a signed-in user,
> I want to manage my backups in Google Drive,
> So that I can view the history and clean up old backups if necessary.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: View the backup list

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am signed in with Google
AND I have multiple saved backups
WHEN I open "Saved backups"
THEN I see a list of all backups
AND each item shows:
  - Backup date and time
  - Number of pets
  - File size
  - App version
```

### Scenario 2: View backup details

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am on the backup list
WHEN I tap a backup
THEN I see full details:
  - Date and time
  - Contents (X pets, Y weigh-ins, Z vaccinations)
  - Size
  - Version of the app that created it
AND I see the options: Restore, Delete
```

### Scenario 3: Delete a specific backup

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am viewing a backup's details
WHEN I tap "Delete"
AND confirm the deletion
THEN the backup is removed from Google Drive
AND no longer appears in the list
```

### Scenario 4: Delete multiple backups

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am on the backup list
WHEN I enable selection mode (long press)
AND select multiple backups
AND tap "Delete selected"
AND confirm
THEN all selected backups are removed
```

### Scenario 5: Manual backup limit

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have 10 saved manual backups (the limit)
WHEN I create a new manual backup
THEN the oldest manual backup is removed automatically
AND the new backup is added
AND I see the notification "Old backup removed to free up space"
```

### Scenario 7: Backups after account deletion

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have backups saved in Google Drive
WHEN I delete my app account
THEN the backups are retained for 90 days (grace period)
AND I see the warning "Your account will be permanently deleted in X days."
AND after 90 days without reactivation, the backups are purged automatically
```

### Scenario 8: Backups after account deletion

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have saved backups
WHEN I delete my account
THEN the backups are scheduled for purging in 30 days
AND after 30 days, all files in the user's bucket are permanently removed
```

### Scenario 6: Total space used

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am on the backup screen
WHEN I view the "Saved backups" section
THEN I see the total number of backups
AND the total space used (e.g., "3 backups • 45.2 KB")
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
| Integration | Flows that cross the UI, repositories, local database, and external providers. |
| Both | Each vertical task uses unit tests for rules and integration tests for I/O boundaries. |

## Acceptance criteria

The scenarios under **Functional requirements** are this spec's testable criteria and must have traceable coverage before the status advances to `Implemented`.

## Preserved product notes

### UI/UX

### Screen: Backup Details

```
┌────────────────────────────────┐
│ ← Backup Details               │
├────────────────────────────────┤
│                                │
│ 📦 BACKUP                      │
│                                │
│ 18/03/2026 at 10:30            │
│ App version: 1.0.0             │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 CONTENTS                    │
│ ┌────────────────────────────┐ │
│ │ 🐱 Pets           2       │ │
│ │ ⚖️ Weigh-ins      15       │ │
│ │ 💉 Vaccinations    8       │ │
│ │ 🪱 Dewormers       6       │ │
│ │ 🔔 Reminders       3       │ │
│ └────────────────────────────┘ │
│                                │
│ 📁 Size: 15.4 KB               │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │        RESTORE             │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │         DELETE             │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### List with Multiple Selection

```
┌────────────────────────────────┐
│ ← Saved Backups     [🗑️] [✓]   │
├────────────────────────────────┤
│ 2 selected                     │
├────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │ ☑️ 18/03/2026 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ☐ 15/03/2026 14:20         │ │
│ │ 2 pets • 14.8 KB          │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ☑️ 10/03/2026 09:15        │ │
│ │ 1 pet • 8.2 KB            │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirm Deletion

```
┌────────────────────────────────┐
│      Delete Backup?            │
├────────────────────────────────┤
│                                │
│ ⚠️ This action cannot be       │
│ undone.                        │
│                                │
│ The backup will be permanently │
│ removed from Firebase          │
│ Storage.                       │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │  CANCEL  │  │    DELETE    │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

---

## Edge cases

- The device loses connectivity or the process is interrupted during the operation.
- The session expires, switches accounts, or lacks sufficient authorization.
- Local and remote data diverge, are incomplete, or were created by different app versions.
- The external provider is unavailable, restricts quota, or changes its API.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Proposal status | On Hold | Demand and the product model still need to be validated. |
| External technology | Undecided | Firebase, Google Drive, and the referenced APIs are historical options, not current commitments. |
| Local source of truth | Preserve Room as the offline foundation | Keeps Petit useful without an account or connectivity. |

## Out of scope

- Implementing this proposal before review, explicit approval, and an index update.
- Treating historical pricing, tier, provider, or timeline examples as current decisions.
- Capabilities covered by the specs declared in `depends_on`.
