# Plan: Automatic Backup

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0301`
- Revalidate demand, privacy, costs, provider terms, and the availability model.

## Proposed sequence

1. Revalidate the spec scenarios against the current product and update obsolete decisions.
2. Create contract tests and domain rules for the first vertical slice.
3. Implement the minimum integration behind repository abstractions, keeping Room as the local source of truth.
4. Deliver UI states and error recovery for the same slice.
5. Repeat the cycle for each task, including migration and compatibility work when necessary.
6. Run the focused tests and relevant Android suites before updating the status.

## Historical technical notes

The class names, APIs, dependencies, and code snippets below came from the original proposal and must be reconciled with the current code and versions before use.

### Technical Requirements

### AutoBackupWorker

```kotlin
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check premium status
        if (!premiumRepository.isPremium()) {
            // Cancel periodic work
            WorkManager.getInstance(applicationContext)
                .cancelUniqueWork(WORK_NAME)
            return Result.failure()
        }

        // Check Wi-Fi if necessary
        val wifiOnly = backupPreferences.isWifiOnly()
        if (wifiOnly && !isOnWifi()) {
            return Result.retry()  // Try again later
        }

        // Run backup
        return backupUseCase()
            .map { backupInfo ->
                // Update timestamp
                backupPreferences.setLastBackupTimestamp(System.currentTimeMillis())

                // Notify if configured
                if (backupPreferences.shouldNotifyOnSuccess()) {
                    notificationHelper.showBackupSuccessNotification(backupInfo)
                }

                Result.success()
            }
            .getOrElse { error ->
                backupPreferences.setLastBackupError(error.message)
                Result.retry()
            }
    }

    private fun isOnWifi(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        const val WORK_NAME = "auto_backup_work"
    }
}
```

### Periodic Backup Scheduling

```kotlin
class BackupScheduler(
    private val workManager: WorkManager,
    private val backupPreferences: BackupPreferencesRepository
) {
    fun scheduleAutoBackup() {
        val intervalHours = backupPreferences.getBackupIntervalHours()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (backupPreferences.isWifiOnly())
                    NetworkType.UNMETERED
                else
                    NetworkType.CONNECTED
            )
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .addTag("auto_backup")
            .build()

        workManager.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelAutoBackup() {
        workManager.cancelUniqueWork(AutoBackupWorker.WORK_NAME)
    }

    fun getNextBackupTime(): Long? {
        val workInfo = workManager.getWorkInfosForUniqueWork(AutoBackupWorker.WORK_NAME)
            .get()
            .firstOrNull()

        return workInfo?.nextScheduleTimeMillis
    }
}
```

### WorkerFactory for Dependency Injection

```kotlin
class PetitWorkerFactory(
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            AutoBackupWorker::class.java.name -> AutoBackupWorker(
                appContext,
                workerParameters,
                premiumRepository,
                backupUseCase,
                backupPreferences,
                notificationHelper
            )
            else -> null
        }
    }
}
```

### BackupPreferencesRepository

```kotlin
interface BackupPreferencesRepository {
    fun isAutoBackupEnabled(): Boolean
    fun setAutoBackupEnabled(enabled: Boolean)

    fun getBackupIntervalHours(): Int
    fun setBackupIntervalHours(hours: Int)

    fun isWifiOnly(): Boolean
    fun setWifiOnly(wifiOnly: Boolean)

    fun shouldNotifyOnSuccess(): Boolean
    fun setNotifyOnSuccess(notify: Boolean)

    fun getLastBackupTimestamp(): Long?
    fun setLastBackupTimestamp(timestamp: Long)

    fun getLastBackupError(): String?
    fun setLastBackupError(error: String?)
}

class BackupPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : BackupPreferencesRepository {

    companion object {
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_INTERVAL_HOURS = intPreferencesKey("backup_interval_hours")
        val WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
        val NOTIFY_ON_SUCCESS = booleanPreferencesKey("backup_notify_success")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val LAST_BACKUP_ERROR = stringPreferencesKey("last_backup_error")
    }

    // Implementations...
}
```

---

## Consolidated context from the original proposal

The content below came from the family's historical README. It is a reference for reassessment, not an approved architecture.

### Historical view — Automatic Backup (former Phase 4)


## Objective

Implement a **daily automatic backup** of data to Google Drive (appDataFolder), scheduled for 2:00 a.m., as a **free** feature for all signed-in users.

## Scope

- ✅ Daily automatic backup (2:00 a.m.) - free
- ✅ Enable/disable automatic backup
- ✅ Wi-Fi-only sync (configurable)
- ✅ Successful backup notification (optional)
- ✅ Automatic retention: 30-day rolling window
- ❌ Real-time sync between devices (Phase 5 - premium)
- ❌ Multi-device conflict resolution (Phase 5 - premium)


## Prerequisites

- Phase 3 complete (manual Google Drive backup)
- WorkManager configured
- Active Google sign-in (backup requires sign-in)


## User Stories

| ID | Feature | Priority |
|----|---------|------------|
| [US-301](../0305-automatic-backup/spec.md) | Automatic Backup | P0 |
| [US-302](../0306-backup-settings/spec.md) | Backup Settings | P0 |
| [US-303](../0307-backup-triggers/spec.md) | Backup Triggers | P1 |


## Architecture

### WorkManager for Daily Backup

```
┌─────────────────────────────────────────────────────────────┐
│                        WorkManager                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PeriodicWorkRequest (24h, scheduled for 2:00 a.m.)         │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │ AutoBackupWorker                                      │     │
│  │                                                       │     │
│  │ - Checks Google sign-in                              │     │
│  │ - Checks Wi-Fi (if configured)                       │     │
│  │ - Runs backup to Google Drive                        │     │
│  │ - Removes backups > 30 days (rolling window)         │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   Google Drive   │
                    │  (appDataFolder) │
                    └─────────────────┘
```

### Automatic Backup Flow

```
┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│  Data Change   │────▶│   Debounce     │────▶│   Check        │
│  (Room write)  │     │   (5 min)      │     │   Conditions   │
└────────────────┘     └────────────────┘     └────────────────┘
                                                      │
                             ┌────────────────────────┼────────┐
                             │                        │        │
                             ▼                        ▼        ▼
                       ┌──────────┐           ┌──────────┐  ┌─────┐
                       │ Premium? │           │ Wi-Fi?   │  │ ... │
                       └──────────┘           └──────────┘  └─────┘
                             │                        │
                             └────────────────────────┘
                                        │
                                        ▼
                              ┌─────────────────┐
                              │ Execute Backup  │
                              └─────────────────┘
```


## Backup Strategy

### Daily Backup (2:00 a.m.)
- Scheduled via WorkManager with PeriodicWorkRequest (24h)
- Fixed time: 2:00 a.m. (ideal time: user asleep, device charging, Wi-Fi enabled)
- Runs in the background via WorkManager
- Respects the Wi-Fi-only setting (default: enabled)
- Runs only if Google sign-in is active

### Retention
- 30-day rolling window for automatic backups
- Manual backups do not count toward this limit (max. 10, managed in Phase 3)
- Auto-cleanup when creating a new backup: removes automatic backups older than 30 days
- After account deletion: permanent purge within 30 days (LGPD)


## Global Acceptance Criteria

- [ ] Automatic backup works in the background
- [ ] Scheduling at 2:00 a.m. works correctly
- [ ] The Wi-Fi-only option is respected
- [ ] Battery optimization handling (Doze mode)
- [ ] Google sign-in is checked before backup
- [ ] 30-day retention works (auto-cleanup)
- [ ] Backup notifications (optional)
- [ ] Integration with the existing backup system


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
