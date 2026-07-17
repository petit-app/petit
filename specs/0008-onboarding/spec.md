---
spec: "0008"
title: Onboarding
family: pet-care
status: Implemented
owner: woliveiras
depends_on: []
---

# Spec: Onboarding

## Context and motivation

A first-time caregiver needs a short introduction to Petit before entering the main pet-care experience.

## Functional requirements

- Open onboarding instead of Home until completion has been persisted.
- Present three pages: a welcome, a summary of weight, vaccination, deworming, and reminder capabilities, and a final call to action.
- Allow the caregiver to advance through the pages or skip from the first two pages.
- Mark onboarding as completed when the caregiver skips or selects the final action, then navigate to Home.
- Bypass onboarding on subsequent app launches after completion.
- Hide the main bottom navigation while onboarding is visible.

## Acceptance criteria

- Given onboarding has not been completed, When Petit starts, Then the onboarding flow opens instead of Home.
- Given the caregiver is on the first or second page, When they select Next, Then the following page and its page indicator are displayed.
- Given the caregiver is on the first or second page, When they select Skip, Then completion is persisted and Home opens.
- Given the caregiver is on the last page, When they select Get started, Then completion is persisted and Home opens.
- Given onboarding was completed previously, When Petit starts again, Then Home opens directly.
- Given onboarding is visible, Then the main bottom navigation is hidden.

## Test strategy

Unit tests cover completion state and event emission; integration and UI tests cover DataStore persistence, start-destination selection, paging, skip, final completion, and bottom-navigation visibility.

## Edge cases

- Ignore repeated completion actions while the preference write is in progress.
- Keep the caregiver in onboarding and surface a recoverable error if completion cannot be persisted.

## Known limitations

- `isCompleting` is not currently consumed by the screen, so repeated taps are not disabled.
- Preference-write failures are not surfaced to the caregiver.
- There is no supported action to reset onboarding from the app.

## Out of scope

- Authentication, account creation, and remote profile setup.
- Pet registration inside the onboarding pager.
