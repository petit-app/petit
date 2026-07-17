# Plan: Onboarding

Spec: [spec.md](./spec.md)

## Sequence

1. Persist onboarding completion in `UserPreferencesRepository`.
2. Select Onboarding or Home as the start destination from the persisted preference.
3. Present the three-page pager with Next, Skip, Get started, and an accessible page indicator.
4. Navigate to Home after a successful completion write and remove Onboarding from the back stack.
5. Harden repeated actions and preference-write failure handling.
6. Add automated coverage for state, persistence, navigation, and UI behavior.

## Architecture

- DataStore is the local source of truth for `hasCompletedOnboarding`.
- `MainActivity` selects the navigation start destination after preferences load.
- `OnboardingViewModel` owns completion and emits navigation only after persistence succeeds.
- `OnboardingScreen` owns pager interaction and presentation.

## Dependencies and risks

- Uses the shared user-preferences store documented by spec `0009`.
- A failed or delayed DataStore write must not leave navigation state inconsistent.
- UI behavior requires Compose navigation and pager tests, which are not present yet.
