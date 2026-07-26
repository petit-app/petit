# Plan: Navigation motion

Spec: [spec.md](./spec.md)

## Status

This plan is **Approved**. The user approved the spec on 2026-07-26.

## Dependencies

- Existing `Screen` destination patterns.
- Existing `PetitNavGraph` Navigation Compose host.
- Existing main-area navigation callbacks in `MainActivity`.
- Navigation Compose 2.9.7 transition and pop-transition APIs.

## Architecture

- Represent destination motion as a small presentation policy with main-area
  and hierarchical classifications.
- Resolve transitions from the initial destination, target destination,
  navigation direction, and layout direction.
- Apply the policy once at the app `NavHost`.
- Keep top-level navigation de-duplicated through the existing navigation
  options, adjusting them only if regression coverage reveals a duplicate-entry
  path.
- Keep animation construction separate from route classification so policy
  tests do not depend on animation clocks.

## Implementation sequence

1. [x] Approve the spec, plan, tasks, and index row.
2. [x] Add focused tests for main-area classification, hierarchical defaults,
   forward navigation, pop navigation, onboarding completion, and RTL
   direction.
3. [x] Add the centralized transition policy and apply it to `PetitNavGraph`.
4. [x] Verify representative top-level and hierarchical flows with Compose
   instrumentation, including saved-state and navigation-result preservation.
5. [~] Verify animator scale behavior, gesture back, RTL direction, rapid
   navigation, and visual quality on an emulator or physical device.
6. [ ] Reconcile spec status, this plan, task checkboxes, and the specs index
   with fresh evidence.

## Commit boundaries

The user authorized local commits. Intended boundaries:

1. approved spec, plan, tasks, PRD objective, and specs index;
2. transition policy, regression coverage, and NavHost integration;
3. verification-driven adjustments and documentation reconciliation, if
   needed.

No push, amend, rebase, merge, force-push, or pull request is authorized.

## Risks and mitigations

- **False hierarchy between main areas:** use the crossfade policy for direct
  navigation to a main area and for pops between main areas.
- **Wrong direction on back:** define pop transitions separately from forward
  transitions and cover both with policy tests.
- **Wrong direction in RTL:** derive leading and trailing offsets from layout
  direction instead of hard-coded left and right assumptions.
- **Route drift:** keep main-area patterns in one named set and default unknown
  destinations to hierarchical motion.
- **State loss:** retain the current NavController and destination definitions;
  verify a form and navigation-result round trip.
- **Duplicate top-level entries:** cover re-selection and repeated taps before
  changing existing navigation options.
- **Accessibility discomfort:** honor the platform animator scale and manually
  review normal and disabled motion.

## Verification

1. Run focused unit tests for the transition policy.
2. Run focused Compose navigation tests on an emulator or device.
3. Run `./gradlew testDebugUnitTest`.
4. Run `./gradlew compileDebugAndroidTestKotlin`.
5. Run `./gradlew spotlessCheck`.
6. Run `./gradlew lintDebug` and report pre-existing failures separately.
7. Run `git diff --check` and `git status --short`.
8. Review normal motion, disabled animations, RTL, and gesture back manually.

## Evidence

- The focused transition-policy unit suite passes.
- `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `spotlessCheck`, and
  `git diff --check` pass.
- Six focused Navigation Compose tests pass on the Pixel 7 API 34 emulator,
  covering main-area crossfade position, hierarchical detail and form
  navigation, reverse pop direction, onboarding replacement, RTL mirroring,
  main-area re-selection, saved state, and navigation-result preservation.
- The existing pet CRUD E2E journey remains blocked by its pre-existing
  `ClearAppStateRule` race: instrumentation logs report missing `pets`, `tasks`,
  `deworming_entries`, and `restorable_revision` tables after the rule deletes
  databases from an already-started application process.
- Disabled animator scale, interactive predictive-back gesture, and final
  visual review remain manual checks.
- `lintDebug` reaches the report but remains blocked by 103 pre-existing
  errors. None reference the navigation-motion files.
