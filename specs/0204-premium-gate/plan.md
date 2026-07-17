# Plan: Petit Cloud Gate

Spec: [spec.md](./spec.md)

## Status

This plan is **Draft**. No step authorizes implementation until the spec has
been explicitly approved.

## Dependencies

- Spec `0201` for persistent Petit identity when a hosted service requires it.
- Specs `0301`–`0307` for user-owned Google Drive backup and recovery.
- Specs `0401`–`0405` for Petit-hosted synchronization and collaboration.
- Validation of hosted-provider costs, data retention, billing policy, and
  entitlement lifecycle before implementation.

## Proposed sequence

1. Define a provider-independent capability catalog with the service models
   `Petit Local`, `User-owned cloud`, and `Petit Cloud`.
2. Represent Google Drive authorization, Petit authentication, and Petit Cloud
   entitlement as independent states.
3. Apply the classification to settings and capability entry points without
   gating local or user-owned-cloud behavior.
4. Add the Petit Cloud explanation and gate only to hosted synchronization,
   multi-device synchronization, and remote family collaboration.
5. Implement entitlement activation, refresh, expiration, and recovery without
   changing the Room local source of truth.
6. Add billing only after the hosted costs, retention policy, and purchase
   mechanism have been separately validated and approved.
7. Verify local, Drive-connected, active-cloud, inactive-cloud, expired-cloud,
   offline, and provider-failure journeys.

## Design boundaries

- Feature availability depends on its service model, not on a generic
  `isPremium` flag.
- Google Drive authorization cannot activate Petit Cloud.
- Petit Cloud expiration cannot disable local capabilities or Google Drive
  backup.
- UI language must explain whether the user is authorizing their own provider
  or purchasing infrastructure operated by Petit.
- Hosted-provider types and billing SDKs remain behind interfaces until their
  selection is approved.

## Risks and validations

- Hosted-service costs may vary with reads, writes, storage, traffic, active
  users, or provider policy changes.
- A generic premium flag could accidentally gate free capabilities.
- Reusing the same Google identity for Drive and Petit Cloud could make two
  distinct consent states appear equivalent.
- Subscription expiration requires an explicit hosted-data retention,
  portability, and deletion policy.
- Remote family collaboration requires participant identity, access control,
  revocation, and auditable ownership rules.

## Planned verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When there is a build: `./gradlew assembleDebug` followed by `./gradlew installDebug`
