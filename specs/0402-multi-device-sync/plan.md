# Plan: Multi-Device Sync

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

The class names, APIs, dependencies, and code snippets below must be reviewed against the current code and versions before use.

### Technical Requirements

### Initial Download

```kotlin
class InitialSyncUseCase(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val database: PetitDatabase
) {
    sealed class Progress {
        object Starting : Progress()
        data class Downloading(val entity: String, val current: Int, val total: Int) : Progress()
        object Complete : Progress()
        data class Error(val message: String) : Progress()
    }

    suspend fun execute(): Flow<Progress> = flow {
        emit(Progress.Starting)

        val userId = authRepository.getCurrentUser()?.id
            ?: throw Exception("Not logged in")

        database.withTransaction {
            // Download pets
            val petsSnapshot = firestore.collection("pets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("deletedAt", null)
                .get().await()
            val pets = petsSnapshot.documents.mapNotNull { it.toPetEntity() }

            emit(Progress.Downloading("pets", pets.size, pets.size))
            pets.forEach { petDao.insertPet(it) }

            // Download weights
            val weightsSnapshot = firestore.collection("weight_entries")
                .whereEqualTo("userId", userId)
                .get().await()
            val weights = weightsSnapshot.documents.mapNotNull { it.toWeightEntity() }

            emit(Progress.Downloading("weigh-ins", weights.size, weights.size))
            weights.forEach { weightDao.upsertWeightEntry(it) }

            // Similar handling for vaccinations and dewormings...
        }

        emit(Progress.Complete)
    }.catch { e ->
        emit(Progress.Error(e.message ?: "Sync error"))
    }
}
```

### Device Tracking

```kotlin
// In Firestore: devices collection
// devices (user_id, device_id, device_name, last_seen_at, app_version)
data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val lastSeenAt: Long = 0,
    val appVersion: String = ""
)

class DeviceTracker(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val context: Context
) {
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun registerDevice() {
        val userId = authRepository.getCurrentUser()?.id ?: return

        val deviceInfo = mapOf(
            "userId" to userId,
            "deviceId" to deviceId,
            "deviceName" to Build.MODEL,
            "lastSeenAt" to System.currentTimeMillis(),
            "appVersion" to BuildConfig.VERSION_NAME
        )

        firestore.collection("devices")
            .document("${userId}_${deviceId}")
            .set(deviceInfo, SetOptions.merge())
            .await()
    }

    suspend fun updateLastSeen() {
        val userId = authRepository.getCurrentUser()?.id ?: return

        firestore.collection("devices")
            .document("${userId}_${deviceId}")
            .update("lastSeenAt", System.currentTimeMillis())
            .await()
    }

    fun getConnectedDevices(): Flow<List<DeviceInfo>> {
        val userId = authRepository.getCurrentUser()?.id ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection("devices")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    val devices = snapshot?.documents?.mapNotNull {
                        it.toObject(DeviceInfo::class.java)
                    } ?: emptyList()
                    trySend(devices)
                }
            awaitClose { registration.remove() }
        }
    }
}
```

### Heartbeat for Last Seen

```kotlin
class SyncHeartbeatWorker(
    context: Context,
    params: WorkerParameters,
    private val deviceTracker: DeviceTracker
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        deviceTracker.updateLastSeen()
        return Result.success()
    }

    companion object {
        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncHeartbeatWorker>(
                15, TimeUnit.MINUTES
            ).build()

            workManager.enqueueUniquePeriodicWork(
                "sync_heartbeat",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
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
