---
spec: "0308"
title: "Google Drive Cloud Infrastructure"
family: backup-recovery
status: In Progress
owner: woliveiras
depends_on: ["0204"]
---

# Spec: Google Drive Cloud Infrastructure

## Context and motivation

Petit needs controlled Google Cloud configuration before its provider-neutral
backup contracts can be connected to Google Drive. The Cloud projects identify
the Android applications and account for API usage; they do not store backup
archives. Each archive remains in the authorizing user's hidden Google Drive
`appDataFolder`, using only the
`https://www.googleapis.com/auth/drive.appdata` scope.

This specification defines the infrastructure-as-code boundary, the strict
separation between test and production, the manual Google Auth Platform
handoff, and the approval workflow. It does not authorize the Android adapter,
a real Terraform plan, or any infrastructure mutation.

## Environment architecture

| Concern | Test | Production |
| --- | --- | --- |
| Google Cloud project | Dedicated test project | Dedicated production project |
| OAuth publishing status | Testing | Published and verified as required |
| Android application | `com.woliveiras.petit.debug` | `com.woliveiras.petit` |
| Signing certificate | Debug SHA-1 | Direct-release SHA-1, when applicable, and Google Play App Signing SHA-1 |
| Terraform root module | `environments/test` | `environments/prod` |
| State bucket | Dedicated test bucket | Dedicated production bucket |
| State object | `google-drive/default.tfstate` | `google-drive/default.tfstate` |
| Execution identity | Test-only identity | Production-only identity |
| Plan and approval | Test-specific | Production-specific |

The environments reuse the same version of the `drive-project` module by
executing it independently. They must not share a project, backend bucket,
state, credentials, IAM policy, OAuth configuration, plan, or approval. They
must not use Terraform CLI workspaces to represent environments, and state must
never be promoted, copied, or referenced across environments.

## Functional requirements

### Scenario 1: Keep test and production isolated

```gherkin
GIVEN the test and production Terraform roots
WHEN either root is initialized, planned, or applied
THEN it addresses only its environment-specific Google Cloud project
AND it uses only its environment-specific backend bucket and execution identity
AND it cannot obtain inputs or state from the other environment
AND no Terraform CLI workspace represents an environment
```

### Scenario 2: Adopt an existing project safely

```gherkin
GIVEN an approved Google Cloud project already exists
WHEN create_project is false and the environment is configured with its project ID
THEN the module reads and manages only the approved project-scoped resources
AND existing resources are adopted with import blocks or a documented import procedure
AND the module does not attempt to recreate or replace the project
```

### Scenario 3: Create a project only with explicit inputs

```gherkin
GIVEN project creation has been separately approved
WHEN create_project is true
THEN project ID, display name, and exactly one organization or folder placement are explicit
AND the billing-account choice is explicit when applicable
AND project deletion is prevented by the provider lifecycle policy
AND the plan exposes the larger creation and IAM blast radius for review
```

Project creation is optional and disabled by default. Creating a project
requires organization-level project-creation permission and may require billing
permission. Adopting an existing project has a smaller blast radius but requires
the operator to reconcile and import resources already managed elsewhere.

### Scenario 4: Enable only approved APIs without unsafe teardown

```gherkin
GIVEN the target project is selected or created
WHEN the drive-project module is applied
THEN serviceusage.googleapis.com is enabled
AND drive.googleapis.com is enabled
AND monitoring.googleapis.com is enabled only when an approved alert requires it
AND removing an API resource from Terraform does not disable the API
AND a normal module destroy cannot disable the production Drive API
```

Every managed project service must use provider behavior that leaves the API
enabled when Terraform stops managing the resource. No unrelated API is
enabled by this capability.

### Scenario 5: Apply explicit least-privilege IAM

```gherkin
GIVEN environment-specific principals have been approved
WHEN IAM changes are planned
THEN every role and member appears explicitly in the plan
AND additive member resources preserve unrelated IAM policy
AND no Owner or Editor role is granted
AND test principals receive no production binding
AND production principals receive no test binding or backend access
```

The access model separates these responsibilities:

- test Terraform administration;
- read-only production planning;
- approved production application through a distinct apply identity;
- manual Google Auth Platform configuration;
- monitoring read access.

The production approval decision is a workflow control, not an IAM role.
Permission to impersonate or invoke the production apply identity must be
controlled outside this module by the approved delivery system. Bootstrap
permissions needed before the module can manage IAM must also be granted and
reviewed outside the environment state.

### Scenario 6: Store state in independent protected backends

```gherkin
GIVEN the two approved pre-existing state buckets
WHEN each environment is initialized with its external backend configuration
THEN test uses gs://<test-state-bucket>/google-drive/default.tfstate
AND production uses gs://<prod-state-bucket>/google-drive/default.tfstate
AND GCS state locking is active
AND bucket versioning, uniform bucket-level access, and public-access prevention are enabled
AND only the corresponding environment executor can read or write state objects
```

Backend bucket creation cannot depend on the state stored in that bucket. The
bootstrap strategy must therefore use one separately approved root/state per
environment, or an approved manual bucket creation procedure. Bootstrap must
configure `force_destroy = false`, versioning, uniform bucket-level access,
public-access prevention, and environment-specific bucket IAM. No bootstrap
apply or state migration is part of this specification's initial
implementation authorization.

Environment roots contain only an empty `backend "gcs" {}` block. Bucket and
prefix are supplied through a non-secret external `*.gcs.tfbackend` file or
explicit initialization arguments. Credentials are supplied through the
runner's Google authentication mechanism, never in backend files. The
`.terraform/` directory is treated as sensitive because Terraform persists the
merged backend configuration there.

### Scenario 7: Hand off Google Auth Platform configuration manually

```gherkin
GIVEN Terraform has produced the non-secret project outputs
WHEN the OAuth operator configures Google Auth Platform
THEN test remains in Testing with an explicit test-user allowlist
AND test has only the debug Android client and debug SHA-1
AND production is published and verified as required
AND production has no debug client
AND production has clients for every approved release signing certificate
AND the only Drive scope declared is drive.appdata
```

Terraform does not create or verify Branding, homepage, privacy policy, support
contact, authorized domains, Audience, test users, Data Access scopes,
publication status, Android OAuth clients, or Google verification. The official
Google provider does not expose a resource for Google Auth Platform Android
clients keyed by package name and SHA-1. In particular,
`google_iam_oauth_client` is a Workforce Identity Federation resource and must
not be used for Android application clients.

No `local-exec`, undocumented REST call, third-party provider, API key, OAuth
client secret, service account for user backup, Firebase resource, or
`google-services.json` may be used to bypass this boundary.

### Scenario 8: Create monitoring only from proven Drive metrics

```gherkin
GIVEN Drive API traffic has been generated in the target project
WHEN an API error alert is enabled
THEN an operator has first confirmed a Drive time series in Cloud Monitoring
AND the policy filters the consumed API resource to drive.googleapis.com
AND the metric semantics, alignment, threshold, and notification channels are documented
AND the policy does not claim to measure an individual user's Drive storage quota
```

Google documents the GA
`serviceruntime.googleapis.com/api/request_count` metric with response-code
labels for consumed APIs. However, actual Drive emission must be confirmed in
each target project after test traffic exists. Therefore alert creation is
disabled by default and cannot be enabled merely because the metric descriptor
exists. Per-user storage quota and `appDataFolder` capacity remain application
and operational concerns; they are not inferred from project request metrics.

### Scenario 9: Enforce plan and apply gates per environment

```gherkin
GIVEN offline validation has passed
WHEN a real Terraform plan is requested
THEN the environment, project ID, backend, credential, executor identity, imports, and expected blast radius are recorded
AND approval is obtained before credentials or the project are accessed
WHEN the plan is complete
THEN creates, updates, replacements, deletes, IAM changes, and API changes are presented
AND a separate explicit approval is required before apply
```

A successful test apply never triggers a production plan or apply. Commands
that mutate infrastructure or state, including apply, destroy, import, state
movement, state removal, and backend migration, always require specific
authorization for the exact environment and operation.

## Terraform module contract

The reusable module accepts only non-secret configuration:

- project ID and optional project creation flag;
- project display name, organization ID or folder ID, and billing account when
  project creation is approved;
- labels;
- explicit principal sets for approved IAM roles;
- optional, evidence-gated request-error alert settings and notification
  channel resource names.

The module exports only:

- project ID;
- project number;
- enabled API names;
- non-sensitive monitoring resource names;
- a manual OAuth handoff checklist containing expected environment, package
  name, and certificate source, but no credential or private fingerprint data.

SHA-1 fingerprints and OAuth client IDs are non-secret operational
configuration, but they remain outside Terraform state and committed tfvars.

## IAM model

The initial role model uses predefined roles without Owner or Editor:

| Responsibility | Intended access | Notes |
| --- | --- | --- |
| Test Terraform administrator | Project/service/IAM/monitoring permissions needed by the approved test plan | Test project and test backend only |
| Production planner | Read-only project, Service Usage, IAM, and monitoring visibility | Needs separate backend object access for state reads and locking, but cannot mutate project resources |
| Production apply identity | Project/service/IAM/monitoring permissions needed by the approved production plan | Invoked only after production approval |
| OAuth operator | `roles/oauthconfig.editor` | Beta predefined role; review its included permissions before grant |
| Monitoring reader | `roles/monitoring.viewer` | No infrastructure or OAuth mutation |
| OAuth reviewer | `roles/oauthconfig.viewer` | Optional independent review role |

The exact bootstrap grants for Terraform executors depend on whether a project
is created or adopted. Project creation can additionally require
`roles/resourcemanager.projectCreator` on the selected parent and
`roles/billing.user` on the selected billing account. Those parent-level grants
are not created by the Drive environment module.

## Non-functional requirements

- Use Terraform CLI `>= 1.13.0, < 2.0.0` and the official HashiCorp Google
  provider `~> 7.40`; commit independent lock files for the test and production
  roots and keep their selected provider versions equal.
- Keep the shared module version identical between an approved test run and a
  later production run.
- Place Terraform version and provider constraints in every executable root;
  a top-level file does not configure nested root modules.
- Use variable validation and resource preconditions for mutually exclusive or
  environment-specific inputs.
- Use additive IAM member resources and stable `for_each` keys.
- Use project and service deletion protection supported by the official
  provider; do not rely on shell hooks.
- Never put credentials, tokens, key material, secrets, state, state backups,
  saved plans, `.terraform/`, generated backend configuration, or real tfvars
  in Git.
- Treat saved plans as sensitive and create them only outside the repository
  when specifically approved.
- Do not read remote state across environments.
- Keep all repository documentation, Terraform identifiers, variables,
  outputs, and comments in English.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Static | `terraform fmt -check -recursive`, variable validation, provider/schema validation, forbidden-resource searches, secret scans, and Git ignore checks. |
| Module | `terraform test` with mocked providers for create/adopt, environment restrictions, API lifecycle, IAM expansion, outputs, and monitoring preconditions. |
| Root integration | `terraform init -backend=false` and `terraform validate` independently in test and production roots. |
| Operational | Separately approved real plans, manual OAuth checklists, API metric-emission check, and physical-device authorization/revocation validation. |

Offline validation proves configuration consistency only. It does not prove a
real plan, IAM sufficiency, API metric emission, OAuth configuration, or Google
Drive behavior.

## Acceptance criteria

- [x] Both environment roots validate independently and reference the same local module version without Terraform CLI workspaces or remote-state coupling.
- [x] Create/adopt project paths, API lifecycle protection, IAM expansion, non-secret outputs, and alert preconditions have automated Terraform tests.
- [x] Test and production backend documentation yields different buckets with the `google-drive` prefix and no committed backend credentials.
- [x] The runbook clearly separates Terraform outputs from manual test OAuth and manual production OAuth work.
- [x] Searches confirm the configuration cannot create API keys, OAuth client secrets, Android OAuth clients, backup service accounts, Firebase resources, or broader Drive scopes.
- [x] Project and service deletion protections are visible in code and documented with import and rollback procedures.
- [x] No real plan, apply, import, state operation, backend migration, project access, or credential use occurs without its required explicit approval.

## Edge cases

- The approved project or an API already exists but is not yet in state.
- The selected project belongs to a different organization or folder than
  expected.
- A package-name and SHA-1 pair already belongs to another Google Cloud project.
- Test and production backend configuration files are accidentally swapped.
- The executor can read the project but cannot read or lock its backend state.
- The executor can enable APIs but cannot update the approved IAM resources.
- A project service disappears from configuration during refactoring.
- A plan proposes project replacement because an immutable project input
  changed.
- The Service Runtime metric descriptor exists but Drive has emitted no time
  series in the target project.
- Drive quotas or pricing change after the module is released.
- Google changes the classification or verification requirements for
  `drive.appdata`.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Spec status | Approved | The user explicitly approved the infrastructure specification on 2026-07-18. |
| Spec number | 0308 | 0304 was previously used by a removed device-transfer spec and is not recycled. |
| Environment model | Two projects, roots, states, identities, IAM policies, plans, and approvals | Prevents test credentials and OAuth configuration from crossing into production. |
| Promotion unit | The same module revision, executed again | State and resources are never promoted between environments. |
| Project lifecycle | Adopt by default; optional explicit creation | Existing projects reduce blast radius; creation remains available under a separate approval. |
| API lifecycle | Leave enabled when Terraform stops managing a service | Avoids accidental Drive outages, especially in production. |
| Backend | Separate pre-existing GCS buckets with a shared object prefix convention | GCS provides remote state and locking while preserving environment isolation. |
| Bootstrap | Separate state or manual procedure, not an environment dependency | A backend cannot create the bucket that must exist before initialization. |
| OAuth automation | Manual | The official provider has no Android Google Auth Platform client resource. |
| OAuth IAM | OAuth Config Editor/Viewer, reviewed because they are Beta | Avoids Owner and Editor while using Google's purpose-built roles. |
| Monitoring | Optional and disabled until Drive emission is proven | A generic metric descriptor alone does not prove useful Drive telemetry. |
| Backup storage | Each user's Google Drive `appDataFolder` | The Google Cloud project identifies the app; it does not hold user backups. |

## Official references

- [Google provider: `google_project`](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/google_project)
- [Google provider: `google_project_service`](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/google_project_service)
- [Google provider: `google_iam_oauth_client`](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/iam_oauth_client)
- [Google Auth Platform OAuthConfig roles](https://cloud.google.com/iam/docs/roles-permissions/oauthconfig)
- [Manage Google Auth Platform OAuth clients](https://support.google.com/cloud/answer/15549257)
- [Google OAuth app state overview](https://developers.google.com/identity/protocols/oauth2/production-readiness/overview)
- [Store application-specific data in Google Drive](https://developers.google.com/workspace/drive/api/guides/appdata)
- [Choose Google Drive API scopes](https://developers.google.com/workspace/drive/api/guides/api-specific-auth)
- [Google Drive API usage limits](https://developers.google.com/workspace/drive/api/guides/limits)
- [Monitor API usage](https://cloud.google.com/apis/docs/monitoring)
- [Terraform GCS backend](https://developer.hashicorp.com/terraform/language/backend/gcs)
- [Terraform partial backend configuration](https://developer.hashicorp.com/terraform/language/backend)
- [Store Terraform state in Cloud Storage](https://cloud.google.com/docs/terraform/resource-management/store-state)
- [Manage sensitive Terraform data](https://developer.hashicorp.com/terraform/language/manage-sensitive-data)

## Out of scope

- Android authorization or Google Drive storage adapters.
- Creation, upload, download, listing, restoration, or deletion of user backups.
- Any service account used by the application backup flow.
- API keys, OAuth client secrets, Firebase, and `google-services.json`.
- Broader Drive scopes, including `drive` and `drive.file`.
- Automated Google Auth Platform Branding, Audience, test users, Data Access,
  publication, Android OAuth clients, or verification.
- Creating backend buckets, migrating state, running a real plan, applying,
  destroying, or importing infrastructure without a later specific approval.
- Petit Cloud hosted storage, synchronization, collaboration, billing, or
  entitlement.
