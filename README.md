# Petit Android App

Android app built with Kotlin, Jetpack Compose, Room, Hilt, and WorkManager.

Petit is a multi-pet health tracker — track weight, vaccinations, deworming, and reminders for cats, dogs, rabbits, birds, fish, and more.

## Technical Docs

- [Product specifications](specs/README.md)
- [Product requirements](prds/2026-07-17-petit-pet-health-management.md)
- [App architecture](docs/app-architecture.md)
- [Data modeling](docs/data-modeling.md)
- [Testing guide](docs/running-tests.md)
- [Accessibility testing guide (TalkBack)](docs/talkback-testing-guide.md)
- [Design system](docs/design-system.md)
- [Public release/compliance checklist](docs/release-compliance-checklist.md)

## Development

Pick the setup that matches your editor:

- [VS Code setup](docs/dev-setup/vscode.md) — uses the JetBrains Kotlin LSP + workspace tasks
- [Android Studio setup](docs/dev-setup/android-studio.md)
- [Android CLI development reference](docs/android-cli-development.md) — SDK, emulator, deeper CLI tooling

Quick commands:

- `./gradlew assembleDebug && ./gradlew installDebug`
- `./gradlew test`
- `./gradlew spotlessCheck`

## Policies

- [License](LICENSE)
- [NOTICE](NOTICE)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [Trademark policy](TRADEMARK_POLICY.md)
- [Code owners](.github/CODEOWNERS)
- [Issue templates](.github/ISSUE_TEMPLATE)
- [Pull request template](.github/pull_request_template.md)
- [Dependabot config](.github/dependabot.yml)

## Licensing Model

This repository is 100% open source:

- Code license: GNU AGPL-3.0.
- You can use, modify, and self-host the project under AGPL terms.

Commercial distribution and use of original brand assets are not allowed without explicit authorization.

## Source Code Availability

For any distributed binary build (APK/AAB), users must have clear access to the corresponding source code.

- Official source repository: this repository
- Preferred reference for releases: corresponding Git tag/commit hash
- If a network-deployed version is modified, make the corresponding source code publicly accessible under AGPL terms
