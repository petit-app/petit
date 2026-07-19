# Plan: Google Drive Cloud Infrastructure

Spec: [spec.md](./spec.md)

## Status

This plan is **In Progress**. The offline Terraform module, independent
environment roots, backend documentation, and OAuth handoff runbook are
implemented, and offline verification is complete. The approval does not
authorize credentials, project access, a real plan, an apply, backend creation,
import, or any unapproved state operation. The controlled test imports and
post-import plan and separately approved apply are complete. The apply added
Drive API management, updated two API lifecycle protections in place, destroyed
nothing, and was followed by a zero-diff plan. Production planning remains
pending under its environment-specific gate.

## Dependencies

- Spec 0204 for the approved free user-owned-cloud boundary.
- Specs 0301–0303 and 0305–0307 for the Google Drive backup behaviors that the
  future Android adapter will implement.
- [Google Drive Cloud Console setup runbook](../../docs/test-runbooks/google-drive-cloud-console-setup.md),
  which must be revised after approval for the two-environment handoff.
- Separate approvals and credentials for every real test or production plan.

## Proposed structure

```text
infra/google-drive/
├── modules/
│   └── drive-project/
│       ├── main.tf
│       ├── services.tf
│       ├── iam.tf
│       ├── monitoring.tf
│       ├── variables.tf
│       ├── outputs.tf
│       ├── versions.tf
│       ├── README.md
│       └── tests/
├── environments/
│   ├── test/
│   │   ├── backend.tf
│   │   ├── main.tf
│   │   ├── providers.tf
│   │   ├── versions.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars.example
│   └── prod/
│       ├── backend.tf
│       ├── main.tf
│       ├── providers.tf
│       ├── versions.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── terraform.tfvars.example
├── bootstrap/
│   └── README.md
├── README.md
└── .gitignore
```

Terraform loads files only from the current module directory, so version and
provider constraints belong in every executable root and in the reusable
module. A single `infra/google-drive/versions.tf` would not constrain the two
nested environment roots and is intentionally omitted.

## Implementation sequence

1. Create the reusable `drive-project` module contract with adopt-by-default
   and explicit optional project creation.
2. Implement project discovery/creation, project deletion protection, and
   project outputs.
3. Enable Service Usage and Drive APIs with teardown behavior that leaves them
   enabled; enable Monitoring only for approved monitoring resources.
4. Add additive, environment-specific IAM member expansion for approved role
   groups without Owner or Editor.
5. Add optional request-error monitoring guarded by an explicit confirmation
   that Drive has emitted the documented Service Runtime time series.
6. Create independent test and production root modules with their own backend,
   provider, variables, outputs, example inputs, and lock files.
7. Add module tests for create/adopt paths, input isolation, API lifecycle, IAM,
   outputs, and monitoring preconditions.
8. Document the backend bootstrap problem and two separate approved bootstrap
   choices without creating buckets or migrating state.
9. Update the Cloud Console runbook to separate Terraform work, test OAuth,
   production OAuth, Android fingerprints, handoff outputs, device checks,
   import, rollback, and monitoring limitations.
10. Run the complete offline verification and review the full diff and Git
    status without staging or committing unrelated work.
11. Stop before any real plan. A later test-plan request must identify its
    backend, credential, executor identity, imports, and blast radius and obtain
    approval before access.

## Project adoption and creation design

- `create_project = false` is the default and resolves the explicit project ID
  as an existing project.
- `create_project = true` requires an explicit display name and exactly one
  organization ID or folder ID. Billing attachment is explicit when required
  by governance or product readiness.
- Created projects use the provider's project deletion prevention.
- Existing projects and APIs are reconciled with import blocks or documented
  import commands before an approved apply when another state already manages
  them.
- Test and production never obtain project IDs from each other or from remote
  state.

## Backend and bootstrap strategy

Each environment root declares a partial GCS backend and is initialized with a
different external `*.gcs.tfbackend` file containing only its bucket and
`prefix = "google-drive"`. Credentials come from the environment-specific
runner identity.

The backend buckets must already exist before those roots initialize. After a
separate approval, choose exactly one bootstrap path per environment:

1. **Dedicated bootstrap Terraform state:** a small root creates and protects
   only that environment's state bucket and bucket IAM, using state that is
   separate from the Drive environment state; or
2. **Governed manual creation:** a cloud administrator creates the bucket with
   the documented controls and records evidence, after which Terraform may
   adopt it in a separately approved bootstrap root.

Neither path may use the Drive environment root to create its own backend.
Bootstrap test and production also remain independent. No state migration is
performed as part of the offline implementation.

## IAM design

- Use additive `google_project_iam_member` resources so the module does not
  become authoritative for unrelated project IAM.
- Represent approved responsibilities as explicit input sets rather than an
  arbitrary role-to-member map.
- Grant OAuth operators `roles/oauthconfig.editor` and optional reviewers
  `roles/oauthconfig.viewer`, with a documented warning that both roles are
  Beta and the editor currently includes broader Google-managed permissions
  than Android-client creation alone.
- Grant monitoring readers `roles/monitoring.viewer`.
- Keep the plan identity read-only and the apply identity distinct in
  production. The delivery system controls who may invoke the apply identity
  after approval.
- Document required bootstrap permissions rather than attempting to grant an
  executor the permission needed for its own first apply.
- Never grant Owner or Editor for convenience.

## Monitoring design

The module may define an optional error-count alert over the GA
`serviceruntime.googleapis.com/api/request_count` metric and the
`consumed_api` resource, filtered to `drive.googleapis.com` and error response
classes. It remains disabled unless an operator has:

1. generated representative Drive traffic in the exact target project;
2. confirmed that the filtered time series exists;
3. approved the threshold, alignment period, response-code classes, and
   notification channels; and
4. recorded the metric semantics in the runbook.

No alert will claim to measure per-user Drive storage capacity, individual
`appDataFolder` quota, successful backup completion, or OAuth health. Those
signals require application telemetry or manual Google consoles outside this
Terraform scope.

## Manual OAuth handoff

After Terraform has been separately applied, the runbook uses non-secret
outputs to select the exact project. The OAuth operator then configures:

- Branding, homepage, privacy policy, support contact, and authorized domains;
- Audience and test users;
- the exact `drive.appdata` Data Access declaration;
- Testing status and only the debug client in the test project;
- production publication and verification as required;
- direct-release and Google Play App Signing clients in production;
- physical-device authorization, revocation, and cross-device validation.

Terraform does not create Android clients or store their client IDs or SHA-1
fingerprints in state.

## Planned verification

- `terraform fmt -check -recursive infra/google-drive`
- `terraform -chdir=infra/google-drive/environments/test init -backend=false`
- `terraform -chdir=infra/google-drive/environments/test validate`
- `terraform -chdir=infra/google-drive/environments/prod init -backend=false`
- `terraform -chdir=infra/google-drive/environments/prod validate`
- `terraform -chdir=infra/google-drive/modules/drive-project test`
- TFLint only if a repository configuration is added and justified.
- `git diff --check`
- full inspection of `git diff`
- `git status --short`
- targeted secret and forbidden-resource searches
- searches for tracked `.tfstate`, `.tfplan`, `.terraform/`, real `.tfvars`,
  credentials, tokens, keystores, and private fingerprints

No real plan will be reported as verified without the exact approved backend,
credentials, executor identity, imports, and Google Cloud project access.

## Risks and rollback

- **Project creation:** high blast radius and parent/billing permissions;
  mitigate with adopt-by-default, explicit inputs, and deletion prevention.
- **Project adoption:** ownership conflict with another Terraform state;
  mitigate by inventory and import before apply.
- **IAM:** additive grants can still be excessive or target the wrong
  principal; expose every binding in the plan and keep environments separate.
- **State:** backend swaps can disclose or corrupt state; use separate buckets,
  locking, versioning, restricted IAM, external configuration, and no
  cross-environment migration.
- **API teardown:** accidental removal could break authorization and Drive
  calls; leave services enabled when Terraform relinquishes management.
- **Monitoring:** generic API metrics can be mistaken for backup or user quota
  health; keep alerting evidence-gated and document semantics.
- **OAuth:** manual drift or the wrong package/SHA-1 pairing can break a build;
  use environment-specific checklists and device validation.
- **Provider changes:** Google provider or OAuth role behavior can change;
  pin and review provider versions and re-check official documentation before
  a production plan.

Rollback means reverting module configuration and producing a new reviewed
plan. It does not mean disabling the Drive API, deleting projects, deleting
OAuth clients, moving state, or destroying backend buckets. Those destructive
actions require their own explicit procedures and approvals.
