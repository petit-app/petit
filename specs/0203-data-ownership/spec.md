---
spec: "0203"
title: "Data Ownership"
family: identity-access
phase: 3
status: On Hold
owner: ""
depends_on: ["0201"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Data Ownership

## Context and motivation

> As a user who has just logged in,
> I want my local data to be linked to my account,
> So that I can synchronize it to the cloud in the future.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before it is approved.

## Functional requirements

### Scenario 1: First login links existing data

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I have 2 pets registered locally
AND I have never logged in before
WHEN I log in with "pessoa-a@example.com"
THEN my 2 pets are marked with ownerId = my userId
AND I can continue using the app normally
```

### Scenario 2: Data created after login is linked automatically

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am logged in as "pessoa-a@example.com"
WHEN I register a new pet named "Luna"
THEN Luna is created with ownerId = my userId
```

### Scenario 3: Data created in anonymous mode has no owner

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am using the app without being logged in
WHEN I register a pet named "Simba"
THEN Simba is created with ownerId = null
```

### Scenario 4: Logging out does not remove ownership

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I have pets linked to my userId
WHEN I log out
THEN the pets retain their ownerId
AND remain visible in the app
```

### Scenario 5: Log in with a different account

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I have pets belonging to "user-a" on the device
AND I log in as "pessoa-b@example.com" (user-b)
THEN I see Person A's pets (local data)
BUT they retain ownerId = user-a
AND new data will have ownerId = user-b
(management of multiple owners is handled in a future sync)
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

### Data Display

### Phase 2: Show all local data

```kotlin
// For now, show all local data regardless of owner
fun getAllPets(): Flow<List<PetEntity>> {
    return petDao.getAllPets()  // No owner filter
}
```

### Future phase (5): Filter by owner for sync

```kotlin
// When implementing sync, filter by owner
fun getPetsForSync(userId: String): Flow<List<PetEntity>> {
    return petDao.getPetsForUser(userId)
}
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
