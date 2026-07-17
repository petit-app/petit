# Plan: Data Ownership

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0201`
- Revalidate demand, privacy, costs, provider terms, and the availability model.

## Proposed sequence

1. Revalidate the spec scenarios against the current product and update obsolete decisions.
2. Create contract tests and domain rules for the first vertical slice.
3. Implement the minimum integration behind repository abstractions, keeping Room as the local source.
4. Deliver UI states and error recovery for the same slice.
5. Repeat the cycle for each task, including migration and compatibility where necessary.
6. Run focused tests and the relevant Android suites before updating the status.

## Historical technical notes

The class names, APIs, dependencies, and code snippets below came from the original proposal and must be reconciled with the current code and versions before use.

### Data Model

### Adding ownerId to entities

```kotlin
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,  // New field
    val name: String,
    // ... other fields
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,  // New field
    val petId: String,
    // ... other fields
)

// Similar for VaccinationEntry, DewormingEntry, Reminder
```

---

### Technical Requirements

### Room Migration

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add ownerId column to all tables
        database.execSQL("ALTER TABLE pets ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE vaccination_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE deworming_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE reminders ADD COLUMN ownerId TEXT DEFAULT NULL")
    }
}
```

### DataOwnershipManager

```kotlin
class DataOwnershipManager(
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val reminderDao: ReminderDao,
    private val database: PetitDatabase
) {
    /**
     * Links all ownerless local data to the current userId
     * Called after the first login
     */
    suspend fun claimOrphanedData(userId: String) {
        database.withTransaction {
            petDao.claimOrphanedPets(userId)
            weightDao.claimOrphanedEntries(userId)
            vaccinationDao.claimOrphanedEntries(userId)
            dewormingDao.claimOrphanedEntries(userId)
            reminderDao.claimOrphanedEntries(userId)
        }
    }
}
```

### Updated DAOs

```kotlin
@Dao
interface PetDao {
    // Existing queries...

    @Query("UPDATE pets SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun claimOrphanedPets(userId: String)

    @Query("SELECT * FROM pets WHERE (ownerId = :userId OR ownerId IS NULL) AND deletedAt IS NULL ORDER BY name")
    fun getPetsForUser(userId: String?): Flow<List<PetEntity>>
}
```

### Integration into the Login Flow

```kotlin
class AuthRepositoryImpl(...) {

    override suspend fun signInWithGoogle(idToken: String): Result<UserInfo> {
        return try {
            // ... existing login code ...

            val user = firebaseAuth.currentUser!!

            // Check whether this is the first login
            val isFirstLogin = userPreferencesRepository.getFirstLoginDate() == null

            if (isFirstLogin) {
                // Link orphaned data to the new user
                dataOwnershipManager.claimOrphanedData(user.id)
                userPreferencesRepository.setFirstLoginDate(System.currentTimeMillis())
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Create entities with an owner

```kotlin
class CreatePetUseCase(
    private val petRepository: PetRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(pet: Pet): Result<Pet> {
        val ownerId = authRepository.getCurrentUser()?.id

        val petWithOwner = pet.copy(ownerId = ownerId)

        return petRepository.insertPet(petWithOwner)
    }
}
```

---

### Data Display

### Phase 2: Show all local data

```kotlin
// For now, show all local data regardless of owner
fun getAllPets(): Flow<List<PetEntity>> {
    return petDao.getAllPets()  // No owner filter
}
```

### Future phase (5): Filter by owner for sync

```kotlin
// When implementing sync, filter by owner
fun getPetsForSync(userId: String): Flow<List<PetEntity>> {
    return petDao.getPetsForUser(userId)
}
```

---


## Risks and validations

- Dependency on external services, authentication, quotas, and contractual changes.
- Privacy and the lifecycle of personal and pet health data.
- Database migrations and compatibility with data created offline or in older versions.
- Concurrency, idempotency, conflicts, and recovery after interruptions.
- Accessibility and clarity of error, waiting, and destructive-confirmation states.

## Planned verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When there is a build: `./gradlew assembleDebug` followed by `./gradlew installDebug`
