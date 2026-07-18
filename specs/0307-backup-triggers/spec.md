---
spec: "0307"
title: "Backup Triggers"
family: backup-recovery
status: In Progress
owner: woliveiras
depends_on: ["0305", "0306"]
---

# Spec: Backup Triggers

## Context and motivation

When automatic backup is enabled, the caregiver's recent changes should be
protected without creating one backup per edit. Durable WorkManager debounce
coalesces restorable changes and remains free, account-independent, and
compatible with periodic backup.

## Functional requirements

### Scenario 1: Schedule after a restorable change

```gherkin
GIVEN automatic backup is enabled
WHEN restorable data or an app-owned pet asset changes
THEN Petit enqueues one unique change-triggered backup with a five-minute delay
AND the work persists if the app process closes
```

### Scenario 2: Debounce consecutive changes

```gherkin
GIVEN a change-triggered backup is pending
WHEN another restorable change occurs before it runs
THEN Petit replaces the pending delay with a new five-minute delay
AND only one complete backup is produced after activity settles
```

### Scenario 3: Include every supported change type

```gherkin
GIVEN automatic backup is enabled
WHEN a pet, photo, weight, vaccination, deworming record, task, reminder state, or restorable preference changes
THEN the pending backup includes the final complete snapshot
```

### Scenario 4: Avoid bookkeeping loops

```gherkin
GIVEN a backup attempt updates its own history, progress, schedule, or provider state
WHEN those internal values change
THEN they do not schedule another backup
```

### Scenario 5: Coalesce with periodic or manual backup

```gherkin
GIVEN a change-triggered backup is pending
WHEN a periodic or manual backup successfully captures an equal or newer local revision
THEN the pending change-triggered request is canceled
AND no duplicate archive is created for the same revision
```

### Scenario 6: Respect disabled or unauthorized state

```gherkin
GIVEN automatic backup is disabled or Drive requires foreground authorization
WHEN local data changes
THEN no runnable change-triggered upload starts
AND local data remains unaffected
AND the UI exposes authorization-required state when applicable
```

### Scenario 7: Retry under WorkManager constraints

```gherkin
GIVEN a change-triggered backup is eligible
WHEN network constraints are unmet or a transient provider error occurs
THEN WorkManager defers or retries with the configured exponential backoff
AND the operation remains unique and idempotent
```

## Trigger boundary

Triggers include all data and app-owned assets included by the 0301 archive.
They exclude backup attempt history, upload progress, provider tokens,
authorization state, WorkManager bookkeeping, caches, and temporary staging so
that the backup process cannot trigger itself.

## Non-functional requirements

- Use unique one-time WorkManager work for the debounce request.
- Persist the local revision captured by successful backups.
- Keep scheduling idempotent under concurrent repository writes.
- Apply the same network constraints as periodic automatic backup.
- Never prompt for authorization from background work.
- Do not make triggers premium or dependent on Petit authentication.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Trigger classification, debounce replacement, revision coalescing, and loop prevention. |
| Integration | Repository mutation hooks, WorkManager persistence, periodic/manual interaction, and worker retry. |
| Instrumented | App-close durability and settings interaction. |
| Manual | Process death, rapid edits, network changes, and authorization revocation. |

## Acceptance criteria

Every functional scenario and trigger-boundary rule requires traceable coverage
before the status can advance to `Implemented`.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Status | In Progress | Provider-neutral triggers are implemented; physical durability remains open. |
| Debounce | Five minutes after the latest restorable change | Coalesces rapid edits without long exposure. |
| Price | Free | Triggering user-owned backup is not a Petit Cloud capability. |
| Duplicate prevention | Local revision watermark | Periodic, manual, and triggered runs share one completion boundary. |
| Retention | None | Triggered backups are not deleted automatically. |

## Out of scope

- Real-time cloud synchronization.
- Triggering from backup bookkeeping or provider state.
- Exact execution timing while Android constraints are unmet.
