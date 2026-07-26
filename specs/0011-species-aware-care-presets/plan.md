# Plan: Species-aware care presets

Spec: [spec.md](./spec.md)

## Status

This plan is **In Progress**. Automated implementation and focused emulator
coverage are complete; physical-device, two-device, and real-provider checks
remain separate.

## Dependencies

- Spec 0001 pet creation/editing and string persistence.
- Spec 0003 vaccination filtering, validation, and history.
- Spec 0004 deworming forms, validation, and history.
- Spec 0006 and backup/local-sharing paths for compatibility verification.
- Spec 0009 for localized visible and accessibility copy.

## Architecture

- Add a pure Kotlin `SpeciesCareCatalog` in the domain layer.
- Represent catalog choices independently from Android resource IDs and map
  their label keys in the presentation layer.
- Keep Room entities, export schema, and transport payloads unchanged.
- Treat legacy/custom persisted values as data, not failed catalog lookups.
- Keep all clinical catalog content static and offline.

## Implementation sequence

1. [x] Add failing exhaustive catalog and compatibility contract tests for all six
   species, manual paths, historical values, and interval safety.
2. [x] Implement the central catalog and integrate breed selection while preserving
   a breed across species changes until explicit user action.
3. [x] Route vaccination choices and validation through the catalog, preserving
   incompatible historical records while restricting new selections.
4. [x] Replace commercial antiparasitic menus with manual medication recording and
   remove automatic next-date inference from preset selection.
5. [x] Add JSON, archive/restore, Nearby, LAN, and conflict compatibility fixtures
   for known, custom, and uncatalogued legacy strings.
6. [x] Deliver the string-key manifest to the localization owner, then add focused
   Compose/accessibility coverage for every species and manual path.
7. [~] Run focused tests, the relevant JVM and instrumented suites, formatting, lint,
   and final diff/worktree checks.
   JVM, 27 focused instrumented tests, formatting, and diff checks pass. Lint
   still fails on the repository's existing debt, and physical/two-device checks
   remain pending.

## Ownership for implementation

- `species_catalog_agent`: domain catalog and pet/vaccination/deworming production
  integration. It must not edit XML resources.
- `localization_agent`: all default and pt-BR XML resources and approved
  localization production changes.
- `verification_agent`: assigned test-only files and independent verification.
- Orchestrator: specs, task state, overlap resolution, review, acceptance,
  verification, documentation, and Git.

## Risks and mitigations

- **Silent data loss:** preserve raw legacy/custom values and test every transport.
- **Clinical overreach:** ship no brand/ingredient list or automatic interval.
- **Localized-value persistence:** keep stable keys or exact manual text; never
  persist a translated display label as a catalog identifier.
- **Old-version compatibility:** do not introduce new persisted enum names.
- **Agent overlap:** resource files belong only to the localization agent; new
  keys are transferred through a manifest.

## Verification

1. Run focused catalog, ViewModel, serialization, archive, and LAN JVM tests.
2. Run focused Compose tests when an emulator/device is available.
3. Run `./gradlew test`, `./gradlew spotlessCheck`, `./gradlew lintDebug`, and
   `git diff --check`.
4. If `assembleDebug` is run, immediately run `installDebug`.
5. Report physical-device, two-device, and real-provider checks separately.
