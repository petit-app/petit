# Runbook: Google Drive Google Cloud Console setup

## Document control

| Field | Value |
| --- | --- |
| Capability | User-owned Google Drive backup |
| Specs | 0204, 0301–0308 |
| Provider | Google Drive API v3 |
| Authorization | Google Identity Services `AuthorizationClient` |
| Scope | `https://www.googleapis.com/auth/drive.appdata` |
| Petit account required | No |
| Firebase required | No |

## Objective

Create the Google Cloud configuration required for Petit to request
`drive.appdata` authorization and store user-owned backup archives in the
Google Drive application data folder.

This runbook configures external infrastructure only. It does not implement the
Android integration. Spec 0308 authorizes offline Terraform implementation,
but every real plan, apply, import, backend operation, and Google Cloud access
retains its own approval gate.

## Expected result

At completion:

- dedicated and unrelated test and production Google Cloud projects are owned
  by the approved Petit maintainers;
- Google Drive API is enabled in both projects without unsafe
  disable-on-destroy behavior;
- test and production use different Terraform state buckets, execution
  identities, IAM, plans, and approvals;
- Google Auth Platform branding, audience, and data access are configured
  independently in each project;
- the only Drive scope requested by the backup capability is `drive.appdata`;
- the test project contains only the debug Android client;
- the production project contains only the approved direct-release and Google
  Play App Signing Android clients;
- test users can complete consent in testing mode;
- no client secret, service account, API key, Firebase project, or
  `google-services.json` is required for device-only Drive backup.

## Responsibilities

- **Cloud owner:** creates the project, configures consent, and controls access.
- **Terraform test administrator:** plans and applies only approved test
  infrastructure using the test backend and identity.
- **Production planner:** has read-only visibility in the production project
  and cannot apply; its backend identity still needs narrowly scoped object
  access to read state and create/delete Terraform's temporary lock object.
- **Production approver/apply operator:** authorizes a specific production plan
  and invokes the distinct production apply identity.
- **OAuth operator:** configures Google Auth Platform manually with the Beta
  `roles/oauthconfig.editor` role or a separately approved equivalent.
- **Monitoring reader:** reviews API telemetry without infrastructure or OAuth
  mutation permission.
- **Release owner:** supplies the release and Google Play App Signing certificate fingerprints.
- **Developer:** supplies the debug application ID and certificate fingerprint.
- **Privacy owner:** supplies public support, homepage, and privacy-policy URLs before production publication.

One person may perform multiple roles, but the credentials and permissions stay
environment-specific. Do not grant Owner or Editor only to complete this
runbook.

## Preconditions

- Two approved existing Google Cloud projects: one dedicated to test and one
  dedicated to production.
- Two approved GCS state buckets, or a separately approved bootstrap procedure.
- Different execution identities and backend IAM for test and production.
- Access to [Google Cloud Console](https://console.cloud.google.com/).
- Final application IDs:
  - debug: `com.woliveiras.petit.debug`;
  - release: `com.woliveiras.petit`.
- Access to the local debug signing certificate.
- Access to the release/upload certificate used for directly installed builds.
- Access to Google Play Console if Play App Signing is or will be enabled.
- Public app homepage, privacy policy, and support contact before publishing OAuth externally.

## Security rules

- Never commit OAuth client secrets, access tokens, refresh tokens, keystores, or exported Cloud credentials.
- Do not create a service account for user-owned Drive backup.
- Do not create an API key for Drive authorization.
- Do not request `drive`, `drive.file`, or another broader Drive scope.
- Do not create a web OAuth client unless a separately approved backend needs offline access.
- Do not add Firebase merely to obtain Google Drive authorization.
- Grant the minimum Google Cloud IAM role required for each maintainer.
- Never store Terraform state, state backups, saved plans, `.terraform/`, real
  tfvars, credentials, or backend authentication parameters in Git.
- Treat every saved Terraform plan as sensitive.
- Never copy, promote, or reference state between test and production.
- Never use Terraform CLI workspaces to represent the two environments.

## Automation boundary

Terraform under [`infra/google-drive`](../../infra/google-drive/README.md)
automates only:

- adoption of the approved existing project, with optional project creation
  available only after a separate approval;
- Service Usage and Google Drive API activation;
- approved additive project IAM members;
- non-secret project and API outputs;
- an optional request-error alert after Drive metric emission is proven.

Terraform deliberately does not automate:

- Branding, homepage, privacy policy, support contact, or authorized domains;
- Audience, publication status, or test users;
- Data Access scope declaration or verification;
- Android OAuth clients and their package/SHA-1 pairs;
- Google branding or OAuth verification;
- physical-device authorization, revocation, and cross-device validation.

The official Google provider has no Google Auth Platform Android-client
resource. `google_iam_oauth_client` is a Workforce Identity Federation resource
and must not be used for Android clients. Do not use `local-exec`, undocumented
REST calls, or third-party providers to claim full automation.

The Google Cloud projects identify Petit and account for API usage. They do not
store backup archives. Each archive is stored in the authorizing user's own
Google Drive `appDataFolder`; no backup is stored in a maintainer's Drive.

## Step 1: Collect Android signing identities

From the repository root, run:

```bash
./gradlew signingReport
```

Record the SHA-1 fingerprint for each installable identity. SHA-256 may also be
recorded for release management, but the Android OAuth client requires the
matching package name and SHA-1 certificate fingerprint.

Prepare this matrix before creating clients:

| Cloud environment | Application ID | Certificate source | SHA-1 |
| --- | --- | --- | --- |
| Test | `com.woliveiras.petit.debug` | Local debug keystore | `<test-debug-sha1>` |
| Production | `com.woliveiras.petit` | Direct-release signing certificate, when applicable | `<prod-direct-release-sha1>` |
| Production | `com.woliveiras.petit` | Google Play App Signing certificate | `<prod-play-app-signing-sha1>` |

If direct release and Play App Signing use different certificates, they need
separate Android OAuth clients even though the package name is the same.

Do not copy keystore passwords or private keys into the evidence for this runbook.

## Step 2: Confirm the two existing Google Cloud projects

Do not create a project during this runbook unless project creation has a
separate reviewed Terraform plan and approval.

1. Record the dedicated test project ID and number.
2. Record the dedicated production project ID and number.
3. Confirm the projects are different and neither contains the other
   environment's OAuth clients or credentials.
4. Confirm the owning organization or account and billing attachment, if any.
5. Review IAM and remove temporary or unnecessary principals through a
   separately reviewed IAM change.

Recommended project evidence:

```text
Environment: test / production
Project name:
Project ID:
Project number:
Owning organization/account:
Billing account attached: yes / no / not applicable
Terraform executor:
OAuth operators:
Monitoring readers:
```

Project IDs are not secrets, but avoid publishing internal organization details unnecessarily.

## Step 3: Apply or confirm Terraform-managed configuration

Complete this step independently for test and production. Never proceed from a
test apply directly into production.

1. Confirm the environment, project ID, backend bucket, `google-drive` prefix,
   credential source, executor identity, imports, and blast radius.
2. Obtain approval before using credentials or accessing the project.
3. Initialize only the selected environment backend.
4. Generate and review the plan without saving it in the repository.
5. Report every create, update, replacement, delete, IAM change, and API change.
6. Stop for a separate apply approval.
7. If apply is approved and completed, capture the non-secret project ID,
   project number, enabled API names, and optional alert-policy name.
8. Open **APIs & Services > Enabled APIs & services** and independently confirm
   that `drive.googleapis.com` and `serviceusage.googleapis.com` are enabled.

If Terraform has not been applied, a cloud administrator may confirm an
already enabled API, but must not silently make infrastructure changes outside
the approved workflow.

Do not enable Firebase Storage, Firestore, or unrelated Google APIs for this
capability. `monitoring.googleapis.com` is enabled only if an approved alert is
created.

Official reference: [Google Drive API overview](https://developers.google.com/workspace/drive/api/guides/about-sdk).

## Step 4: Configure Google Auth Platform branding

Repeat this manual configuration in each project. Do not copy a test OAuth
configuration or client into production. Production URLs and contacts must be
the final public values; test configuration must clearly identify its limited
audience.

Open **Google Auth Platform > Branding** and configure:

- app name: `Petit`;
- user support email;
- app logo approved for production use;
- app homepage URL;
- privacy policy URL;
- terms of service URL, if available;
- authorized domains for every public URL;
- developer contact email addresses.

Verify that the consent copy describes access as Google Drive backup, not Petit
Cloud login, subscription, synchronization, or broad Drive access.

Do not claim that Petit can see other Drive files. The `appDataFolder` is hidden
and accessible only to the app that created its contents.

## Step 5: Configure the audience

Open **Google Auth Platform > Audience**.

For the dedicated test project:

1. Select the appropriate user type for the owning organization.
2. Keep the app in testing status.
3. Add every Google account that will execute provider integration tests as a test user.
4. Record who owns test-user maintenance.

For the dedicated production project:

1. Confirm that branding and public policy URLs are final.
2. Review Google API Services User Data Policy compliance.
3. Publish the app for the intended external or internal audience.
4. Complete any review requested by Google before release.

The `drive.appdata` scope is currently classified by Google as non-sensitive,
but the project must still present accurate branding and data-use information.

Official references:

- [Google authorization production readiness](https://developers.google.com/identity/protocols/oauth2/production-readiness/policy-compliance)
- [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy)

## Step 6: Configure data access

Open **Google Auth Platform > Data Access**.

Perform this step independently in test and production and record the project
ID before saving.

1. Add this exact scope:

   ```text
   https://www.googleapis.com/auth/drive.appdata
   ```

2. Confirm that no broader Drive scope is configured for the backup capability.
3. Save the configuration.
4. Record the scope classification displayed by the console.

The Android app must request this scope only when the user chooses a Google
Drive capability. It must not request Drive access during first launch or local-only use.

Official reference: [Store application-specific data](https://developers.google.com/workspace/drive/api/guides/appdata).

## Step 7: Create Android OAuth clients

Open **Google Auth Platform > Clients** or **APIs & Services > Credentials**, depending on the current console navigation.

Create each client only in the project assigned by the signing matrix:

1. Select **Create client**.
2. Select application type **Android**.
3. Enter a descriptive name, such as:
   - `Petit Android debug`;
   - `Petit Android direct release`;
   - `Petit Android Play release`.
4. Enter the exact package name.
5. Enter the matching SHA-1 fingerprint.
6. Create the client.
7. Record the generated Android OAuth client ID.

Required and permitted in the test project:

```text
Petit Android debug
Package: com.woliveiras.petit.debug
SHA-1: <local debug SHA-1>
```

Required before production:

```text
Petit Android Play release
Package: com.woliveiras.petit
SHA-1: <Play App Signing SHA-1>
```

If testers install a release APK signed outside Google Play, also create the
direct-release client for that certificate in the production project.

The test project must not contain `com.woliveiras.petit` release clients. The
production project must not contain `com.woliveiras.petit.debug` or any debug
certificate. If a package/SHA-1 pair is already registered in another project,
stop and identify the owning project; do not create a replacement client with a
different or incorrect fingerprint.

Do not download or commit a client-secret JSON file. Android OAuth clients do
not require a client secret in the app.

## Step 8: Configure quota visibility and alerts

1. Open **APIs & Services > Google Drive API > Quotas & System Limits**.
2. Review per-project and per-user request quotas.
3. Generate representative Drive API traffic in the exact target project.
4. In Metrics Explorer, confirm a time series for
   `serviceruntime.googleapis.com/api/request_count`, resource
   `consumed_api`, filtered to service `drive.googleapis.com`.
5. Record the observed response-code labels and sampling behavior.
6. Only after this evidence exists, propose the error class, threshold,
   alignment period, duration, and notification channels for approval.
7. Enable the Terraform alert only through a separately reviewed plan.
8. Record the operational contact responsible for quota incidents.

Petit does not impose a backup-count or retention limit. Google Drive storage
and API quotas still apply and must be reported accurately to the user.

The Service Runtime request metric counts completed API requests. It does not
measure a successful Petit backup, OAuth health, an individual user's storage
quota, or `appDataFolder` capacity. If the filtered Drive time series is absent,
leave Terraform alerting disabled and retain quota/error review as a manual
operational procedure.

Official references:

- [Google Drive API usage limits](https://developers.google.com/workspace/drive/api/guides/limits)
- [Google Drive API error handling](https://developers.google.com/workspace/drive/api/guides/handle-errors)

## Step 9: Review the final project configuration

Confirm the following for each environment and record both project IDs:

- [ ] The intended environment-specific Google Cloud project is selected.
- [ ] Test and production project IDs, state buckets, and executors differ.
- [ ] Google Drive API and Service Usage API are enabled.
- [ ] Terraform service resources leave APIs enabled when unmanaged.
- [ ] No Terraform plan proposes project replacement or an unreviewed IAM grant.
- [ ] Branding identifies Petit accurately.
- [ ] Homepage and privacy-policy URLs are public and use authorized domains.
- [ ] Test is in Testing with the explicit test-user allowlist.
- [ ] Production is published and verified as required.
- [ ] `drive.appdata` is the only Drive scope used by backup.
- [ ] A debug Android OAuth client matches `com.woliveiras.petit.debug` and its SHA-1.
- [ ] Production Android OAuth clients match every distribution certificate.
- [ ] The Play client uses the Play App Signing certificate, not only the upload certificate.
- [ ] The test project contains no production client.
- [ ] The production project contains no debug client.
- [ ] No API key, service account, web client secret, Firebase dependency, or `google-services.json` was added for Drive backup.
- [ ] No credential or private signing material was added to Git.

## Step 10: Hand off non-secret configuration

Create one record per environment. Record only the non-secret values needed by
the implementation and release owners:

```text
Environment: test / production
Google Cloud project ID:
Google Cloud project number:
Terraform module revision:
Terraform provider version:
Backend bucket:
Backend prefix: google-drive
Terraform executor identity:
Drive API enabled: yes / no
OAuth publication status: testing / production
Configured scope: https://www.googleapis.com/auth/drive.appdata

Debug Android OAuth client ID:
Debug package / SHA-1:

Direct-release Android OAuth client ID, if applicable:
Direct-release package / SHA-1:

Play Android OAuth client ID:
Play package / App Signing SHA-1:

Test users:
Privacy policy URL:
Support email:
Quota/incident owner:
Drive request metric emitted: yes / no / not yet tested
Alert policy name, if approved:
Configuration date:
Executor:
Reviewer:
```

Store operational evidence outside the repository if it contains account names,
organization details, or internal screenshots.

## Import and existing-resource reconciliation

Before a first apply, inspect whether the existing project, API, IAM member, or
monitoring policy is already managed by another Terraform state. Do not create
a second owner for the same resource.

- Existing projects remain data sources while `create_project = false`; do not
  import the project into the Drive environment state.
- Already enabled project services can be represented by
  `google_project_service`, but import them if governance requires an explicit
  adoption record or another state currently owns them.
- Additive IAM members use exact role/member pairs. Compare the plan with the
  current policy and do not import or duplicate authoritative IAM resources.
- Import IDs and resource addresses must identify the exact environment and
  resource. Import remains a state mutation and requires specific approval.

Never use `terraform state rm`, `state mv`, or cross-environment import as a
shortcut for ownership conflicts.

## Rollback

Normal rollback is a reviewed configuration change followed by a new plan. It
must preserve these safety properties:

- do not delete an existing or Terraform-created project;
- do not disable Service Usage or Google Drive APIs;
- do not remove manual OAuth clients without release-impact review;
- do not delete or migrate a backend bucket;
- do not copy, restore, or promote state between environments;
- do not treat disconnecting a user as a request to delete their remote backup.

If an IAM grant is wrong, resolve the exact role/member pair and review its
removal plan. If an alert is noisy, disable or tune only that policy through a
reviewed plan. Destructive recovery and state rollback require their own
procedure and approval.

## Troubleshooting

### Authorization returns a developer configuration error

Check:

- the installed build's actual application ID;
- the certificate that signed the installed APK;
- the matching SHA-1 in the Android OAuth client;
- that the client belongs to the same project where Drive API is enabled;
- that the account is an allowed test user while the OAuth app is in testing.

Debug and release builds commonly fail differently because they use different
application IDs or signing certificates.

### Consent requests an unexpectedly broad Drive permission

Stop testing and inspect both Cloud Data Access and the Android authorization
request. The backup flow must request only `drive.appdata`.

### Authorization succeeds but appDataFolder calls fail

Check:

- Google Drive API is enabled in the correct project;
- the access token contains `drive.appdata`;
- requests use `spaces=appDataFolder` when listing;
- uploads set `appDataFolder` as the parent;
- the user's Drive storage quota is not exhausted.

### Automatic backup later requires interaction

This is not a Cloud Console error by itself. Google grants can be revoked or
require renewed consent. Background work must record `Authorization required`
and direct the user to a foreground reconnect action; it must not store a
long-lived refresh token on the device.

Official reference: [Authorize access to Google user data on Android](https://developer.android.com/identity/authorization).

## Completion record

```text
Date/time:
Environment: test / production
Executor:
Reviewer:
Project ID:
Project number:
Backend bucket and prefix:
Terraform module revision:
Terraform provider version:
Drive API enabled: Pass / Fail
Service Usage API enabled: Pass / Fail
IAM reviewed: Pass / Fail
Branding complete: Pass / Fail / Blocked
Audience/test users configured: Pass / Fail / Blocked
drive.appdata scope configured: Pass / Fail
Debug Android client configured: Pass / Fail
Direct-release Android client configured: Pass / Not applicable / Fail
Play Android client configured: Pass / Not applicable / Fail
Quota owner recorded: Pass / Fail
Drive request metric emitted: Pass / Not yet tested / Fail
Alert enabled: Pass / Not approved / Not applicable / Fail
No secrets committed: Pass / Fail
No state or saved plan committed: Pass / Fail
No project, backend, or state mutation without approval: Pass / Fail
Overall result: Pass / Fail / Blocked
Evidence location:
Notes:
```
