---
spec: "0002"
title: Weight tracking
family: pet-care
status: Implemented
owner: woliveiras
depends_on: ["0001"]
---

# Spec: Weight tracking

## Context and motivation

The pet owner needs to record weight over time to identify changes in the pet's health.

## Functional requirements

- Record the date, weight in kg or g, and an optional note.
- Normalize persistence to grams and keep at most one active weight entry per pet per day.
- List the history by descending date and display a bar chart of weight changes.
- Allow editing and soft deletion of a weight entry.
- Reject weights less than or equal to zero, weights over 50 kg, and future dates.

## Acceptance criteria

- Given `3.5 kg` or `350 g`, When the pet owner saves, Then 3500 or 350 grams are persisted, respectively.
- Given a weight entry for the same pet and date, When another is saved, Then the previous entry is replaced.
- Given several weight entries, When the pet owner opens the screen, Then they see the history in descending order and a chart with dates and kg values.
- Given an invalid value or future date, When the pet owner saves, Then they see an error and nothing is persisted.
- Given a weight entry, When the pet owner edits or deletes it, Then the list and chart reflect the change, and deletion is soft.

## Test strategy

Unit tests cover conversion and validation; integration tests cover daily uniqueness, Room, sorting, and soft delete; UI tests cover the form and chart.

## Out of scope

- Clinical diagnosis or automatic recommendation of an ideal range.
