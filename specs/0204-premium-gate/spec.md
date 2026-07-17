---
spec: "0204"
title: "Premium Gate"
family: identity-access
status: On Hold
owner: woliveiras
depends_on: ["0201"]
---

# Spec: Premium Gate

## Context and motivation

> As an app user,
> I want to understand which features are premium,
> So that I can decide whether a subscription is worthwhile.

This is a historical hypothesis that has not yet been implemented. The product, external provider, availability, and monetization must be revalidated before it is approved.

## Functional requirements

### Scenario 1: See a premium indicator on a locked feature

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am a free user
WHEN I see the "Real-time synchronization" option in settings
THEN I see a ⭐ or 🔒 icon indicating that it is premium
AND when I tap it, I see information about the premium plan
```

### Scenario 2: Try to use a premium feature

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am a free user
WHEN I try to enable "Real-time synchronization"
THEN I see a bottom sheet or dialog explaining:
  - What the feature does
  - That it is exclusive to premium users
  - A button to view plans
```

### Scenario 3: List premium benefits

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am in the app
WHEN I open "View premium plans"
THEN I see a list of benefits:
  - ☁️ Real-time cloud synchronization
  - 📱 Multiple devices synchronized automatically
  - 👨‍👩‍👧 Share with family
  - 📄 Export PDF (future)
```

### Scenario 4: Check premium status

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am a premium user
WHEN I open settings
THEN I see "Plan: Premium"
AND I do not see lock indicators on premium features
AND the premium features are available
```

### Scenario 5: Free features available without login

- [ ] This scenario is fulfilled and verified at the boundary defined by the test strategy.

```gherkin
GIVEN that I am not logged in
WHEN I use the app
THEN I can register pets, record weights and vaccinations, and create reminders
AND I can export/import JSON
BUT I cannot back up to Google Drive (requires login)
AND I cannot use real-time synchronization (premium)
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

### Features by Tier

| Feature | Free (no login) | Free (with login) | Premium |
|---------|------------------|------------------|---------|
| Pet registration | ✅ | ✅ | ✅ |
| Weight tracking + chart | ✅ | ✅ | ✅ |
| Vaccination/Deworming | ✅ | ✅ | ✅ |
| Local reminders | ✅ | ✅ | ✅ |
| Export/Import JSON | ✅ | ✅ | ✅ |
| Google Login | ❌ | ✅ | ✅ |
| Manual Google Drive backup | ❌ | ✅ | ✅ |
| Automatic Google Drive backup (2 a.m.) | ❌ | ✅ | ✅ |
| Restore Google Drive backup | ❌ | ✅ | ✅ |
| Device-to-device transfer (Nearby) | ✅ | ✅ | ✅ |
| Real-time synchronization (Firebase Firestore) | 🔒 | 🔒 | ✅ |
| Multiple synchronized devices | 🔒 | 🔒 | ✅ |
| Share with family | 🔒 | 🔒 | ✅ |

---

### UI/UX

### Settings with Gates

```
┌────────────────────────────────┐
│ ← Settings                     │
├────────────────────────────────┤
│                                │
│ 📦 DATA                        │
│ ┌────────────────────────────┐ │
│ │ 📤 Export data            │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📥 Import data            │ │
│ └────────────────────────────┘ │
│                                │
│ ☁️ BACKUP (GOOGLE DRIVE)       │
│ ┌────────────────────────────┐ │
│ │ 💾 Manual backup           │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ⏰ Automatic backup (2 a.m.)│ │
│ └────────────────────────────┘ │
│                                │
│ 📶 TRANSFER                    │
│ ┌────────────────────────────┐ │
│ │ 🔄 Share data              │ │
│ └────────────────────────────┘ │
│                                │
│ 🔒 PREMIUM                     │
│ ┌────────────────────────────┐ │
│ │ 🔄 Real-time sync         ⭐ │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Unlock automatic           │ │
│ │ multi-device sync!         │ │
│ │ [View plans]              │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Bottom Sheet: Locked Feature

```
┌────────────────────────────────┐
│                    ─────       │
│                                │
│         ⭐                     │
│   Premium Feature              │
│                                │
│   Real-time cloud              │
│   synchronization              │
│                                │
│   Your data synchronizes       │
│   automatically across all     │
│   your devices.                │
│                                │
│ ┌────────────────────────────┐ │
│ │       VIEW PLANS           │ │
│ └────────────────────────────┘ │
│                                │
│        Not now                 │
│                                │
└────────────────────────────────┘
```

### Screen: Premium Plans

```
┌────────────────────────────────┐
│ ← Petit Premium                │
├────────────────────────────────┤
│                                │
│         ⭐ PREMIUM             │
│                                │
│ Take better care of your pets  │
│                                │
├────────────────────────────────┤
│                                │
│ ✅ Real-time synchronization   │
│ ✅ Multiple devices            │
│ ✅ Share with family           │
│ ✅ Priority support            │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │       MONTHLY              │ │
│ │       R$ 9,90/month        │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │       ANNUAL               │ │
│ │       R$ 79,90/year        │ │
│ │       (save 33%)           │ │
│ └────────────────────────────┘ │
│                                │
│ Cancel anytime.                │
│ Your local data is yours.      │
│                                │
└────────────────────────────────┘
```

---

### Note on Billing

The implementation using Google Play Billing (subscription purchase) was a later hypothesis in the old roadmap. If this hypothesis is validated, the proposed scope would be:

1. ✅ Display visual gates
2. ✅ Check premium status via Firebase Firestore
3. ✅ Lock premium features in code
4. ⏳ Integrate with Google Play Billing (future implementation)

Premium status can be set manually in Firebase Console for testing.

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
