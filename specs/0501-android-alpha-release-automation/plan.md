# Plan: Android alpha release automation

Spec: [spec.md](./spec.md)

## Status

This plan is **Approved**. The user approved the spec, plan, and tasks on
2026-07-26.

## Dependencies

- Existing Android release signing in `app/build.gradle.kts`.
- Existing Spotless, build, and unit test behavior in
  `.github/workflows/ci.yml`.
- Existing publishable pt-BR image assets and image sources under
  `docs/store-listing/`.
- An existing `com.woliveiras.petit` Play Console application.
- A private upload keystore for signed-build verification.
- Separately configured Google Play Developer API access for external
  validation and deployment.
- A protected GitHub Environment named `alpha` for remote deployment.

No PRD or ADR is planned because the capability is repository release
infrastructure, does not change the application flow, and does not create an
irreversible data or runtime architecture decision.

## Architecture

- Pin Fastlane with Bundler and keep the lockfile compatible with the local and
  Linux runner platforms.
- Put release constants and input validation in small pure Ruby helpers so they
  can be tested without Google credentials or publication.
- Keep `track: "alpha"` and `release_status: "completed"` inside the Fastfile.
- Make the local environment and GitHub workflow supply the same file-based
  credential and existing `keystore.properties` contracts.
- Move publishable metadata into Supply's canonical
  `fastlane/metadata/android/` hierarchy. Keep only editable image source
  material under `docs/store-listing/`.
- Use a separate manual GitHub Actions workflow that prepares ephemeral OIDC
  credentials and signing files, calls the Fastlane lane, and always cleans up.
- Preserve `.github/workflows/ci.yml`; the release lane independently runs the
  required release checks before upload.

## Implementation sequence

1. [x] Approve and record the spec boundary. Update the spec and index from
   `Draft` to `Approved` only after explicit user approval.
2. [x] Add the reproducible Bundler and Fastlane foundation, pure release
   configuration checks, lane discovery, signed release build, metadata
   validation, validate-only, and alpha deploy behavior.
3. [x] Move the existing publishable pt-BR assets to the canonical Fastlane
   hierarchy, add conservative localized listing text and version-code release
   notes, and verify that no publishable duplicate remains.
4. [x] Add the manual protected GitHub Actions workflow with OIDC, ephemeral
   upload signing, minimal permissions, concurrency, cleanup, and static
   contract tests while preserving the existing CI workflow.
5. [x] Update `.gitignore`, `README.md`, the new release runbook, beta guide,
   compliance checklist, and affected store-listing source documentation.
6. [x] Run local/static verification and, when the private upload key is
   available, a real signed AAB build. Reconcile tasks and plan from fresh
   evidence.
7. [~] Keep validate-only API verification and tester availability pending.
   The real version-code-3 alpha deployment completed through GitHub Actions
   run `30252123569`.

## Vertical delivery boundaries

### 1. Fastlane alpha release

Deliver one local command that checks formatting and tests, validates signing
and metadata, builds the AAB, prevents a known duplicate version code, and
either validates or deploys only to alpha. Pure helpers and lane configuration
tests provide non-publishing evidence.

### 2. Protected remote invocation

Deliver a manual GitHub workflow that reconstructs only the inputs required by
the same Fastlane lane. Static workflow tests prove trigger, refs, environment,
permissions, concurrency, Fastlane delegation, cleanup, and the absence of
binary artifact upload.

### 3. Operator documentation

Deliver the local, Google, and GitHub setup runbook with accurate evidence
boundaries, fallback procedures, and the retained manual upload path.

## Planned files

The implementation may create or update:

- `Gemfile`
- `Gemfile.lock`
- `fastlane/Appfile`
- `fastlane/Fastfile`
- `fastlane/metadata/android/**`
- narrowly scoped Fastlane helper and test files
- `.github/workflows/release-alpha.yml`
- narrowly scoped workflow validation tests or scripts
- `.gitignore`
- `README.md`
- `docs/release-automation.md`
- `docs/beta-testing-guide.md`
- `docs/release-compliance-checklist.md`
- documentation beside `docs/store-listing/pt-BR/source/` if needed
- this spec folder and `specs/README.md`

The exact helper/test filenames will follow the smallest structure that lets
configuration be tested without publication. No production Android source or
existing CI workflow change is planned.

## Security review

- Confirm the current ignored keystore and properties remain untracked before
  every commit.
- Inspect the Fastlane version's Application Default and Workload Identity JSON
  support before pinning it.
- Inspect every selected GitHub action in its official repository, choose a
  supported revision, and pin its full commit SHA.
- Confirm the workflow has only `contents: read` and `id-token: write`.
- Confirm the release service account setup excludes production permission.
- Confirm no trigger can run fork or pull request code with release secrets.
- Confirm the track and release status cannot be overridden by workflow input.
- Confirm secret checks and cleanup never print secret values.
- Confirm artifacts exclude the AAB, mapping, keystore, properties, and
  credentials.
- Confirm workflow concurrency allows only one active alpha release.
- Confirm generated auth files and Fastlane reports are ignored.

## Verification plan

### Level 1: local and static

- Install from `Gemfile.lock` with Bundler.
- Check Ruby syntax for Fastlane and helper files.
- Run pure release configuration and metadata contract tests.
- Run `bundle exec fastlane lanes` and confirm all required Android lanes.
- Validate workflow YAML.
- Run `actionlint` when available.
- Test workflow trigger, ref restriction, environment, permissions,
  concurrency, cleanup, Fastlane command, alpha-only policy, and absence of
  artifact uploads.
- Run targeted tracked-secret and sensitive-path searches.
- Run `./gradlew spotlessCheck`.
- Run `./gradlew :app:test`.
- Run `git diff --check`.
- Inspect the complete diff and `git status --short`.

### Level 2: signed build

- Use the existing private local upload keystore without exposing it.
- Run `bundle exec fastlane android build_release`.
- Confirm `versionCode`, `versionName`, AAB path, valid signature, signing
  identity selection, and mapping path.
- Do not treat the build as Google Play acceptance.

### Level 3: API validation without publication

- Requires separate authorization and valid credentials.
- Run `bundle exec fastlane android validate_play_credentials`.
- Run `bundle exec fastlane android validate_alpha`.
- Record package, source commit, version, alpha track, validate-only result,
  and any permission or compatibility failure.

### Level 4: real alpha deployment

- Requires separate publication authorization, environment approval, an unused
  version code, and complete Play setup.
- Run the local or `workflow_dispatch` deployment once.
- Record the workflow/local run, Play edit result, version code, source commit,
  ref, and alpha status.

### Level 5: tester availability

- Have an approved tester install or update the exact release from Google Play.
- Record the observed version and tester access result separately from upload
  success.

## Commit boundaries

The user authorized local commits, but the repository requires confirmation
immediately before each commit. Proposed boundaries after spec approval:

1. `chore(release): add fastlane alpha deployment`
2. `ci(release): automate alpha deployment`
3. `docs(release): document automated publishing`

Before each commit, list the exact files, exclude the existing unrelated
`specs/0012-...` and test changes, run relevant checks, and request explicit
confirmation. No push, amend, rebase, merge, force-push, pull request, Google
Play publication, or external configuration is authorized.

## Risks and mitigations

- **Wrong track:** keep alpha immutable inside Fastlane and deny service-account
  production permission.
- **Duplicate version:** query known tracks before upload and preserve the Play
  rejection as a clear fallback.
- **Credential leakage:** use OIDC, private temporary paths, log-safe checks,
  ignored files, and unconditional cleanup.
- **Wrong signing identity:** use the existing upload-key contract, verify the
  AAB signature, and reserve Play acceptance as external evidence.
- **Metadata drift:** keep one publishable directory and reject duplicates or a
  missing version-specific changelog.
- **Concurrent edits:** serialize the workflow and do not cancel an active
  deploy.
- **Dependency drift:** lock Ruby dependencies and pin GitHub actions by full
  SHA after official-source review.
- **False completion:** keep API validation, deploy, and tester installation
  pending until their real boundaries run.
- **Parallel work contamination:** stage only this capability's reviewed files
  and preserve all unrelated working-tree changes.

## Rollback and interruption

- Before a Play edit is committed, stop the lane and let the uncommitted edit
  expire or delete it through an authorized operator procedure.
- After a completed alpha release, do not reuse the version code. Halt or
  replace the alpha release through an explicitly authorized Play operation.
- Never automate rollback to production.
- Canceling a GitHub run must still execute cleanup; if runner termination
  prevents cleanup, GitHub destroys the ephemeral hosted runner and the runbook
  requires credential review before retrying.
- Revert repository automation through a normal reviewed commit without
  deleting existing Play releases or signing identities.
