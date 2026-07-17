---
spec: "0301"
title: "Manual Backup"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0201"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Manual Backup

## Context and motivation

> As an app user,
> I want to back up my data to Google Drive,
> So that I can recover it if I lose my phone.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: Successfully create a backup (user already signed in)

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am signed in with Google
AND I have an internet connection
WHEN I open Settings > "Google Drive Backup"
AND I tap "Back up now"
THEN I see a progress indicator
AND the backup is uploaded to Google Drive (appDataFolder)
AND I see the message "Backup completed successfully"
AND I see the date/time of the latest backup
```

### Scenario 2: Backup without internet access

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have no internet connection
WHEN I try to create a backup
THEN I see the message "No connection. Connect to the internet to create a backup."
AND the backup does not start
```

### Scenario 3: Backup while signed out (triggers sign-in)

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am not signed in
WHEN I try to create a backup
THEN I see a dialog explaining that Google sign-in is required
AND I have a "Sign in with Google" option
WHEN I sign in successfully
THEN the backup starts automatically
```

### Scenario 4: First backup

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have never created a backup before
WHEN I create my first backup
THEN the file is created in the Google Drive appDataFolder
AND the metadata is initialized
AND I see "Backup completed successfully"
```

### Scenario 5: Subsequent backup

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I already have previous backups
WHEN I create a new backup
THEN a new file is created (it does not replace the previous one)
AND the metadata is updated
AND old backups are retained (up to the limit)
```

### Scenario 6: Error during backup

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I start a backup
WHEN an error occurs (network drops, quota exceeded, etc.)
THEN I see a specific error message
AND the partial backup is discarded
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

### Screen: Cloud Backup

```
┌────────────────────────────────┐
│ ← Cloud Backup                 │
├────────────────────────────────┤
│                                │
│ ☁️ GOOGLE DRIVE                 │
│                                │
│ Connected as:                  │
│ person-a@example.com           │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 LATEST BACKUP               │
│ ┌────────────────────────────┐ │
│ │ 18/03/2026 at 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │        BACK UP NOW         │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ 📂 SAVED BACKUPS           ▶   │
│ 3 backups (45.2 KB total)      │
│                                │
├────────────────────────────────┤
│                                │
│ ℹ️ Backups are stored in the   │
│ Google Drive appDataFolder    │
│ (hidden).                     │
│                                │
└────────────────────────────────┘
```

### Screen: Cloud Backup (Backing Up)

```
┌────────────────────────────────┐
│ ← Cloud Backup                 │
├────────────────────────────────┤
│                                │
│                                │
│         ┌─────────┐            │
│         │  ████░░ │            │
│         └─────────┘            │
│                                │
│      Backing up...             │
│      Uploading data            │
│                                │
│      Do not close the app      │
│                                │
│                                │
└────────────────────────────────┘
```

### State: Success

```
┌────────────────────────────────┐
│                                │
│            ✅                  │
│                                │
│   Backup completed             │
│   successfully!               │
│                                │
│   18/03/2026 at 10:30          │
│   15.4 KB                      │
│                                │
│   ┌────────────────────────┐   │
│   │          OK            │   │
│   └────────────────────────┘   │
│                                │
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
