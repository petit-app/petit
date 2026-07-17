# Plan: Cloud Conflict Resolution

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0401`
- Revalidate demand, privacy, costs, provider terms, and the availability model.

## Proposed Sequence

1. Revalidate the spec scenarios against the current product and update obsolete decisions.
2. Create contract tests and domain rules for the first vertical slice.
3. Implement the minimum integration behind repository abstractions, keeping Room as the local source of truth.
4. Deliver UI states and error recovery for the same slice.
5. Repeat the cycle for each task, including migration and compatibility work when needed.
6. Run the focused tests and relevant Android suites before updating the status.

## Historical Technical Notes

The class names, APIs, dependencies, and code snippets below came from the original proposal and must be reconciled with the current code and versions before use.

### Technical Requirements

### ConflictResolver

```kotlin
class ConflictResolver {

    sealed class Resolution {
        object KeepLocal : Resolution()
        object UseRemote : Resolution()
        data class Merge(val merged: Any) : Resolution()
    }

    fun <T : SyncableEntity> resolve(local: T?, remote: T): Resolution {
        // Does not exist locally: use remote
        if (local == null) {
            return Resolution.UseRemote
        }

        // Check deletions
        val localDeleted = local.deletedAt != null
        val remoteDeleted = remote.deletedAt != null

        return when {
            // Both deleted: use the newer record
            localDeleted && remoteDeleted -> {
                if (remote.updatedAt >= local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }

            // Only remote is deleted
            remoteDeleted -> {
                // If the deletion is newer than the local update, accept the deletion
                if (remote.deletedAt!! >= local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }

            // Only local is deleted
            localDeleted -> {
                // If the remote update is newer than the local deletion, restore it
                if (remote.updatedAt > local.deletedAt!!) Resolution.UseRemote
                else Resolution.KeepLocal
            }

            // Neither is deleted: compare updatedAt
            else -> {
                if (remote.updatedAt > local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }
        }
    }
}
```

### Applying the Resolution

```kotlin
class SyncProcessor(
    private val conflictResolver: ConflictResolver,
    private val petDao: PetDao
) {
    suspend fun processRemotePet(remote: PetFirestoreModel) {
        val local = petDao.getPetById(remote.id)

        when (val resolution = conflictResolver.resolve(local, remote.toEntity())) {
            is ConflictResolver.Resolution.UseRemote -> {
                petDao.insertPet(remote.toEntity().copy(syncStatus = "SYNCED"))
            }
            is ConflictResolver.Resolution.KeepLocal -> {
                // Local is newer and must be uploaded again
                if (local != null && local.syncStatus != "SYNCED") {
                    // It will be sent in the next upload cycle
                }
            }
            is ConflictResolver.Resolution.Merge -> {
                // Future implementation for field-level merge
            }
        }
    }
}
```

### Clock Synchronization

To ensure that `updatedAt` is reliable across devices:

```kotlin
object SyncClock {
    /**
     * Returns a timestamp for use in updatedAt
     * Accounts for possible clock differences between devices
     */
    fun now(): Long {
        // For simplicity, use System.currentTimeMillis()
        // In production, consider using Firestore server timestamps
        // or NTP to synchronize clocks
        return System.currentTimeMillis()
    }
}

// In Firestore, use FieldValue.serverTimestamp() for updated_at
// The updated_at column can use FieldValue.serverTimestamp()
// However, for Last-Write-Wins, the client sends the local timestamp
```

### Conflict Logging (Debug)

```kotlin
class ConflictLogger(
    private val analyticsTracker: AnalyticsTracker
) {
    fun logConflict(
        entityType: String,
        entityId: String,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long,
        resolution: String
    ) {
        if (BuildConfig.DEBUG) {
            Log.d("ConflictResolver", """
                Conflict detected:
                  Entity: $entityType/$entityId
                  Local updatedAt: $localUpdatedAt
                  Remote updatedAt: $remoteUpdatedAt
                  Resolution: $resolution
            """.trimIndent())
        }

        // Analytics to monitor conflict frequency
        analyticsTracker.trackEvent("sync_conflict", mapOf(
            "entity_type" to entityType,
            "resolution" to resolution
        ))
    }
}
```

---


## Risks and Validation

- Dependence on external services, authentication, quotas, and contractual changes.
- Privacy and the lifecycle of personal and pet health data.
- Database migrations and compatibility with data created offline or in older versions.
- Concurrency, idempotency, conflicts, and recovery after interruptions.
- Accessibility and clarity of error, waiting, and destructive-confirmation states.

## Planned Verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When a build is run: `./gradlew assembleDebug` followed by `./gradlew installDebug`
