---
spec: "0007"
title: Home dashboard
family: pet-care
status: In Progress
owner: woliveiras
depends_on: ["0001", "0002", "0003", "0004", "0005"]
---

# Spec: Home dashboard

## Context and motivation

The caregiver needs a quick view of the pets' health and the care that requires attention.

## Current state

The home screen supports the empty state, pet cards, upcoming tasks, recent activity, Quick Add, settings navigation, and refresh. Overall health is calculated in the view model but is not carried into or displayed by the pet cards.

## Functional requirements

- Display a welcome message and an action to add the first pet when there is no data.
- Display pet cards with photo, name, latest weight, overall status, and next event.
- Display up to five upcoming tasks and recent activity, with access to the full lists.
- Open the profile when the pet is tapped and provide Quick Add for weight, vaccination, deworming, task, and a new pet.
- Provide access to settings and manual refresh.

## Acceptance criteria

- Given there are no pets, When the home screen opens, Then the user sees a welcome message and a button to add the first pet.
- Given there are pets, When the home screen opens, Then the user sees an individual summary and visual health indicator.
- Given there are tasks, Then the user sees up to five upcoming tasks and can open the full list.
- Given there are health events, Then the user sees recent activity and can open the full timeline.
- When a pet or quick action is tapped, Then the app navigates to the profile or correct flow, prompting for pet selection when necessary.

## Test strategy

Unit tests cover aggregation and the empty state; integration tests cover repositories and navigation; UI tests cover states, Quick Add, refresh, and accessibility.

## Known limitations

- Pet cards do not display the calculated overall health status.
- The “All good” banner and a separate alerts section have not been implemented.
- Quick Add replaced the original Speed Dial.
