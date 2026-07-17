---
spec: "0302"
title: "Restore Backup"
family: backup-recovery
status: On Hold
owner: woliveiras
depends_on: ["0301"]
---

# Spec: Restore Backup

## Context and motivation

> As a signed-in user,
> I want to restore my data from a backup in Google Drive,
> So that I can recover my data on a new phone or after reinstalling the app.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: Successfully restore a backup

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am signed in with Google
AND I have backups saved in Google Drive
WHEN I open "Saved backups"
AND select a backup to restore
AND confirm the restore
THEN I see the download progress
AND the data is restored to the local database
AND I see the message "Data restored successfully"
```

### Scenario 2: Restore on a new device

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I installed the app on a new phone
AND signed in with my Google account
WHEN I open "Restore from backup"
THEN I see a list of available backups
AND I can select which one to restore
```

### Scenario 3: Restore replaces local data

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have local data
AND restore a backup
WHEN I confirm "Replace local data"
THEN ALL local data is deleted
AND the backup data is imported
AND I see the backup data on the home screen
```

### Scenario 4: Restore with merge

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have local data
AND restore a backup
WHEN I choose "Merge with local data"
THEN the data is merged (last-write-wins)
AND unique data from both sources is retained
```

### Scenario 5: Restore with no backups

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have no backups in Google Drive
WHEN I open "Saved backups"
THEN I see the message "No backups found"
AND I see a suggestion to create my first backup
```

### Scenario 6: Download error

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I select a backup to restore
WHEN the connection fails during the download
THEN I see an error message
AND the local data is not changed
AND I can try again
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

### Screen: Backup List

```
┌────────────────────────────────┐
│ ← Saved Backups                │
├────────────────────────────────┤
│                                │
│ Select a backup to             │
│ restore:                       │
│                                │
├────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │ 📦 18/03/2026 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📦 15/03/2026 14:20        │ │
│ │ 2 pets • 14.8 KB          │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📦 10/03/2026 09:15        │ │
│ │ 1 pet • 8.2 KB            │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirm Restore

```
┌────────────────────────────────┐
│       Restore Backup           │
├────────────────────────────────┤
│                                │
│ Backup from 18/03/2026 10:30   │
│ 2 pets • 15.4 KB              │
│                                │
│ ⚠️ You have local data.        │
│ What would you like to do?     │
│                                │
│ ○ Replace local data           │
│   (deletes all and restores)   │
│                                │
│ ● Merge with local data        │
│   (keeps the latest data)      │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │  CANCEL  │  │   RESTORE    │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

### State: Restoring

```
┌────────────────────────────────┐
│                                │
│                                │
│         ┌─────────┐            │
│         │  ████░░ │            │
│         └─────────┘            │
│                                │
│      Restoring backup...       │
│      Downloading data          │
│                                │
│      Do not close the app      │
│                                │
│                                │
└────────────────────────────────┘
```

### State: No Backups

```
┌────────────────────────────────┐
│ ← Saved Backups                │
├────────────────────────────────┤
│                                │
│                                │
│         📭                     │
│                                │
│   No backups found             │
│                                │
│   Create your first backup     │
│   to protect your data.        │
│                                │
│ ┌────────────────────────────┐ │
│ │        BACK UP NOW         │ │
│ └────────────────────────────┘ │
│                                │
│                                │
└────────────────────────────────┘
```

---

### Onboarding Flow (New Device)

```kotlin
class OnboardingViewModel(...) {

    fun checkForBackups() {
        viewModelScope.launch {
            // Check whether the user has backups
            backupStorageRepository.listBackups()
                .onSuccess { backups ->
                    if (backups.isNotEmpty()) {
                        // Show restore option
                        _showRestoreOption.value = true
                    }
                }
        }
    }
}
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
