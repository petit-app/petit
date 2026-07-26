---
spec: "0011"
title: Species-aware care presets
family: pet-care
phase: 1
status: In Progress
owner: woliveiras
depends_on: ["0001", "0003", "0004"]
origin: prds/2026-07-17-petit-pet-health-management.md
---

# Spec: Species-aware care presets

## Context and motivation

Petit supports cats, dogs, rabbits, birds, hamsters, and an `OTHER` escape
hatch, but preset behavior is inconsistent. Breed lists are embedded in the pet
form, vaccine filtering is embedded in `VaccineType`, and deworming offers the
same mixture of commercial brands and one active ingredient to every species.
The forms also prefill care intervals that are not derived from the animal's
individual risk, jurisdiction, product label, or veterinary instruction.

The caregiver needs convenient recording options without receiving an implied
diagnosis or prescription. The implementation must centralize species-aware
choices while preserving every legacy and custom string already stored or
transported by Room, JSON export/import, backup/restore, Nearby transfer, and
LAN synchronization.

## Supported species and initial catalog policy

No new species are introduced.

| Species | Breed presets for new selection | Vaccine presets for new records | Antiparasitic medication presets |
| --- | --- | --- | --- |
| `CAT` | Existing cat keys, mixed breed, manual Other | Existing feline types, rabies, manual Other | Manual entry only |
| `DOG` | Existing dog keys, mixed breed, manual Other | Existing canine types, rabies, manual Other | Manual entry only |
| `RABBIT` | Manual entry only | RHDV, myxomatosis, manual Other | Manual entry only |
| `BIRD` | Manual entry only | Avian polyomavirus, manual Other | Manual entry only |
| `HAMSTER` | Manual entry only | Manual Other only | Manual entry only |
| `OTHER` | Manual entry only | Manual Other only | Manual entry only |

Existing values excluded from a new-selection catalog remain historical or
custom data. They are not deleted, rewritten, or hidden.

## Functional requirements

- Provide one pure, testable domain catalog for breed, vaccine, and
  antiparasitic recording options by `PetType`.
- Keep `CAT`, `DOG`, `RABBIT`, `BIRD`, `HAMSTER`, and `OTHER` as the complete
  supported-species set.
- Keep a visible manual-entry path for breed, vaccine, and medication for every
  species.
- Reuse the existing `VaccineType` values for supported named vaccines; do not
  add persistence-sensitive enum values in this spec.
- Restrict new vaccine selection to the matrix above. In particular, rabies is
  not a universal new-entry preset for rabbits, birds, hamsters, or `OTHER`.
- Replace the current cross-species commercial medication menus with manual
  medication entry. A future named medication catalog requires a separately
  approved country scope, veterinary review, and source/update policy.
- Do not prefill a next vaccination or antiparasitic date from a catalog in the
  initial version. Caregivers may record a date supplied by their veterinarian
  or product label.
- When the caregiver changes a pet's species, preserve the current breed as an
  editable custom value until they explicitly replace or clear it.
- Keep a historical vaccine that is incompatible with the pet's current species
  visible and editable without silently changing its type. New or changed
  selections must satisfy the current species catalog.
- Display an advisory explanation that presets facilitate record entry and do
  not replace veterinary guidance.

## Veterinary safety policy

- Never calculate or suggest dosage, route, diagnosis, indication, or treatment.
- Never infer that a brand, active ingredient, vaccine, or schedule is safe for
  an individual animal.
- Do not mix commercial brands, active ingredients, and generic categories in
  one unlabeled catalog.
- Treat product availability and authorization as country-specific.
- Add a named clinical preset or suggested interval only after its species,
  market, source version, reviewer, and review date are documented and approved.
- Preserve names entered from a veterinarian or product label exactly as user
  data, including brands and proper nouns.

## Compatibility and persistence

- Keep the current persisted fields and formats: `petType`, `breed`,
  `vaccineType`, `customVaccineTypeName`, deworming `type`, and `medication`.
- Do not persist a new preset identifier and do not require a Room migration.
- Preserve uncatalogued and custom breed and medication strings byte-for-byte through
  edit, Room mapping, JSON export/import, portable backup/restore, Nearby
  transfer, LAN synchronization, and conflict resolution.
- Preserve `VaccineType.OTHER` plus `customVaccineTypeName` for custom vaccines.
- Do not add new persisted vaccine enum names without a separate backward- and
  forward-compatibility design.
- Do not rewrite old incompatible values during species changes, import,
  restore, transfer, or synchronization.

## Non-functional requirements

- Keep the catalog pure Kotlin and independent of Compose, Android resources,
  Room, and network access.
- Keep Room as the local source of truth and all catalog behavior available
  offline.
- Use resource keys for every visible label, warning, and accessibility string.
- Keep lookup order deterministic and make every species mapping exhaustive.
- Do not log clinical values, medication names, or pet names as part of catalog
  selection.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Exact six-species matrices, manual paths, stable stored values, incompatible historical vaccines, species changes, and absence of automatic intervals. |
| Integration | Room and JSON round trips, portable archive/restore, Nearby/LAN transport, and conflict payloads with known, custom, and unknown legacy values. |
| Instrumented | Pet, vaccination, and deworming forms for all six species, manual entry, species changes, legacy edit behavior, localization, and accessibility. |
| Manual | Physical-device form review; two-device transfer remains separate and must not be claimed without execution. |

Every changed production behavior receives a test. Catalog tests must enumerate
all six `PetType` values so adding a species cannot silently inherit a default.

## Acceptance criteria

- [x] Given any supported species, when a form requests preset choices, then it
  receives only the deliberate choices in the matrix and a manual-entry path.
- [x] Given rabbit, bird, hamster, or `OTHER`, when a new vaccination is created,
  then rabies is not offered as a universal preset.
- [x] Given any species and deworming category, when medication is recorded, then
  the app requests the actual medication manually and offers no cross-species
  commercial-product recommendation.
- [x] Given a breed and a species change, when the existing value is not in the
  new catalog, then it remains visible and editable until explicitly replaced or
  cleared.
- [x] Given a historical vaccine that is now incompatible, when it is opened,
  then the recorded value remains visible and a save without changing that value
  does not silently rewrite or delete it.
- [x] Given a new or changed incompatible vaccine selection, when the caregiver
  saves, then validation rejects it.
- [x] Given any preset selection, when the form updates, then no next date,
  dosage, diagnosis, or treatment is inferred by the catalog.
- [x] Given known, custom, and uncatalogued legacy strings, when they pass through Room,
  JSON, backup/restore, Nearby, or LAN, then their persisted representation is
  preserved.
- [~] Given English or Brazilian Portuguese, when the forms and advisory copy are
  rendered, then every visible and accessibility string uses that locale.
  Automated resource, JVM, and focused Compose checks pass; a physical-device
  pt-BR and TalkBack review remains pending.

## Edge cases

- A custom value can equal a localized label while differing from the stable
  legacy key.
- A language change must not change persisted breed or medication data.
- A pet can change species after historical care records already exist.
- Imported data may contain custom or uncatalogued strings from another version;
  those strings remain data even when they are not offered as new presets.
- Unsupported persisted enum names continue to follow the existing versioned
  import contract; this spec does not add a new enum or promise a new fallback.
- `OTHER` species and manual `OTHER` option are distinct concepts and must not be
  conflated in validation or UI state.

## Decisions

| Decision | Choice | Reasoning |
| --- | --- | --- |
| Architecture | Pure static `SpeciesCareCatalog` | Centralizes policy without network, Room, or Compose coupling. |
| Species | Existing six only | Generalization does not require inventing new species. |
| Persistence | Keep current strings and enum names | Avoids a Room migration and preserves transport compatibility. |
| Breed scope | Existing cat/dog presets; manual-only for other species | No new breed list is added without a traceable product decision. |
| Vaccine scope | Existing named types with explicit species mapping; manual Other always | Refactors current behavior while removing an unsafe universal fallback. |
| Medication scope | Manual entry only | International brand/ingredient suitability cannot be inferred safely. |
| Suggested intervals | None in the initial catalog | Scheduling depends on veterinary/product guidance and individual risk. |
| Historical incompatibility | Preserve and allow unchanged edit with warning | Health history must not be silently destroyed after a species/catalog change. |
| ADR | Not required | The choice is reversible static policy with unchanged persistence. |

## Evidence and sources

- [WSAVA vaccination guidelines](https://wsava.org/Global-Guidelines/Vaccination-Guidelines/)
  cover dog and cat vaccination and are guidance for veterinary professionals;
  they do not justify one global all-species vaccine catalog.
- [ESCCAP GL1](https://www.esccap.org/guidelines/gl1/) and its
  [individual deworming guidance](https://www.esccap.org/deworming-cats/)
  make parasite control dependent on local legislation, epidemiology, individual
  risk, and veterinary advice.
- [Rabbit Welfare Association vaccination guidance](https://rabbitwelfare.co.uk/welfare-need/vaccines/)
  shows that rabbit vaccine availability and protocols change and require
  current veterinary review.

These sources support the safety boundary and conservative manual path. They do
not approve a product, dose, diagnosis, or individual schedule in Petit.

## Out of scope

- New species.
- Dosage, diagnosis, treatment recommendation, contraindication, or prescribing.
- A downloaded or remotely updated catalog.
- A country-specific brand or active-ingredient catalog.
- A Room migration or new export schema version.
- Rewriting historical records.
