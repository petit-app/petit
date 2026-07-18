# Plan: Local conflict resolution

Spec: [spec.md](./spec.md)

## Implementation status

Steps 1–7 are implemented locally. The remaining open task is physical
two-device convergence validation for edits, deletes, and equal-time ties.

## Starting point and delivered outcome

The original `updatedAt` merge and separate `SyncLog` write were characterized
before implementation. They now use one pure resolver, batch DAO writes, and a
single Room transaction for the imported entities and audit log.

## Implementation sequence

1. Create characterization tests for the current insertion, most recent version, and log behavior.
2. Choose and document a stable tie-breaker for equal timestamps.
3. Implement a pure resolver that compares creation, editing, and `deletedAt`.
4. Cover determinism, idempotency, symmetry, and duplicate batches with table-driven tests.
5. Integrate the resolver into transfer 0102 within a transaction with `SyncLog`.
6. Implement the history query and screen with correct counts.
7. Make the resolver the only rule consumed by future spec 0104.

## Base rule to preserve

1. UUID missing locally: insert the remote version.
2. Compare active events and deletions by the most recent effective time.
3. Newer remote event: update or apply the soft delete.
4. Newer local event: keep the local version.
5. Equal times: prefer a tombstone, then compare the type-aware, length-prefixed
   canonical payload; never “keep local” on both sides.

## Dependencies and integration

- Depends on the bundle and merge tested by spec 0102.
- Spec 0104 will depend on this resolver before applying LAN changesets.
- Room provides the transaction boundary, and `SyncLog` records the result.

## Risks and mitigation

| Risk | Mitigation |
| --- | --- |
| Tie produces different results | Use a tie-break key derived from stable data and test both orders. |
| Later deletion is lost | Compare `deletedAt` as an event, not only as a flag. |
| Resolver differs between transports | Expose a single pure API used by all importers. |
| Log diverges from the transaction | Persist entities and the log within the same transaction boundary. |
| Incorrect clocks | Document the limitation and prepare a logical version/ID as a future evolution. |

## Final verification

1. Run table-driven unit tests with both input orders.
2. Run Room integration, rollback, and history tests.
3. Run `./gradlew spotlessCheck` and `./gradlew test`.
4. Run the same bundle twice and confirm idempotent state/counts.
5. Validate on two devices that edits and deletions converge.
