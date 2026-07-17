# Plano: Configurações de Backup

Spec: [spec.md](./spec.md)

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0305`
- Revalidar demanda, privacidade, custos, termos do provedor e modelo de disponibilidade.

## Sequenciamento proposto

1. Revalidar os cenários da spec com o produto atual e atualizar decisões obsoletas.
2. Criar testes de contrato e regras de domínio para a primeira fatia vertical.
3. Implementar a integração mínima atrás de abstrações de repositório, mantendo Room como fonte local.
4. Entregar estados de UI e recuperação de erros para a mesma fatia.
5. Repetir o ciclo por tarefa, incluindo migração e compatibilidade quando necessário.
6. Executar os testes focados e as suítes Android relevantes antes de atualizar o status.

## Notas técnicas históricas

Os nomes de classes, APIs, dependências e trechos de código abaixo vieram da proposta original e precisam ser reconciliados com o código e versões atuais antes de uso.

### Requisitos Técnicos

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
            backupScheduler.scheduleAutoBackup()  // Re-schedule com nova frequência
        }

        loadSettings()
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        backupPreferences.setWifiOnly(wifiOnly)

        if (backupPreferences.isAutoBackupEnabled()) {
            backupScheduler.scheduleAutoBackup()  // Re-schedule com nova constraint
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
                        successMessage = "Backup realizado!"
                    )}

                    // Re-schedule para resetar timer
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
        // Toggle principal
        SwitchPreference(
            title = "Backup automático",
            subtitle = "Salva seus dados automaticamente",
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

            // Frequência
            ListPreference(
                title = "Frequência",
                value = uiState.intervalHours,
                options = listOf(
                    6 to "A cada 6 horas",
                    24 to "A cada 24 horas",
                    168 to "Uma vez por semana"
                ),
                onValueChange = { viewModel.setIntervalHours(it) }
            )

            // Wi-Fi only
            SwitchPreference(
                title = "Apenas em Wi-Fi",
                subtitle = "Economiza dados móveis",
                checked = uiState.isWifiOnly,
                onCheckedChange = { viewModel.setWifiOnly(it) }
            )

            // Notificação
            SwitchPreference(
                title = "Notificar após backup",
                subtitle = "Mostra notificação de sucesso",
                checked = uiState.shouldNotifyOnSuccess,
                onCheckedChange = { viewModel.setNotifyOnSuccess(it) }
            )
        }

        // Botão backup agora
        Button(
            onClick = { viewModel.backupNow() },
            enabled = !uiState.isBackingUp
        ) {
            if (uiState.isBackingUp) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Fazer backup agora")
            }
        }
    }
}
```

---


## Riscos e validações

- Dependência de serviços externos, autenticação, quota e mudanças contratuais.
- Privacidade e ciclo de vida de dados pessoais e de saúde do pet.
- Migrações de banco e compatibilidade com dados criados offline ou em versões antigas.
- Concorrência, idempotência, conflitos e recuperação após interrupções.
- Acessibilidade e clareza dos estados de erro, espera e confirmação destrutiva.

## Verificação planejada

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- Quando houver build: `./gradlew assembleDebug` seguido de `./gradlew installDebug`
