# Plano: Backup Manual

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0201`
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

                // Upload para appDataFolder do Google Drive
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
        // Verificar login (se não logado, retorna erro que dispara fluxo de login)
        val currentUser = authRepository.getCurrentUser()
            ?: return Result.failure(LoginRequiredException("Login necessário para backup"))

        // Verificar conexão
        if (!connectivityManager.isConnected()) {
            return Result.failure(NoConnectionException("Sem conexão de internet"))
        }

        // Exportar dados
        val exportBundle = exportDataUseCase.exportAll()

        // Enviar para Google Drive
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
                        successMessage = "Backup realizado com sucesso!"
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

## Contexto agregado da proposta original

O conteúdo abaixo veio do README histórico da família. Ele é referência para reavaliação, não uma arquitetura aprovada.

### Visão histórica — Backup Google Drive (antiga Fase N+1)


> **Status**: Em holding — poderá ser reavaliada se houver demanda validada por backup na nuvem.

## Motivo do Holding

Backup no Google Drive foi adiado porque:
1. Export/Import JSON já atende como backup manual
2. A demanda imediata é compartilhamento local entre dispositivos da casa
3. Requer Firebase Auth (também em holding)
4. Poderá ser reavaliado se houver demanda validada por backup automático na nuvem

## Specs Preservadas

### Backup Manual (ex-Fase 3)
- [US-N11: Backup Manual](../0301-manual-backup/spec.md)
- [US-N12: Restaurar Backup](../0302-restore-backup/spec.md)
- [US-N13: Gerenciar Backups](../0303-manage-backups/spec.md)

### Backup Automático (ex-Fase 4)
- [README original do auto-backup](../0305-automatic-backup/plan.md)
- [US-N14a: Backup Automático](../0305-automatic-backup/spec.md)
- [US-N14b: Configurações de Backup](../0306-backup-settings/spec.md)
- [US-N14c: Triggers de Backup](../0307-backup-triggers/spec.md)

### Transferência Device-to-Device (referência histórica)
- [US-204 original](../0304-device-transfer/spec.md) — Esta proposta serviu como referência para o compartilhamento familiar, mas permanece não implementada neste formato


## Pré-requisitos

- Fase 2 completa (Firebase Auth)
- Google Cloud Console com Drive API habilitada
- OAuth configurado para Drive API (scope: `https://www.googleapis.com/auth/drive.appdata`)


## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-201](../0301-manual-backup/spec.md) | Backup Manual | P0 |
| [US-202](../0302-restore-backup/spec.md) | Restaurar Backup | P0 |
| [US-203](../0303-manage-backups/spec.md) | Gerenciar Backups | P1 |
| [US-204](../0304-device-transfer/spec.md) | Transferência Device-to-Device | P1 |


## Arquitetura

### Google Drive API — appDataFolder

Backups são salvos no **appDataFolder** do Google Drive:
- Pasta especial oculta do usuário (não aparece no Drive UI)
- Acessível apenas pelo app que criou os dados
- Automaticamente isolada por conta Google
- Sem consumir quota de armazenamento do usuário na maioria dos casos

### Fluxo de Backup

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

### Fluxo de Restore

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Google   │────▶│ ExportBundle│────▶│    Room     │
│    Drive    │     │    JSON     │     │  Database   │
└─────────────┘     └─────────────┘     └─────────────┘
```


## Configuração Google Drive API

### 1. Google Cloud Console

1. Habilitar Google Drive API no projeto
2. Configurar OAuth consent screen
3. Adicionar scope: `https://www.googleapis.com/auth/drive.appdata`
4. Baixar `google-services.json` (se ainda não tiver)

### 2. Dependências

```kotlin
dependencies {
    // Google Drive API
    implementation("com.google.android.gms:play-services-drive:VERSION")
    implementation("com.google.api-client:google-api-client-android:VERSION")
    implementation("com.google.apis:google-api-services-drive:VERSION")
}
```

### 3. Permissões no Manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />
```


## Estrutura de Arquivos no Google Drive

```
appDataFolder/
└── {userId}/
    ├── petit_backup_2026-03-18T10-30-00Z.json    (mais recente)
    ├── petit_backup_2026-03-15T14-20-00Z.json
    ├── petit_backup_2026-03-10T09-15-00Z.json
    └── metadata.json                           (índice de backups)
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


## Política de Retenção de Backups

| Tipo | Retenção | Regra |
|------|----------|-------|
| Backups manuais (Fase 3) | Até o usuário deletar (máx 10) | Usuário controla; ao atingir 10, o mais antigo é removido automaticamente |
| Backups automáticos (Fase 4) | Últimos 30 dias (rolling window) | Cleanup automático mantém custo previsível |
| Cancelamento de premium | 90 dias após expiração | Grace period para re-assinar sem perder dados |
| Exclusão de conta | 30 dias, depois purge permanente | Atende LGPD (direito ao esquecimento) com margem para recuperação |

### LGPD (Lei 13.709/2018)

- **Princípio da necessidade**: guardar apenas pelo tempo necessário à finalidade
- **Direito à eliminação**: o usuário pode pedir exclusão a qualquer momento
- Prazos de retenção devem constar nos Termos de Uso e Política de Privacidade


## Critérios de Aceite Globais

- [ ] Usuário premium pode fazer backup manual
- [ ] Usuário premium pode restaurar de backup
- [ ] Lista de backups mostra data e tamanho
- [ ] Pode deletar backups antigos
- [ ] Funciona apenas com conexão de internet
- [ ] Feedback claro durante operações (progress)
- [ ] Tratamento de erros de rede/quota
- [ ] RLS garante isolamento por usuário
- [ ] Máximo de 10 backups manuais por usuário (auto-cleanup do mais antigo)
- [ ] Backups mantidos por 90 dias após cancelamento de premium
- [ ] Backups purgados em até 30 dias após exclusão de conta


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
