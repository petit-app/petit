# Practical Google Play Alpha Testing Guide

This guide separates one-time Play setup, automated alpha deployment, tester
management, and manual fallback. See
[Android release automation](release-automation.md) for the complete security
and configuration runbook.

## Prerequisites

- Active Google Play Console account
- Existing `com.woliveiras.petit` app in Play Console
- Signed release AAB build using the registered upload key
- Public privacy policy URL
- App icon (512x512 PNG)
- At least two screenshots
- Tester email list

## 1. Create the App in Play Console

1. Open https://play.google.com/console.
2. Select Create app.
3. Fill basic fields:

- App name
- Default language (English recommended)
- App type: App
- Distribution: Free or paid

4. Confirm required declarations and create.

## 2. Complete Mandatory App Content

In Play Console, complete these sections before publishing:

- Privacy policy
- App access
- Ads declaration
- Content rating questionnaire
- Target audience
- Data safety form

## 3. Configure Store Listing

Recommended fields:

- App name
- Short description (<= 80 chars)
- Full description (<= 4000 chars)
- Phone screenshots
- App icon
- Optional feature graphic

Keep listing text aligned with implemented app behavior.

## 4. Configure Release Automation

Complete the one-time setup in the
[release automation runbook](release-automation.md):

- Google Play Developer API;
- dedicated publisher service account with app-level testing permissions;
- local credential file outside the repository;
- GitHub Workload Identity Federation;
- protected GitHub Environment `alpha`;
- temporary upload-key secrets.

The repository uses `fastlane/metadata/android/` as the source of truth for
listing text, release notes, and publishable images.

## 5. Prepare the Release Version

Before building, update app version in:

`app/build.gradle.kts`

Example:

```kotlin
defaultConfig {
	versionCode = 4
	versionName = "2.0.1"
}
```

Add localized release notes using the exact version code:

```text
fastlane/metadata/android/pt-BR/changelogs/4.txt
```

Fastlane never changes the version automatically.

## 6. Build Locally Without Publishing

Install locked dependencies and build:

```bash
bundle install
bundle exec fastlane android build_release
```

The lane runs Spotless and unit tests, builds the signed AAB, verifies its JAR
signature, and reports the R8 mapping when generated.

`app/build/outputs/bundle/release/app-release.aab`

This is signed-build evidence only. It does not prove that Google Play accepts
the upload key or release.

## 7. Validate Through the API Without Publishing

With separately authorized credentials:

```bash
export GOOGLE_PLAY_JSON_KEY=/absolute/private/path/play-publisher.json
bundle exec fastlane android validate_play_credentials
bundle exec fastlane android validate_alpha
```

`validate_alpha` uses Google Play `validate_only`. It is an external API check,
not a deployment.

## 8. Deploy Locally to Alpha

After separate publication authorization:

```bash
bundle exec fastlane android deploy_alpha
```

The lane publishes only to `alpha` with release status `completed`.

## 9. Deploy Through GitHub Actions

1. Commit or tag the exact source.
2. Open Actions -> **Release Android Alpha**.
3. Select `main` or an approved `v*` tag.
4. Run the workflow.
5. Wait for the `alpha` environment reviewer.
6. Inspect the source SHA/ref summary and Play result.

Local and GitHub deployments call the same Fastlane lane.

## 10. Add Testers

1. Open Testers tab.
2. Create email list.
3. Add tester emails.
4. Save and assign the list to the alpha track.
5. Share invite link with testers.

## 11. Tester Instructions

Share this checklist with testers:

1. Accept invite link.
2. Install app from Play Store.
3. Use core flows.
4. Report bugs with:

- Steps to reproduce
- Expected vs actual result
- Screenshot/video when possible

## 12. Collect Feedback

Use one or more channels:

- Play Console private reviews
- Google Forms
- Team chat channel

## 13. Release Updates

For each new beta iteration:

1. Increase `versionCode`.
2. Update `versionName`.
3. Add `<versionCode>.txt` release notes for every metadata locale.
4. Run the local checks and signed build.
5. Validate through the API when authorized.
6. Deploy locally or through the protected workflow.

Notes:

- `versionCode` must be greater than the last uploaded build on Play Console.
- Keep `versionName` aligned with release notes to simplify tester communication.

## 14. Manual Play Console Fallback

Use the console only when automation is unavailable or an already committed
release needs intervention:

1. Run `bundle exec fastlane android build_release`.
2. Open Testing -> Alpha.
3. Create a release.
4. Upload `app/build/outputs/bundle/release/app-release.aab`.
5. Copy reviewed release notes from
   `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
6. Confirm the source commit or tag.
7. Start the alpha release.

Do not create independent listing copy in Play Console without applying the
same reviewed change to the canonical repository metadata.

## 15. Production Promotion

When beta quality is acceptable:

- No critical crashes
- Core flows validated
- Policy sections complete
- Support and rollback plan ready

Production promotion is outside the first automation. Perform it only through
a separately reviewed and authorized process.

## Common Issues

### Data safety form incomplete

Complete all fields in Policy -> App content.

### targetSdk rejected

Ensure project `targetSdk` satisfies current Play policy.

### Signing issues

Use Google Play App Signing and verify upload key configuration.

### Version code rejected

Version codes cannot be reused. Update the source version explicitly and add
the matching localized changelog.

### Workflow cannot access credentials

Confirm that the run uses `main` or a `v*` tag, the `alpha` environment is
approved, and exactly one Google authentication mode is configured.
