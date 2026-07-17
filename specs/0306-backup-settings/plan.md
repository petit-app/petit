# Plan: Backup Settings

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0305`
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

### Technical Requirements

### ViewModel

```kotlin
class BackupSettingsViewModel(
    private val backupPreferences: BackupPreferencesRepository,
    private val backupScheduler: BackupScheduler,
    private val createBackupUseCase: CreateBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupSettingsUiState())
    val uiState: StateFlow<BackupSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update { it.copy(
            isAutoBackupEnabled = backupPreferences.isAutoBackupEnabled(),
            intervalHours = backupPreferences.getBackupIntervalHours(),
            isWifiOnly = backupPreferences.isWifiOnly(),
            shouldNotifyOnSuccess = backupPreferences.shouldNotifyOnSuccess(),
            lastBackupTimestamp = backupPreferences.getLastBackupTimestamp(),
            lastBackupError = backupPreferences.getLastBackupError(),
            nextBackupTimestamp = backupScheduler.getNextBackupTime()
        )}
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        backupPreferences.setAutoBackupEnabled(enabled)

        if (enabled) {
            backupScheduler.scheduleAutoBackup()
        } else {
            backupScheduler.cancelAutoBackup()
        }

        loadSettings()
    }

    fun setIntervalHours(hours: Int) {
        backupPreferences.setBackupIntervalHours(hours)

        if (backupPreferences.isAutoBackupEnabled()) {
            backupScheduler.scheduleAutoBackup()  // Reschedule with the new frequency
        }

        loadSettings()
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        backupPreferences.setWifiOnly(wifiOnly)

        if (backupPreferences.isAutoBackupEnabled()) {
            backupScheduler.scheduleAutoBackup()  // Reschedule with the new constraint
        }

        loadSettings()
    }

    fun setNotifyOnSuccess(notify: Boolean) {
        backupPreferences.setNotifyOnSuccess(notify)
        loadSettings()
    }

    fun backupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }

            createBackupUseCase()
                .onSuccess {
                    _uiState.update { it.copy(
                        isBackingUp = false,
                        successMessage = "Backup completed!"
                    )}

                    // Reschedule to reset the timer
                    if (backupPreferences.isAutoBackupEnabled()) {
                        backupScheduler.scheduleAutoBackup()
                    }

                    loadSettings()
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

data class BackupSettingsUiState(
    val isAutoBackupEnabled: Boolean = false,
    val intervalHours: Int = 24,
    val isWifiOnly: Boolean = true,
    val shouldNotifyOnSuccess: Boolean = false,
    val lastBackupTimestamp: Long? = null,
    val lastBackupError: String? = null,
    val nextBackupTimestamp: Long? = null,
    val isBackingUp: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
```

### Composable

```kotlin
@Composable
fun BackupSettingsScreen(
    viewModel: BackupSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Main toggle
        SwitchPreference(
            title = "Automatic backup",
            subtitle = "Saves your data automatically",
            checked = uiState.isAutoBackupEnabled,
            onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
        )

        if (uiState.isAutoBackupEnabled) {
            // Status
            BackupStatusCard(
                lastBackup = uiState.lastBackupTimestamp,
                nextBackup = uiState.nextBackupTimestamp,
                lastError = uiState.lastBackupError
            )

            // Frequency
            ListPreference(
                title = "Frequency",
                value = uiState.intervalHours,
                options = listOf(
                    6 to "Every 6 hours",
                    24 to "Every 24 hours",
                    168 to "Once a week"
                ),
                onValueChange = { viewModel.setIntervalHours(it) }
            )

            // Wi-Fi only
            SwitchPreference(
                title = "Wi-Fi only",
                subtitle = "Saves mobile data",
                checked = uiState.isWifiOnly,
                onCheckedChange = { viewModel.setWifiOnly(it) }
            )

            // Notification
            SwitchPreference(
                title = "Notify after backup",
                subtitle = "Shows a success notification",
                checked = uiState.shouldNotifyOnSuccess,
                onCheckedChange = { viewModel.setNotifyOnSuccess(it) }
            )
        }

        // Back up now button
        Button(
            onClick = { viewModel.backupNow() },
            enabled = !uiState.isBackingUp
        ) {
            if (uiState.isBackingUp) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Back up now")
            }
        }
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
