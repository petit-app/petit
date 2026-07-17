# Plan: Real-Time Sync

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0201`
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

### Technical requirements

### SyncEngine

```kotlin
interface SyncEngine {
    val syncState: StateFlow<SyncState>

    fun startSync()
    fun stopSync()
    suspend fun syncNow(): Result<SyncResult>
    suspend fun uploadPending(): Result<Int>
    suspend fun downloadAll(): Result<Int>
}

sealed class SyncState {
    object Disabled : SyncState()
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}

data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int
)
```

### SyncEngineImpl

```kotlin
class SyncEngineImpl(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val syncPreferences: SyncPreferencesRepository
) : SyncEngine {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Disabled)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun startSync() {
        val userId = authRepository.getCurrentUser()?.id ?: return

        _syncState.value = SyncState.Idle

        // Upload pending items first
        scope.launch {
            uploadPending()
        }

        // Start Firestore snapshot listeners for each collection
        startPetsListener(userId)
        startWeightsListener(userId)
        startVaccinationsListener(userId)
        startDewormingsListener(userId)
    }

    private fun startPetsListener(userId: String) {
        val registration = firestore.collection("pets")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> handlePetUpsert(change.document)
                            DocumentChange.Type.REMOVED -> handlePetDelete(change.document.id)
                        }
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private suspend fun handlePetUpsert(document: DocumentSnapshot) {
        val remotePet = document.toPetEntity() ?: return
        val localPet = petDao.getPetById(remotePet.id)

        // Last-write-wins: update if the remote item is newer
        if (localPet == null || remotePet.updatedAt > localPet.updatedAt) {
            petDao.insertPet(remotePet.copy(syncStatus = "SYNCED"))
        }
    }

    private suspend fun handlePetDelete(petId: String) {
        val localPet = petDao.getPetById(petId)
        if (localPet != null && localPet.deletedAt == null) {
            petDao.softDeletePet(localPet.id)
        }
    }

    override suspend fun uploadPending(): Result<Int> {
        val userId = authRepository.getCurrentUser()?.id
            ?: return Result.failure(Exception("Not logged in"))

        var uploadedCount = 0

        // Upload pending pets
        petDao.getPendingSyncPets().collect { pets ->
            pets.forEach { pet ->
                try {
                    firestore.collection("pets")
                        .document(pet.id)
                        .set(pet.toFirestoreMap(), SetOptions.merge())
                        .await()

                    petDao.updateSyncStatus(pet.id, "SYNCED")
                    uploadedCount++
                } catch (e: Exception) {
                    // Keep as PENDING to retry later
                }
            }
        }

        // Similar handling for other entities...

        return Result.success(uploadedCount)
    }

    override fun stopSync() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        _syncState.value = SyncState.Disabled
    }
}
```

### Firestore Models

```kotlin
data class PetFirestoreModel(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val birthDate: Long? = null,
    val sex: String? = null,
    val microchipNumber: String? = null,
    val passportNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null
) {
    fun toEntity() = PetEntity(
        id = id,
        ownerId = userId,
        name = name,
        birthDate = birthDate,
        sex = sex,
        microchipNumber = microchipNumber,
        passportNumber = passportNumber,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = "SYNCED"
    )
}

fun PetEntity.toFirestoreMap() = mapOf(
    "id" to id,
    "userId" to (ownerId ?: ""),
    "name" to name,
    "birthDate" to birthDate,
    "sex" to sex,
    "microchipNumber" to microchipNumber,
    "passportNumber" to passportNumber,
    "notes" to notes,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "deletedAt" to deletedAt
)

fun DocumentSnapshot.toPetEntity(): PetEntity? {
    return try {
        val model = toObject(PetFirestoreModel::class.java) ?: return null
        model.toEntity()
    } catch (e: Exception) {
        null
    }
}
```

### DAO Updates

```kotlin
@Dao
interface PetDao {
    // Existing queries...

    @Query("SELECT * FROM pets WHERE syncStatus = 'PENDING_SYNC' AND deletedAt IS NULL")
    fun getPendingSyncPets(): Flow<List<PetEntity>>

    @Query("UPDATE pets SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
```

---

## Additional context from the original proposal

The content below came from the family’s historical README. It is a reference for reevaluation, not an approved architecture.

### Historical overview — Cloud Sync


> **Status**: On Hold — may be reevaluated if there is validated demand for real-time remote sync.

## Reason for being On Hold

Cloud sync was deferred because:
1. Local family sharing meets the current sharing demand
2. It requires Firebase Auth and cloud infrastructure (operating costs)
3. Local sync via NSD on the home Wi-Fi network is sufficient for household use
4. The hypothesis of offering it as a premium feature can only be considered after validating demand and sustainability

## Related specs

- [US-N21: Real-Time Sync](../0401-realtime-cloud-sync/spec.md)
- [US-N22: Multiple Devices](../0402-multi-device-sync/spec.md)
- [US-N23: Cloud Conflict Resolution](../0403-cloud-conflict-resolution/spec.md)
- [US-N24: Offline-First Sync](../0404-offline-cloud-sync/spec.md)
- [US-N25: Family Sharing (cloud)](../0405-cloud-family-sharing/spec.md)


## Prerequisites

- Google Login implemented
- Google Drive Backup implemented as a fallback
- Collections configured in Firestore with Security Rules
- Active premium user


## User Stories

| ID | Feature | Priority |
|----|---------|------------|
| [US-401](../0401-realtime-cloud-sync/spec.md) | Real-Time Sync | P0 |
| [US-402](../0402-multi-device-sync/spec.md) | Multiple Devices | P0 |
| [US-403](../0403-cloud-conflict-resolution/spec.md) | Conflict Resolution | P0 |
| [US-404](../0404-offline-cloud-sync/spec.md) | Offline-First Sync | P1 |
| [US-405](../0405-cloud-family-sharing/spec.md) | Family Sharing | P2 |


## Architecture

### Sync model

```
┌─────────────────────────────────────────────────────────────┐
│                        Device A                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐   │
│  │    Room     │◀───▶│  SyncEngine │◀───▶│  Firestore  │   │
│  │   (truth)   │     │             │     │  (Firebase) │   │
│  └─────────────┘     └─────────────┘     └─────────────┘   │
│                                                  │           │
└─────────────────────────────────────────────────────────────┘
                                                   │
                    ┌──────────────────────────────┴──────────┐
                    │    Firebase Firestore (with Security Rules) │
                    │  ┌─────────────────────────────────────┐ │
                    │  │ pets (userId, Security Rules)          │ │
                    │  │ weight_entries (userId, Security Rules)│ │
                    │  │ vaccination_entries (userId, Rules)    │ │
                    │  │ deworming_entries (userId, Rules)      │ │
                    │  └─────────────────────────────────────┘ │
                    └──────────────────────────────────────────┘
                                                   │
┌──────────────────────────────────────────────────┼───────────┐
│                        Device B                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐   │
│  │    Room     │◀───▶│  SyncEngine │◀───▶│  Firestore  │   │
│  │   (truth)   │     │             │     │  (Firebase) │   │
│  └─────────────┘     └─────────────┘     └─────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Firestore structure (Firebase)

```
// Firestore collections (each with Security Rules)

// pets/{petId}
{
  "userId": "user-uid",
  "name": "Luna",
  "birthDate": 1700000000000,
  "sex": "F",
  "microchipNumber": null,
  "passportNumber": null,
  "photoUri": null,
  "notes": null,
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000,
  "deletedAt": null
}

// weight_entries/{entryId}, vaccination_entries/{entryId}, deworming_entries/{entryId}
// (similar structure with userId + petId + Security Rules)

// sync_metadata/{userId}
{
  "lastSyncAt": 1700000000000,
  "deviceCount": 1
}
```

### Sync flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Local Write   │────▶│   SyncEngine    │────▶│   Firestore    │
│                 │     │                 │     │                 │
│ 1. Write Room   │     │ 2. Mark PENDING │     │ 3. Upsert       │
│                 │     │    SYNC         │     │                 │
│                 │◀────│                 │◀────│ 4. Confirm      │
│                 │     │ 5. Mark SYNCED  │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Firestore     │────▶│   SyncEngine    │────▶│   Local Room    │
│   Realtime      │     │                 │     │                 │
│                 │     │ 1. Receive      │     │ 2. Compare      │
│                 │     │    change       │     │    updatedAt    │
│                 │     │                 │     │ 3. Update if    │
│                 │     │                 │     │    newer        │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```


## Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /pets/{petId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /weight_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /vaccination_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /deworming_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }

    // Family sharing (future)
    match /families/{familyId} {
      allow read: if request.auth != null && request.auth.uid in resource.data.memberIds;
      allow write: if request.auth != null && request.auth.uid == resource.data.createdBy;
    }
    match /pets/{petId} {
      allow read, write: if request.auth != null && (
        request.auth.uid == resource.data.userId ||
        (resource.data.familyId != null &&
         request.auth.uid in get(/databases/$(database)/documents/families/$(resource.data.familyId)).data.memberIds)
      );
    }
  }
}
```


## Global acceptance criteria

- [ ] Data syncs between devices in real time
- [ ] Offline-first principle is maintained (Room is the source of truth)
- [ ] Conflicts are resolved automatically (last-write-wins)
- [ ] Sync works in the background
- [ ] Petit Cloud gate is enforced
- [ ] Sync does not block the UI
- [ ] Sync errors are handled gracefully
- [ ] Visual sync status indicator
- [ ] Firestore Security Rules ensure per-user data isolation


## Risks and validations

- Dependency on external services, authentication, quotas, and contractual changes.
- Privacy and lifecycle of personal and pet health data.
- Database migrations and compatibility with data created offline or by older versions.
- Concurrency, idempotency, conflicts, and recovery after interruptions.
- Accessibility and clarity of error, waiting, and destructive confirmation states.

## Planned verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When a build is run: `./gradlew assembleDebug` followed by `./gradlew installDebug`
