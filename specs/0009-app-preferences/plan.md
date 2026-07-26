# Plan: App preferences

Spec: [spec.md](./spec.md)

## Status

The original theme/locale selection work is complete. The supported-locale
completeness extension was **Approved** for implementation on 2026-07-20.

## Sequence

1. [x] Model supported theme and language choices with safe fallback behavior.
2. [x] Persist theme, language, and onboarding state in the user-preferences DataStore.
3. [x] Observe preferences in `MainActivity` and `SettingsViewModel`.
4. [x] Apply theme through `PetitTheme` and language through `LocaleHelper`.
5. [x] Present selection sheets with the current option marked.
6. [x] Add automated coverage for fallback, persistence, application, and Settings interaction.
7. [x] Add a deterministic English/pt-BR resource contract for keys, kinds,
   plurals, placeholders, and `translatable="false"`.
8. [x] Complete pt-BR resources and route app-owned Compose, Snackbar, worker,
   notification, task-template, and accessibility copy through resources.
9. [~] Verify Settings, backup settings/history/saved backups/restore,
   notifications, and representative care flows under an actual pt-BR context.
   Automated contracts, JVM tests, and focused locale-configured Compose tests
   pass. Physical-device Settings/backup/restore/notification and TalkBack
   review remains pending.

## Architecture

- `UserPreferencesRepository` is the source of truth for appearance, language, and onboarding completion.
- `MainActivity` observes theme changes and recomposes the root theme.
- `SettingsViewModel` persists selections and delegates locale application to `LocaleHelper`.
- Android 13 and later use `LocaleManager` for per-app locales; earlier versions apply the persisted locale during startup and explicitly request a restart after selection.

## Dependencies and risks

- The onboarding flow in spec `0008` shares the same DataStore.
- Locale behavior differs before and after Android 13.
- Resource/configuration support and the in-app language selector must remain aligned.
- Persisted caregiver data must not be translated; system-generated display
  templates must be localized at the presentation/notification boundary.
- `values-es` remains outside the supported-locale contract.

## Verification

1. Demonstrate the resource-parity regression test failing against the current
   73-key gap before applying translations.
2. Run the focused resource, ViewModel, worker/notification, and locale-configured
   Compose tests.
3. Run `./gradlew test`, `./gradlew spotlessCheck`, `./gradlew lintDebug`, and
   `git diff --check`.
4. If `assembleDebug` is run, immediately run `installDebug`.
