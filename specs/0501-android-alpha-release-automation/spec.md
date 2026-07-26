---
spec: "0501"
title: Android alpha release automation
family: release-engineering
phase: 1
status: In Progress
owner: woliveiras
depends_on: []
origin: "User request, 2026-07-26"
---

# Spec: Android alpha release automation

## Context and motivation

Petit has a signed Android release build and manual Google Play guidance, but
it has no reproducible command for publishing an Android App Bundle (AAB),
release notes, or store listing metadata. Release operators must currently
assemble the bundle locally and complete the upload through Google Play
Console.

This specification adds repository release infrastructure and documentation.
It does not change an application flow, runtime behavior, or product
requirement. A PRD is not required because the work changes how maintainers
publish an existing app, not what caregivers can do in Petit.

The checkout confirms:

- the Android application ID is `com.woliveiras.petit`;
- the current release is `versionCode = 2`, `versionName = "1.0.1"`;
- release signing reads a local, ignored `keystore.properties`;
- Google Play App Signing is the documented signing model;
- CI already runs Spotless, a debug build, and unit tests;
- the repository has no `Gemfile`, lockfile, or Fastlane configuration;
- `docs/store-listing/pt-BR/` contains publishable images and their source
  material, but no versioned title, descriptions, or release notes;
- a repository bugfix document records a Google Play Console report for release
  1 (`1.0.0`), which is checkout evidence of an earlier publication. API access
  must still be validated before relying on that external state.

## Classification and approval boundary

- Classification: release infrastructure and documentation.
- Product flow change: none.
- PRD: not required.
- ADR: not required. Fastlane, metadata layout, and workflow authentication are
  repository delivery choices that can be changed without migrating caregiver
  data or changing an irreversible application contract.
- Approval gate: the user approved this spec, plan, and task list on
  2026-07-26.
- External gate: no Google Cloud, GitHub, or Google Play mutation is authorized
  by approving this spec. A real API validation or alpha deploy requires
  credentials and separate explicit authorization.

## Release architecture

Fastlane is the single deployment entry point. Local operators and GitHub
Actions call the same lanes. The GitHub workflow prepares ephemeral
credentials and signing material, then delegates release checks, build
validation, and Play upload behavior to Fastlane.

```text
Local operator                         GitHub Actions workflow_dispatch
GOOGLE_PLAY_JSON_KEY                   OIDC-generated credential file
ignored keystore.properties            temporary keystore.properties
           \                           /
            bundle exec fastlane android deploy_alpha
                              |
                 checks -> signed AAB -> alpha
                              |
              metadata + changelog + R8 mapping
```

The implementation must add a Bundler-managed Fastlane installation with:

- `Gemfile` and `Gemfile.lock`, including the Linux platform used by GitHub
  Actions;
- `fastlane/Appfile`;
- `fastlane/Fastfile`;
- `fastlane/metadata/android/`;
- only the helper and test files needed to validate release configuration
  without publication.

## Fastlane contract

### `android validate_play_credentials`

- Require `GOOGLE_PLAY_JSON_KEY`.
- Require an absolute path to an existing, readable credential file outside
  the repository for local use.
- Accept the Application Default Credentials JSON produced by
  `google-github-actions/auth` for Workload Identity Federation.
- Call Fastlane's Play credential validation without uploading an AAB or
  committing a Play edit.
- Return a clear failure for a missing file, unreadable JSON, rejected
  identity, disabled API, unknown package, or insufficient Play permissions.
- Never print credential contents.

Credential validation is an external API operation. It remains unverified
until it is run with an authorized identity.

### `android build_release`

- Run `./gradlew spotlessCheck`.
- Run the existing unit test suite with `./gradlew :app:test`.
- Build the release bundle with `./gradlew :app:bundleRelease`.
- Require the existing local `keystore.properties` contract.
- Fail before build when the properties file, referenced keystore, alias, or
  required values are missing.
- Require `app/build/outputs/bundle/release/app-release.aab`.
- Verify that the AAB has a valid JAR signature and was produced through the
  configured release signing identity.
- Read and report the source `versionCode` and `versionName`, but never modify
  either value.
- Expose the R8 mapping path when
  `app/build/outputs/mapping/release/mapping.txt` exists.

### `android validate_alpha`

- Run the same preflight, checks, signed build, metadata validation, and
  version checks as `deploy_alpha`.
- Call `upload_to_play_store` with `track: "alpha"`,
  `release_status: "completed"`, and `validate_only: true`.
- Include the AAB, the version-specific pt-BR changelog, listing metadata,
  supported images, screenshots, and R8 mapping when present.
- Never select or promote to production.

`validate_only` exercises Google Play's validation boundary but is not a
publication and is not proof that testers can install the app. It still
requires separate authorization because it accesses an external service.

### `android deploy_alpha`

- Run the same release checks, signed build, metadata validation, and version
  checks as `validate_alpha`.
- Confirm that the expected AAB exists after the build.
- Query used version codes from the standard Play tracks before upload and fail
  clearly if the source `versionCode` is already present.
- Also translate a duplicate-version response from Google Play into an
  actionable failure, because an artifact not currently visible on a standard
  track may still reserve its version code.
- Call `upload_to_play_store` with an immutable `track: "alpha"` and explicit
  `release_status: "completed"`.
- Upload the AAB, version-specific release notes, localized listing text,
  supported graphics and screenshots, and the R8 mapping when generated.
- Never increment or rewrite `versionCode` or `versionName`.
- Never accept a production track as a lane parameter.
- Never promote a release to production.

The supported local command is:

```bash
bundle exec fastlane android deploy_alpha
```

## Metadata source of truth

`fastlane/metadata/android/` is the only source of truth for files that Supply
can publish. The initial locale is `pt-BR`, and new locales use the same
structure:

```text
fastlane/metadata/android/
└── pt-BR/
    ├── title.txt
    ├── short_description.txt
    ├── full_description.txt
    ├── changelogs/
    │   └── 2.txt
    └── images/
        ├── icon.png
        ├── featureGraphic.png
        ├── phoneScreenshots/
        ├── sevenInchScreenshots/
        └── tenInchScreenshots/
```

- The existing publishable PNG files move into this Fastlane hierarchy. They
  are not copied.
- Editable image sources remain in `docs/store-listing/pt-BR/source/`.
- The existing ZIP is not a source of truth and must not remain as an
  independently maintained publishable copy.
- The initial pt-BR title and descriptions must use conservative claims
  supported by the current repository and public documentation.
- Release notes are stored under each locale's `changelogs/` directory with a
  filename that exactly matches the source `versionCode`.
- `deploy_alpha` fails when a supported locale lacks title, short description,
  full description, or the changelog for the current version code.
- Validation enforces Play length limits, non-empty files, expected image
  locations, supported extensions, and deterministic screenshot ordering.
- The metadata contract permits additional locale directories without changing
  lane code.
- A repository check rejects publishable duplicates under
  `docs/store-listing/` and rejects missing Fastlane metadata.

## Android signing

Google Play App Signing remains enabled, with a separate upload key used to
sign the AAB before upload.

Local builds preserve the current `keystore.properties` workflow. The file and
the referenced keystore remain ignored and outside Git.

The GitHub `alpha` environment stores:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`;
- `ANDROID_UPLOAD_STORE_PASSWORD`;
- `ANDROID_UPLOAD_KEY_ALIAS`;
- `ANDROID_UPLOAD_KEY_PASSWORD`.

The release job must:

1. create a private temporary directory under the runner's temporary path;
2. decode the keystore without printing its encoded or decoded contents;
3. create `keystore.properties` without logging its secret values;
4. use restrictive file permissions;
5. run the shared Fastlane lane;
6. remove the keystore, properties, and generated Google credential file in an
   `always()` cleanup step, including after failure;
7. never upload signing material or credential files as artifacts.

Local signature checks prove which configured upload key signed the generated
AAB. Only an authorized Google Play validation or upload can prove that Google
Play accepts that key for `com.woliveiras.petit`.

## Google Play authentication

GitHub Actions uses Workload Identity Federation through a dedicated Google
Cloud service account. The GitHub Environment provides these non-secret
variables:

- `GCP_WORKLOAD_IDENTITY_PROVIDER`;
- `GCP_PLAY_PUBLISHER_SERVICE_ACCOUNT`.

The release job grants only:

```yaml
permissions:
  contents: read
  id-token: write
```

Federation must restrict the incoming GitHub identity to the
`petit-app/petit` repository, the approved `main` or `v*` ref, and the `alpha`
environment where supported by the configured claims. The service account is
granted only the Google Play Console permissions required to view app
information and create releases on the alpha track. It must not receive global
administrator or production-release permission for convenience.

The workflow uses `google-github-actions/auth` after checkout and passes its
generated Application Default Credentials file to `GOOGLE_PLAY_JSON_KEY`. The
selected Fastlane version must document support for Application Default or
Workload Identity JSON through Supply's `json_key` input. `gha-creds-*.json` is
ignored and removed after the job.

A temporary fallback may use the GitHub Environment secret
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. It must be documented as less recommended,
must never be written outside the runner's private temporary directory, and
must not coexist with the OIDC path in one run.

## GitHub Actions contract

The new `.github/workflows/release-alpha.yml` workflow must:

- run only through `workflow_dispatch`;
- reject refs other than `main` and tags matching `v*` before credentials or
  signing secrets are used;
- use the GitHub Environment `alpha`;
- support required reviewers, disabled self-review, and matching deployment
  branch/tag restrictions configured in repository settings;
- use concurrency with cancellation disabled so one alpha release cannot
  replace another in progress;
- keep the existing `.github/workflows/ci.yml` unchanged;
- call `bundle exec fastlane android deploy_alpha` instead of reimplementing
  the upload;
- run on trusted repository code, not pull request or fork code;
- use current official actions verified during implementation and pin them to
  full commit SHAs when compatible with Dependabot's existing GitHub Actions
  configuration;
- fail with a clear message when an approved ref, signing secret, OIDC
  variable, credential, metadata file, version, AAB, or mapping contract is
  invalid;
- never expose secret values through command tracing or diagnostic output;
- not upload the signed AAB, R8 mapping, keystore, properties, or credential
  JSON as GitHub artifacts;
- record the source commit SHA and ref in the workflow summary so the
  distributed binary can be traced to source.

The workflow does not run automatically on pushes to `main` and does not
promote to production.

## Security requirements

- No credentials, service-account JSON, keystores, passwords, or tokens are
  tracked by Git.
- `.gitignore` covers `gha-creds-*.json`, Fastlane reports and logs, temporary
  signing files, and generated local release artifacts without ignoring the
  versioned metadata.
- Release commands do not use shell tracing around secrets.
- Secret presence checks report only variable names, never values.
- Temporary credentials and signing files are removed even when a build or
  upload fails.
- Supply receives the hard-coded alpha track from the Fastfile.
- Concurrency prevents overlapping alpha edits and uploads.
- No workflow event accepts pull request code.
- The release service account cannot publish to production.
- A real release requires the protected `alpha` environment and its external
  reviewer and ref restrictions.

## Automation and manual boundaries

Fastlane and the Google Play Publishing API can automate:

- AAB upload;
- assignment and release to the alpha track;
- localized release notes;
- localized title, short description, and full description;
- supported icons, graphics, and screenshots;
- track promotion in general, although production promotion is excluded here.

The following may still require Google Play Console or Google Cloud setup:

- initial application, Google Cloud project, API, service account, and
  Workload Identity Federation setup;
- Play Console permissions and environment protection;
- legal terms and agreements;
- Data Safety;
- content rating;
- target audience;
- policy declarations;
- Google review;
- initial tester groups and opt-in configuration when not covered by the API;
- investigation or rollback after Google Play has committed a release.

The repository records evidence of a previous release, but the service account
must still be tested against the existing app before any deployment lane is
used.

## Documentation requirements

Implementation must update:

- `README.md` with a short Release Automation section, principal local
  commands, and links to detailed documentation;
- `docs/release-automation.md` with architecture, prerequisites, Google Cloud
  and Play Console setup, local setup, GitHub Environment setup, OIDC,
  fallback authentication, signing secrets, metadata, commands, manual
  dispatch, validate-only, troubleshooting, rollback, interruption, and manual
  responsibilities;
- `docs/beta-testing-guide.md` to separate initial setup, local Fastlane
  deployment, GitHub Actions deployment, tester management, and the retained
  manual fallback;
- `docs/release-compliance-checklist.md` with Fastlane, metadata, signing,
  version, credential, artifact, source commit/tag, and external-evidence
  checks;
- `.gitignore` with the release-specific generated and sensitive patterns;
- image-source documentation affected by moving the publishable outputs.

All repository documentation, comments, and examples remain in English.

## Test strategy and evidence levels

| Evidence level | Required verification | Completion rule |
| --- | --- | --- |
| 1. Local and static | Ruby syntax, Fastlane lane discovery, pure release configuration tests, metadata contracts, workflow YAML, `actionlint`, minimal permissions, trigger/ref/track/lane checks, secret-path checks, Spotless, unit tests, and `git diff --check` | May be completed from fresh local output |
| 2. Signed build | Real `bundleRelease`, expected AAB, source version values, valid AAB signature, selected upload key, and R8 mapping when generated | May be completed only with an actual private upload keystore |
| 3. API validation without publication | Credential validation and `validate_alpha` with `validate_only` against `com.woliveiras.petit` | Pending until separately authorized credentials and API execution are available |
| 4. Real alpha deployment | Authorized `deploy_alpha`, Google Play edit committed to alpha, accepted version code, and recorded workflow or local run | Pending until separately authorized |
| 5. Tester availability | Approved tester can install or update the exact alpha release through Google Play | Pending until observed by a real tester |

Tests added for this capability must not contact Google Play or modify external
state. They should use pure helpers and static workflow checks. Mocks and
`validate_only` must be reported at their actual evidence level.

## Acceptance criteria

- [x] Given the repository dependencies, when Bundler installs from
  `Gemfile.lock`, then Fastlane is reproducible locally and on the Linux GitHub
  runner.
- [x] Given the Fastfile, when its syntax and lane discovery are checked, then
  `validate_play_credentials`, `build_release`, `validate_alpha`, and
  `deploy_alpha` are available under the Android platform.
- [x] Given release configuration tests without Play credentials, when they
  run, then they prove that publication is fixed to alpha, the release status
  is `completed`, production promotion is unavailable, and missing inputs fail
  safely without an upload.
- [x] Given current source version metadata, when either alpha lane runs, then
  it uses exactly `versionCode = 2` and `versionName = "1.0.1"` unless those
  values were explicitly changed in a reviewed source change.
- [ ] Given a missing or already-used version code, when preflight runs, then
  deployment stops with an actionable error and does not change Gradle version
  values.
- [x] Given valid local signing configuration, when `build_release` runs, then
  it executes Spotless and unit tests, produces the expected release AAB,
  verifies its signature, and locates its R8 mapping when generated.
- [x] Given absent or incomplete signing configuration, when a release lane
  starts, then it fails before upload without logging secret values.
- [x] Given the canonical metadata directory, when metadata validation runs,
  then every supported locale has valid title, short description, full
  description, current-version changelog, and correctly located publishable
  images without a second publishable copy under `docs/store-listing/`.
- [ ] Given valid Play credentials and an unused version code, when
  `bundle exec fastlane android validate_alpha` is separately authorized, then
  Google Play validates the alpha edit without publishing it.
- [ ] Given valid credentials, upload signing, an unused version code, and
  separately authorized publication, when
  `bundle exec fastlane android deploy_alpha` runs, then the signed AAB,
  localized metadata, release notes, images, screenshots, and available
  mapping are committed only to the alpha track with status `completed`.
- [x] Given the release workflow, when static validation runs, then it proves
  the only trigger is `workflow_dispatch`, refs are limited to `main` and
  `v*`, the environment is `alpha`, permissions are `contents: read` and
  `id-token: write`, concurrency is present, and the workflow calls the shared
  Fastlane deploy lane.
- [ ] Given a pull request, fork, push, unauthorized ref, missing secret, or
  missing OIDC setting, when GitHub evaluates the workflow, then no alpha
  publication begins and no secret is exposed.
- [ ] Given a workflow failure or success, when cleanup runs, then temporary
  keystore, properties, and Google credentials are removed and none are
  uploaded as artifacts.
- [ ] Given a completed automated alpha run, when its record is inspected, then
  the exact source commit SHA and ref can be associated with the distributed
  binary.
- [x] Given an operator who cannot use automation, when the beta guide is read,
  then the manual Play Console upload remains available as a documented
  fallback.
- [x] Given a local build, static check, mock, API validation, real deploy, or
  tester install, when results are reported, then each result is assigned only
  to its matching evidence level and external criteria remain pending until
  actually executed.

## Edge cases

- The version code exists in an inactive, draft, or superseded Play artifact
  even when standard track queries do not return it.
- The OIDC provider authenticates the repository but not the selected ref or
  environment.
- The GitHub Environment exists without required reviewers or matching
  deployment branch/tag rules.
- The auth action creates an external-account credential JSON that an older
  Fastlane version cannot consume.
- A service account can authenticate but lacks app-level alpha release
  permissions.
- The app exists in Play Console but the Publishing API has not been enabled.
- A metadata locale has text but lacks a current-version changelog.
- A screenshot filename changes its ordering in the Play listing.
- R8 succeeds but the expected mapping file is missing.
- Gradle produces an unsigned or incorrectly signed bundle.
- A workflow is manually dispatched from a stale tag whose version code has
  already been uploaded.
- An upload edit succeeds but committing the edit fails.
- Google accepts the alpha release but no tester group can access it.
- Cleanup runs after an earlier step did not create all temporary files.

## Decisions

| Decision | Choice | Reasoning |
| --- | --- | --- |
| Delivery tool | Fastlane Supply through Bundler | Supports AABs, localized metadata, changelogs, images, tracks, mappings, and validate-only through one local and CI entry point. |
| Deployment source | Fastlane lanes only | Keeps local and GitHub behavior aligned and avoids a second upload implementation in YAML. |
| Track | Hard-coded `alpha` | Prevents accidental production publication in the first automation. |
| Release status | Explicit `completed` | Makes an authorized alpha deploy available to configured testers; validate-only remains the non-publishing path. |
| Metadata | `fastlane/metadata/android/` is canonical | Matches Supply's supported layout and removes independently maintained publishable copies. |
| Authentication | GitHub OIDC plus Workload Identity Federation | Avoids a routine long-lived Google service-account key in GitHub. |
| Authentication fallback | Environment secret JSON, temporary and discouraged | Provides an explicit recovery path when federation cannot yet be configured. |
| Signing | Existing local properties plus ephemeral CI upload key | Preserves the local contract and Google Play App Signing separation. |
| Workflow trigger | Manual dispatch from `main` or `v*` | Prevents automatic publication and ties releases to approved source refs. |
| Artifacts | Do not upload signed AAB or mapping to GitHub | Reduces exposure in the public repository and leaves Play as the release binary destination. |
| PRD | Not required | No application or product flow changes. |
| ADR | Not required | The delivery tooling is repository-scoped and reversible. |

## Official sources

- [Fastlane `upload_to_play_store`](https://docs.fastlane.tools/actions/upload_to_play_store/)
- [Google Play Developer API](https://developers.google.com/android-publisher)
- [Google Play API setup](https://developers.google.com/android-publisher/getting_started)
- [GitHub Actions environments](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
- [GitHub Actions deployment OIDC](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments)
- [Google GitHub Actions authentication](https://github.com/google-github-actions/auth)

These sources confirm that Supply accepts AABs, metadata, localized changelogs,
images, mappings, alpha tracks, explicit release status, validate-only, and
Application Default or Workload Identity credential JSON. They also establish
the protected-environment and short-lived OIDC authentication model. Exact
dependency and action revisions must be rechecked from their official
repositories immediately before implementation and recorded in the lockfile or
full workflow SHA.

## Out of scope

- Automatic production promotion or production deployment.
- Automatic deployment on push, merge, or pull request.
- Google Play Console application creation.
- Creation or mutation of Google Cloud, Workload Identity, service accounts,
  Play permissions, GitHub Environment settings, reviewers, secrets, or
  variables.
- Real API validation, publication, or tester installation without separate
  authorization.
- Automatic `versionCode` or `versionName` changes.
- Tester group or email-list automation.
- Legal, policy, Data Safety, content rating, target audience, or Google review
  automation.
