# Tasks: Navigation motion

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec approved on 2026-07-26. Implementation may proceed.

## Tasks

- [x] **Select motion from navigation intent** (test-type: unit)
  - blocked-by: spec approval
  - desired behavior: one presentation policy classifies main-area and
    hierarchical destinations and chooses forward, pop, and RTL direction
    without depending on animation timing.
  - acceptance criteria: main-area switches, re-selection, onboarding
    completion, parameterized routes, unknown routes, hierarchical forward
    navigation, pops to main areas, and RTL behavior match `spec.md`.
  - test expectations: table-driven unit tests cover initial route, target
    route, navigation direction, layout direction, and expected motion.
  - verification: focused transition-policy unit tests.

- [x] **Animate representative navigation flows without losing state**
  (test-type: both)
  - blocked-by: select motion from navigation intent
  - desired behavior: the app NavHost uses a 300 ms directional slide for
    hierarchy and a 150 ms crossfade for main areas while retaining the current
    back stack and destination state.
  - acceptance criteria: pet list to detail to form, reverse pop, main-area
    switch, onboarding completion, form state, and navigation results match
    `spec.md`.
  - test expectations: policy tests remain the primary duration and direction
    contract; focused Compose tests exercise representative real navigation and
    state preservation.
  - verification: focused JVM tests and Compose navigation instrumentation.
  - evidence: focused policy tests pass; six Compose tests pass on the Pixel 7
    API 34 emulator for crossfade, detail and form slides, reverse pop,
    onboarding replacement, RTL, re-selection, saved state, and navigation
    results.

- [~] **Validate motion accessibility and navigation resilience**
  (test-type: integration)
  - blocked-by: animate representative navigation flows without losing state
  - desired behavior: animations respect the Android animator scale, mirror in
    RTL, remain coherent with gesture back, and settle safely after repeated
    navigation input.
  - acceptance criteria: disabled animations, RTL, predictive back, rapid
    input, destination interactivity, and duplicate-entry behavior match
    `spec.md`.
  - test expectations: focused instrumented coverage where deterministic, plus
    explicit manual evidence for gesture and visual quality.
  - verification: emulator or device checks at normal and disabled animator
    scales, RTL visual review, gesture-back review, nearest test suite,
    formatting, lint, and diff checks.
  - evidence: RTL and repeated main-area selection pass in focused emulator
    tests. Disabled animator scale, interactive gesture back, and visual review
    remain open. The existing pet CRUD E2E is blocked by missing Room tables
    caused by its pre-existing database-clear fixture. `testDebugUnitTest`,
    `compileDebugAndroidTestKotlin`, `spotlessCheck`, and `git diff --check`
    pass. `lintDebug` reports 103 pre-existing errors and none reference this
    feature's files.

- [ ] **Reconcile navigation-motion delivery evidence** (test-type: both)
  - blocked-by: all implementation tasks
  - desired behavior: spec, plan, tasks, and index status reflect only behavior
    supported by fresh automated and manual evidence.
  - acceptance criteria: completed work is marked `[x]`, no stale `[~]`
    remains, temporary harnesses are removed, and any device-only or
    pre-existing verification gap is reported.
  - test expectations: no new test code unless verification reveals a missing
    acceptance criterion.
  - verification: focused and nearest suites, `spotlessCheck`, `lintDebug`,
    `git diff --check`, `git status --short`, and manual checks listed in
    `plan.md`.
