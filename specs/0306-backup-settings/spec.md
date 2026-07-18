---
spec: "0306"
title: "Backup Settings"
family: backup-recovery
status: In Progress
owner: woliveiras
depends_on: ["0305"]
---

# Spec: Backup Settings

## Context and motivation

The caregiver needs clear control over Google Drive connection, automatic
backup, network constraints, notifications, manual execution, and history. No
setting is gated by a Petit account or payment.

## Functional requirements

### Scenario 1: See independent service state

```gherkin
GIVEN I open Backup settings
THEN I see whether Google Drive is disconnected, authorizing, connected, needs authorization, or unavailable
AND I do not see the state described as Petit Cloud login or premium
```

### Scenario 2: Enable and disable automatic backup

```gherkin
GIVEN Google Drive is authorized
WHEN I enable automatic backup
THEN one daily periodic request is scheduled
AND the UI says Android runs it when conditions permit
WHEN I disable automatic backup
THEN the periodic request is canceled
AND manual backup remains available
```

### Scenario 3: Configure network usage

```gherkin
GIVEN automatic backup is enabled
WHEN I enable "Unmetered networks only"
THEN WorkManager requires NetworkType.UNMETERED
WHEN I disable it
THEN WorkManager requires NetworkType.CONNECTED
AND the unique schedule is updated without duplication
```

### Scenario 4: Configure completion notifications

```gherkin
GIVEN I can change "Notify after backup"
WHEN it is enabled and an automatic backup succeeds
THEN I receive a silent localized notification with non-clinical counts
WHEN it is disabled
THEN success produces no notification
AND authorization-required and actionable failure notifications remain permitted
```

### Scenario 5: Back up now independently

```gherkin
GIVEN Google Drive is authorized
WHEN I choose "Back up now"
THEN a manual backup begins immediately through the shared backup use case
AND it does not reset, duplicate, or replace the periodic schedule
```

### Scenario 6: Preview accurate history and schedule state

```gherkin
GIVEN backup attempts exist
WHEN I open Backup settings
THEN I see at most the three most recent attempts
AND I can open the complete backup history
AND each preview shows trigger, start time, completion time, status, size, and non-clinical counts
AND scheduled work is described as inexact
AND failures provide an actionable category without sensitive data
```

### Scenario 7: Browse backup history incrementally

```gherkin
GIVEN more than five backup attempts exist
WHEN I open Backup history
THEN the five most recent attempts are shown first
WHEN I choose "Load more"
THEN the next five attempts are appended in stable newest-first order
AND attempts already shown are not duplicated
AND loading does not delete or retain attempts differently
AND "Load more" is hidden when no additional attempts exist
```

### Scenario 8: Disconnect Google Drive

```gherkin
GIVEN Google Drive is connected
WHEN I disconnect and confirm
THEN Drive access is revoked
AND automatic work is canceled
AND local data and remote backups are not deleted
AND I can reconnect later
```

## Settings model

- Google Drive connection state.
- Automatic backup enabled, default `false`.
- Network requirement: connected or unmetered, default unmetered.
- Notify after successful automatic backup, default `false`.
- Last attempt and last success summaries.

The settings model does not contain a retention period, maximum backup count,
premium flag, exact run time, or provider credential.

## Non-functional requirements

- Persist preferences in DataStore.
- Update unique WorkManager requests atomically with preference changes.
- Localize all visible copy and content descriptions.
- Do not show an exact future execution time WorkManager cannot guarantee.
- Keep actionable errors distinct from transient background retries.
- Never expose pet names, clinical values, access tokens, or file contents in history or notifications.
- Retrieve history through a bounded page contract so the dedicated screen does
  not require every stored attempt in memory.
- Keep history ordering stable by start time and a deterministic attempt-ID tie-breaker.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Defaults, persistence mapping, state copy, notification policy, schedule descriptions, stable ordering, and page boundaries. |
| Integration | DataStore, WorkManager update/cancel, bounded history retrieval, manual action, and disconnect. |
| Instrumented | Settings accessibility, authorization launcher, toggles, three-item history preview, five-item history pages, load-more states, and confirmations. |

## Acceptance criteria

Every functional scenario and settings-model rule requires traceable coverage
before the status can advance to `Implemented`.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Status | In Progress | Provider-neutral settings are implemented; real Drive disconnect validation remains open. |
| Automatic default | Off | Remote automation requires explicit opt-in. |
| Network default | Unmetered | Reduces unexpected mobile-data use. |
| Schedule presentation | Inexact | WorkManager does not promise a precise execution time. |
| Manual backup | Always available with Drive authorization | Automation settings must not gate basic backup. |
| Settings history preview | Three most recent attempts | Keeps settings concise while retaining immediate operational context. |
| Dedicated history page | Five attempts per load | Supports incremental browsing with a predictable accessible action. |
| History retention | Unchanged by pagination | Viewing fewer attempts must never delete stored history. |
| Retention controls | None | Petit does not delete user-owned backups automatically. |

## Out of scope

- Selecting exact clock times.
- Configuring automatic retention or backup-count limits.
- Petit Cloud billing or entitlement settings.
