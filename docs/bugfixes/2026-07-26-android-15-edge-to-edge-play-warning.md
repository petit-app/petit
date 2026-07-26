# Bugfix: Android 15 edge-to-edge Play Console warnings

**Status:** investigating
**Date opened:** 2026-07-26
**Source:** user-provided Google Play Console report for release 1 (1.0.0)
**Outage:** no

## Summary

Google Play Console reports that release 1 (1.0.0) might not display edge-to-edge
for all users and uses deprecated Android 15 window APIs or parameters. The
published release must opt into edge-to-edge on older Android versions, handle
system bar insets, and avoid direct app-owned use of deprecated system bar color
APIs.

## Impact

- Users of release 1 (1.0.0) may see content or controls obscured by system bars
  on Android 15 and later.
- The release has two Google Play Console user-experience warnings.
- The warning remains associated with release 1 until a corrected bundle with a
  higher version code is uploaded and analyzed by Google Play.

## Reproduction

**Environment:** local `main` at `c6f0081`, Android target SDK 35, local release
bundle generated on 2026-07-20 before the edge-to-edge cleanup commit

**Steps:**

1. Inspect the release identified by Google Play: version code 1, version name
   1.0.0.
2. Inspect the timestamp of `app/build/outputs/bundle/release/app-release.aab`.
3. Compare it with commit `c6f0081`, which removed the app-owned deprecated
   status and navigation bar configuration on 2026-07-26.
4. Inspect DEX references to `Window.setStatusBarColor`,
   `Window.setNavigationBarColor`, and edge-to-edge setup.

**Expected:** The bundle uploaded as the next release is built from the corrected
source, has a version code greater than 1, calls `enableEdgeToEdge()`, keeps
interactive UI outside system bar insets, and has no direct app-owned calls or
theme attributes for deprecated system bar colors.

**Actual:** Before this fix, the initially available release bundle predated `c6f0081`, was
configured as version code 1/version name 1.0.0, and contained the deprecated
API references reported by Google Play. The custom bottom navigation did not
apply navigation bar insets and visibly overlapped three-button system
navigation. The root scaffold applied only its bottom padding to navigation
content, leaving onboarding dependent on fixed spacing rather than explicit
top/side safe insets. Compatibility implementations inside AndroidX still
reference older APIs conditionally for supported pre-Android-15 versions; these
library-owned references are not direct Petit API usage.

**Reproduction command or loop:**

```bash
stat -f '%Sm %N' -t '%Y-%m-%d %H:%M:%S %z' \
  app/build/outputs/bundle/release/app-release.aab
git show --stat c6f0081
rg -n \
  'enableEdgeToEdge|statusBarColor|navigationBarColor|setStatusBarColor|setNavigationBarColor' \
  app/src/main
apkanalyzer dex reference-tree \
  --references-to 'android.view.Window void setStatusBarColor(int)' \
  app/build/outputs/apk/debug/app-debug.apk
apkanalyzer dex reference-tree \
  --references-to 'android.view.Window void setNavigationBarColor(int)' \
  app/build/outputs/apk/debug/app-debug.apk
```

## Hypotheses

| Rank | Hypothesis | Prediction | Result |
| --- | --- | --- | --- |
| 1 | Release 1 does not consistently keep interactive UI outside system bar insets. | Onboarding or the custom bottom bar would overlap a status bar, cutout, gesture area, or three-button navigation. | confirmed for the custom bottom bar with three-button navigation on API 36; onboarding lacks an explicit top/side inset contract |
| 2 | Google Play analyzed the pre-cleanup release 1 bundle for deprecated APIs. | The local bundle predates `c6f0081`, while current source already removes direct deprecated calls. | confirmed locally; the exact uploaded bundle hash is not available |
| 3 | The app does not enable edge-to-edge. | `MainActivity.onCreate` would omit `enableEdgeToEdge()`. | falsified; the call exists and predates release 1 |
| 4 | Current Petit source still directly sets deprecated system bar colors. | App-owned Kotlin or theme resources would contain the setters or attributes. | falsified on current `main`; they were removed by `c6f0081` |
| 5 | AndroidX compatibility code alone causes the deprecated-API warning. | A freshly built corrected bundle would have only AndroidX-owned references, and Google Play would still report them after analysis. | interim bundle has only AndroidX-owned calls; Play analysis remains pending |

## Investigation Log

- 2026-07-26: Confirmed `compileSdk = 36`, `targetSdk = 35`, version code 1,
  and version name 1.0.0.
- 2026-07-26: Confirmed `MainActivity.onCreate` calls
  `androidx.activity.enableEdgeToEdge()`.
- 2026-07-26: Confirmed current light and dark themes omit
  `android:statusBarColor`, `android:navigationBarColor`, and
  `android:windowLightStatusBar`.
- 2026-07-26: Confirmed commit `c6f0081` removed the direct
  `window.statusBarColor` assignment and deprecated theme attributes.
- 2026-07-26: Confirmed the local release bundle was built on 2026-07-20,
  before `c6f0081` was committed on 2026-07-26.
- 2026-07-26: DEX reference analysis attributed remaining compatibility calls
  to `androidx.activity.EdgeToEdgeApi*` and `androidx.core.view.WindowCompat`.
- 2026-07-26: Regression contract failed against `c6f0081^` with the two
  deprecated theme resources and the direct `Theme.kt` assignment.
- 2026-07-26: Regression contract separately failed while the corrected source
  still reused version code 1/version name 1.0.0.
- 2026-07-26: Incremented the corrected release to version code 2/version name
  1.0.1 and confirmed the regression contract passes.
- 2026-07-26: Generated and signed a fresh release bundle. Gradle release
  metadata confirms version code 2/version name 1.0.1; SHA-256:
  `1f13c92dc57e0f0ab8b2f56a99f9a9c374c99ebdb619fa2a12d3e134d4123def`.
- 2026-07-26: Inspected the fresh release DEX with the R8 mapping. Calls to
  `setStatusBarColor` and `setNavigationBarColor` are owned by
  `androidx.activity.EdgeToEdgeApi*`, not Petit source.
- 2026-07-26: Independent Kotlin review found that the root scaffold ignores
  top/side `innerPadding`, onboarding has no explicit safe-inset handling, and
  the custom bottom navigation has fixed height without navigation bar padding.
- 2026-07-26: Reproduced on an API 36 emulator. Gesture-mode onboarding remained
  usable, but three-button navigation visibly overlapped the Petit bottom bar.
- 2026-07-26: Found that Settings still renders the hard-coded version `1.0.0`
  after the release configuration advanced to 1.0.1.
- 2026-07-26: Added navigation-bar padding to the custom bottom navigation and
  full root-scaffold inset handling for routes without that navigation.
- 2026-07-26: The pre-fix instrumented bottom-navigation assertion failed on
  API 36 in three-button mode: the add action ended at y=2358 while the safe
  content boundary ended at y=2274, proving an 84-pixel overlap.
- 2026-07-26: Both focused inset assertions pass after the fix on API 36 in
  three-button mode. Manual checks also pass in gesture and three-button modes.
- 2026-07-26: Settings now renders `BuildConfig.VERSION_NAME`; the source
  contract and the complete unit test suite pass.

## Regression Test

- Test files:
  - `app/src/test/java/com/woliveiras/petit/EdgeToEdgeCompatibilityTest.kt`
  - `app/src/test/java/com/woliveiras/petit/AppVersionContractTest.kt`
  - `app/src/androidTest/java/com/woliveiras/petit/PetitRootScaffoldInsetsTest.kt`
  - `app/src/androidTest/java/com/woliveiras/petit/presentation/navigation/PetitBottomNavBarInsetsTest.kt`
- Test names:
  - `releaseSourceEnablesEdgeToEdgeWithoutAppOwnedDeprecatedSystemBarConfiguration`
  - `settingsSupportingTextUsesTheGeneratedReleaseVersion`
  - `contentActionsStayOutsideSystemBarsWhenBottomBarIsHidden`
  - `actionsStayAboveTheBottomSystemInset`
- Failure observed before fix: yes; the contract failed against `c6f0081^` for
  both theme resources and the direct Kotlin assignment, then failed against
  current source until the release version advanced beyond 1 (1.0.0). The
  bottom-navigation bounds assertion also failed before its inset fix with an
  84-pixel overlap in three-button mode.
- Boundary:
  - Strengthen the source/resource release contract to verify the call inside
    `MainActivity.onCreate`, strip comments/strings before checking forbidden
    APIs, and cover the full Android 15 deprecated window API/attribute set.
  - Use the real activity and system insets in an instrumented test to verify
    actionable semantics bounds rather than relying only on source inspection.
  - Verify that the settings supporting text is bound directly to
    `BuildConfig.VERSION_NAME`.

## Fix

- Root causes:
  1. Release 1 does not consistently consume system bar insets: the custom
     bottom navigation ignores navigation bar insets, and onboarding has no
     explicit top/side safe-inset policy.
  2. Release 1 was built before the app-owned deprecated system bar
     configuration was removed.
  3. The repository reused version code 1, so corrected source could not be
     represented by a new Play release.
- Code change:
  1. When the root scaffold has no bottom bar, apply and consume its complete
     `innerPadding` so onboarding respects top, side, and bottom safe areas.
     Preserve the existing nested-scaffold behavior for routes with the Petit
     bottom bar to avoid double top insets.
  2. Apply navigation bar padding inside `PetitBottomNavBar` so its background
     can draw edge-to-edge while all interactive content stays above gesture
     and three-button navigation.
  3. Preserve the deprecated-API cleanup already present in `c6f0081`.
  4. Strengthen the regression coverage described above.
  5. Render the settings version from `BuildConfig.VERSION_NAME`.
  6. Keep version code 2/version name 1.0.1 and rebuild the final signed bundle.
- Why this fixes the root cause: the final bundle will keep interactive content
  outside system UI on each affected root path, opt into backward-compatible
  edge-to-edge, contain no direct Petit-owned deprecated system bar
  configuration, and be eligible for a new Google Play analysis.

## Verification

- [x] Deprecated-API regression fails against the source state before `c6f0081`
- [x] Insets regression fails against current source before the UI fix
- [x] All focused regression tests pass against corrected source
- [x] Complete unit test suite and focused instrumented inset tests pass
- [x] `spotlessCheck` passes after the complete fix
- [x] Final release bundle is rebuilt with version code 2/version name 1.0.1
- [x] Final DEX references are attributed only to AndroidX compatibility code
- [ ] Edge-to-edge is manually checked on Android 15 with gesture navigation
- [ ] Edge-to-edge is manually checked on Android 15 with three-button navigation
- [x] Edge-to-edge is manually checked on an available later Android version
  with gesture and three-button navigation
- [x] Settings version is sourced from `BuildConfig.VERSION_NAME` (1.0.1)
- [ ] Google Play finishes analyzing the new bundle without the two release warnings
- [x] Temporary instrumentation removed

## Follow-ups

- [ ] Upload the signed version code 2 bundle to Google Play Console.
- [ ] Record the Google Play analysis result; this external verification cannot
  be completed locally.
