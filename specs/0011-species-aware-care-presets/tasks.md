# Tasks: Species-aware care presets

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec approved on 2026-07-20; implementation status: **In Progress** pending
> physical-device accessibility validation.

## Tasks

- [x] **Centralize deliberate choices for all six species** (test-type: unit)
  - blocked-by: spec approval
  - desired behavior: a pure catalog returns the exact breed, vaccine, and
    manual medication behavior documented for every `PetType`.
  - acceptance criteria: exhaustive mapping, deterministic order, manual path,
    and no clinical interval or commercial medication recommendation.
  - test expectations: demonstrate RED before implementation, then cover every
    species and fail when a future species lacks an explicit mapping.
  - verification: `./gradlew testDebugUnitTest --tests '*SpeciesCareCatalogTest'`

- [x] **Preserve breed values across species-aware pet editing** (test-type: both)
  - blocked-by: centralize deliberate choices for all six species
  - desired behavior: forms use the central breed catalog and retain any current
    legacy/custom value until explicit replacement or clearing.
  - acceptance criteria: cat/dog presets, manual-only species, Other entry, and
    species-change behavior match the spec without changing persistence.
  - test expectations: ViewModel and Compose coverage for all six species and a
    custom legacy breed.
  - verification: focused JVM and pet-form Compose tests

- [x] **Apply the catalog to vaccination without losing history** (test-type: both)
  - blocked-by: centralize deliberate choices for all six species
  - desired behavior: new choices are species-compatible while historical
    incompatible and custom records remain readable and unchanged.
  - acceptance criteria: exact vaccine matrix, unchanged historical edit,
    incompatible-new-selection rejection, manual Other, and no inferred due date.
  - test expectations: catalog, validation, ViewModel, Room, and Compose tests.
  - verification: focused vaccination JVM and Compose tests

- [x] **Make antiparasitic recording conservative and species-deliberate** (test-type: both)
  - blocked-by: centralize deliberate choices for all six species
  - desired behavior: remove the universal commercial menu, keep treatment
    category and manual medication entry, and stop preset-driven due-date inference.
  - acceptance criteria: all species have a deliberate manual path; legacy brand
    and ingredient strings remain visible/editable and are never rewritten.
  - test expectations: ViewModel, validation, and Compose tests for each treatment
    category, species, and legacy/custom medication.
  - verification: focused deworming JVM and Compose tests

- [x] **Prove persisted and transported compatibility** (test-type: both)
  - blocked-by: pet, vaccination, and antiparasitic integration
  - desired behavior: known, custom, and uncatalogued legacy strings survive Room,
    JSON, backup/restore, Nearby, LAN, and conflict resolution.
  - acceptance criteria: no schema migration, new enum value, translated-label
    persistence, silent fallback loss, or transport rewrite.
  - test expectations: unit/Room/archive/LAN fixtures cover all six species.
  - verification: focused export/import, archive/restore, Nearby, and LAN tests

- [~] **Verify localized and accessible species-aware forms** (test-type: integration)
  - blocked-by: previous tasks; approved 0009 localization work
  - desired behavior: English and pt-BR forms, warnings, manual fields, and
    content descriptions use resources and remain accessible.
  - acceptance criteria: no English fallback in pt-BR and no untranslated label
    is persisted as domain data.
  - test expectations: targeted locale-configured Compose tests and manual review.
  - verification: 27 focused instrumentation tests, `./gradlew test`, and
    `./gradlew spotlessCheck` pass; physical-device pt-BR/TalkBack review remains
    pending, and lint retains repository-level failures.
