# Plano: Restaurar Backup

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0301`
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
        // Verificar premium
        if (!premiumRepository.isPremium()) {
            return Result.failure(PremiumRequiredException("Restauração requer plano premium"))
        }

        // Baixar backup
        val exportBundle = backupStorageRepository.downloadBackup(fileName)
            .getOrElse { return Result.failure(it) }

        // Aplicar restauração
        return when (mode) {
            is RestoreMode.Replace -> replaceAllData(exportBundle)
            is RestoreMode.Merge -> mergeData(exportBundle)
        }
    }

    private suspend fun replaceAllData(bundle: ExportBundle): Result<RestoreResult> {
        return database.withTransaction {
            // Limpar todos os dados locais
            database.petDao().deleteAll()
            database.weightEntryDao().deleteAll()
            database.vaccinationDao().deleteAll()
            database.dewormingDao().deleteAll()
            database.reminderDao().deleteAll()

            // Importar dados do backup
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
                        successMessage = "Restaurados ${result.petsRestored} pets e ${result.totalEntries} registros"
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

### Download do Backup

```kotlin
// Em BackupStorageRepositoryImpl
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
