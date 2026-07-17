# Plan: Manage Backups

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

### List Backups

```kotlin
// In BackupStorageRepositoryImpl
override suspend fun listBackups(): Result<List<BackupInfo>> {
    return withContext(Dispatchers.IO) {
        try {
            val storage = FirebaseStorage.getInstance()
            val listResult = storage.reference.child("backups/$userId").listAll().await()

            val backups = listResult.items
                .filter { it.name.startsWith("petit_backup_") && it.name.endsWith(".json") }
                .map { ref ->
                    val metadata = ref.metadata.await()
                    BackupInfo(
                        fileName = ref.name,
                        path = ref.path,
                        createdAt = Instant.ofEpochMilli(metadata.creationTimeMillis),
                        sizeBytes = metadata.sizeBytes,
                        petCount = 0,  // Load from metadata
                        appVersion = metadata.getCustomMetadata("appVersion") ?: "unknown"
                    )
                }

            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Delete Backup

```kotlin
override suspend fun deleteBackup(fileId: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            driveService.files().delete(fileId).execute()

            // Update metadata
            removeFromMetadata(fileId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

suspend fun deleteMultipleBackups(fileIds: List<String>): Result<Int> {
    var deletedCount = 0

    fileIds.forEach { fileId ->
        deleteBackup(fileId).onSuccess { deletedCount++ }
    }

    return Result.success(deletedCount)
}
```

### Automatic Cleanup of Old Backups

```kotlin
class BackupCleanupUseCase(
    private val backupStorageRepository: BackupStorageRepository
) {
    companion object {
        const val MAX_BACKUPS = 10
    }

    suspend fun cleanupOldBackups(): Result<Int> {
        val backups = backupStorageRepository.listBackups()
            .getOrElse { return Result.failure(it) }

        if (backups.size <= MAX_BACKUPS) {
            return Result.success(0)
        }

        // Sort by date (oldest first for deletion)
        val toDelete = backups
            .sortedBy { it.createdAt }
            .take(backups.size - MAX_BACKUPS)

        var deletedCount = 0
        toDelete.forEach { backup ->
            backupStorageRepository.deleteBackup(backup.fileId)
                .onSuccess { deletedCount++ }
        }

        return Result.success(deletedCount)
    }
}
```

### ViewModel

```kotlin
class ManageBackupsViewModel(
    private val backupStorageRepository: BackupStorageRepository,
    private val restoreBackupUseCase: RestoreBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageBackupsUiState())
    val uiState: StateFlow<ManageBackupsUiState> = _uiState.asStateFlow()

    fun loadBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            backupStorageRepository.listBackups()
                .onSuccess { backups ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        backups = backups,
                        totalSize = backups.sumOf { it.sizeBytes }
                    )}
                }
        }
    }

    fun toggleSelection(fileId: String) {
        _uiState.update { state ->
            val newSelection = if (fileId in state.selectedIds) {
                state.selectedIds - fileId
            } else {
                state.selectedIds + fileId
            }
            state.copy(
                selectedIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = _uiState.value.selectedIds.toList()
            _uiState.update { it.copy(isDeleting = true) }

            backupStorageRepository.deleteMultipleBackups(toDelete)
                .onSuccess { count ->
                    _uiState.update { it.copy(
                        isDeleting = false,
                        selectedIds = emptySet(),
                        isSelectionMode = false
                    )}
                    loadBackups()  // Reload list
                }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(
            selectedIds = emptySet(),
            isSelectionMode = false
        )}
    }
}

data class ManageBackupsUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val backups: List<BackupInfo> = emptyList(),
    val totalSize: Long = 0,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)
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
