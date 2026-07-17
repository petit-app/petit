---
spec: "0405"
title: "Cloud Family Sharing"
family: cloud-sync
status: On Hold
owner: woliveiras
depends_on: ["0201", "0401"]
---

# Spec: Cloud Family Sharing

## Context and motivation

> As a premium user,
> I want to share my pets' data with my family,
> So that we can all track and record pet information together.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before approval.

## Functional requirements

### Scenario 1: Create a family group

- [ ] This scenario is covered and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am a premium user
WHEN I open Settings > "Family"
AND I tap "Create family group"
THEN a group is created
AND I become the administrator
AND I receive an invitation code
```

### Scenario 2: Invite a member

- [ ] This scenario is covered and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am the admin of a family group
WHEN I share the invitation code "PETIT-ABC123"
AND another person enters the code in their app
THEN they join the family group
AND can see all pets in the group
```

### Scenario 3: Everyone can view and edit

- [ ] This scenario is covered and verified at the boundary defined by the test strategy.

```gherkin
GIVEN Person A and Person B are in the same family group
WHEN Person B adds a weight measurement for a pet
THEN Person A sees the weight measurement automatically
AND Person A can also add or edit data
```

### Scenario 4: Admin permissions

- [ ] This scenario is covered and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am the group admin
WHEN I open the member list
THEN I can:
  - Remove members
  - Generate a new invitation code
  - Delete the group
```

### Scenario 5: A member leaves the group

- [ ] This scenario is covered and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am a member of a family group
WHEN I select "Leave group"
THEN I lose access to the shared data
AND the data remains available to the other members
AND my personal data (not shared) remains with me
```

### Scenario 6: Private vs. shared pets

- [ ] This scenario is covered and verified at the boundary defined by the test strategy.

```gherkin
GIVEN I am a member of a family group
WHEN I add a new pet
THEN I can choose:
  - "Share with family" (everyone can see it)
  - "Keep private" (only I can see it)
```

---

## Non-functional requirements

- [ ] Preserve Petit's local operation when authentication, the network, or an external service is unavailable.
- [ ] Protect personal and pet health data during storage, transit, and deletion.
- [ ] Provide accessible, understandable loading, success, empty, and error states.
- [ ] Prevent silent data loss or duplication during interrupted operations.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Eligibility, validation, state, conflict, and data transformation rules. |
| Integration | Flows that cross the UI, repositories, local database, and external providers. |
| Both | Each vertical task uses unit tests for rules and integration tests for I/O boundaries. |

## Acceptance criteria

The scenarios under **Functional requirements** are this spec's testable acceptance criteria and must have traceable coverage before the status advances to `Implemented`.

## Preserved product notes

### UI/UX

### Screen: Family

```
┌────────────────────────────────┐
│ ← Family                       │
├────────────────────────────────┤
│                                │
│ 👨‍👩‍👧 FAMILY GROUP               │
│ ┌────────────────────────────┐ │
│ │ Example Family             │ │
│ │ 3 members                  │ │
│ └────────────────────────────┘ │
│                                │
│ 👥 MEMBERS                     │
│ ┌────────────────────────────┐ │
│ │ 👤 Person A (you)           │ │
│ │    Admin                   │ │
│ │                            │ │
│ │ 👤 Person B                 │ │
│ │    Member                  │ │
│ │                            │ │
│ │ 👤 Person C                 │ │
│ │    Member                  │ │
│ └────────────────────────────┘ │
│                                │
│ 🔗 INVITATION                  │
│ ┌────────────────────────────┐ │
│ │ Code: PETIT-ABC123         │ │
│ │ [Copy]  [Share]            │ │
│ │                            │ │
│ │ Expires in 7 days          │ │
│ │ [Generate new code]        │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │       LEAVE GROUP          │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Screen: Join a Group

```
┌────────────────────────────────┐
│ ← Join a Family Group          │
├────────────────────────────────┤
│                                │
│ Enter the invitation code:     │
│                                │
│ ┌────────────────────────────┐ │
│ │ PETIT-                     │ │
│ └────────────────────────────┘ │
│                                │
│ Ask the person who created     │
│ the family group for the code. │
│                                │
│ ┌────────────────────────────┐ │
│ │          JOIN              │ │
│ └────────────────────────────┘ │
│                                │
│             or                 │
│                                │
│ ┌────────────────────────────┐ │
│ │      CREATE NEW GROUP      │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Selector When Creating a Pet

```
┌────────────────────────────────┐
│                                │
│ Sharing                        │
│                                │
│ ○ 👤 Private                   │
│   Only you will see this pet   │
│                                │
│ ● 👨‍👩‍👧 Example Family           │
│   All members will see it      │
│                                │
└────────────────────────────────┘
```

---

## Edge cases

- The device loses connectivity or the process is interrupted during the operation.
- The session expires, switches accounts, or lacks sufficient authorization.
- Local and remote data diverge, are incomplete, or were created by different app versions.
- The external provider is unavailable, imposes quota limits, or changes its API.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Proposal status | On Hold | Demand and the product model still need to be validated. |
| External technology | Undecided | Firebase, Google Drive, and the referenced APIs are historical options, not current commitments. |
| Local source of truth | Preserve Room as the offline foundation | Keeps Petit useful without an account or connectivity. |

## Out of scope

- Implementing this proposal before review, explicit approval, and an index update.
- Treating historical examples of pricing, tier, provider, or schedule as current decisions.
- Features covered by the specs listed in `depends_on`.
