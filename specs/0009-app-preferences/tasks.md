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
- [x] **Align language behavior across supported Android versions** (test-type: integration)
  - blocked-by: select and apply the app language
  - desired behavior: define and implement immediate or explicit restart behavior before Android 13 and reconcile Spanish selector support.
  - acceptance criteria: every configured locale has intentional selector behavior, and users understand when restart is required.
  - verification: `./gradlew test`
- [x] **Add automated app-preference regression tests** (test-type: both)
  - blocked-by: align language behavior across supported Android versions
  - desired behavior: cover fallback, DataStore persistence, theme recomposition, locale application, and selection sheets.
  - acceptance criteria: every acceptance criterion has automated coverage across relevant Android API levels.
  - verification: `./gradlew test`

- [x] **Enforce supported-locale resource contracts** (test-type: unit)
  - blocked-by: updated spec approval
  - desired behavior: canonical and pt-BR resources remain aligned by key, kind,
    plural quantities, placeholders, and translatability policy.
  - acceptance criteria: the current tree produces a deterministic RED failure
    naming 73 missing strings; after the fix, missing/extra/incompatible
    resources fail the test while deliberate `translatable="false"` resources do not.
  - verification: `./gradlew testDebugUnitTest --tests '*PtBrResourceContractTest'`

- [x] **Remove app-owned mixed-language output** (test-type: both)
  - blocked-by: enforce supported-locale resource contracts
  - desired behavior: complete the 73 pt-BR strings and route visible Compose,
    Snackbar, worker, notification, system-task, and accessibility copy through
    resources without translating caregiver data or proper names.
  - acceptance criteria: Settings, backup settings, saved backups, restore,
    notification, care labels, and chart semantics contain no app-owned English
    fallback in pt-BR.
  - test expectations: unit coverage for generated templates plus targeted
    locale-configured Compose/instrumented checks.
  - verification: focused JVM and Android locale tests

- [~] **Verify formatting and all supported-language flows** (test-type: both)
  - blocked-by: remove app-owned mixed-language output
  - desired behavior: placeholders, plural quantities, dates, numbers, units,
    content descriptions, and restart behavior respect the active locale.
  - acceptance criteria: English and pt-BR both pass the contract and
    representative Settings, backup, restore, notification, and care journeys.
  - verification: resource/JVM contracts, `./gradlew test`, 27 focused emulator
    tests, and `./gradlew spotlessCheck` pass; physical-device journeys remain
    pending and `lintDebug` retains repository-level failures.
