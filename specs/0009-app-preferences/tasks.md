# Tasks: App preferences

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Persist theme and language preferences** (test-type: integration)
  - blocked-by: none
  - desired behavior: store supported selections in DataStore and expose them as a reactive flow.
  - acceptance criteria: values survive restart, and unknown stored values safely fall back to System.
  - verification: source evidence in `UserPreferencesRepositoryImpl` and `AppSettings`.
- [x] **Apply the selected app theme** (test-type: integration)
  - blocked-by: persist theme and language preferences
  - desired behavior: follow the system or force Light or Dark at the application root.
  - acceptance criteria: the active theme follows the persisted selection.
  - verification: source evidence in `MainActivity` and `PetitTheme`.
- [x] **Select and apply the app language** (test-type: integration)
  - blocked-by: persist theme and language preferences
  - desired behavior: choose System, English, or Brazilian Portuguese and apply the per-app locale.
  - acceptance criteria: Settings reflects the saved language; Android 13 and later update through `LocaleManager`.
  - verification: source evidence in `SettingsScreen`, `SettingsViewModel`, and `LocaleHelper`.
- [ ] **Align language behavior across supported Android versions** (test-type: integration)
  - blocked-by: select and apply the app language
  - desired behavior: define and implement immediate or explicit restart behavior before Android 13 and reconcile Spanish selector support.
  - acceptance criteria: every configured locale has intentional selector behavior, and users understand when restart is required.
  - verification: `./gradlew test`
- [ ] **Add automated app-preference regression tests** (test-type: both)
  - blocked-by: align language behavior across supported Android versions
  - desired behavior: cover fallback, DataStore persistence, theme recomposition, locale application, and selection sheets.
  - acceptance criteria: every acceptance criterion has automated coverage across relevant Android API levels.
  - verification: `./gradlew test`
