---
spec: "0012"
title: International dog and cat breed catalog
family: pet-care
phase: 1
status: Approved
owner: woliveiras
depends_on: ["0001", "0006", "0011"]
origin: prds/2026-07-17-petit-pet-health-management.md
---

# Spec: International dog and cat breed catalog

## Context and motivation

Spec 0011 centralized species-aware care choices but deliberately retained a
small hand-maintained breed list. That list omits many cat and dog breeds, has
no external identity, and cannot represent differing recognition across
registries.

Petit needs a searchable international breed catalog without adding a runtime
service or turning a registry classification into veterinary advice. Existing
Room, export, backup, Nearby, LAN, and custom values must remain compatible.

In this spec, "species" means `CAT` or `DOG`, while "breed" means a named
population within one of those species. Mixed breed, unknown breed, and custom
text are useful recording choices but are not represented as VBO breed
concepts.

## Source and inclusion policy

- Use the [Vertebrate Breed Ontology (VBO)](https://monarchinitiative.org/ontologies/vbo)
  as the identity spine for cat and dog breeds.
- Pin every generated catalog to a published VBO release and checksum.
- Include a dog breed for new selection when its VBO term has fully or
  partially recognized provenance from FCI.
- Include a cat breed for new selection when its VBO term has fully or
  partially recognized provenance from FIFe or TICA.
- Keep recognition status and authority separate. One authority's status must
  not be presented as universal recognition.
- Keep VBO terms that lack the configured authority provenance out of the
  new-selection list. Their names can still be preserved as manual or
  historical data.
- Record the source URL and review date for every registry cross-reference.
- Do not copy breed-standard prose or registry images into the generated
  catalog.
- Ship the VBO CC BY 4.0 attribution and the exact snapshot version in the app.

The generated artifact is a reviewed input to Petit, not a live mirror. A
catalog update requires a source diff, generated-data validation, and normal app
release review.

## Catalog model

Every catalog breed contains:

- a permanent `VBO:NNNNNNN` identifier;
- species (`CAT` or `DOG`);
- a canonical English fallback name;
- an English display name and a reviewed Brazilian Portuguese display name;
- searchable aliases where the source supplies them;
- zero or more source-specific registry codes and recognition statuses;
- source provenance;
- the VBO release that supplied the term.

Petit also owns these non-breed choices:

| ID | Meaning | Persisted fallback |
| --- | --- | --- |
| `PETIT:MIXED_BREED` | The animal has ancestry from more than one breed | existing `MIXED_BREED` key |
| `PETIT:UNKNOWN_BREED` | The caregiver does not know the breed | canonical English fallback |
| none | The caregiver enters exact custom text or leaves the field empty | exact text or `null` |

An app-owned category must never be labeled as recognized by VBO, FCI, FIFe, or
TICA.

## Offline artifact and update workflow

- Generate a deterministic, size-bounded JSON asset outside the Android runtime.
- Keep the source manifest, checksums, authority crosswalk, localized labels,
  and generated snapshot under version control.
- The generator must reject duplicate IDs, malformed namespaces, species
  mismatches, missing required display names, missing source provenance, and
  unsupported recognition states.
- The committed asset must be reproducible from the pinned inputs.
- The app must not require network permission, an account, or Petit-managed
  infrastructure to browse the catalog.
- If the asset cannot be loaded, manual entry and existing persisted values
  remain available.
- Updating VBO or an authority crosswalk is an explicit repository change. No
  runtime background refresh or silent remote update is allowed.

## Breed selection experience

- Replace the cat/dog breed dropdown with a searchable, accessible dedicated
  screen. Do not present the catalog in a dialog, modal sheet, or popup.
- Keep rabbit, bird, hamster, and `OTHER` breed behavior from spec 0011
  unchanged.
- Open the dedicated screen from the breed field while retaining the pet form
  and all unsaved form values on the navigation back stack.
- Treat selection on the dedicated screen as a draft. A catalog breed, mixed
  breed, unknown breed, manual value, or empty value changes the pet form only
  after the caregiver activates the explicit confirmation action.
- After confirmation, return to the same pet form with the confirmed ID,
  fallback, and localized display value applied together.
- Navigating back without confirmation discards changes made on the selector
  screen and leaves the pet form's previous breed value unchanged.
- Preselect the form's current breed when the dedicated screen opens. An
  unknown future ID or exact custom value must remain visible and selectable.
- Search the active species only.
- Match the localized display name, canonical name, and aliases without case or
  accent sensitivity.
- Sort results deterministically by the active locale's display name. Do not
  imply popularity through undocumented ranking.
- Keep mixed breed, unknown breed, and manual entry visible without requiring a
  search result.
- Show a clear empty state and retain the manual-entry action when no catalog
  result matches.
- Keep the search field and screen title visible while the result list scrolls.
  Keep the confirmation action reachable without scrolling to the end of the
  catalog.
- A species change clears an unsaved catalog selection that belongs to the
  previous species, but preserves a value loaded from an existing pet until the
  caregiver explicitly replaces or clears it.
- Registry recognition must not be displayed as health, behavior, suitability,
  purity, or pedigree verification.

## Persistence and compatibility

- Add optional `breedId` to the Room pet entity, domain model, and portable pet
  representation.
- A catalog selection stores its VBO ID in `breedId` and its canonical English
  name in the existing `breed` field as a portable fallback.
- A Petit-owned category stores its namespaced ID and documented fallback.
- Manual entry stores `breedId = null` and preserves `breed` exactly as entered,
  subject only to the existing field validation.
- Empty breed data stores both fields as `null`.
- UI display resolves a known `breedId` through the bundled locale data. If the
  ID is unknown, missing, or removed from a later catalog, display the persisted
  `breed` fallback without changing either field.
- The Room migration may backfill `breedId` only for exact, species-compatible
  legacy preset keys with a reviewed one-to-one mapping. It must not fuzzy-match
  user text or rewrite `breed`.
- Unknown future `breedId` values received through import or synchronization
  are preserved with their fallback instead of rejected.
- Add `breedId` as an optional field to schema version 1 exports. Existing
  readers already ignore unknown JSON fields, while new readers treat a missing
  `breedId` as `null`; therefore this additive field does not require an export
  schema-version bump.
- Preserve `breedId` and `breed` through Room, JSON export/import, portable
  backup/restore, Nearby transfer, LAN synchronization, and conflict
  resolution.
- Conflict handling treats `breedId` and `breed` as one pet-field decision. It
  must not combine an ID from one version with a fallback from another.

## Localization and accessibility

- English and Brazilian Portuguese catalog display names must be complete in
  the shipped snapshot. A proper name may intentionally be identical in both
  locales.
- Catalog names and aliases live in the versioned catalog locale data. Normal UI
  labels, guidance, errors, and accessibility text remain Android resources.
- Do not translate or normalize custom breed text.
- The searchable selector must expose its label, current selection, expanded
  state, result count or empty state, and manual-entry action to accessibility
  services.
- Keyboard and switch-access users must be able to search, choose a result,
  choose a non-breed category, enter a custom value, and clear a selection.

## Non-functional requirements

- Catalog lookup and search remain available offline.
- Loading and searching the catalog must not block the main thread.
- The committed snapshot has deterministic ordering and bounded size.
- Search results must remain responsive for the full shipped cat and dog
  catalog on the project's minimum supported Android version.
- Imported identifiers and aliases are validated as external input before use.
- Do not log pet names, custom breed text, or search text.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Source parsing, inclusion rules, IDs, authority status, locale lookup, normalized search, legacy mapping, unknown-ID fallback, and deterministic generation. |
| Integration | Room migration, mapper, JSON, archive, Nearby, LAN, and conflict round trips for VBO, Petit-owned, custom, missing, and unknown future IDs. |
| Instrumented | Dedicated searchable selector screen for cat and dog, explicit confirmation, back-without-confirmation, locale switching, empty search, manual entry, species changes, state restoration, navigation results, and accessibility semantics. |
| Manual | Physical-device review in English and pt-BR with TalkBack and a large catalog; two-device transfer remains a separately reported check. |

## Acceptance criteria

- [ ] Given the pinned sources, when the catalog is generated twice, then both
  outputs are byte-for-byte identical and record the same release, checksums,
  provenance, and attribution.
- [ ] Given a VBO dog term, when it lacks full or partial FCI recognition
  provenance, then it is not offered for new dog selection.
- [ ] Given a VBO cat term, when it lacks full or partial FIFe or TICA
  recognition provenance, then it is not offered for new cat selection.
- [ ] Given a catalog ID, when it is resolved in English or pt-BR, then the
  correct reviewed display name is returned without changing persisted data.
- [ ] Given a cat or dog search, when the caregiver types a canonical name,
  localized name, or alias with different case or accents, then matching breeds
  for that species are returned in deterministic locale order.
- [ ] Given no matching result, when the selector shows its empty state, then
  mixed breed, unknown breed, and manual entry remain available.
- [ ] Given the caregiver opens breed selection for a cat or dog, when
  navigation completes, then a dedicated full-screen destination is visible
  and the partially completed pet form remains on the back stack.
- [ ] Given a breed choice is highlighted on the dedicated screen, when the
  caregiver has not confirmed it, then the pet form's breed value remains
  unchanged.
- [ ] Given a catalog, mixed, unknown, manual, or empty choice, when the
  caregiver confirms it, then the selector returns to the same pet form and
  applies the complete confirmed value.
- [ ] Given the caregiver changes the draft choice and navigates back without
  confirmation, when the pet form resumes, then its prior breed value is
  unchanged.
- [ ] Given a catalog selection, when the pet is saved and reloaded, then its
  `breedId` and canonical fallback remain paired and the localized name is
  displayed.
- [ ] Given custom breed text, when the pet is saved, exported, restored,
  transferred, or synchronized, then `breedId` remains `null` and the text is
  preserved exactly.
- [ ] Given an existing exact legacy preset key, when the Room migration runs,
  then only a reviewed species-compatible one-to-one `breedId` is added and the
  existing `breed` value is unchanged.
- [ ] Given an unknown future `breedId` and fallback, when the record is
  imported or synchronized, then both are preserved and the fallback is
  displayed.
- [ ] Given two conflicting versions of a pet, when conflict resolution chooses
  one version, then `breedId` and `breed` both come from that version.
- [ ] Given a schema version 1 export with or without `breedId`, when either the
  current or updated compatible parser reads it, then the pet remains
  importable without losing the fallback.
- [ ] Given airplane mode and no account, when the caregiver opens and searches
  the catalog, then all catalog and manual-entry behavior remains available.
- [ ] Given TalkBack in English or pt-BR, when the caregiver searches, selects,
  clears, enters, or confirms a breed, then the dedicated screen, current draft
  selection, result state, confirmation action, and navigation outcome are
  announced with localized text.
- [ ] Given the app's legal or attribution screen, when the caregiver inspects
  catalog sources, then the VBO license, release, and configured registry
  sources are visible.

## Edge cases

- The same alias can identify different breeds or variants. Search may return
  each candidate but must not choose automatically.
- A breed can have different recognition states across authorities.
- A VBO term can be deprecated or replaced after it has been persisted.
- A known ID can arrive with an incorrect or localized fallback. Display uses
  the known catalog entry, but persistence remains unchanged until explicit
  selection or edit.
- A custom value can equal a catalog display name. It remains custom until the
  caregiver explicitly selects the catalog entry.
- An old client can edit a pet while ignoring `breedId`. The next synchronized
  complete pet version must not reattach a stale ID to changed breed text.
- Locale collation can change result order without changing catalog identity.
- The generated asset can be absent or malformed in a development build.

## Decisions

| Decision | Choice | Reasoning |
| --- | --- | --- |
| Identity | VBO permanent IDs | Gives breed concepts stable, source-backed identity across locales and registries. |
| Authority filter | FCI for dogs; FIFe or TICA for cats | Matches the approved international scope while retaining source-specific recognition. |
| Delivery | Reviewed, pinned offline snapshot | Preserves Petit's offline-first behavior and avoids runtime availability or unreviewed remote changes. |
| Persistence | Optional `breedId` plus existing `breed` fallback | Supports localization and identity without losing old-client display or custom values. |
| Export compatibility | Additive optional field in schema version 1 | Old parsing ignores the field and new parsing accepts its absence. |
| Search ranking | Locale alphabetical, no popularity score | Avoids an unsupported claim about breed popularity. |
| ADR | Required | Persisting external identifiers and source policy is hard to reverse and has viable alternatives. |

## Evidence and sources

- [VBO overview](https://monarchinitiative.org/ontologies/vbo)
- [VBO term metadata and recognition provenance](https://monarch-initiative.github.io/vertebrate-breed-ontology/ontologymodeling/metadata/)
- [OBO Foundry VBO entry and license](https://obofoundry.org/ontology/vbo.html)
- [FCI breed nomenclature](https://fci.be/en/Nomenclature/)
- [FIFe recognized breeds and EMS codes](https://fifeweb.org/cats/breeds/)
- [TICA breed standards](https://tica.org/breed-standards/)

These sources establish identifiers, names, codes, provenance, and registry
recognition. They do not establish an individual animal's breed, pedigree,
health, temperament, or care requirements.

## Out of scope

- Breed inference from a photo, DNA result, appearance, or free text.
- Pedigree verification or claims that an individual animal is purebred.
- Health, temperament, suitability, prevalence, popularity, or care advice by
  breed.
- Breed catalogs for rabbit, bird, hamster, or `OTHER`.
- Runtime downloads, automatic remote updates, or a Petit catalog service.
- Registry images or full breed-standard prose.
- Changes to vaccination or antiparasitic behavior from spec 0011.
