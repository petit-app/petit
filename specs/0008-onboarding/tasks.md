# Tasks: Onboarding

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Route first-time caregivers to onboarding** (test-type: integration)
  - blocked-by: none
  - desired behavior: wait for user preferences and choose Onboarding until completion has been saved.
  - acceptance criteria: incomplete onboarding opens first; completed onboarding opens Home.
  - verification: source evidence in `MainActivity` and `UserPreferencesRepository`.
- [x] **Present and navigate the onboarding pages** (test-type: integration)
  - blocked-by: route first-time caregivers to onboarding
  - desired behavior: show the welcome, capability summary, and call-to-action pages with Next and Skip controls.
  - acceptance criteria: all three pages and the accessible page indicator reflect the current position.
  - verification: source evidence in `OnboardingScreen`.
- [x] **Persist completion and enter Home** (test-type: integration)
  - blocked-by: present and navigate the onboarding pages
  - desired behavior: persist completion from Skip or Get started and remove Onboarding from the back stack.
  - acceptance criteria: a subsequent launch bypasses onboarding.
  - verification: source evidence in `OnboardingViewModel` and `PetitNavGraph`.
- [x] **Harden onboarding completion failures** (test-type: both)
  - blocked-by: persist completion and enter Home
  - desired behavior: disable repeated completion actions and show a recoverable error when persistence fails.
  - acceptance criteria: only one completion write runs at a time, and failure does not navigate away.
  - verification: `./gradlew test`
- [x] **Add automated onboarding regression tests** (test-type: both)
  - blocked-by: harden onboarding completion failures
  - desired behavior: cover preference defaults, persistence, start destination, page navigation, skip, completion, and bottom-bar visibility.
  - acceptance criteria: every acceptance criterion has automated coverage.
  - verification: `./gradlew test`

- [x] **Cover the onboarding-to-home E2E journey** (test-type: integration)
  - blocked-by: persist completion and enter Home
  - desired behavior: exercise the real Activity, navigation, and DataStore from a clean app state.
  - acceptance criteria: the three-page journey reaches Home, completion survives Activity recreation, and onboarding does not return.
  - verification: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woliveiras.petit.e2e.OnboardingJourneyTest`
