# Drive project module

This module prepares one isolated Google Cloud project for Petit's future
Google Drive integration. Invoke it once from the test root and again from the
production root. Do not use CLI workspaces or remote state to select an
environment.

## Managed resources

- Optional `google_project`, disabled by default and protected with
  `deletion_policy = "PREVENT"`.
- `google_project_service` for Service Usage and Drive, plus Monitoring only
  when an approved alert is enabled. Services are abandoned without being
  disabled when Terraform relinquishes management.
- Additive `google_project_iam_member` resources for explicit responsibility
  groups.
- Optional `google_monitoring_alert_policy` for Drive API error request counts,
  guarded by confirmation that the metric has been emitted in the target
  project.

## Project modes

Existing projects are the default:

```hcl
module "drive_project" {
  source = "../../modules/drive-project"

  environment             = "test"
  project_id              = "example-test-project"
  expected_project_number = "123456789012"
}
```

`expected_project_number` is optional for reusable callers but required by the
Petit environment roots. It stops planning when `project_id` resolves to a
different immutable project number.

Project creation additionally requires a display name and exactly one
organization or folder. It has organization, billing, IAM, and lifecycle blast
radius and requires a separate approval before use.

## IAM responsibilities

The module exposes named principal sets rather than an arbitrary role map:

| Input | Environment | Roles |
| --- | --- | --- |
| `terraform_test_admin_members` | Test only | Service Usage Admin, Project IAM Admin, Monitoring Editor |
| `terraform_plan_members` | Production only | Browser, Service Usage Viewer, IAM Security Reviewer, Monitoring Viewer |
| `terraform_apply_members` | Production only | Service Usage Admin, Project IAM Admin, Monitoring Editor |
| `oauth_config_editor_members` | Either | OAuth Config Editor (Beta) |
| `oauth_config_viewer_members` | Either | OAuth Config Viewer (Beta) |
| `monitoring_viewer_members` | Either | Monitoring Viewer |

No Owner or Editor role is available. The OAuth Config Editor role is
purpose-built but currently includes Google-managed permissions beyond Android
client creation, including Firebase-namespaced permissions. Review the official
Beta role before every grant. This module does not create Firebase resources.

The first apply identity must already have the permissions needed to manage the
approved resources. Parent-level Project Creator and Billing Account User
permissions, backend bucket access, and permission to invoke a production apply
identity are bootstrap responsibilities outside this module.

## Monitoring semantics

The optional policy counts completed requests from:

```text
metric:   serviceruntime.googleapis.com/api/request_count
resource: consumed_api
service:  drive.googleapis.com
label:    response_code_class = 4xx or 5xx
```

The default is 5xx, more than five requests in a five-minute alignment window
for five minutes. These defaults are examples, not operational approval.
Enable the policy only after representative Drive traffic exists and Metrics
Explorer confirms the exact filtered time series in that environment.

This metric does not prove that a Petit backup succeeded. It does not expose an
individual user's Drive storage, `appDataFolder` capacity, or OAuth status.

## Outputs

- `project_id`
- `project_number`
- `enabled_apis`
- `drive_api_error_alert_name`
- `oauth_handoff`, containing only environment, expected package, certificate
  source descriptions, and the exact Drive scope

No output contains credentials, tokens, client secrets, SHA-1 values, or OAuth
client IDs.

## Tests

Tests mock the official Google provider and do not require credentials:

```bash
terraform init -backend=false
terraform test
```

The tests cover existing and created projects, project placement, API teardown
protection, environment-specific IAM expansion, monitoring preconditions, and
Drive-only metric filtering.
