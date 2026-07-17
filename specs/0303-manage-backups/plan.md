# Plano: Gerenciar Backups

Spec: [spec.md](./spec.md)

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

### Listar Backups

```kotlin
// Em BackupStorageRepositoryImpl
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
                        petCount = 0,  // Carregar do metadata
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

### Deletar Backup

```kotlin
override suspend fun deleteBackup(fileId: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            driveService.files().delete(fileId).execute()

            // Atualizar metadata
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

### Auto-cleanup de Backups Antigos

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

        // Ordenar por data (mais antigo primeiro para deletar)
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
                    loadBackups()  // Recarregar lista
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
