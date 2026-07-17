---
spec: "0009"
title: App preferences
family: pet-care
status: Implemented
owner: woliveiras
depends_on: []
---

# Spec: App preferences

## Context and motivation

The caregiver needs Petit to follow their visual and language preferences across app sessions.

## Functional requirements

- Offer System, Light, and Dark theme choices in Settings.
- Apply the selected theme to the app and persist it in DataStore.
- Offer System, English, and Brazilian Portuguese language choices in Settings.
- Apply the selected per-app language and persist it in DataStore.
- Fall back to System when a stored theme or language value is unknown.
- Display the current selection in Settings and mark it in the selection sheet.

## Acceptance criteria

- Given System theme is selected, When the system appearance changes, Then Petit follows the system appearance.
- Given Light or Dark is selected, When the preference is saved, Then the app uses that theme and restores it after restart.
- Given System, English, or Brazilian Portuguese is selected, When the preference is saved, Then Settings reflects the selection and the supported locale is applied.
- Given a malformed stored theme or unknown language code, When preferences are loaded, Then Petit falls back to System without crashing.
- Given a theme or language sheet is open, When the caregiver chooses an option, Then the preference is saved and the sheet closes.

## Test strategy

Unit tests cover enum/code fallback and ViewModel state; integration and UI tests cover DataStore persistence, theme application, Android locale application, selection sheets, and restart behavior.

## Edge cases

- Android versions before 13 require an app restart before a newly selected language takes effect.
- System locale and appearance can change while the app is not running.
- Stored values from a newer or older app version may be unknown.

## Known limitations

- On Android versions before 13, the selected language is persisted but is not applied until restart.
- Spanish resources and locale configuration exist, but Spanish is not exposed as an explicit in-app language choice.
- App-preference and reminder-preference DataStores are independent and have no shared reset operation.

## Out of scope

- Reminder scheduling preferences, which belong to spec `0005`.
- Font-size, contrast, and other accessibility overrides controlled by Android.
