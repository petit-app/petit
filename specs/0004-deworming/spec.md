---
spec: "0004"
title: Deworming records
family: pet-care
status: In Progress
owner: woliveiras
depends_on: ["0001"]
---

# Spec: Deworming records

## Context and motivation

The caregiver needs to record dewormers and external antiparasitic treatments to keep the pet's protection up to date.

## Current state

Deworming records can be created, edited, listed in deterministic descending
date order, and soft-deleted. Status is clock-controlled and rendered with an
accessible visual indicator. Internal and external summaries select the latest
applicable treatment independently, counting `BOTH` in both categories.

Beta feedback showed a discoverability gap: every entry point outside the form
is named "dewormer" (quick add, speed dial, pet card, alerts, and reminder
settings), so caregivers looking for a flea and tick record do not realize the
`EXTERNAL` option lives inside that form.

## Functional requirements

- Record type `INTERNAL`, `EXTERNAL`, or `BOTH`, medication, administration date, next dose, and notes.
- Require a medication, prevent future administration dates, and require the next dose to be after administration.
- Calculate `OK`, `SCHEDULED`, or `OVERDUE` for each record.
- List history by date, with visual indicators, editing, and soft delete.
- Count `BOTH` in the internal and external categories when the category view is available.
- Name the feature after antiparasitic treatment, not after the internal
  dewormer, in every entry point outside the form.
- Offer internal dewormer and flea and tick treatment as separate entry points
  that open the form with the matching type preselected.

## Acceptance criteria

- Given valid internal, external, or combined records, When they are saved, Then the correct type is persisted and the status is calculated.
- Given a next dose in five days, Then the indicator is `SCHEDULED`; Given an overdue dose, Then it is `OVERDUE`.
- Given records of different types, When the history is opened, Then they appear in descending order.
- Given type `BOTH`, When health by category is calculated, Then it counts as both internal and external.
- Given a record, When it is edited or deleted, Then the screen reflects the change and deletion is logical.
- Given the quick-add menu, When the caregiver looks for a flea and tick record, Then an entry names flea and tick treatment and opens the deworming form with `EXTERNAL` preselected.
- Given the same menu, When the caregiver looks for an internal dewormer, Then a separate entry opens the form with `INTERNAL` preselected.
- Given a preselected type, When the caregiver changes it before saving, Then the chosen type is persisted.
- Given any entry point outside the form, When it is rendered in a supported language, Then its label names antiparasitic treatment rather than the internal dewormer alone.

## Test strategy

Every changed production behavior receives a unit test. Unit tests cover status,
validation, category projection, `BOTH`, and deterministic latest-record
selection with a controlled clock. Room tests cover ordering and soft delete;
Compose tests cover the form, category summaries, and all status indicators.

## Edge cases

- The category summary for `INTERNAL` considers `INTERNAL` and `BOTH`; the
  `EXTERNAL` summary considers `EXTERNAL` and `BOTH`.
- The latest applicable administration wins. Equal dates are resolved
  deterministically by `updatedAt`, then by ID.
- A record without a next dose has status `OK`.

## Decisions

- Category summaries are displayed above the descending chronological history.
- Date-dependent behavior uses an injectable clock.
- Every status is represented by text and a visual indicator, not color alone.
- The user-facing vocabulary is antiparasitic treatment, with internal dewormer
  and flea and tick treatment as its two named types. The internal identifiers
  stay `deworming` and `INTERNAL`/`EXTERNAL`/`BOTH`, so this is a strings and
  navigation change with no data migration.

## Known limitation

- Status shown by an already-open screen is refreshed when the screen is recreated; it does not run a midnight ticker.
