---
spec: "0004"
title: Deworming records
family: pet-care
status: Implemented
owner: woliveiras
depends_on: ["0001"]
---

# Spec: Deworming records

## Context and motivation

The caregiver needs to record dewormers and external antiparasitic treatments to keep the pet's protection up to date.

## Functional requirements

- Record type `INTERNAL`, `EXTERNAL`, or `BOTH`, medication, administration date, next dose, and notes.
- Require a medication, prevent future administration dates, and require the next dose to be after administration.
- Calculate `OK`, `SCHEDULED`, or `OVERDUE` for each record.
- List history by date, with visual indicators, editing, and soft delete.
- Count `BOTH` in the internal and external categories when the category view is available.

## Acceptance criteria

- Given valid internal, external, or combined records, When they are saved, Then the correct type is persisted and the status is calculated.
- Given a next dose in five days, Then the indicator is `SCHEDULED`; Given an overdue dose, Then it is `OVERDUE`.
- Given records of different types, When the history is opened, Then they appear in descending order.
- Given type `BOTH`, When health by category is calculated, Then it counts as both internal and external.
- Given a record, When it is edited or deleted, Then the screen reflects the change and deletion is logical.

## Test strategy

Unit tests cover status and categories; integration tests cover Room, ordering, and soft delete; UI tests cover the form and indicators.

## Known limitation

The current API calculates status per record; visual separation and aggregate calculation by category remain pending.
