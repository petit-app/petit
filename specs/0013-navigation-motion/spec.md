---
spec: "0013"
title: Navigation motion
family: pet-care
phase: 1
status: In Progress
owner: woliveiras
depends_on: []
origin: prds/2026-07-17-petit-pet-health-management.md
---

# Spec: Navigation motion

## Context and motivation

Petit currently relies on the default Navigation Compose transition, which
crossfades destinations. The same visual treatment is used when the caregiver
changes a main area and when they move deeper into a care workflow. This makes
the navigation hierarchy harder to perceive.

Navigation motion must distinguish two intents:

- changing between the main areas Home, Pets, Add, Tasks, and Profile;
- moving forward or backward through a hierarchical flow such as pet list,
  pet detail, and pet form.

The motion is presentation behavior only. It must not change routes, back-stack
semantics, saved state, data loading, or the result returned by a destination.

## Navigation levels

The main areas are:

| Main area | Route |
| --- | --- |
| Home | `home` |
| Pets | `pets` |
| Add | `quick-add` |
| Tasks | `tasks` |
| Profile | `settings` |

Onboarding is not a main area. Its completion transition to Home uses the
discreet main-area treatment because it replaces onboarding instead of adding
another level to the caregiver's navigation history.

All other full-screen destinations are hierarchical. This includes selection,
detail, form, history, configuration, confirmation, backup, restore, family
group, pairing, transfer, and timeline destinations.

## Motion behavior

### Hierarchical navigation

- When a caregiver advances to a hierarchical destination, the new screen
  enters from the trailing horizontal edge and the current screen moves toward
  the leading edge.
- When a caregiver uses the app back action, system back button, or back
  gesture, the current screen exits toward the trailing edge and the previous
  screen returns from the leading edge.
- Direction follows layout direction. In left-to-right layouts, forward enters
  from the right and back exits to the right. In right-to-left layouts, those
  directions are mirrored.
- A pop from a hierarchical destination to a main area remains a backward
  slide. It must not be mistaken for a main-area switch.
- Forward and backward transitions use the same duration and compatible motion
  curves so that a completed navigation and its reverse feel related.

### Main-area navigation

- Navigating directly to Home, Pets, Add, Tasks, or Profile without popping a
  hierarchical destination uses a short crossfade with no horizontal
  direction.
- Popping between two main-area entries also uses the discreet crossfade.
- Re-selecting the current main area must not create a visible transition or a
  duplicate destination.

### Timing and accessibility

- The hierarchical slide completes in 300 milliseconds.
- The main-area crossfade completes in 150 milliseconds.
- Motion must honor the Android animator duration scale. When system animations
  are disabled, destination changes complete without waiting for an animation.
- Motion must not delay accessibility focus after the destination becomes
  current.
- No transition may flash an empty frame or expose content outside the app's
  navigation container.

## Non-functional requirements

- Transition selection is centralized and independently testable.
- Navigation motion must not recreate destination ViewModels beyond the
  existing Navigation Compose lifecycle.
- Navigation arguments, saved state, scroll state, draft form values, and
  navigation results must remain unchanged.
- Rapid repeated navigation must not leave two destinations interactive or
  produce duplicate main-area entries.
- The implementation must use the existing Navigation Compose dependency and
  must not add a third-party animation library.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Route classification and transition-policy decisions for forward navigation, back-stack pops, main-area switches, onboarding completion, and unknown parameterized routes. |
| Instrumented | Representative main-area crossfade, hierarchical forward slide, reverse pop, RTL direction, disabled system animations, state preservation, and rapid navigation. |
| Manual | Visual review on an emulator or device at normal and disabled animation scales, including gesture back on a supported Android version. |

## Acceptance criteria

- [x] Given Home is current, when the caregiver selects Pets from the bottom
  navigation, then Pets replaces the main-area presentation with a 150 ms
  crossfade and no horizontal slide.
- [x] Given any main area is current, when the caregiver selects another main
  area, then the same discreet crossfade policy is used.
- [x] Given a main area is already current, when the caregiver selects it
  again, then no duplicate destination or visible transition is created.
- [x] Given the pet list is current, when the caregiver opens a pet detail,
  then the detail enters from the trailing edge while the list moves toward the
  leading edge over 300 ms.
- [x] Given the pet detail is current, when the caregiver opens a form or care
  history, then the same forward hierarchical slide is used.
- [x] Given a hierarchical destination is current, when the caregiver
  navigates back, then the current screen exits toward the trailing edge and
  the previous destination returns from the leading edge over 300 ms.
- [x] Given a hierarchical destination above a main area, when it is popped,
  then the transition is the reverse hierarchical slide rather than the
  main-area crossfade.
- [x] Given onboarding is completed, when Home replaces onboarding, then Home
  uses the discreet transition and onboarding is not retained on the back
  stack.
- [x] Given a right-to-left layout direction, when the caregiver advances and
  returns through a hierarchical flow, then both horizontal directions are
  mirrored.
- [ ] Given Android system animations are disabled, when any navigation occurs,
  then the destination change completes without an animation delay.
- [ ] Given a partially completed form or a scrolled destination, when the
  caregiver navigates to a child and returns, then its existing state and
  navigation result behavior are preserved.
- [ ] Given repeated navigation input during a transition, when the transition
  settles, then only the current destination is interactive and the back stack
  contains no unintended duplicate main-area entry.

## Edge cases

- Parameterized routes are classified from their destination patterns rather
  than concrete argument values.
- Deep links and restored back stacks use the transition implied by the actual
  navigation or pop direction.
- A destination not recognized as a main area defaults to hierarchical motion.
- Dialogs, sheets, menus, speed-dial content, and animations within a screen
  retain their existing component-specific motion.

## Decisions

| Decision | Choice | Reasoning |
| --- | --- | --- |
| Hierarchical motion | Directional horizontal slide | Direction communicates forward and backward depth. |
| Main-area motion | Short crossfade | Main areas are peers, so lateral direction would imply a false hierarchy. |
| Policy location | Central navigation transition policy | One policy keeps route classification and animation direction consistent. |
| Layout direction | Mirror leading and trailing edges in RTL | Navigation direction must follow the active layout direction. |
| Dependencies | Existing Navigation Compose APIs | The current dependency already supports destination and pop transitions. |

## Out of scope

- Shared-element transitions between cards, pet photos, or titles.
- Vertical transitions, scale transitions, parallax, or custom physics.
- Changes to the bottom navigation layout or destination set.
- Changes to routes, deep-link definitions, or back-stack ownership.
- Animations inside an individual destination.
