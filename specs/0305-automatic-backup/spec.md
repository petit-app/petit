---
spec: "0305"
title: "Automatic Backup"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0301"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Automatic Backup

## Context and motivation

> As a signed-in user,
> I want my data to be saved automatically to Google Drive every day at 2:00 a.m.,
> So that I do not have to worry about backing it up manually.

This is a historical hypothesis that has not yet been implemented. Product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: Automatic backup enabled by default (signed-in user)

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am signed in with Google
WHEN I enable automatic backup in settings
THEN WorkManager schedules a daily backup at 2:00 a.m.
AND I see "Automatic backup enabled — next backup at 2:00 a.m."
```

### Scenario 2: Daily backup runs in the background

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN automatic backup is enabled
WHEN it is 2:00 a.m.
THEN the backup runs automatically
EVEN IF the app is closed
AND I do not need to open the app
AND the backup is saved to Google Drive
```

### Scenario 3: Backup over Wi-Fi only

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN "Back up over Wi-Fi only" is enabled
AND I am connected to a mobile network (4G/5G)
WHEN the automatic backup is due to run
THEN the backup is postponed
AND it runs when I connect to Wi-Fi
```

### Scenario 4: Back up only when signed in

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN an automatic backup is scheduled
AND I am no longer signed in (signed out)
WHEN it is 2:00 a.m.
THEN the backup does NOT run
AND I see the notification "Sign in to continue automatic backups"
```

### Scenario 5: Wi-Fi-only setting is respected

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN "Back up over Wi-Fi only" is enabled
AND I am connected to a mobile network (4G/5G) at 2:00 a.m.
WHEN the automatic backup is due to run
THEN the backup is postponed
AND it runs when I connect to Wi-Fi
AND I see the notification "Waiting for Wi-Fi to back up"
```

### Scenario 6: No internet

- [ ] This scenario is implemented and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I have no internet connection
WHEN the automatic backup is due to run
THEN the backup fails silently
AND it will be attempted again next time
AND I can see "Last backup: 2 days ago (failed)" in settings
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

### Backup Settings

```
┌────────────────────────────────┐
│ ← Automatic Backup             │
├────────────────────────────────┤
│                                │
│ ☁️ AUTOMATIC BACKUP            │
│ ┌────────────────────────────┐ │
│ │ Enabled               [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ Last backup:               │ │
│ │ Today at 10:30 ✅          │ │
│ │                            │ │
│ │ Next backup:               │ │
│ │ Tomorrow at 10:30          │ │
│ └────────────────────────────┘ │
│                                │
│ ⚙️ SETTINGS                    │
│ ┌────────────────────────────┐ │
│ │ Frequency           24h  ▶ │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Wi-Fi only           [ON]  │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Notify on success    [OFF] │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    BACK UP NOW             │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Backup Notification

```
┌────────────────────────────────┐
│ 🐱 Petit                         │
│ Backup completed successfully  │
│ 2 pets saved • 15.4 KB        │
│                                │
│                      [Dismiss] │
└────────────────────────────────┘
```

---

### Available Frequencies

| Option | Hours | Description |
|-------|-------|-----------|
| Frequent | 6 | Every 6 hours |
| Daily | 24 | Once a day |
| Weekly | 168 | Once a week |

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
