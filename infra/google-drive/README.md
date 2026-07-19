# Google Drive Google Cloud infrastructure

This directory prepares two isolated Google Cloud projects for Petit's future
Google Drive backup adapter. It does not implement the Android adapter and it
does not store user backups. The Cloud project identifies the application and
accounts for API usage; every backup remains in the authorizing user's hidden
Google Drive `appDataFolder` under the
`https://www.googleapis.com/auth/drive.appdata` scope.

## Environment isolation

Test and production are separate Terraform root modules. They must always use
different:

- Google Cloud projects;
- GCS backend buckets and state;
- execution identities and credentials;
- project and backend IAM;
- Google Auth Platform configurations and Android clients;
- plans, reviews, and approvals.

Terraform CLI workspaces are not environments here. State and resources are
never copied or promoted. Promotion means running the same reviewed module
revision again from the production root with production-only inputs.

| Environment | Root | Android package | OAuth certificate sources |
| --- | --- | --- | --- |
| Test | `environments/test` | `com.woliveiras.petit.debug` | Debug SHA-1 only |
| Production | `environments/prod` | `com.woliveiras.petit` | Direct-release SHA-1 when applicable and Google Play App Signing SHA-1 |

## Terraform boundary

Terraform can:

- adopt an existing project, which is the default;
- optionally create a project after a separate approval and explicit parent
  and billing inputs;
- enable Cloud Resource Manager, Service Usage, and Google Drive APIs;
- leave managed APIs enabled if Terraform relinquishes management;
- add explicitly approved IAM members without becoming authoritative for the
  complete project policy;
- create an optional Drive API request-error alert only after metric emission
  is confirmed in the exact target project;
- output non-secret project, API, alert, and manual OAuth handoff values.

Terraform does not configure Google Auth Platform Branding, Audience, test
users, Data Access, publication, verification, or Android OAuth clients. The
official Google provider does not expose the package-name/SHA-1 Android client
resource. `google_iam_oauth_client` is for Workforce Identity Federation and is
not a substitute.

The module creates no API key, OAuth client secret, backup service account,
Firebase resource, or `google-services.json`.

## Existing projects

Each environment uses an approved existing project. Keep its operational
identity only in the ignored local `terraform.tfvars`:

| Environment | Project ID | Expected project number |
| --- | --- | --- |
| Test | `<test-project-id>` | `<test-project-number>` |
| Production | `<production-project-id>` | `<production-project-number>` |

Both checked-in examples and the ignored local configurations use:

```hcl
create_project = false
```

`expected_project_number` is an immutable identity guard: Terraform reports an
error if the configured project ID resolves to another project number. Before
the first real plan, inventory any already enabled APIs and IAM bindings. An
enabled `google_project_service` can normally be adopted by applying the
resource, but a resource managed by another Terraform state must be reconciled
before proceeding. If an import is required, prefer a temporary reviewed import
block or the documented resource ID:

```text
google_project_service.apis["drive.googleapis.com"]
<project-id>/drive.googleapis.com
```

Do not import anything until the exact resource, environment, state, and import
operation receive explicit approval.

## Backend initialization

The GCS buckets must exist before the environment roots are initialized. See
[`bootstrap/README.md`](bootstrap/README.md) for the bootstrap choices and
required protection.

Copy the environment's committed placeholder example to the ignored local
filename, then insert the approved non-secret bucket and service-account
identities locally:

```hcl
bucket                      = "approved-environment-state-bucket"
prefix                      = "google-drive"
impersonate_service_account = "approved-environment-executor@example.iam.gserviceaccount.com"
```

The prefix produces `google-drive/default.tfstate` for the default CLI
workspace. Do not create or select another workspace.

Credentials must come from the environment-specific runner through Application
Default Credentials or an approved impersonation mechanism. Never add a
credential path or JSON body to backend or provider configuration. Terraform
stores merged backend configuration under `.terraform/`, so that directory is
ignored and must be treated as sensitive.

A production planner is read-only in the target Google Cloud project. Because
the GCS backend locks state during plan, its backend identity still needs the
approved bucket object permissions required to read state and create/delete the
lock object. Keep this bucket access separate from project mutation permission;
do not disable locking.

Initialization is environment-specific:

```bash
terraform -chdir=infra/google-drive/environments/test init \
  -backend-config=backend.gcs.tfbackend
```

```bash
terraform -chdir=infra/google-drive/environments/prod init \
  -backend-config=backend.gcs.tfbackend
```

Production `backend.gcs.tfbackend` uses the planning identity. A separately
approved apply must reinitialize with `backend.apply.gcs.tfbackend`, which uses
the production apply identity, and override
`terraform_impersonate_service_account` with that same apply identity. Never
use the apply configuration to generate an unapproved plan.

Changing a backend can prompt for state migration. Stop and obtain a specific
migration approval instead of accepting that prompt.

## Offline workflow

Offline checks do not require Google credentials and must run before a real
plan:

```bash
terraform fmt -check -recursive infra/google-drive
terraform -chdir=infra/google-drive/environments/test init -backend=false
terraform -chdir=infra/google-drive/environments/test validate
terraform -chdir=infra/google-drive/environments/prod init -backend=false
terraform -chdir=infra/google-drive/environments/prod validate
terraform -chdir=infra/google-drive/modules/drive-project test
```

Mocked module tests do not create infrastructure. Do not run `terraform test`
with a real provider substituted for the checked-in mock provider.

## Real plan gate

Before any real plan, record and approve:

- environment;
- project ID;
- backend bucket and `google-drive` prefix;
- credential source and executor identity;
- existing resources and required imports;
- expected project, API, IAM, monitoring, and state blast radius.

Use a real tfvars file outside the repository or the ignored
`terraform.tfvars` path. A plan command without `-out` avoids persisting a plan
artifact:

```bash
terraform -chdir=infra/google-drive/environments/test plan
```

After the plan, report every create, update, replacement, delete, IAM change,
and API change, then stop for apply approval. Saved plan files contain state and
input data and must be treated as sensitive. Never store them in the
repository.

A test apply does not authorize or trigger any production command.

## Rollback and destroy safety

- Created projects use `deletion_policy = "PREVENT"`.
- Managed APIs use `disable_on_destroy = false` and
  `deletion_policy = "ABANDON"`.
- Backend buckets must use `force_destroy = false`.
- Rollback is a reviewed configuration change and new plan, not a destroy.

Never delete a project, disable Drive, delete an OAuth client, remove or move
state, migrate a backend, or destroy a state bucket as an ordinary rollback.
Each operation needs an exact target, recovery procedure, and separate
approval.

## Official references

- [Google project resource](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/google_project)
- [Google project service resource](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/google_project_service)
- [Google Auth Platform OAuthConfig roles](https://cloud.google.com/iam/docs/roles-permissions/oauthconfig)
- [Terraform GCS backend](https://developer.hashicorp.com/terraform/language/backend/gcs)
- [Terraform partial backend configuration](https://developer.hashicorp.com/terraform/language/backend)
- [Store Terraform state in Cloud Storage](https://cloud.google.com/docs/terraform/resource-management/store-state)
- [Store application-specific Drive data](https://developers.google.com/workspace/drive/api/guides/appdata)
