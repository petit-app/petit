# Plan: Offline-First Sync

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

### Network Listener

```kotlin
class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Initial state
        val isCurrentlyOnline = connectivityManager.activeNetwork != null
        trySend(isCurrentlyOnline)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
}
```

### Auto-Sync on Reconnect

```kotlin
class SyncOnReconnectManager(
    private val networkMonitor: NetworkMonitor,
    private val syncEngine: SyncEngine,
    private val premiumRepository: PremiumRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .filter { isOnline -> isOnline }  // Only when connectivity returns
                .collect {
                    if (premiumRepository.isPremium()) {
                        syncEngine.uploadPending()
                    }
                }
        }
    }
}
```

### Pending Item Count

```kotlin
class PendingSyncCounter(
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao
) {
    data class PendingCount(
        val pets: Int = 0,
        val weights: Int = 0,
        val vaccinations: Int = 0,
        val dewormings: Int = 0
    ) {
        val total: Int get() = pets + weights + vaccinations + dewormings
        val isEmpty: Boolean get() = total == 0
    }

    fun getPendingCount(): Flow<PendingCount> = combine(
        petDao.countPendingSync(),
        weightDao.countPendingSync(),
        vaccinationDao.countPendingSync(),
        dewormingDao.countPendingSync()
    ) { pets, weights, vaccinations, dewormings ->
        PendingCount(pets, weights, vaccinations, dewormings)
    }
}

// DAOs
@Query("SELECT COUNT(*) FROM pets WHERE syncStatus = 'PENDING_SYNC' AND deletedAt IS NULL")
fun countPendingSync(): Flow<Int>
```

### Firestore Offline Handling

```kotlin
// Firestore has native offline persistence (isPersistenceEnabled = true).
// Room remains the source of truth for local queries.
// Pending data remains in Room with syncStatus = PENDING_SYNC
// and is uploaded through WorkManager when connectivity returns.
```

### WorkManager for Pending Sync

```kotlin
class UploadPendingWorker(
    context: Context,
    params: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val premiumRepository: PremiumRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!premiumRepository.isPremium()) {
            return Result.success()
        }

        return syncEngine.uploadPending()
            .map { Result.success() }
            .getOrElse { Result.retry() }
    }

    companion object {
        fun scheduleIfPending(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<UploadPendingWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniqueWork(
                "upload_pending",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
```

### Mark as Pending When Saving

```kotlin
class PetRepositoryImpl(
    private val petDao: PetDao,
    private val premiumRepository: PremiumRepository,
    private val syncEngine: SyncEngine
) : PetRepository {

    override suspend fun insertPet(pet: PetEntity) {
        val syncStatus = if (premiumRepository.isPremium()) {
            "PENDING_SYNC"
        } else {
            "LOCAL_ONLY"
        }

        petDao.insertPet(pet.copy(syncStatus = syncStatus))

        // Attempt to sync immediately when online
        if (syncStatus == "PENDING_SYNC") {
            syncEngine.uploadPending()
        }
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
