# Tasks: Android alpha release automation

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec approved on 2026-07-26. Repository implementation may proceed, but API
> validation and publication retain their separate authorization gates.

## Tasks

- [x] **Publish one signed alpha release through Fastlane** (test-type: both)
  - blocked-by: explicit spec approval
  - summary: add the locked Fastlane foundation and shared Android lanes for
    credential validation, release build, validate-only, and alpha deployment.
  - desired behavior: local operators can run one reproducible lane that checks
    the repository, validates signing and metadata, preserves source versions,
    rejects known duplicate version codes, and can publish only to alpha.
  - acceptance criteria: lane discovery, missing-input failures, immutable
    alpha track, explicit completed status, signed AAB, version reporting,
    metadata/changelog validation, optional mapping upload, duplicate handling,
    validate-only, and no production promotion match `spec.md`.
  - test expectations: pure Ruby tests cover constants and preflight behavior;
    syntax and lane discovery execute without publication; real API behavior
    remains at evidence level 3 or 4.
  - verification: Ruby syntax, pure configuration tests,
    `bundle exec fastlane lanes`, `bundle exec fastlane android build_release`,
    focused secret searches, and Gradle release output inspection.

- [x] **Version canonical pt-BR Play metadata** (test-type: both)
  - blocked-by: explicit spec approval
  - summary: move existing publishable images into Supply's canonical hierarchy
    and add reviewed title, descriptions, and version-code release notes.
  - desired behavior: one versioned directory contains every file Fastlane can
    publish while editable image sources remain clearly separate.
  - acceptance criteria: required pt-BR text, length limits, changelog for the
    source version code, image locations, screenshot ordering, supported
    extensions, and absence of duplicate publishable copies match `spec.md`.
  - test expectations: metadata contract tests run without credentials or
    network access and fail on a missing, empty, oversized, misplaced, or
    duplicated file.
  - verification: focused metadata tests, file inventory, duplicate search, and
    review of all localized text and image paths.

- [x] **Dispatch the shared alpha lane from GitHub Actions** (test-type: both)
  - blocked-by: Fastlane alpha release and canonical metadata
  - summary: add a separate protected manual workflow with OIDC, ephemeral
    signing files, minimal permissions, concurrency, and unconditional cleanup.
  - desired behavior: trusted `main` or `v*` source can invoke the same Fastlane
    deploy lane through the protected `alpha` environment without storing a
    long-lived Google key or leaking the upload key.
  - acceptance criteria: manual-only trigger, approved refs, environment,
    permissions, action SHA pins, OIDC variables, fallback separation, secret
    checks, temporary files, cleanup, concurrency, Fastlane delegation, no
    binary artifacts, and source SHA summary match `spec.md`.
  - test expectations: workflow parsing and static contract tests cover every
    security invariant; `actionlint` runs when available; no test consumes
    credentials or invokes a deployment.
  - verification: workflow contract tests, YAML parsing, `actionlint`, official
    action revision review, targeted secret/log searches, and confirmation that
    `.github/workflows/ci.yml` is unchanged.

- [x] **Document release setup, operation, and fallback** (test-type: integration)
  - blocked-by: Fastlane and workflow contracts
  - summary: add the detailed runbook and update README, beta guidance,
    compliance checks, ignore rules, and store-listing source guidance.
  - desired behavior: a release operator can configure local signing, Play API
    access, OIDC, GitHub Environment protection, metadata, validation, deploy,
    troubleshooting, cleanup, rollback, and manual fallback without copying a
    secret into Git.
  - acceptance criteria: every documentation requirement and manual boundary in
    `spec.md` is covered in English, including the less-recommended JSON
    fallback and the five evidence levels.
  - test expectations: link/path checks and targeted documentation searches
    confirm commands, variable names, no personal paths, no sample secrets, and
    no claim that local evidence proves external deployment.
  - verification: documentation review, command/path searches, secret-pattern
    audit, and `git diff --check`.

- [~] **Complete local and signed-build verification** (test-type: both)
  - blocked-by: all repository implementation tasks
  - summary: run fresh local/static and signed-build checks, remove temporary
    output, reconcile artifacts, and report external gaps.
  - desired behavior: evidence levels 1 and 2 are complete when their real
    prerequisites are available, while levels 3 through 5 remain pending.
  - acceptance criteria: locked dependency install, Ruby/Fastlane checks,
    metadata and workflow tests, `actionlint`, Spotless, unit tests, signed AAB,
    source version, signature, mapping, secret audit, diff check, plan/tasks
    reconciliation, and clean task-owned status are reported accurately.
  - test expectations: no new test code unless a verification gap exposes an
    uncovered acceptance criterion.
  - verification: every command in the first two levels of `plan.md`, full diff
    review, and `git status --short`.
  - current evidence: Ruby syntax and contracts, lane discovery, locked gems,
    workflow lint, metadata audit, secret-path audit, and a signed AAB build
    passed. The latest rerun stops at Spotless because unrelated navigation
    work in the shared checkout currently has formatting violations; those
    files are intentionally untouched.

- [ ] **Validate the alpha edit against Google Play** (test-type: integration)
  - blocked-by: complete local verification; separate API authorization and
    credentials
  - summary: validate credentials and the complete alpha edit with
    `validate_only` without publication.
  - desired behavior: Google Play accepts the package, identity, unused version
    code, signed AAB, metadata, changelog, images, and available mapping without
    committing the edit.
  - acceptance criteria: evidence records the real package, source commit,
    version, track, credential mode, validation result, and any permission or
    Fastlane compatibility failure.
  - test expectations: this is a real external integration check, not a mock or
    local substitute.
  - verification: separately authorized
    `bundle exec fastlane android validate_play_credentials` and
    `bundle exec fastlane android validate_alpha`.

- [ ] **Deploy and observe a real alpha release** (test-type: integration)
  - blocked-by: successful API validation; separate publication authorization;
    configured tester access
  - summary: run one protected deployment and confirm availability for an
    approved tester.
  - desired behavior: the exact signed build and metadata are committed only to
    alpha and an approved tester can install or update that version.
  - acceptance criteria: Play accepts the unused version code and upload key;
    the release is completed on alpha; workflow/local run and source SHA/ref are
    recorded; tester availability is observed separately.
  - test expectations: deployment and tester observation are real external
    evidence and cannot be replaced by validate-only, mocks, workflow lint, or
    a local build.
  - verification: separately authorized alpha deployment record and tester
    installation observation.
