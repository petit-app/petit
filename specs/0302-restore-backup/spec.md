---
spec: "0302"
title: "Restore Google Drive Backup"
family: backup-recovery
status: In Progress
owner: woliveiras
depends_on: ["0301"]
---

# Spec: Restore Google Drive Backup

## Context and motivation

The caregiver needs to restore a complete Google Drive backup on the same or a
different device without a Petit account or paid entitlement. Restore must
validate the entire ZIP archive before changing local state and must preserve
device-bound security identities.

## Functional requirements

### Scenario 1: Restore on a new device

```gherkin
GIVEN Petit has no local user data
AND Google Drive is authorized
AND a compatible backup exists
WHEN I select the backup and confirm restore
THEN Petit downloads and validates the complete archive
AND restores all restorable data, preferences, reminder state, and pet assets
AND rebuilds device-local schedules and asset URIs
```

### Scenario 2: Reject an invalid archive before mutation

```gherkin
GIVEN an archive is truncated, oversized, unsafe, incompatible, or has a checksum mismatch
WHEN I attempt to restore it
THEN Petit rejects the archive before changing Room or installed assets
AND explains the failure
AND local data remains unchanged
```

### Scenario 3: Replace local data exactly

```gherkin
GIVEN I have local restorable data
WHEN I choose REPLACE and confirm the destructive action
THEN the backup becomes the exact restorable local state
AND records and app-owned assets absent from the backup are removed
AND Google authorization, device identity, pairing keys, and family authorization are preserved
```

### Scenario 4: Merge with local data

```gherkin
GIVEN I have local data and select a backup
WHEN I choose MERGE
THEN entities use the shared deterministic conflict resolver
AND unique data from both sources is retained
AND assets follow the winning entity version
AND current-device preferences remain unless the user explicitly chooses to apply backup preferences
```

### Scenario 5: Handle interruption and retry

```gherkin
GIVEN a restore download or validation is in progress
WHEN the operation is interrupted or canceled
THEN no partial database or asset state is installed
AND temporary files are removed
AND I can retry safely
```

### Scenario 6: Require foreground authorization when necessary

```gherkin
GIVEN Drive authorization was revoked or requires new consent
WHEN I start restore
THEN Petit requests authorization in foreground
AND does not treat Petit Cloud authentication as a substitute
```

## Validation and installation rules

- Download into an app-private temporary location.
- Enforce compressed size, total uncompressed size, entry count, per-entry size,
  canonical path, media type, and schema limits.
- Reject absolute paths, traversal segments, duplicate paths, undeclared files,
  missing files, checksum mismatches, and unsupported schemas.
- Parse and validate all JSON and referential integrity before mutation.
- Stage assets under unique app-private paths that are not referenced by active
  rows, then install their references inside the Room transaction.
- If the Room transaction fails, discard newly staged assets. After a successful
  commit, remove old unreferenced assets through idempotent cleanup.
- Persist enough installation state to remove orphaned staging files safely
  after process death; user-visible data must resolve to either the complete old
  state or the complete restored state.
- Reschedule restored reminders after commit.

## Non-functional requirements

- Preserve Room as the source of truth.
- Use the exact same entity conflict resolver as local sharing.
- Never overwrite device-bound credentials from a backup.
- Never execute, render, or trust archive filenames as paths.
- Report download and validation progress accessibly.
- Keep retry idempotent and cleanup idempotent.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | ZIP safety, manifest validation, schema compatibility, mode rules, and error mapping. |
| Integration | Download, filesystem staging, Room transaction, two-phase asset installation, recovery cleanup, and reminder rescheduling. |
| Instrumented | Restore UI, destructive confirmation, authorization, and new-device journey. |
| Manual | Real Drive download, interruption, revocation, and second-device restoration. |

## Acceptance criteria

Every functional scenario and validation/installation rule requires traceable
coverage before the status can advance to `Implemented`.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Status | In Progress | Provider-independent restore is implemented; real Drive and second-device validation remain open. |
| Modes | MERGE and REPLACE | Users can combine data or recreate a snapshot explicitly. |
| Archive trust | Validate completely before mutation | A remote ZIP is untrusted input. |
| Device-bound data | Never restored | Cloning identities or authorization would be unsafe. |
| Client encryption | None | Restore relies on Google Drive protection per 0301. |

## Out of scope

- Automatically selecting a backup without user confirmation.
- Restoring Petit Cloud credentials, Google tokens, device identities, or local-sharing keys.
- Deleting the remote archive after restore.
