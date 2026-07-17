---
spec: "0001"
title: Pet management
family: pet-care
status: Implemented
owner: woliveiras
depends_on: []
---

# Spec: Pet management

## Context and motivation

The pet owner needs to register and maintain each pet's data to track its individual health history.

## Functional requirements

- Register, list, view, edit, and soft-delete a pet.
- Require a name (1–50 characters) and type; accept a photo, non-future birth date, sex, breed, color, microchip, passport, and notes.
- Support cat, dog, rabbit, bird, hamster, and other.
- Display profile shortcuts for vaccinations, weight, deworming, and sharing.
- Persist data locally and hide records with a populated `deletedAt`.

## Acceptance criteria

- Given a valid name and type, When the pet owner saves, Then the pet appears in the list after the app restarts.
- Given an empty name, When the pet owner tries to save, Then they see “Name is required” and nothing is persisted.
- Given an existing pet, When the pet owner edits and saves it, Then its fields and `updatedAt` are updated.
- Given an existing pet, When the pet owner confirms deletion, Then the pet disappears from active queries and retains a populated `deletedAt`.
- Given the picker or camera, When the pet owner provides a JPG or PNG image up to 5 MB and saves, Then the image is available in the profile.

## Test strategy

Unit tests cover validation and mappings; integration tests cover Room, CRUD, persistence, and soft delete; UI tests cover the form and navigation.

## Edge cases

- Reject future birth dates and field values that exceed their limits.
- Preserve deleted pets in the database without displaying them in active queries.
- Handle missing or lost access to the photo URI without corrupting other data.

## Known limitation

The app selects images from the gallery, but camera capture and explicit validation of the 5 MB limit have not yet been verified in the implementation.

## Out of scope

- Cross-device synchronization and profile sharing.
