# Plan: Backup Triggers

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0305`, `0306`
- Revalidate demand, privacy, costs, provider terms, and the availability model.

## Proposed sequence

1. Revalidate the spec scenarios against the current product and update obsolete decisions.
2. Create contract tests and domain rules for the first vertical slice.
3. Implement the minimum integration behind repository abstractions, keeping Room as the local source of truth.
4. Deliver UI states and error recovery for the same slice.
5. Repeat the cycle for each task, including migration and compatibility work when necessary.
6. Run the focused tests and relevant Android suites before updating the status.

## Historical technical notes

The class names, APIs, dependencies, and code snippets below must be reviewed against the current code and versions before use.

### Architecture

### Trigger Flow

```
┌─────────────────────────────────────────────────────────────┐
│                           App                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PetRepository        WeightRepository       VaccinationRepo│
│  ┌───────────┐        ┌───────────┐         ┌───────────┐  │
│  │  insert() │        │  insert() │         │  insert() │  │
│  └─────┬─────┘        └─────┬─────┘         └─────┬─────┘  │
│        │                    │                     │         │
│        └────────────────────┼─────────────────────┘         │
│                             │                               │
│                             ▼                               │
│                   ┌─────────────────┐                       │
│                   │ BackupTrigger   │                       │
│                   │   Manager       │                       │
│                   └────────┬────────┘                       │
│                            │                                │
│                            ▼                                │
│                   ┌─────────────────┐                       │
│                   │   Debouncer     │                       │
│                   │   (5 min)       │                       │
│                   └────────┬────────┘                       │
│                            │                                │
│                            ▼                                │
│                   ┌─────────────────┐                       │
│                   │  WorkManager    │                       │
│                   │ OneTimeRequest  │                       │
│                   └─────────────────┘                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### Technical Requirements

### BackupTriggerManager

```kotlin
class BackupTriggerManager(
    private val workManager: WorkManager,
    private val backupPreferences: BackupPreferencesRepository,
    private val premiumRepository: PremiumRepository
) {
    companion object {
        const val DEBOUNCE_DELAY_MINUTES = 5L
        const val WORK_TAG = "backup_on_change"
    }

    fun onDataChanged() {
        // Check whether automatic backup is enabled and the user is premium
        if (!backupPreferences.isAutoBackupEnabled() || !premiumRepository.isPremium()) {
            return
        }

        // Cancel the previous pending work (debounce)
        workManager.cancelAllWorkByTag(WORK_TAG)

        // Schedule new work with a delay
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (backupPreferences.isWifiOnly())
                    NetworkType.UNMETERED
                else
                    NetworkType.CONNECTED
            )
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BackupOnChangeWorker>()
            .setInitialDelay(DEBOUNCE_DELAY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueue(workRequest)
    }

    fun cancelPendingBackup() {
        workManager.cancelAllWorkByTag(WORK_TAG)
    }

    fun hasPendingBackup(): Boolean {
        val workInfos = workManager.getWorkInfosByTag(WORK_TAG).get()
        return workInfos.any { !it.state.isFinished }
    }
}
```

### BackupOnChangeWorker

```kotlin
class BackupOnChangeWorker(
    context: Context,
    params: WorkerParameters,
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check premium status (it may have expired while waiting)
        if (!premiumRepository.isPremium()) {
            return Result.success()  // Not a failure; just skip
        }

        // Run backup
        return backupUseCase()
            .map {
                backupPreferences.setLastBackupTimestamp(System.currentTimeMillis())
                backupPreferences.setLastBackupError(null)
                Result.success()
            }
            .getOrElse { error ->
                // Do not retry a change-triggered backup
                // The next periodic backup will try again
                backupPreferences.setLastBackupError(error.message)
                Result.success()  // Mark as successful to prevent repeated attempts
            }
    }
}
```

### Integration into Repositories

```kotlin
class PetRepositoryImpl(
    private val petDao: PetDao,
    private val backupTriggerManager: BackupTriggerManager
) : PetRepository {

    override suspend fun insertPet(pet: PetEntity) {
        petDao.insertPet(pet)
        backupTriggerManager.onDataChanged()
    }

    override suspend fun updatePet(pet: PetEntity) {
        petDao.updatePet(pet)
        backupTriggerManager.onDataChanged()
    }

    override suspend fun deletePet(id: String) {
        petDao.softDeletePet(id)
        backupTriggerManager.onDataChanged()
    }
}
```

### Using the Callback/Listener Pattern

```kotlin
// Alternative: use an event pattern
interface DataChangeListener {
    fun onDataChanged(entityType: EntityType)
}

enum class EntityType {
    PET, WEIGHT, VACCINATION, DEWORMING
}

class DataChangePublisher {
    private val listeners = mutableListOf<DataChangeListener>()

    fun addListener(listener: DataChangeListener) {
        listeners.add(listener)
    }

    fun notifyDataChanged(entityType: EntityType) {
        listeners.forEach { it.onDataChanged(entityType) }
    }
}

// BackupTriggerManager implements DataChangeListener
class BackupTriggerManager(...) : DataChangeListener {
    override fun onDataChanged(entityType: EntityType) {
        // Trigger backup with debounce
        onDataChanged()
    }
}
```

### Avoid Conflicts with Periodic Backup

```kotlin
class AutoBackupWorker(...) : CoroutineWorker(...) {

    override suspend fun doWork(): Result {
        // Cancel the pending change-triggered backup (avoid duplication)
        WorkManager.getInstance(applicationContext)
            .cancelAllWorkByTag(BackupTriggerManager.WORK_TAG)

        // Continue with the normal backup...
        return backupUseCase()
            .map { Result.success() }
            .getOrElse { Result.retry() }
    }
}
```

---


## Risks and validation

- Dependency on external services, authentication, quotas, and contractual changes.
- Privacy and the lifecycle of personal and pet health data.
- Database migrations and compatibility with data created offline or in older versions.
- Concurrency, idempotency, conflicts, and recovery after interruptions.
- Accessibility and clarity of error, waiting, and destructive confirmation states.

## Planned verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When a build is run: `./gradlew assembleDebug` followed by `./gradlew installDebug`
