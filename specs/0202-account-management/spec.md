---
spec: "0202"
title: "Account Management"
family: identity-access
status: On Hold
owner: woliveiras
depends_on: ["0201"]
---

# Spec: Account Management

## Context and motivation

> As a logged-in app user,
> I want to manage my account (view data, log out, delete the account),
> So that I have control over my identity in the app.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before it is approved.

## Functional requirements

### Scenario 1: View account information

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am logged in as "pessoa-a@example.com"
WHEN I go to Settings > My Account
THEN I see:
  - My Google profile photo
  - My name "Person A"
  - My email "pessoa-a@example.com"
  - My plan status (Free/Premium)
  - Date of last login
```

### Scenario 2: Log out

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am logged in
WHEN I tap "Log out"
AND confirm the action
THEN I am logged out of Firebase Auth
AND I return to the "Anonymous" state
AND my local data remains on the device
AND I can continue using the app without being logged in
```

### Scenario 3: Logging out preserves local data

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I have 2 registered pets
AND I am logged in
WHEN I log out
THEN I can still see my 2 pets
AND I can add new data
AND the data is not deleted
```

### Scenario 4: Switch accounts

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am logged in as "pessoa-a@example.com"
WHEN I log out
AND log in with "pessoa-b@example.com"
THEN I am authenticated as Person B
AND the local data remains (Person A's data)
(data association by account is handled by the cloud-sync family)
```

### Scenario 5: Delete account

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am logged in
WHEN I tap "Delete my account"
THEN I see a warning explaining the consequences:
  - "Your account will be removed from Firebase"
  - "Local data will remain on the device"
  - "Cloud data will be removed within 30 days"
WHEN I confirm by typing "DELETE"
THEN my account is removed from Firebase
AND the cloud data is scheduled to be purged in 30 days
AND I am logged out
AND I return to anonymous mode
```

---

## Non-functional requirements

- [ ] Preserve Petit's local operation when authentication, the network, or an external service is unavailable.
- [ ] Protect personal and pet health data during storage, transfer, and deletion.
- [ ] Provide accessible and understandable loading, success, empty, and error states.
- [ ] Prevent silent data loss or duplication during interrupted operations.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Eligibility, validation, state, conflict, and data transformation rules. |
| Integration | Flows that cross the interface, repositories, local database, and external providers. |
| Both | Each vertical task uses unit tests for rules and integration tests for I/O boundaries. |

## Acceptance criteria

The scenarios in **Functional requirements** are the testable criteria for this spec and must have traceable coverage before the status advances to `Implemented`.

## Preserved product notes

### UI/UX

### Screen: My Account

```
┌────────────────────────────────┐
│ ← My Account                   │
├────────────────────────────────┤
│                                │
│         ┌──────────┐           │
│         │          │           │
│         │   📷     │           │
│         │          │           │
│         └──────────┘           │
│         Person A               │
│   pessoa-a@example.com         │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ Plan: Free                 │ │
│ │ [Upgrade ⭐]               │ │
│ └────────────────────────────┘ │
│                                │
│ 📅 ACTIVITY                    │
│ ┌────────────────────────────┐ │
│ │ Last login: 18/03/2026     │ │
│ │ Member since: 01/01/2026   │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │          LOG OUT           │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │     DELETE MY ACCOUNT      │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirm Logout

```
┌────────────────────────────────┐
│          Log out               │
├────────────────────────────────┤
│                                │
│ You will be signed out of your │
│ Google account.                │
│                                │
│ Your local data will be kept   │
│ on the device.                 │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │  CANCEL  │  │   LOG OUT    │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

### Dialog: Delete Account

```
┌────────────────────────────────┐
│     ⚠️ Delete Account          │
├────────────────────────────────┤
│                                │
│ This action is irreversible!   │
│                                │
│ • Your account will be removed │
│ • Cloud data will be removed   │
│   within 30 days               │
│ • Local data will be kept      │
│                                │
│ Type DELETE to confirm:        │
│ ┌────────────────────────────┐ │
│ │                            │ │
│ └────────────────────────────┘ │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │  CANCEL  │  │    DELETE    │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

---

## Edge cases

- The device loses connectivity or the process is interrupted midway through the operation.
- The session expires, switches accounts, or lacks sufficient authorization.
- Local and remote data diverges, is incomplete, or was created by different app versions.
- The external provider is unavailable, limits quotas, or changes its API.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Proposal status | On Hold | Demand and the product model still need to be validated. |
| External technology | Undecided | Firebase, Google Drive, and the cited APIs are historical options, not current commitments. |
| Local source of truth | Preserve Room as the offline foundation | Keeps Petit useful without an account or connectivity. |

## Out of scope

- Implementing this proposal before review, explicit approval, and an index update.
- Treating historical examples of pricing, tiers, providers, or schedules as current decisions.
- Functionality covered by the specs declared in `depends_on`.
