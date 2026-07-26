---
status: accepted
date: 2026-07-26
---

# Use VBO identifiers for cat and dog breed identity

## Context and Problem Statement

Petit's small localized key list cannot identify the same breed across
languages or reconcile different registry names. How should Petit identify cat
and dog breeds while remaining offline, preserving custom data, and recording
where recognition claims came from?

## Decision Drivers

* Stable identity across English and Brazilian Portuguese
* Explicit provenance for registry names, codes, and recognition
* Offline use without a Petit-managed service
* Backward compatibility with exact custom and legacy breed strings
* An open license suitable for a committed derived catalog

## Considered Options

* Continue maintaining Petit-only breed keys and labels
* Use FCI, FIFe, and TICA identifiers directly
* Use Wikidata identifiers and multilingual labels
* Use VBO identifiers with source-specific registry metadata

## Decision Outcome

Chosen option: "Use VBO identifiers with source-specific registry metadata",
because VBO provides permanent breed IDs, synonyms, provenance, and recognition
annotations under CC BY 4.0. FCI, FIFe, and TICA remain authorities for
inclusion and source-specific status, while VBO provides one identity layer.

Petit will persist an optional VBO ID beside the existing canonical-name or
custom-text fallback. The app will consume a pinned, reviewed snapshot and will
not query VBO or a registry at runtime.

### Consequences

* Good, because localized labels and aliases no longer define persistence
  identity.
* Good, because registry disagreement can be represented without inventing one
  universal recognition status.
* Good, because catalog use stays offline and does not create hosted
  infrastructure cost.
* Good, because VBO attribution, release, and source provenance can be audited.
* Bad, because Petit must maintain generation, localization, crosswalk review,
  and source-update tooling.
* Bad, because VBO can include concepts outside Petit's configured recognition
  policy and cannot be consumed without filtering.
* Bad, because a persisted external namespace is costly to replace if VBO
  stewardship or licensing changes.
* Bad, because registry prose and images have separate rights and cannot be
  assumed reusable from VBO's metadata license.

### Confirmation

Automated checks reproduce the pinned snapshot, validate VBO namespaces and
source provenance, and reject catalog entries that do not satisfy spec 0012.
Persistence and transport tests verify that known, unknown, custom, and legacy
values keep their ID and fallback pairing.

## Pros and Cons of the Options

### Continue Petit-only keys

* Good, because implementation and licensing remain simple.
* Good, because every entry is directly curated for the product.
* Bad, because coverage, synonyms, and cross-registry identity remain manual.
* Bad, because adding languages multiplies identity-like local keys.

### Use registry identifiers directly

* Good, because each identifier comes from the recognizing authority.
* Good, because registry codes are familiar on pedigree documents.
* Bad, because FCI, FIFe, and TICA do not provide one shared namespace.
* Bad, because the same breed can have different names, codes, variants, and
  statuses across registries.

### Use Wikidata identifiers

* Good, because Wikidata has a multilingual, CC0 knowledge graph and stable
  item identifiers.
* Good, because its query interface supports broad discovery.
* Bad, because community graph shape and completeness are not specific to
  veterinary breed interoperability.
* Bad, because registry recognition and its provenance would require additional
  modeling and review.

### Use VBO identifiers with registry metadata

* Good, because VBO is designed as a computable source for vertebrate breed
  names and veterinary data interoperability.
* Good, because terms can carry synonyms, codes, status, and source
  annotations.
* Bad, because inclusive VBO coverage requires a Petit-specific authority
  filter.
* Bad, because Brazilian Portuguese display names still require review.

## More Information

* [Spec 0012](../../specs/0012-international-breed-catalog/spec.md)
* [VBO overview](https://monarchinitiative.org/ontologies/vbo)
* [VBO metadata model](https://monarch-initiative.github.io/vertebrate-breed-ontology/ontologymodeling/metadata/)
* [OBO Foundry VBO entry](https://obofoundry.org/ontology/vbo.html)
* [FCI breed nomenclature](https://fci.be/en/Nomenclature/)
* [FIFe recognized breeds](https://fifeweb.org/cats/breeds/)
* [TICA breed standards](https://tica.org/breed-standards/)

Revisit this decision if VBO stops publishing versioned artifacts, changes to
an incompatible license, loses source provenance required by the inclusion
policy, or no longer has active stewardship.
