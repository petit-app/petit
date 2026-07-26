# Release and Compliance Checklist (Public Repository)

This document defines the minimum steps to publish updates safely in a public repository while protecting security, brand assets, and licensing constraints.

## 1. Scope of publication

Before every release, confirm that this repository contains only public technical content.

- Keep only technical docs in `docs/`.
- Keep product strategy, roadmap, and monetization docs in the separate specs repository.
- Do not publish business planning documents in this repository.

## 2. Sensitive files and secrets audit

Run before creating a release tag:

```bash
git ls-files | rg 'petit-release-key\.jks|keystore\.properties|local\.properties|google-services\.json' || true
```

Expected result:

- No sensitive files tracked.
- Only template files may appear (for example, `keystore.properties.template`).

Also check for accidental secrets in text files:

```bash
rg -n --hidden --glob '!build/**' --glob '!.git/**' \
  'api[_-]?key|secret|token|client_secret|storePassword|keyPassword'
```

If any real secret is found:

1. Remove it from source immediately.
2. Rotate compromised credentials.
3. Rewrite history only if needed and coordinated.

## 3. Legal and policy compliance gate

Confirm these files exist and are up to date:

- `LICENSE` (GNU AGPL-3.0)
- `NOTICE`
- Public privacy policy URL (`https://woliveiras.github.io/petit/privacy-policy/`)
- `TRADEMARK_POLICY.md`
- `SECURITY.md`
- `CONTRIBUTING.md`

Confirm public messaging consistency:

- Open-source use, modification, and self-hosting are allowed under AGPL terms.
- Brand assets (name/logo/identity) are restricted by trademark policy.

## 4. Build and quality gates

Run locally before release:

```bash
./gradlew assembleDebug && ./gradlew installDebug
./gradlew test
./gradlew spotlessCheck
```

For an Android alpha release, use the shared release lane:

```bash
bundle install
bundle exec fastlane android build_release
```

Confirm:

- Fastlane came from `Gemfile.lock`.
- `bundle exec fastlane lanes` lists the four Android release lanes.
- The release lane ran Spotless and unit tests.
- The signed AAB exists at the expected release path.
- `jarsigner -verify` reports `jar verified`.
- The R8 mapping exists for the minified release.
- Local signature success is not reported as Google Play acceptance.

Optional cleanup when style fails:

```bash
./gradlew spotlessApply
```

## 5. Dependency and security hygiene

Before tagging:

- Review Dependabot or dependency alerts.
- Check for critical/high CVEs in runtime and build dependencies.
- Prefer patch/minor upgrades for security updates.
- Confirm no newly introduced risky permissions/unsafe APIs.
- Keep `.github/dependabot.yml` active with:
  - grouped security updates,
  - reduced PR noise for minor/patch,
  - semver-major updates reviewed manually.

## 6. Release metadata checklist

- `versionCode` and `versionName` were reviewed in
  `app/build.gradle.kts`.
- The version code has not already been used in Google Play.
- Fastlane did not increment or rewrite either version.
- `fastlane/metadata/android/` is the only publishable metadata source.
- Every locale has non-empty title, short description, and full description.
- Every locale has `changelogs/<versionCode>.txt`.
- Publishable graphics and screenshots exist in Supply's expected directories.
- No independent publishable PNG, JPEG, or ZIP remains under
  `docs/store-listing/`.
- Release notes include:
  - What changed.
  - Any migration notes.
  - Security-relevant fixes.
- Public docs links still valid.

## 7. Automated alpha release gate

Before local or GitHub deployment:

- Run the Fastlane configuration and metadata tests.
- Validate `.github/workflows/release-alpha.yml` with `actionlint`.
- Confirm the only workflow trigger is `workflow_dispatch`.
- Confirm refs are limited to `main` and `v*`.
- Confirm the workflow uses the protected `alpha` environment.
- Confirm permissions are only `contents: read` and `id-token: write` on the
  release job.
- Confirm all release actions are pinned to full commit SHAs.
- Confirm workflow concurrency prevents overlapping alpha deployments.
- Confirm the workflow calls
  `bundle exec fastlane android deploy_alpha`.
- Confirm Fastlane hard-codes `track: "alpha"` and
  `release_status: "completed"`.
- Confirm there is no production promotion parameter.
- Confirm the service account lacks production-release permission.
- Confirm no AAB, mapping, keystore, properties, or credential JSON is uploaded
  as a GitHub artifact.
- Confirm cleanup uses `always()` and removes temporary signing and credential
  files.

## 8. Signing and credential gate

- Local `keystore.properties` and its upload keystore remain ignored.
- GitHub uses the protected upload-key secrets documented in
  [Android release automation](release-automation.md).
- GitHub OIDC variables identify the approved Workload Identity provider and
  publisher service account.
- Federation restricts repository, `alpha` environment, and approved refs.
- Exactly one Google authentication mode is configured.
- The JSON fallback, if temporarily used, is stored only as the protected
  `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` environment secret.
- `gha-creds-*.json` and Fastlane generated reports are ignored.
- Logs contain no keystore, password, token, or credential JSON content.
- `git ls-files` shows no private signing or credential material.

## 9. Source provenance

Record before distribution:

- exact commit SHA;
- source ref;
- release tag when applicable;
- `versionCode`;
- `versionName`;
- AAB SHA-256 when policy requires it;
- local or GitHub workflow run;
- Play alpha result.

The commit or tag must correspond to the source used for the distributed
binary. Do not infer this relationship from an uncommitted local build.

## 10. External evidence levels

Track these separately:

1. local/static validation;
2. signed AAB build;
3. Google Play API validate-only;
4. real alpha deployment;
5. real tester installation.

Do not mark levels 3 through 5 complete without the corresponding authorized
external execution.

## 11. GitHub recommended settings

Apply once per repository (and review quarterly).

### Branch protection (main)

- Require pull request before merge.
- Require at least 1 approving review.
- Dismiss stale approvals on new commits.
- Require status checks to pass.
- Block force push and deletion on protected branch.

### Security settings

- Enable Dependabot alerts.
- Enable Dependabot security updates.
- Enable secret scanning and push protection (if available).
- Keep private vulnerability reporting enabled (Security Advisories).

### Collaboration governance

- Add CODEOWNERS for critical areas:
  - `app/src/main/java/com/woliveiras/petit/data/**`
  - `app/src/main/java/com/woliveiras/petit/worker/**`
  - `LICENSE`, `NOTICE`, `TRADEMARK_POLICY.md`, `SECURITY.md`
- Keep issue and PR templates active.

## 12. Pre-public-release final check (quick run)

```bash
# 1) Sensitive tracked files
git ls-files | rg 'petit-release-key\.jks|keystore\.properties|local\.properties|google-services\.json' || true

# 2) Secret patterns in repo
rg -n --hidden --glob '!build/**' --glob '!.git/**' \
  'api[_-]?key|secret|token|client_secret|storePassword|keyPassword'

# 3) Build + tests + style
bundle exec fastlane android build_release

# 4) Fastlane and workflow contracts
ruby fastlane/test/release_config_test.rb
ruby fastlane/test/fastfile_contract_test.rb
ruby fastlane/test/workflow_contract_test.rb
actionlint .github/workflows/release-alpha.yml

# 5) Source provenance
git rev-parse HEAD
git status --short
```

Passing these checks permits a separately authorized API validation or release.
It does not prove a Google Play deployment.

## 13. Incident fallback (if something leaks)

- Revoke and rotate exposed credentials immediately.
- Remove sensitive artifact from latest commit.
- Evaluate history rewrite and communication impact.
- Publish security advisory if users are affected.
- Add a regression checklist item to prevent recurrence.
