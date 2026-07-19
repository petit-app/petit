# Backend bootstrap strategy

The test and production Drive roots cannot create the GCS buckets that must
already exist before their backends initialize. Backend bootstrap is therefore
separate from both environment states and requires a later explicit approval.

No backend bucket, bootstrap state, or state migration is created by the
checked-in Drive environment roots.

## Required isolation

Create or adopt two different buckets:

```text
test:       gs://<test-state-bucket>/google-drive/default.tfstate
production: gs://<production-state-bucket>/google-drive/default.tfstate
```

Both buckets were bootstrapped in `europe-west1` on 2026-07-19 with uniform
bucket-level access, enforced public-access prevention, Object Versioning, and
a 30-day soft-delete policy. Bucket IAM grants `roles/storage.objectAdmin` only
as follows:

- the dedicated test Terraform executor on the test bucket;
- the dedicated production plan and apply executors on the production bucket.

Cross-environment object listing was tested through impersonation and denied.
The service accounts have no user-managed keys.

`iam.googleapis.com` was enabled to create the infrastructure-only service
accounts. `cloudresourcemanager.googleapis.com` was bootstrapped in both
projects so those identities can read project metadata and IAM; the Drive
module subsequently manages Cloud Resource Manager with protected API teardown
semantics.

The test executor must have no access to the production bucket. The production
executor must not depend on or read test state. Bucket IAM is separate from
project IAM managed by the Drive module.

## Required bucket controls

Each bucket must have:

- Object Versioning enabled;
- uniform bucket-level access enabled;
- public access prevention enforced;
- `force_destroy = false` if managed by Terraform;
- Google-managed encryption at rest, or a separately governed customer-managed
  key with a documented recovery lifecycle;
- auditability appropriate to the owning organization;
- bucket-level IAM limited to the environment executor and a small recovery
  administrator set.

The backend execution identity needs object read, create, update, delete, list,
and lock access. HashiCorp documents `roles/storage.objectAdmin` on the bucket
as the normal GCS backend role. Bucket administration should remain with a
smaller bootstrap/recovery identity, not the routine plan identity.

This means a production planner can be read-only in the Google Cloud project
while still creating and deleting the backend lock object. Object Versioning,
restricted bucket IAM, audit logs, and workflow controls mitigate the state
mutation capability inherent in the predefined backend role.

## Approved bootstrap options

Choose one option independently for each environment after approval.

### Option A: dedicated bootstrap Terraform state

Use a small root module whose only responsibilities are the environment's
state bucket and bucket IAM. Its state must be separate from
`google-drive/default.tfstate`; it can begin locally under a controlled
procedure or use an organization bootstrap backend that already exists.

After the bucket is created, initialize the Drive environment root with the
external backend file. Do not automatically migrate the bootstrap state into
the Drive backend.

### Option B: governed manual bucket creation

A cloud administrator creates and protects the bucket, records its name,
settings, IAM, executor, reviewer, and evidence, and hands only the non-secret
bucket name to the Terraform operator. A later dedicated bootstrap root may
adopt it after a separately approved import.

## Bootstrap approval record

Record before any bucket creation or adoption:

```text
Environment:
Bucket name:
Owning project:
Location:
Executor identity:
Recovery administrator:
Versioning:
Uniform bucket-level access:
Public access prevention:
Encryption mode:
IAM bindings:
Bootstrap state location:
Expected creates, updates, replacements, and deletes:
Approver:
```

## State recovery

GCS backend locking is automatic. Do not use `-lock=false`. If a lock remains
after a failed operation, verify that no writer is active and obtain approval
before `force-unlock`.

Object versions support recovery from accidental writes or deletion, but state
rollback is still a state mutation. Identify the exact object generation,
preserve the current generation, and obtain explicit approval before recovery.
Never copy test state to production or production state to test.

## Prohibited bootstrap actions

- Creating both buckets from one Drive environment state.
- Giving either routine executor access to both buckets.
- Storing credentials in backend configuration.
- Committing state, state backups, plans, real tfvars, or `.terraform/`.
- Accepting a state migration prompt without a reviewed migration procedure.
- Setting `force_destroy = true` for a state bucket.
- Deleting old customer-managed encryption keys before every state generation
  has been rewritten and recovery has been validated.
