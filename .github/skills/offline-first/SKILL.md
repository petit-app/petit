---
name: offline-first
description: "Offline-first architecture patterns for Petit Android app. Use when: implementing data persistence, designing Room database operations, working with sync status tracking, building cache strategies, implementing WorkManager background tasks, designing data flows that must work without network, handling export/import, planning future Firebase/Firestore cloud sync. Covers Room as source of truth, soft delete, SyncStatus, WorkManager, DataStore."
---

# Offline-First — Petit Android

## When to Use

- Implementing any data read/write operation
- Designing data flow for new features
- Working with Room database
- Adding background processing (WorkManager)
- Planning future Firebase/Firestore cloud sync integration
- Implementing export/import functionality
- Handling data conflicts

## Core Principle

> **The app MUST be fully functional without any network connection.**
> Room database is the single source of truth. The UI always reads from Room.
> Network operations (future Firebase/Firestore sync) are asynchronous, background, and never block the UI.

## Architecture

```
┌──────────────┐
│    UI Layer   │  ← Always reads from Room via Flow
│  (Composable) │
└──────┬───────┘
       │ collectAsStateWithLifecycle()
┌──────┴───────┐
│  ViewModel   │  ← Exposes StateFlow<UiState>
└──────┬───────┘
       │ Repository.getAll(): Flow<List<T>>
┌──────┴───────┐
│  Repository  │  ← Maps Entity ↔ Domain Model
└──────┬───────┘
       │ DAO.getAll(): Flow<List<Entity>>
┌──────┴───────┐
│   Room DAO   │  ← Single source of truth
└──────┬───────┘
       │
┌──────┴───────┐
│  Room DB     │  ← SQLite on device
└──────────────┘

  ┌────────────────────┐
  │  WorkManager       │  ← Background sync via Firebase (future)
  │  (sync/backup)     │  ← Reminders (current)
  └────────────────────┘
```

## Data Flow Rules

### Rule 1: UI Reads ONLY from Room

```kotlin
// GOOD — ViewModel reads from local repository
@HiltViewModel
class CatListViewModel @Inject constructor(
    private val catRepository: CatRepository,
) : ViewModel() {

    val cats: StateFlow<List<Cat>> = catRepository.getAllCats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// BAD — ViewModel fetches from network
class CatListViewModel(private val api: CatApi) : ViewModel() {
    fun loadCats() {
        viewModelScope.launch {
            _cats.value = api.getCats() // BLOCKS ON NETWORK!
        }
    }
}
```

### Rule 2: Writes Go to Room First

```kotlin
// GOOD — save locally, mark as pending sync
suspend fun insertCat(cat: Cat) {
    val entity = cat.toEntity().copy(
        syncStatus = "LOCAL_ONLY",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )
    catDao.insertCat(entity)
    // Future: trigger sync via WorkManager
}

// BAD — save to server first, then local
suspend fun insertCat(cat: Cat) {
    api.createCat(cat) // What if no network?
    catDao.insertCat(cat.toEntity())
}
```

### Rule 3: Flow-Based Reactivity

```kotlin
// GOOD — Room emits Flow automatically when data changes
@Query("SELECT * FROM cats WHERE deletedAt IS NULL ORDER BY name")
fun getAllCats(): Flow<List<CatEntity>>
// When a cat is inserted/updated/deleted, observers are notified automatically

// BAD — polling or manual refresh
fun getAllCats(): List<CatEntity> // One-shot, no reactivity
```

## Soft Delete Pattern

**Never hard-delete data.** All deletions are logical (soft delete) to support:

1. Undo operations
2. Future sync reconciliation
3. Conflict resolution

### Implementation

```kotlin
// Entity — always has deletedAt field
@Entity(tableName = "cats")
data class CatEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    // ... other fields
    val deletedAt: Long? = null,  // null = active, timestamp = deleted
    val syncStatus: String = "LOCAL_ONLY"
)

// DAO — soft delete implementation
@Dao
interface CatDao {
    // All queries MUST filter out soft-deleted records
    @Query("SELECT * FROM cats WHERE deletedAt IS NULL ORDER BY name")
    fun getAllCats(): Flow<List<CatEntity>>

    // Soft delete: set timestamp, don't remove row
    @Query("UPDATE cats SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteCat(id: String, timestamp: Long = System.currentTimeMillis())

    // Hard delete only for purge operations (e.g., "delete all data" in settings)
    @Query("DELETE FROM cats")
    suspend fun deleteAllCats()
}

// Repository — hides soft delete logic from ViewModel
override suspend fun softDeleteCat(id: String) {
    catDao.softDeleteCat(id)
}
```

### Rules:

- **Every** `SELECT` query includes `WHERE deletedAt IS NULL`
- **Soft delete** sets `deletedAt = System.currentTimeMillis()`
- **Hard delete** (`DELETE FROM`) only in "delete all data" feature
- **Cascade** delete: `ForeignKey.CASCADE` handles child records automatically

## Sync Status Tracking

Every entity has a `syncStatus` field for future Firebase/Firestore cloud sync:

```kotlin
enum class SyncStatus {
    LOCAL_ONLY,    // Never synced (created offline, or sync not enabled)
    PENDING_SYNC,  // Modified locally, needs to be pushed to server
    SYNCED,        // In sync with server
    CONFLICT       // Local and server versions differ
}
```

### State Transitions

```
             ┌────────────────┐
 Create  →   │  LOCAL_ONLY    │
             └───────┬────────┘
                     │ (sync enabled + push success)
             ┌───────┴────────┐
             │    SYNCED      │
             └───────┬────────┘
                     │ (local edit)
             ┌───────┴────────┐
             │ PENDING_SYNC   │
             └───────┬────────┘
                     │ (push success)  │ (server also changed)
                     ↓                  ↓
                  SYNCED            CONFLICT
```

### Rules:

- New records start as `LOCAL_ONLY`
- Local edits change status to `PENDING_SYNC`
- Successful sync changes status to `SYNCED`
- Conflict detection uses `updatedAt` timestamps (last-write-wins)
- Currently: All records stay `LOCAL_ONLY`

## WorkManager for Background Tasks

### Current Usage: Reminders

```kotlin
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString("reminderId") ?: return Result.failure()
        val reminder = reminderRepository.getReminderById(reminderId) ?: return Result.failure()

        showNotification(reminder)

        if (reminder.repeatInterval != null) {
            scheduleNextOccurrence(reminder)
        }

        return Result.success()
    }
}
```

### Future Usage: Sync

```kotlin
// Pattern for future Firebase sync worker
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // 1. Get all PENDING_SYNC records
        // 2. Push to Firestore
        // 3. Update syncStatus to SYNCED
        // 4. Pull Firestore changes
        // 5. Merge with local data
        return Result.success()
    }
}

// Schedule periodic sync
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
    repeatInterval = 15, repeatIntervalTimeUnit = TimeUnit.MINUTES,
)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()
```

### Rules:

- **WorkManager** for all background tasks (not AlarmManager, not raw threads)
- **Hilt integration** via `@HiltWorker` / `@AssistedInject`
- **Network constraints** on sync workers (but NOT on local reminders)
- **Retry policy** for failed sync attempts
- **Unique work** to prevent duplicate workers

## Export/Import (Offline Backup)

### Export Pattern

```kotlin
data class ExportBundle(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val cats: List<Cat>,
    val weightEntries: List<WeightEntry>,
    val vaccinationEntries: List<VaccinationEntry>,
    val dewormingEntries: List<DewormingEntry>,
    val reminders: List<Reminder>,
)
```

### Rules:

- Export includes **all active records** (deletedAt IS NULL)
- Import validates all data before writing to Room
- Import uses a **transaction** to ensure atomicity
- Version field for future schema evolution
- No network required — pure file I/O

## DataStore Preferences

```kotlin
// GOOD — DataStore for user settings (offline, no network)
val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
val THEME_MODE = stringPreferencesKey("theme_mode")
```

### Rules:

- **DataStore** for app preferences and settings
- **Room** for domain data (cats, health records, reminders)
- **Never** store domain data in DataStore
- **Never** require network to read preferences

## Repository Layer Discipline

The Repository layer is the **only gateway** between ViewModels and the data layer. This is critical for offline-first because:

1. **Abstraction**: Future Firebase sync logic lives in Repository, not ViewModel
2. **Testability**: Mock the Repository interface to test ViewModels offline
3. **Consistency**: Soft delete, syncStatus, and timestamp management happen in one place

```kotlin
// GOOD — ViewModel depends on Repository interface
@HiltViewModel
class CatDetailViewModel @Inject constructor(
    private val catRepository: CatRepository,
    private val weightEntryRepository: WeightEntryRepository,
) : ViewModel()

// BAD — ViewModel bypasses Repository, directly accesses DAO
@HiltViewModel
class CatDetailViewModel @Inject constructor(
    private val catDao: CatDao,           // ⚠️ skips Repository layer
    private val weightEntryDao: WeightEntryDao,  // ⚠️ no place for sync logic
) : ViewModel()
```

If a Repository method you need doesn't exist, **add it to the Repository** — don't inject a DAO as a shortcut.

## Offline-First Checklist

Before merging any feature, verify:

- [ ] All data reads come from Room (not network)
- [ ] UI works with no network connection
- [ ] New entities have `deletedAt`, `syncStatus`, `createdAt`, `updatedAt`
- [ ] All SELECT queries filter `WHERE deletedAt IS NULL`
- [ ] Writes save to Room immediately (no network dependency)
- [ ] `Flow<T>` used for observable data
- [ ] Background tasks use WorkManager (not AlarmManager)
- [ ] Export/import works fully offline
- [ ] No hardcoded assumptions about network availability
- [ ] ViewModels inject Repositories only — never DAOs directly
- [ ] External side effects (WorkManager, scheduler) have error handling separate from DB writes
