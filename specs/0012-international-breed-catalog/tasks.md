# Tasks: International dog and cat breed catalog

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec and ADR approved on 2026-07-26. Implementation may proceed.

## Tasks

- [x] **Generate a reviewed offline breed snapshot** (test-type: both)
  - blocked-by: spec and ADR approval
  - desired behavior: pinned VBO and registry inputs produce one deterministic,
    validated cat/dog asset with locale data, provenance, checksums, and
    attribution.
  - acceptance criteria: generation is reproducible; malformed IDs, duplicate
    IDs, unsupported recognition, species mismatches, missing provenance, and
    missing locale names fail generation.
  - test expectations: parser fixtures plus an integration check that regenerates
    the committed artifact.
  - verification: focused generator tests and byte-for-byte snapshot comparison.

- [x] **Search and resolve catalog identities offline** (test-type: unit)
  - blocked-by: reviewed offline breed snapshot
  - desired behavior: the domain catalog resolves VBO and Petit-owned IDs,
    searches only the active species across locale names and aliases, and falls
    back safely for unknown IDs.
  - acceptance criteria: case/accent normalization, locale ordering, ambiguous
    aliases, unknown IDs, non-breed categories, and malformed-asset fallback
    match the spec.
  - test expectations: exhaustive pure tests in English and pt-BR without
    Android or network dependencies.
  - verification: focused catalog repository and search tests.

- [x] **Persist identity without rewriting custom or legacy data** (test-type: both)
  - blocked-by: search and resolve catalog identities offline
  - desired behavior: optional `breedId` remains paired with the existing
    fallback, exact legacy keys receive only reviewed mappings, and custom text
    remains ID-less.
  - acceptance criteria: Room migration, create/edit, unknown future ID,
    species change, old-client fallback change, and custom value behavior match
    the spec.
  - test expectations: migration, mapper, repository, and ViewModel tests prove
    no fuzzy mapping or silent rewrite.
  - verification: focused Room migration and pet persistence tests.

- [x] **Preserve paired fields across sharing paths** (test-type: integration)
  - blocked-by: persist identity without rewriting custom or legacy data
  - desired behavior: `breedId` and `breed` survive schema version 1 JSON,
    archive, Nearby, LAN, and conflicts as one versioned decision.
  - acceptance criteria: old payload without ID, new payload with ID, unknown
    ID, custom text, tombstone, and conflicting pet versions remain compatible.
  - test expectations: round-trip fixtures at every existing serialization and
    transport boundary.
  - verification: focused export/import, archive, Nearby, LAN, and conflict
    suites.

- [x] **Let caregivers search and choose cat and dog breeds** (test-type: both)
  - blocked-by: offline search and paired persistence
  - desired behavior: an accessible searchable selector replaces the large
    dropdown for cats and dogs while other species retain manual entry.
  - acceptance criteria: locale search, deterministic results, selection,
    clearing, mixed/unknown/custom paths, empty state, species change, state
    restoration, and save/reload match the spec.
  - test expectations: ViewModel/state tests plus focused Compose tests for
    English and pt-BR.
  - verification: focused JVM and instrumented pet-form suites.

- [x] **Expose catalog provenance and license** (test-type: both)
  - blocked-by: reviewed offline breed snapshot
  - desired behavior: caregivers can inspect the VBO license, pinned release,
    and configured registry sources without copying protected standards or
    images.
  - acceptance criteria: attribution content is generated from snapshot
    metadata, localized UI copy is complete, and the packaged license is
    present.
  - test expectations: asset/package contract test and focused UI coverage.
  - verification: attribution contract and settings/about screen tests.

- [~] **Complete release-level catalog verification** (test-type: both)
  - blocked-by: all implementation tasks
  - desired behavior: automated and manual evidence covers the full offline,
    compatibility, localization, accessibility, and source-update story.
  - acceptance criteria: focused and nearest suites pass; temporary fixtures are
    removed; tasks and plan are reconciled; remaining lint debt or physical and
    two-device gaps are reported rather than inferred.
  - test expectations: no new test code unless a verification gap reveals a
    missing acceptance criterion.
  - verification: generator reproduction, `./gradlew test`,
    `./gradlew spotlessCheck`, `./gradlew lintDebug`, build/install as required,
    `git diff --check`, `git status --short`, physical TalkBack review, and
    separately reported two-device checks.
  - evidence: generator reproduction, JVM tests, formatting, Android-test
    compilation, Room migration, and focused Compose tests ran on the API 34
    emulator. `lintDebug` remains blocked by pre-existing repository debt;
    physical TalkBack and two-device validation remain open.
