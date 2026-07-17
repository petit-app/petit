---
spec: "0204"
title: "Petit Cloud Gate"
family: identity-access
status: Draft
owner: woliveiras
depends_on: ["0201"]
---

# Spec: Petit Cloud Gate

## Context and motivation

> As a Petit user,
> I want to understand why a capability requires payment or authorization,
> So that I only pay when Petit provides infrastructure with an ongoing cost.

Petit is local-first. A login, external authorization, or convenient feature is
not by itself a reason to charge. Payment is required only for capabilities
that use infrastructure operated and paid for by Petit, such as hosted storage,
real-time synchronization, and remote collaboration.

Google Drive backup is a separate service model. The user authorizes Petit to
store backups in the user's own Google Drive. This is a Google authorization,
not a Petit Cloud account, and the capability remains free.

## Service models

| Service model | Identity requirement | Cost to the user | Examples |
| --- | --- | --- | --- |
| Petit Local | No account | Free | Pet care, reminders, JSON/PDF export, local sharing |
| User-owned cloud | Authorization for the selected provider | Free | Google Drive backup, restore, and backup management |
| Petit Cloud | Persistent Petit identity | Paid | Hosted real-time sync, multi-device sync, remote family collaboration |

The Google account used to authorize Drive may also be offered as a way to
authenticate a Petit Cloud account. These are distinct grants and must be
presented and handled separately.

## Functional requirements

### Scenario 1: Use the local app without an account

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I do not have a Petit account
WHEN I use Petit Local
THEN I can use every local pet-care capability
AND I can export and import JSON
AND I can export PDF when that capability is available
AND I can transfer or share data through supported local connections
AND I am not asked to purchase Petit Cloud
```

### Scenario 2: Connect a user-owned Google Drive for free

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I do not have a Petit Cloud subscription
WHEN I choose a Google Drive backup or restore capability
THEN Petit explains that Google authorization is required
AND Petit requests only the Drive permissions needed for that capability
AND I can authorize my Google account without purchasing Petit Cloud
AND backups are stored in my Google Drive
```

### Scenario 3: See a gate on Petit-hosted cloud capabilities

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I do not have an active Petit Cloud subscription
WHEN I view a capability that uses Petit-managed infrastructure
THEN I see a lock indicator and the label "Petit Cloud"
AND I can open an explanation of the hosted service
AND free local and Google Drive capabilities remain available
```

### Scenario 4: Understand what Petit Cloud funds

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I open the Petit Cloud plan screen
WHEN I review the plan benefits
THEN I see only capabilities backed by Petit-managed infrastructure:
  - Real-time cloud synchronization
  - Automatic synchronization across multiple devices
  - Remote family collaboration
AND I see that local data remains available without a subscription
AND I see that Google Drive backup does not require a subscription
```

### Scenario 5: Use Petit Cloud with an active entitlement

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am authenticated with an active Petit Cloud entitlement
WHEN I use a Petit Cloud capability
THEN the capability is available
AND settings display "Petit Cloud: Active"
AND the entitlement is not inferred solely from Google Drive authorization
```

### Scenario 6: Preserve local data after Petit Cloud expires

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that my Petit Cloud entitlement expires
WHEN Petit refreshes my entitlement
THEN Petit stops new use of Petit-managed cloud services
AND all data already stored locally remains available
AND local editing, export, and local sharing remain available
AND Google Drive backup remains available when its authorization is valid
AND Petit explains how hosted data is retained, exported, or deleted
```

### Scenario 7: Distinguish local and remote family sharing

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I want to share pet data with family
WHEN I compare the available sharing methods
THEN local device-to-device or local-network sharing is free and requires no Petit account
AND continuous remote collaboration through Petit-managed infrastructure requires Petit Cloud
```

## Capability classification

| Capability | Account or authorization | Offering |
| --- | --- | --- |
| Pet registration and health history | None | Petit Local — free |
| Weight tracking and charts | None | Petit Local — free |
| Vaccination and deworming records | None | Petit Local — free |
| Local reminders | None | Petit Local — free |
| JSON import and export | None | Petit Local — free |
| PDF export | None | Petit Local — free |
| Nearby and local-network transfer | None | Petit Local — free |
| Local family group | None | Petit Local — free |
| Manual Google Drive backup | Google Drive authorization | User-owned cloud — free |
| Automatic Google Drive backup | Google Drive authorization | User-owned cloud — free |
| Restore and manage Google Drive backups | Google Drive authorization | User-owned cloud — free |
| Real-time sync hosted by Petit | Petit Cloud account | Petit Cloud — paid |
| Automatic multi-device sync hosted by Petit | Petit Cloud account | Petit Cloud — paid |
| Remote family collaboration hosted by Petit | Petit Cloud account for each participant | Petit Cloud — paid |

## Entitlement rules

- A login or Google authorization must never automatically imply a paid entitlement.
- A paid entitlement must never be required for a capability that runs locally
  or stores data only in a user-owned provider.
- Petit Cloud entitlement must be determined independently from the user's
  Google Drive authorization state.
- If a capability changes from user-owned storage to Petit-managed
  infrastructure, its classification must be reviewed before release.
- Prices, quotas, retention periods, providers, and purchase mechanisms require
  validation before Petit Cloud is approved for implementation.

## Non-functional requirements

- [ ] Preserve Petit's local operation when identity, billing, the network, or an external service is unavailable.
- [ ] Request the narrowest external-provider authorization needed for each capability.
- [ ] Keep Google Drive authorization and Petit Cloud authentication as separate states.
- [ ] Protect personal and pet health data during storage, transfer, retention, export, and deletion.
- [ ] Explain paid gates and authorization requests in accessible, plain language.
- [ ] Prevent entitlement failures from deleting, hiding, or corrupting local data.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Capability classification, authorization state, entitlement state, and gate rules. |
| Integration | Google Drive authorization remains independent from Petit Cloud authentication and billing. |
| UI/E2E | Local, Drive-connected, inactive-cloud, active-cloud, and expired-cloud journeys. |

## Acceptance criteria

The scenarios in **Functional requirements** are the testable criteria for this
spec and must have traceable coverage before the status advances to
`Implemented`.

## Product language

Use the following labels consistently:

- **Petit Local** for capabilities that run on the device.
- **Connect Google Drive** for user-owned backup authorization.
- **Petit Cloud** for paid capabilities using Petit-managed infrastructure.
- **Petit account** only for identity used by Petit-managed services.

Do not describe Google Drive authorization as purchasing, activating, or
logging in to Petit Cloud.

## Edge cases

- Google Drive authorization is revoked while the Petit Cloud entitlement remains active.
- The Petit Cloud entitlement expires while Google Drive authorization remains valid.
- The user selects different Google accounts for Drive and Petit authentication.
- The user purchases Petit Cloud but has not authenticated a persistent Petit identity.
- Billing, identity, Drive, or the hosted cloud provider is temporarily unavailable.
- Provider quotas or prices change and alter Petit's marginal operating cost.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Proposal status | Draft | The service model is defined and awaits explicit approval. |
| Charging principle | Charge only for Petit-managed infrastructure with ongoing cost | Login and convenience alone do not justify payment. |
| Local capabilities | Free without a Petit account | Petit remains useful offline and local-first. |
| Google Drive backup | Free with separate Google authorization | Storage belongs to the user and is not hosted by Petit. |
| Hosted synchronization and collaboration | Petit Cloud, paid | Petit operates the remote infrastructure and bears its cost. |
| PDF export | Free and local | It does not require Petit-managed infrastructure. |
| Family sharing | Local sharing is free; remote continuous collaboration is paid | The transport and operating cost differ. |
| External technology | Undecided | Firebase and other providers remain implementation options, not product requirements. |
| Local source of truth | Room | Entitlement or connectivity changes must not compromise local access. |

## Out of scope

- Selecting the hosted-cloud provider.
- Defining subscription prices, billing periods, margins, or provider-specific quotas.
- Implementing billing, authentication, Drive integration, or cloud synchronization before approval.
- Charging for features solely because they are valuable, convenient, or require a login.
