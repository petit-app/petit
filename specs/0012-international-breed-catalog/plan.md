# Plan: International dog and cat breed catalog

Spec: [spec.md](./spec.md)

## Status

This plan is **Approved**. The user approved the dedicated confirmation screen
revision on 2026-07-26.

## Dependencies

- Spec 0001 pet creation, editing, Room persistence, and validation.
- Spec 0006 schema version 1 JSON export and import.
- Spec 0011 species-aware catalog behavior and legacy breed keys.
- Existing portable archive, Nearby, LAN, and conflict-resolution paths.
- ADR 0001 for the VBO identity and snapshot decision.

## Architecture

- Generate a small deterministic JSON catalog from pinned, reviewed sources.
- Load the committed asset through a domain-facing catalog repository that has
  no runtime network dependency.
- Keep source parsing and generation outside the Android runtime.
- Add optional `breedId` beside the existing fallback/custom `breed` value.
- Resolve known IDs to locale data at presentation time.
- Preserve unknown IDs and exact manual values at every persistence and
  transport boundary.

## Implementation sequence

1. [x] Accept the proposed ADR and spec, then commit the documentation boundary.
2. [x] Add a reproducible VBO and authority-crosswalk generator with fixture
   tests, validation, pinned checksums, attribution, and a generated snapshot.
3. [x] Add catalog loading, locale resolution, search, Petit-owned categories,
   and exact legacy-key mapping through vertical domain tests.
4. [x] Add `breedId` to the domain and Room model with an additive migration and
   paired-field invariants.
5. [x] Preserve the optional ID and fallback through JSON, portable archive,
   Nearby, LAN, and conflict resolution without changing schema version 1.
6. [x] Replace cat/dog breed dropdown behavior with the searchable accessible
   selector while leaving other species unchanged.
7. [x] Add source and license attribution, finish English and pt-BR catalog
   review, and validate failure fallback.
8. [~] Run focused, nearest-suite, formatting, lint, build/install, physical
   accessibility, and separately scoped two-device checks.
9. [ ] Replace the dialog with a dedicated breed-selection navigation
   destination. Keep the pet form on the back stack, return confirmed choices
   through navigation state, and discard unconfirmed drafts on back.

## Commit boundaries

The user authorized local commits, one per important change. The intended
boundaries are:

1. approved PRD, spec, plan, tasks, index, and ADR;
2. reproducible catalog source pipeline and snapshot;
3. domain catalog, Room migration, and persistence compatibility;
4. searchable localized and accessible UI;
5. transport, attribution, and final verification adjustments if they are not
   already contained in the preceding vertical changes.
6. dedicated breed-selection screen and removal of the dialog.

No push, amend, rebase, merge, force-push, or pull request is authorized.

## Risks and mitigations

- **Registry disagreement:** retain authority and status provenance instead of
  flattening recognition into one boolean.
- **External source drift:** pin releases and checksums; update through reviewed
  repository changes only.
- **Data loss:** keep `breed` as fallback and pair it atomically with `breedId`.
- **Incorrect legacy mapping:** backfill exact reviewed keys only; never use
  fuzzy inference.
- **Old-client edits:** invalidate a stale ID when the paired fallback changes.
- **Localization gaps:** require complete English and pt-BR display names in the
  generator validation.
- **Catalog failure:** keep current value and manual entry available when the
  bundled asset fails to load.
- **Form state loss during navigation:** keep the existing form ViewModel and
  saved state on the back stack; return only an explicitly confirmed selection.
- **Accidental selection:** keep screen choices as drafts until confirmation;
  back navigation does not publish a result.
- **License misuse:** ship attribution from VBO and exclude registry prose and
  images.

## Verification

1. Run generator fixtures and reproduce the committed snapshot byte for byte.
2. Run focused catalog, search, migration, mapper, serialization, archive,
   Nearby, LAN, and conflict tests.
3. Run focused Compose tests on an emulator or device.
4. Run `./gradlew test`, `./gradlew spotlessCheck`, `./gradlew lintDebug`, and
   `git diff --check`.
5. If `assembleDebug` is run, immediately run `installDebug`.
6. Review the selector on a physical device in English and pt-BR with TalkBack.
7. Report two-device transport separately and do not infer it from JVM or
   single-device evidence.
