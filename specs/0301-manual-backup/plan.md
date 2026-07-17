# Plan: Manual Backup

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0201`
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

### BackupStorageRepository

```kotlin
interface BackupStorageRepository {
    suspend fun createBackup(data: ExportBundle): Result<BackupInfo>
    suspend fun listBackups(): Result<List<BackupInfo>>
    suspend fun downloadBackup(fileName: String): Result<ExportBundle>
    suspend fun deleteBackup(fileName: String): Result<Unit>
    suspend fun getBackupMetadata(): Result<BackupMetadata?>
}

data class BackupInfo(
    val fileId: String,
    val fileName: String,
    val createdAt: Instant,
    val sizeBytes: Long,
    val petCount: Int,
    val appVersion: String
)

data class BackupMetadata(
    val backups: List<BackupInfo>,
    val lastBackupAt: Instant?
)
```

### GoogleDriveBackupRepository

```kotlin
class GoogleDriveBackupRepository(
    private val driveService: Drive,
    private val authRepository: AuthRepository
) : BackupStorageRepository {

    override suspend fun createBackup(data: ExportBundle): Result<BackupInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val json = Json.encodeToString(data)
                val timestamp = Instant.now().toString().replace(":", "-")
                val fileName = "petit_backup_$timestamp.json"

                // Upload to the Google Drive appDataFolder
                val fileMetadata = com.google.api.services.drive.model.File()
                    .setName(fileName)
                    .setParents(listOf("appDataFolder"))

                val mediaContent = ByteArrayContent("application/json", json.toByteArray())

                val file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, createdTime, size")
                    .execute()

                val backupInfo = BackupInfo(
                    fileId = file.id,
                    fileName = file.name,
                    createdAt = Instant.parse(file.createdTime.toString()),
                    sizeBytes = file.size,
                    petCount = data.pets.size,
                    appVersion = data.metadata.appVersion
                )

                Result.success(backupInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

### BackupUseCase

```kotlin
class CreateBackupUseCase(
    private val authRepository: AuthRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val googleDriveBackupRepository: GoogleDriveBackupRepository,
    private val connectivityManager: ConnectivityManager
) {
    suspend operator fun invoke(): Result<BackupInfo> {
        // Check sign-in status (if signed out, return an error that triggers the sign-in flow)
        val currentUser = authRepository.getCurrentUser()
            ?: return Result.failure(LoginRequiredException("Sign-in required for backup"))

        // Check connection
        if (!connectivityManager.isConnected()) {
            return Result.failure(NoConnectionException("No internet connection"))
        }

        // Export data
        val exportBundle = exportDataUseCase.exportAll()

        // Upload to Google Drive
        return googleDriveBackupRepository.createBackup(exportBundle)
    }
}
```

### ViewModel

```kotlin
class BackupViewModel(
    private val createBackupUseCase: CreateBackupUseCase,
    private val backupStorageRepository: BackupStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        loadBackupInfo()
    }

    private fun loadBackupInfo() {
        viewModelScope.launch {
            backupStorageRepository.getBackupMetadata()
                .onSuccess { metadata ->
                    _uiState.update { it.copy(
                        lastBackup = metadata?.backups?.firstOrNull(),
                        totalBackups = metadata?.backups?.size ?: 0,
                        isLoading = false
                    )}
                }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }

            createBackupUseCase()
                .onSuccess { backupInfo ->
                    _uiState.update { it.copy(
                        isBackingUp = false,
                        lastBackup = backupInfo,
                        successMessage = "Backup completed successfully!"
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isBackingUp = false,
                        errorMessage = error.message
                    )}
                }
        }
    }
}

data class BackupUiState(
    val isLoading: Boolean = true,
    val isBackingUp: Boolean = false,
    val lastBackup: BackupInfo? = null,
    val totalBackups: Int = 0,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
```

---

## Consolidated context from the original proposal

The content below came from the family's historical README. It is a reference for reevaluation, not an approved architecture.

### Historical overview — Google Drive Backup


> **Status**: On Hold — may be reevaluated if there is validated demand for cloud backup.

## Reason for the Hold

Google Drive backup was postponed because:
1. JSON Export/Import already serves as a manual backup
2. The immediate demand is local sharing among household devices
3. It requires Firebase Auth (also On Hold)
4. It may be reevaluated if there is validated demand for automatic cloud backup

## Preserved Specs

### Manual Backup
- [US-N11: Manual Backup](../0301-manual-backup/spec.md)
- [US-N12: Restore Backup](../0302-restore-backup/spec.md)
- [US-N13: Manage Backups](../0303-manage-backups/spec.md)

### Automatic Backup
- [Original automatic-backup README](../0305-automatic-backup/plan.md)
- [US-N14a: Automatic Backup](../0305-automatic-backup/spec.md)
- [US-N14b: Backup Settings](../0306-backup-settings/spec.md)
- [US-N14c: Backup Triggers](../0307-backup-triggers/spec.md)

### Device-to-Device Transfer (historical reference)
- [Original US-204](../0304-device-transfer/spec.md) — This proposal served as a reference for family sharing but remains unimplemented in this form


## Prerequisites

- Google Login implemented
- Google Cloud Console with the Drive API enabled
- OAuth configured for the Drive API (scope: `https://www.googleapis.com/auth/drive.appdata`)


## User Stories

| ID | Feature | Priority |
|----|---------|------------|
| [US-201](../0301-manual-backup/spec.md) | Manual Backup | P0 |
| [US-202](../0302-restore-backup/spec.md) | Restore Backup | P0 |
| [US-203](../0303-manage-backups/spec.md) | Manage Backups | P1 |
| [US-204](../0304-device-transfer/spec.md) | Device-to-Device Transfer | P1 |


## Architecture

### Google Drive API — appDataFolder

Backups are saved in the Google Drive **appDataFolder**:
- A special hidden user folder (not shown in the Drive UI)
- Accessible only to the app that created the data
- Automatically isolated by Google account
- Does not consume the user's storage quota in most cases

### Backup Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    Room     │────▶│ ExportBundle│────▶│   Google    │
│  Database   │     │    JSON     │     │    Drive     │
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
                                              ▼
                                        appDataFolder/
                                        └── petit_backup_2026-03-15.json
```

### Restore Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Google   │────▶│ ExportBundle│────▶│    Room     │
│    Drive    │     │    JSON     │     │  Database   │
└─────────────┘     └─────────────┘     └─────────────┘
```


## Google Drive API Configuration

### 1. Google Cloud Console

1. Enable the Google Drive API in the project
2. Configure the OAuth consent screen
3. Add the scope: `https://www.googleapis.com/auth/drive.appdata`
4. Download `google-services.json` (if not already available)

### 2. Dependencies

```kotlin
dependencies {
    // Google Drive API
    implementation("com.google.android.gms:play-services-drive:VERSION")
    implementation("com.google.api-client:google-api-client-android:VERSION")
    implementation("com.google.apis:google-api-services-drive:VERSION")
}
```

### 3. Manifest Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
```


## Google Drive File Structure

```
appDataFolder/
└── {userId}/
    ├── petit_backup_2026-03-18T10-30-00Z.json    (latest)
    ├── petit_backup_2026-03-15T14-20-00Z.json
    ├── petit_backup_2026-03-10T09-15-00Z.json
    └── metadata.json                           (backup index)
```

### Metadata File

```json
{
  "backups": [
    {
      "fileId": "abc123",
      "fileName": "petit_backup_2026-03-18T10:30:00Z.json",
      "createdAt": "2026-03-18T10:30:00Z",
      "sizeBytes": 15420,
      "petCount": 2,
      "appVersion": "1.0.0"
    }
  ],
  "lastBackupAt": "2026-03-18T10:30:00Z"
}
```


## Backup Retention Policy

| Type | Retention | Rule |
|------|----------|-------|
| Manual backups | Until the user deletes them (max. 10) | User-controlled; upon reaching 10, the oldest is removed automatically |
| Automatic backups | Last 30 days (rolling window) | Automatic cleanup keeps costs predictable |
| Premium cancellation | 90 days after expiration | Grace period to resubscribe without losing data |
| Account deletion | 30 days, then permanent purge | Complies with LGPD (right to erasure) while allowing a recovery window |

### LGPD (Law 13,709/2018)

- **Necessity principle**: retain data only for as long as needed for its purpose
- **Right to erasure**: the user can request deletion at any time
- Retention periods must be stated in the Terms of Use and Privacy Policy


## Global Acceptance Criteria

- [ ] A premium user can create a manual backup
- [ ] A premium user can restore from a backup
- [ ] The backup list shows date and size
- [ ] Old backups can be deleted
- [ ] Works only with an internet connection
- [ ] Clear feedback during operations (progress)
- [ ] Network/quota error handling
- [ ] RLS ensures per-user isolation
- [ ] Maximum of 10 manual backups per user (automatic cleanup of the oldest)
- [ ] Backups retained for 90 days after premium cancellation
- [ ] Backups purged within 30 days after account deletion


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
