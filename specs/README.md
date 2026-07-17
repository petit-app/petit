# Specs Index

Index of all Petit specifications. Update this file when creating, approving,
implementing, placing on hold, or completing a spec.

Each capability lives in a global `specs/NNNN-<slug>/` folder containing
`spec.md`, `plan.md`, and `tasks.md`. Family, phase, and status are defined in
the `spec.md` frontmatter and reflected in the tables below; there are no
folders organized by phase or status.

## Numbering

| Block | Family | Reserved for |
| --- | --- | --- |
| 0001–0099 | pet-care | Pet registration, health, history, and routine care |
| 0100–0199 | local-sharing | Family sharing without a remote server |
| 0200–0299 | identity-access | Identity, account, and authorization |
| 0300–0399 | backup-recovery | Backup, restore, and transfer |
| 0400–0499 | cloud-sync | Remote synchronization and cloud collaboration |

## Status

| Status | Meaning |
| --- | --- |
| Draft | Proposed behavior; not yet approved |
| Approved | Spec approved for implementation |
| In Progress | Implementation is partial or in progress |
| Implemented | Core behavior is available in the codebase |
| Completed | All criteria and tasks have been completed and verified |
| On Hold | Hypothesis retained, with no current implementation commitment |

## Specs

### pet-care

PRD: [Pet health management in Petit](../docs/prds/2026-07-17-petit-pet-health-management.md)

| Spec | Title | Phase | Status | Depends on / Origin |
| --- | --- | --- | --- | --- |
| [0001](0001-pet-management/spec.md) | Pet management | 1 | Implemented | Petit PRD |
| [0002](0002-weight-tracking/spec.md) | Weight tracking | 1 | Implemented | 0001 |
| [0003](0003-vaccination/spec.md) | Vaccination records | 1 | Implemented | 0001 |
| [0004](0004-deworming/spec.md) | Deworming records | 1 | Implemented | 0001 |
| [0005](0005-reminders/spec.md) | Local tasks and reminders | 1 | Implemented | 0001 |
| [0006](0006-export-import/spec.md) | JSON export and import | 1 | Implemented | 0001–0005 |
| [0007](0007-home-dashboard/spec.md) | Home dashboard | 1 | Implemented | 0001–0005 |

### local-sharing

| Spec | Title | Phase | Status | Depends on / Origin |
| --- | --- | --- | --- | --- |
| [0101](0101-device-pairing/spec.md) | Device pairing | 2 | In Progress | Petit PRD |
| [0102](0102-one-shot-transfer/spec.md) | One-shot data transfer | 2 | In Progress | 0101 |
| [0103](0103-family-group/spec.md) | Local family group | 2 | In Progress | 0101 |
| [0104](0104-local-network-sync/spec.md) | Local network sync | 2 | Draft | 0101, 0103 |
| [0105](0105-local-conflict-resolution/spec.md) | Local conflict resolution | 2 | In Progress | 0102 |

### identity-access

| Spec | Title | Phase | Status | Depends on / Origin |
| --- | --- | --- | --- | --- |
| [0201](0201-google-login/spec.md) | Google Login | 3 | On Hold | Historical roadmap |
| [0202](0202-account-management/spec.md) | Account management | 3 | On Hold | 0201 |
| [0203](0203-data-ownership/spec.md) | Data ownership | 3 | On Hold | 0201 |
| [0204](0204-premium-gate/spec.md) | Premium gate | 3 | On Hold | 0201 |

### backup-recovery

| Spec | Title | Phase | Status | Depends on / Origin |
| --- | --- | --- | --- | --- |
| [0301](0301-manual-backup/spec.md) | Manual backup | 4 | On Hold | 0201 |
| [0302](0302-restore-backup/spec.md) | Restore Backup | 4 | On Hold | 0301 |
| [0303](0303-manage-backups/spec.md) | Manage Backups | 4 | On Hold | 0301 |
| [0304](0304-device-transfer/spec.md) | Device-to-device transfer | 4 | On Hold | 0101 |
| [0305](0305-automatic-backup/spec.md) | Automatic backup | 4 | On Hold | 0301 |
| [0306](0306-backup-settings/spec.md) | Backup settings | 4 | On Hold | 0305 |
| [0307](0307-backup-triggers/spec.md) | Backup triggers | 4 | On Hold | 0305, 0306 |

### cloud-sync

| Spec | Title | Phase | Status | Depends on / Origin |
| --- | --- | --- | --- | --- |
| [0401](0401-realtime-cloud-sync/spec.md) | Real-Time Sync | 5 | On Hold | 0201 |
| [0402](0402-multi-device-sync/spec.md) | Multi-Device Sync | 5 | On Hold | 0401 |
| [0403](0403-cloud-conflict-resolution/spec.md) | Cloud conflict resolution | 5 | On Hold | 0401 |
| [0404](0404-offline-cloud-sync/spec.md) | Offline-First Sync | 5 | On Hold | 0401 |
| [0405](0405-cloud-family-sharing/spec.md) | Cloud family sharing | 5 | On Hold | 0201, 0401 |
