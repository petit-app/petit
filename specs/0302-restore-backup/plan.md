# Plan: Restore Backup

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0301`
- Revalidate demand, privacy, costs, provider terms, and the availability model.

## Proposed sequence

1. Revalidate the spec scenarios against the current product and update obsolete decisions.
2. Create contract tests and domain rules for the first vertical slice.
3. Implement the minimal integration behind repository abstractions, keeping Room as the local source.
4. Deliver UI states and error recovery for the same slice.
5. Repeat the cycle for each task, including migration and compatibility when necessary.
6. Run focused tests and the relevant Android suites before updating the status.

## Historical technical notes

The class names, APIs, dependencies, and code snippets below must be reviewed against the current code and versions before use.

### Technical Requirements

### RestoreBackupUseCase

```kotlin
class RestoreBackupUseCase(
    private val premiumRepository: PremiumRepository,
    private val backupStorageRepository: BackupStorageRepository,
    private val importDataUseCase: ImportDataUseCase,
    private val database: PetitDatabase
) {
    sealed class RestoreMode {
        object Replace : RestoreMode()
        object Merge : RestoreMode()
    }

    suspend operator fun invoke(
        fileId: String,
        mode: RestoreMode
    ): Result<RestoreResult> {
        // Check premium status
        if (!premiumRepository.isPremium()) {
            return Result.failure(PremiumRequiredException("Restore requires a premium plan"))
        }

        // Download backup
        val exportBundle = backupStorageRepository.downloadBackup(fileName)
            .getOrElse { return Result.failure(it) }

        // Apply restore
        return when (mode) {
            is RestoreMode.Replace -> replaceAllData(exportBundle)
            is RestoreMode.Merge -> mergeData(exportBundle)
        }
    }

    private suspend fun replaceAllData(bundle: ExportBundle): Result<RestoreResult> {
        return database.withTransaction {
            // Clear all local data
            database.petDao().deleteAll()
            database.weightEntryDao().deleteAll()
            database.vaccinationDao().deleteAll()
            database.dewormingDao().deleteAll()
            database.reminderDao().deleteAll()

            // Import backup data
            importDataUseCase.import(bundle, ConflictResolution.REPLACE)

            Result.success(RestoreResult(
                petsRestored = bundle.pets.size,
                totalEntries = bundle.weightEntries.size +
                              bundle.vaccinationEntries.size +
                              bundle.dewormingEntries.size
            ))
        }
    }

    private suspend fun mergeData(bundle: ExportBundle): Result<RestoreResult> {
        return importDataUseCase.import(bundle, ConflictResolution.MERGE)
            .map {
                RestoreResult(
                    petsRestored = bundle.pets.size,
                    totalEntries = bundle.weightEntries.size +
                                  bundle.vaccinationEntries.size +
                                  bundle.dewormingEntries.size,
                    merged = true
                )
            }
    }
}

data class RestoreResult(
    val petsRestored: Int,
    val totalEntries: Int,
    val merged: Boolean = false
)
```

### ViewModel

```kotlin
class RestoreViewModel(
    private val backupStorageRepository: BackupStorageRepository,
    private val restoreBackupUseCase: RestoreBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestoreUiState())
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()

    init {
        loadBackups()
    }

    private fun loadBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            backupStorageRepository.listBackups()
                .onSuccess { backups ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        backups = backups,
                        isEmpty = backups.isEmpty()
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )}
                }
        }
    }

    fun restoreBackup(fileId: String, mode: RestoreBackupUseCase.RestoreMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }

            restoreBackupUseCase(fileId, mode)
                .onSuccess { result ->
                    _uiState.update { it.copy(
                        isRestoring = false,
                        restoreSuccess = true,
                        successMessage = "Restored ${result.petsRestored} pets and ${result.totalEntries} records"
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isRestoring = false,
                        errorMessage = error.message
                    )}
                }
        }
    }
}

data class RestoreUiState(
    val isLoading: Boolean = true,
    val isRestoring: Boolean = false,
    val backups: List<BackupInfo> = emptyList(),
    val isEmpty: Boolean = false,
    val restoreSuccess: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
```

### Backup Download

```kotlin
// In BackupStorageRepositoryImpl
override suspend fun downloadBackup(fileName: String): Result<ExportBundle> {
    return withContext(Dispatchers.IO) {
        try {
            val storage = FirebaseStorage.getInstance()
            val ref = storage.reference.child("backups/$userId/$fileName")
            val MAX_SIZE = 10 * 1024 * 1024L
            val bytes = ref.getBytes(MAX_SIZE).await()

            val json = bytes.decodeToString()
            val exportBundle = Json.decodeFromString<ExportBundle>(json)

            Result.success(exportBundle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---


## Risks and validations

- Dependency on external services, authentication, quota, and contractual changes.
- Privacy and lifecycle of personal and pet health data.
- Database migrations and compatibility with data created offline or in older versions.
- Concurrency, idempotency, conflicts, and recovery after interruptions.
- Accessibility and clarity of error, waiting, and destructive confirmation states.

## Planned verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When a build is run: `./gradlew assembleDebug` followed by `./gradlew installDebug`
