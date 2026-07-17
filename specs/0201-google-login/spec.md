---
spec: "0201"
title: "Google Login"
family: identity-access
phase: 3
status: On Hold
owner: ""
depends_on: []
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Google Login

## Context and motivation

> As an app user,
> I want to log in with my Google account,
> So that I can back up my data to Google Drive and access premium features.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before it is approved.

## Functional requirements

### Scenario 1: Successful login

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am using the app without being logged in
WHEN I tap "Sign in with Google"
THEN I see the system Google account picker
WHEN I select my account
AND authorize the app
THEN I am successfully authenticated
AND I see my name/photo on the settings screen
AND the state changes to "Authenticated"
```

### Scenario 2: First login associates existing data

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I have local data (pets, weights, etc.)
AND I have never logged in before
WHEN I sign in with Google for the first time
THEN my local data is associated with my userId
AND I can continue using the app normally
```

### Scenario 3: Login canceled

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I start the login process
WHEN I cancel the account picker
OR close the dialog
THEN I return to the previous state (anonymous)
AND I do not see an error message
AND I can try again
```

### Scenario 4: Network error during login

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I have no internet connection
WHEN I try to log in
THEN I see the message "No connection. Try again."
AND I remain in anonymous mode
AND the app continues to work normally offline
```

### Scenario 5: Login triggered when attempting a backup

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am using the app without being logged in
AND I have local data (pets, weights, etc.)
WHEN I try to "Back up to Google Drive"
THEN I see a dialog explaining that login is required
AND I have a "Sign in with Google" option
WHEN I log in successfully
THEN the backup starts automatically
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

### Settings Screen (Logged Out)

```
┌────────────────────────────────┐
│ ← Settings                     │
├────────────────────────────────┤
│                                │
│ 👤 ACCOUNT                     │
│ ┌────────────────────────────┐ │
│ │        🔒                  │ │
│ │  You are not logged in     │ │
│ │                            │ │
│ │  Log in to protect your    │ │
│ │  data and access premium   │ │
│ │  features.                 │ │
│ │                            │ │
│ │ ┌────────────────────────┐ │ │
│ │ │ 🔵 Sign in with Google│ │ │
│ │ └────────────────────────┘ │ │
│ └────────────────────────────┘ │
│                                │
│ 📦 DATA                        │
│ ...                            │
└────────────────────────────────┘
```

### Settings Screen (Logged In)

```
┌────────────────────────────────┐
│ ← Settings                     │
├────────────────────────────────┤
│                                │
│ 👤 ACCOUNT                     │
│ ┌────────────────────────────┐ │
│ │ ┌────┐                     │ │
│ │ │ 📷 │ Person A            │ │
│ │ └────┘ pessoa-a@example.com │ │
│ │        Plan: Free          │ │
│ │                            │ │
│ │ [Manage account] [Log out] │ │
│ └────────────────────────────┘ │
│                                │
│ ⭐ PREMIUM                     │
│ ┌────────────────────────────┐ │
│ │ Unlock cloud sync,         │ │
│ │ automatic backup, and more!│ │
│ │ [View plans]              │ │
│ └────────────────────────────┘ │
└────────────────────────────────┘
```

### Login Flow

```
┌──────────────────────────────────────────────────┐
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │                                            │  │
│  │  Choose an account                         │  │
│  │                                            │  │
│  │  ┌────┐  pessoa-a@example.com              │  │
│  │  │ 📷 │  Person A                          │  │
│  │  └────┘                                    │  │
│  │                                            │  │
│  │  ┌────┐  pessoa-b@example.com              │  │
│  │  │ 📷 │  Person B                          │  │
│  │  └────┘                                    │  │
│  │                                            │  │
│  │  ┌────────────────────────────────────┐   │  │
│  │  │ + Use another account              │   │  │
│  │  └────────────────────────────────────┘   │  │
│  │                                            │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
└──────────────────────────────────────────────────┘
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
