---
spec: "0009"
title: App preferences
family: pet-care
status: In Progress
owner: woliveiras
depends_on: []
---

# Spec: App preferences

## Context and motivation

The caregiver needs Petit to follow their visual and language preferences across app sessions.

## Current state

Theme and language choices are persisted and applied through the settings
flow. Android 13 and later apply language changes through `LocaleManager`;
earlier versions explain that a restart is required and apply the persisted
locale before rendering the next app session. The platform locale catalog and
the in-app selector both expose English and Brazilian Portuguese.

At the start of the completeness extension, the canonical resource set had 793
strings and 3 plurals, while Brazilian Portuguese had 720 strings and 3
plurals. Those 73 missing strings fell back to English in backup settings,
saved backups, restore, and a backup notification. Additional
visible/accessibility copy bypassed resources through hardcoded or persisted
domain display labels. The implemented contract now keeps the two supported
resource sets in parity.

## Functional requirements

- Offer System, Light, and Dark theme choices in Settings.
- Apply the selected theme to the app and persist it in DataStore.
- Offer System, English, and Brazilian Portuguese language choices in Settings.
- Apply the selected per-app language and persist it in DataStore.
- Fall back to System when a stored theme or language value is unknown.
- Display the current selection in Settings and mark it in the selection sheet.
- Treat English and Brazilian Portuguese as the complete official locale set.
  The existing `values-es` directory does not make Spanish officially supported.
- Require parity between every translatable canonical string, plural, and array
  and Brazilian Portuguese, including resource kind, plural quantities, and
  formatting placeholders.
- Exclude a canonical `translatable="false"` resource deliberately and require
  it not to be translated in locale-specific files.
- Source all visible app-owned copy from resources, including Compose text,
  Snackbars, dialogs, worker/notification text, and accessibility semantics.
- Render system-generated task and care labels in the active locale without
  rewriting caregiver-entered names, brands, proper nouns, or acronyms.
- Use locale-aware date, number, unit, and quantity formatting where the value
  is presented to the caregiver.

## Acceptance criteria

- Given System theme is selected, When the system appearance changes, Then Petit follows the system appearance.
- Given Light or Dark is selected, When the preference is saved, Then the app uses that theme and restores it after restart.
- Given System, English, or Brazilian Portuguese is selected, When the preference is saved, Then Settings reflects the selection and the supported locale is applied.
- Given a malformed stored theme or unknown language code, When preferences are loaded, Then Petit falls back to System without crashing.
- Given a theme or language sheet is open, When the caregiver chooses an option, Then the preference is saved and the sheet closes.
- Given Brazilian Portuguese is active, When any Settings, backup, saved-backup,
  restore, notification, care-form, Snackbar, or accessibility path is rendered,
  Then no app-owned English resource fallback is visible or announced.
- Given a canonical translatable resource is added or changed, When resource
  contract tests run, Then Brazilian Portuguese must provide the same resource
  kind, plural quantities, and compatible formatting placeholders.
- Given an app-owned resource is marked `translatable="false"`, When locale
  resources are validated, Then no localized duplicate is required or allowed.
- Given a caregiver-entered pet, breed, vaccine, medication, clinic, or brand
  name, When the locale changes, Then the value is preserved exactly rather than
  translated.
- Given a system-generated task or notification, When it is displayed or
  announced, Then its app-owned template is resolved in the active locale.

## Test strategy

Unit tests cover enum/code fallback and ViewModel state. A source-level resource
contract test compares canonical and pt-BR keys, resource kinds, plural
quantities, `translatable` policy, and printf signatures. Integration and UI
tests cover DataStore persistence, theme application, Android locale application,
selection sheets, restart behavior, and representative Settings, backup,
restore, notification, care, and accessibility copy under pt-BR. A bounded
static check plus human review audits visible literals that bypass resources.

## Edge cases

- Android versions before 13 require an app restart before a newly selected language takes effect.
- System locale and appearance can change while the app is not running.
- Stored values from a newer or older app version may be unknown.
- Escaped percent signs are literals, not formatting parameters.
- Positional and non-positional placeholders must retain compatible index and
  conversion types across locales.
- Proper nouns, trademarks, acronyms, caregiver-entered values, MIME types,
  routes, log tags, and technical identifiers are not translated merely to
  satisfy key parity.
- Existing system-generated task titles may contain an old persisted display
  label; presentation must distinguish those from caregiver-authored titles
  before applying localization.

## Known limitations

- On Android versions before 13, the selected language is persisted but is not applied until restart.
- App-preference and reminder-preference DataStores are independent and have no shared reset operation.
- Spanish resources exist but Spanish is not advertised or covered by the
  supported-locale completeness contract.

## Out of scope

- Reminder scheduling preferences, which belong to spec `0005`.
- Font-size, contrast, and other accessibility overrides controlled by Android.
