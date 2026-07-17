---
spec: "0003"
title: Vaccination records
family: pet-care
status: In Progress
owner: woliveiras
depends_on: ["0001"]
---

# Spec: Vaccination records

## Context and motivation

The pet owner needs to maintain the vaccination schedule and traceability of the pet's doses.

## Current state

Vaccination records can be created, edited, listed, and soft-deleted. Status is calculated from the next-dose date, but the form does not yet require a name for `OTHER`, and the history UI only renders an explicit visual state for overdue doses.

## Functional requirements

- Record the vaccine type filtered by species, administration date, next dose, and optional veterinarian, clinic, batch, and note details.
- Require a custom name for the `OTHER` type.
- Calculate `OK` when there is no next dose or it is more than 30 days away, `SCHEDULED` when it is 0–30 days away, and `OVERDUE` when past due.
- Group history by type and display a visual status.
- Allow editing and soft deletion.

The catalog includes feline vaccines (`V3`, `V4`, `V5`, `FELV`, `FIV`), canine vaccines (`DHPP`, `BORDETELLA`, `LEPTOSPIROSIS`, `LEISHMANIA`, `GRIPE_CANINA`), rabbit vaccines (`RHDV`, `MYXOMATOSIS`), bird vaccines (`POLYOMAVIRUS`), and the general options `RABIES` and `OTHER`.

## Acceptance criteria

- Given a valid vaccination, When the pet owner saves it, Then it appears in the history and its status is calculated.
- Given a next dose in five days, Then the status is `SCHEDULED`; Given a past-due date, it is `OVERDUE`.
- Given a vaccine without a next dose, Then the status is `OK` and no next dose is displayed.
- Given multiple doses of the same type, Then the summary identifies the latest dose and the history preserves every administration.
- Saved optional traceability details are available in the detail view.

## Test strategy

Every changed production behavior receives a unit test. Unit tests cover status
boundaries, validation, species filtering, edit timestamps, and grouping with a
controlled clock. Room integration tests cover persistence, latest-dose
selection, complete history, and soft delete. Compose tests cover the `OTHER`
form branch and the three visible status states. An E2E test is added only if
the complete save-to-history journey is not already covered at those boundaries.

## Edge cases

- The next dose must be after the administration date; the administration date cannot be in the future.
- Rabies and `OTHER` are general options; all other types must match the pet's species.
- The custom name for `OTHER` is trimmed and must contain visible characters.
- Changing from `OTHER` to a catalog type does not persist the custom name.

## Decisions

- Date-dependent validation and status calculation use an injectable clock.
- The main summary is grouped by vaccine type and shows its latest dose; the
  complete chronological history remains accessible without dropping older doses.
- `OK`, `SCHEDULED`, and `OVERDUE` always use an explicit text and visual indicator.

## Known limitations

- The form accepts `OTHER` without the required custom name.
- The history only highlights `OVERDUE`; `OK` and `SCHEDULED` are not displayed as visual states.
- The full history is grouped by month rather than by vaccine type.
