# Bugfix: Release breed catalog is empty

**Status:** fixed
**Date opened:** 2026-07-27
**Source:** user report
**Outage:** no

## Summary

Version 1.0.1 contains the pinned breed catalog asset, but the minified release
runtime cannot deserialize it and presents no catalog breeds. Cat and dog breed
selection should list the packaged catalog while preserving the existing
mixed-breed, unknown, manual-entry, and no-selection options.

## Impact

- Affects version 1.0.1 built from source commit
  `a2bcb8b6cbfb2eddbd17a6d075148a66263f0c58`.
- Affects both cat and dog catalog selection in release builds.
- Caregivers can still enter a breed manually, but cannot search or select a
  catalog breed.
- Debug builds and debug unit tests do not expose the failure.

## Reproduction

**Environment:** local Android emulator, universal APK generated from the
minified version 1.0.1 AAB

**Steps:**

1. Install the universal APK generated from
   `app/build/outputs/bundle/release/app-release.aab`.
2. Complete or skip onboarding.
3. Start pet registration.
4. Select `Dog` or `Cat`.
5. Open the breed selection screen with an empty search query.

**Expected:** the empty query lists 356 dog breeds or 96 cat breeds from
`breed_catalog.json`.

**Actual:** the screen reports `No matching breeds` and only exposes the Petit
fallback choices.

**Reproduction command or loop:**

```bash
java -jar bundletool-all-1.18.3.jar build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=petit.apks \
  --mode=universal
unzip petit.apks universal.apk
adb install universal.apk
```

After installation, select a cat or dog in the pet form and open the breed
selector. The release screen reports no matching breeds.

## Hypotheses

| Rank | Hypothesis | Prediction | Result |
| --- | --- | --- | --- |
| 1 | R8 removes or renames the Gson-reflected catalog document fields. | The release mapping/usage report removes the document fields, while the minified runtime produces an empty catalog. | confirmed |
| 2 | The JSON asset was omitted from the release bundle. | `base/assets/breed_catalog.json` is absent or differs from the source asset. | falsified |
| 3 | The published workflow used source from before the catalog implementation. | The workflow source SHA predates the breed catalog commits. | falsified |
| 4 | The catalog itself is malformed. | Parsing the same asset in the unminified debug test fails. | falsified |

## Investigation Log

- 2026-07-27: GitHub Actions run `30208085376` was confirmed to have built
  version code 2 / version name 1.0.1 from
  `a2bcb8b6cbfb2eddbd17a6d075148a66263f0c58`.
- 2026-07-27: The source commit contains `breed_catalog.json`,
  `BreedCatalog`, `BreedSelectionViewModel`, and the dedicated breed selection
  screen.
- 2026-07-27: The AAB contains `base/assets/breed_catalog.json`; its SHA-256 is
  `21ffe12180b09207dc0d0ee0b5329ea77fec1e786bb8d0e748015598bcc3c8ee`,
  identical to the source asset.
- 2026-07-27: Fresh debug asset verification passed and parsed 356 dog breeds
  and 96 cat breeds.
- 2026-07-27: R8 `usage.txt` reports the private fields of `CatalogDocument`
  and its nested document models as removed.
- 2026-07-27: Installing a universal APK generated from the minified AAB
  reproduced an empty dog catalog with the empty search query.
- 2026-07-27: `BreedCatalog.fromJsonOrEmpty` and the ViewModel asset-loading
  boundary both convert parsing failures to an empty catalog, hiding the
  release-only deserialization error from the UI and logs.

## Regression Test

- Test file:
  `app/src/test/java/com/woliveiras/petit/domain/model/BreedCatalogR8ContractTest.kt`
- Test name:
  `catalog document fields declare stable serialized names`
- Failure observed before fix: yes
- Runtime boundary: rebuild the minified release AAB, install its universal APK,
  and verify that the empty query lists the expected dog and cat catalog counts

The structural regression test should require explicit serialized names on
every Gson-reflected catalog document field. The release verification must also
inspect the R8 outputs and exercise the installed minified artifact because a
debug-only parser test cannot prove release behavior.

## Fix

- Root cause: Gson deserializes private catalog document models by reflection,
  while their fields have no stable serialized-name annotations or equivalent
  R8 keep contract. Release minification removes or renames those fields.
  Parsing then fails validation and the fallback silently returns an empty
  catalog.
- Code change: declare explicit `@SerializedName` values on every
  reflected catalog document field so Gson's consumer rules retain the required
  release members and serialized names. Keep the public catalog model and JSON
  format unchanged.
- Why this fixes the root cause: the JSON contract no longer depends on
  obfuscatable Kotlin property names, and a future release-only parser failure
  cannot silently remove the packaged catalog data.

## Verification

- [x] Structural regression test fails before the fix
- [x] Structural regression test passes after the fix
- [x] Existing `BreedCatalogTest` and `BreedCatalogAssetTest` pass
- [x] Minified release mapping retains the reflected document fields
- [x] Minified release AAB contains the unchanged catalog asset
- [x] Installed minified release lists 356 dog breeds and 96 cat breeds
- [x] Mixed-breed, unknown, manual-entry, and no-selection paths remain visible
- [x] Related pet-form and breed-selection tests pass
- [x] Temporary instrumentation was not added

## Follow-ups

- [ ] Add a release-artifact smoke check to the release verification workflow
  so reflected asset parsers are exercised after minification.
- [ ] Audit other Gson-reflected private document models for the same R8 risk.
- [ ] Make packaged-catalog loading failures observable while preserving manual
  entry as the safe fallback.
