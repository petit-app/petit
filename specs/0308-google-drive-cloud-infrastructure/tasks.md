# Tasks: Google Drive Cloud Infrastructure

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **In Progress**. Offline implementation is complete; every real
> Google Cloud or state operation retains its separate gate.

## Tasks

- [x] **Create or adopt one protected Drive project** (test-type: both)
  - blocked-by: spec approval
  - summary: implement the reusable module's adopt-by-default and explicit project-creation paths.
  - desired behavior: each invocation resolves exactly one project, prevents accidental project deletion, and exposes only non-secret project identity outputs.
  - acceptance criteria: existing-project, created-project, organization/folder exclusivity, billing choice, replacement risk, and import paths match `spec.md`.
  - verification: `terraform -chdir=infra/google-drive/modules/drive-project test`

- [x] **Enable approved APIs without teardown outages** (test-type: both)
  - blocked-by: previous task
  - summary: manage Service Usage and Drive, plus Monitoring only when an approved alert needs it.
  - desired behavior: APIs remain enabled if Terraform relinquishes management and unrelated services are absent.
  - acceptance criteria: lifecycle settings, dependencies, outputs, and destroy behavior are covered by module tests and static inspection.
  - verification: `terraform -chdir=infra/google-drive/modules/drive-project test`

- [x] **Apply explicit environment IAM responsibilities** (test-type: both)
  - blocked-by: first task
  - summary: expand approved principal sets into additive role-member resources without Owner or Editor.
  - desired behavior: test administration, production planning/apply, manual OAuth, and monitoring responsibilities remain distinct and reviewable.
  - acceptance criteria: stable IAM addresses, empty defaults, cross-environment isolation, Beta OAuth-role warning, and bootstrap permission documentation match `spec.md`.
  - verification: `terraform -chdir=infra/google-drive/modules/drive-project test` and targeted IAM searches

- [x] **Add evidence-gated Drive API monitoring** (test-type: both)
  - blocked-by: API task
  - summary: add the optional request-error alert without representing it as backup success or individual Drive quota monitoring.
  - desired behavior: alert creation is impossible until metric emission, filter, threshold, alignment, and notification channels are explicitly confirmed.
  - acceptance criteria: disabled defaults, Drive-only filters, response-code semantics, and guard conditions are tested.
  - verification: `terraform -chdir=infra/google-drive/modules/drive-project test`

- [x] **Wire independent test and production roots** (test-type: integration)
  - blocked-by: module tasks
  - summary: instantiate the exact same local module through two standalone roots with independent inputs, providers, lock files, and partial backends.
  - desired behavior: neither root uses Terraform CLI workspaces, remote state, credentials, project IDs, or configuration from the other.
  - acceptance criteria: both roots initialize with `-backend=false`, validate independently, and select the same Google provider version.
  - verification: `terraform -chdir=infra/google-drive/environments/test init -backend=false`, `terraform -chdir=infra/google-drive/environments/test validate`, `terraform -chdir=infra/google-drive/environments/prod init -backend=false`, and `terraform -chdir=infra/google-drive/environments/prod validate`

- [x] **Document protected backend bootstrap and state handling** (test-type: integration)
  - blocked-by: spec approval
  - summary: document separate test/prod GCS buckets, IAM, bootstrap options, external backend configuration, plan sensitivity, recovery, and migration gates.
  - desired behavior: operators can prepare `google-drive/default.tfstate` independently without asking an environment root to create its own backend.
  - acceptance criteria: documentation requires versioning, uniform access, public-access prevention, `force_destroy = false`, locking, separate executor access, and explicit approval before bucket creation or migration.
  - verification: documentation review and searches for committed backend credentials, state, plans, `.terraform/`, and real tfvars

- [x] **Update the Google Drive Cloud Console handoff runbook** (test-type: integration)
  - blocked-by: module and environment tasks
  - summary: distinguish Terraform outputs from manual test OAuth, manual production OAuth, fingerprints, device validation, import, rollback, and monitoring procedures.
  - desired behavior: the runbook states that Cloud projects identify the app while each user's `appDataFolder` stores that user's backups.
  - acceptance criteria: test has only its debug client; production has only approved release clients; Terraform does not create or verify Android OAuth clients; no maintainer Drive stores user backups.
  - verification: documentation review against specs 0204 and 0301–0307 and the official references in `spec.md`

- [x] **Verify the complete offline implementation** (test-type: integration)
  - blocked-by: all implementation and documentation tasks
  - summary: run formatting, initialization without backends, validation, Terraform tests, diff checks, status checks, and security searches.
  - desired behavior: fresh evidence covers both roots and the reusable module without using Cloud credentials or changing infrastructure.
  - acceptance criteria: all required offline commands pass or their exact limitation is reported; no temporary artifacts or unrelated changes are included.
  - verification: commands listed in `plan.md`, full `git diff`, and `git status --short`

- [x] **Generate a controlled test plan** (test-type: integration)
  - blocked-by: offline verification; separate test-plan approval
  - summary: initialize the approved test backend and inspect a real test plan using only the approved test identity.
  - desired behavior: the plan summary exposes all creates, updates, replacements, deletes, IAM changes, APIs, imports, and blast radius without saving a plan in the repository.
  - acceptance criteria: environment, project ID, backend, credential, executor identity, imports, and expected blast radius are recorded before access; execution stops again before apply.
  - verification: separately approved test plan and human review
  - result: after the approved import of two existing APIs and four IAM members, the 2026-07-19 plan reported one Drive API create, two in-place API lifecycle-protection updates, zero replacements, and zero deletes.
  - apply result: the separately approved test apply added Drive API management, updated the two imported API lifecycle protections, destroyed nothing, and was followed by a zero-diff plan.

- [ ] **Generate a separately controlled production plan** (test-type: integration)
  - blocked-by: approved module version and test evidence; separate production-plan approval
  - summary: rerun the exact approved module version in the production root with the production backend and identity.
  - desired behavior: production planning does not depend on test state or automatically follow a test apply.
  - acceptance criteria: production resources, IAM, APIs, replacements, deletes, imports, and blast radius receive a production-specific review; execution stops before apply.
  - verification: separately approved production plan and human review
