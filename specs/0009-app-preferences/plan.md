# Plan: App preferences

Spec: [spec.md](./spec.md)

## Sequence

1. Model supported theme and language choices with safe fallback behavior.
2. Persist theme, language, and onboarding state in the user-preferences DataStore.
3. Observe preferences in `MainActivity` and `SettingsViewModel`.
4. Apply theme through `PetitTheme` and language through `LocaleHelper`.
5. Present selection sheets with the current option marked.
6. Add automated coverage for fallback, persistence, application, and Settings interaction.

## Architecture

- `UserPreferencesRepository` is the source of truth for appearance, language, and onboarding completion.
- `MainActivity` observes theme changes and recomposes the root theme.
- `SettingsViewModel` persists selections and delegates locale application to `LocaleHelper`.
- Android 13 and later use `LocaleManager` for per-app locales.

## Dependencies and risks

- The onboarding flow in spec `0008` shares the same DataStore.
- Locale behavior differs before and after Android 13.
- Resource/configuration support and the in-app language selector must remain aligned.
