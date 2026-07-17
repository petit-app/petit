# Plano: Gatilhos de Backup

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0305`, `0306`
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

### Arquitetura

### Fluxo de Trigger

```
┌─────────────────────────────────────────────────────────────┐
│                           App                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PetRepository        WeightRepository       VaccinationRepo│
│  ┌───────────┐        ┌───────────┐         ┌───────────┐  │
│  │  insert() │        │  insert() │         │  insert() │  │
│  └─────┬─────┘        └─────┬─────┘         └─────┬─────┘  │
│        │                    │                     │         │
│        └────────────────────┼─────────────────────┘         │
│                             │                               │
│                             ▼                               │
│                   ┌─────────────────┐                       │
│                   │ BackupTrigger   │                       │
│                   │   Manager       │                       │
│                   └────────┬────────┘                       │
│                            │                                │
│                            ▼                                │
│                   ┌─────────────────┐                       │
│                   │   Debouncer     │                       │
│                   │   (5 min)       │                       │
│                   └────────┬────────┘                       │
│                            │                                │
│                            ▼                                │
│                   ┌─────────────────┐                       │
│                   │  WorkManager    │                       │
│                   │ OneTimeRequest  │                       │
│                   └─────────────────┘                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### Requisitos Técnicos

### BackupTriggerManager

```kotlin
class BackupTriggerManager(
    private val workManager: WorkManager,
    private val backupPreferences: BackupPreferencesRepository,
    private val premiumRepository: PremiumRepository
) {
    companion object {
        const val DEBOUNCE_DELAY_MINUTES = 5L
        const val WORK_TAG = "backup_on_change"
    }

    fun onDataChanged() {
        // Verificar se backup automático está ativado e é premium
        if (!backupPreferences.isAutoBackupEnabled() || !premiumRepository.isPremium()) {
            return
        }

        // Cancelar trabalho pendente anterior (debounce)
        workManager.cancelAllWorkByTag(WORK_TAG)

        // Agendar novo trabalho com delay
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (backupPreferences.isWifiOnly())
                    NetworkType.UNMETERED
                else
                    NetworkType.CONNECTED
            )
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BackupOnChangeWorker>()
            .setInitialDelay(DEBOUNCE_DELAY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueue(workRequest)
    }

    fun cancelPendingBackup() {
        workManager.cancelAllWorkByTag(WORK_TAG)
    }

    fun hasPendingBackup(): Boolean {
        val workInfos = workManager.getWorkInfosByTag(WORK_TAG).get()
        return workInfos.any { !it.state.isFinished }
    }
}
```

### BackupOnChangeWorker

```kotlin
class BackupOnChangeWorker(
    context: Context,
    params: WorkerParameters,
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Verificar premium (pode ter expirado enquanto esperava)
        if (!premiumRepository.isPremium()) {
            return Result.success()  // Não é falha, apenas skip
        }

        // Executar backup
        return backupUseCase()
            .map {
                backupPreferences.setLastBackupTimestamp(System.currentTimeMillis())
                backupPreferences.setLastBackupError(null)
                Result.success()
            }
            .getOrElse { error ->
                // Não fazer retry para backup por alteração
                // O próximo periódico tentará novamente
                backupPreferences.setLastBackupError(error.message)
                Result.success()  // Marcar como sucesso para não ficar tentando
            }
    }
}
```

### Integração nos Repositories

```kotlin
class PetRepositoryImpl(
    private val petDao: PetDao,
    private val backupTriggerManager: BackupTriggerManager
) : PetRepository {

    override suspend fun insertPet(pet: PetEntity) {
        petDao.insertPet(pet)
        backupTriggerManager.onDataChanged()
    }

    override suspend fun updatePet(pet: PetEntity) {
        petDao.updatePet(pet)
        backupTriggerManager.onDataChanged()
    }

    override suspend fun deletePet(id: String) {
        petDao.softDeletePet(id)
        backupTriggerManager.onDataChanged()
    }
}
```

### Usando Callback/Listener Pattern

```kotlin
// Alternativa: usar padrão de eventos
interface DataChangeListener {
    fun onDataChanged(entityType: EntityType)
}

enum class EntityType {
    PET, WEIGHT, VACCINATION, DEWORMING
}

class DataChangePublisher {
    private val listeners = mutableListOf<DataChangeListener>()

    fun addListener(listener: DataChangeListener) {
        listeners.add(listener)
    }

    fun notifyDataChanged(entityType: EntityType) {
        listeners.forEach { it.onDataChanged(entityType) }
    }
}

// BackupTriggerManager implementa DataChangeListener
class BackupTriggerManager(...) : DataChangeListener {
    override fun onDataChanged(entityType: EntityType) {
        // Trigger backup com debounce
        onDataChanged()
    }
}
```

### Evitar Conflito com Backup Periódico

```kotlin
class AutoBackupWorker(...) : CoroutineWorker(...) {

    override suspend fun doWork(): Result {
        // Cancelar backup por alteração pendente (evitar duplicação)
        WorkManager.getInstance(applicationContext)
            .cancelAllWorkByTag(BackupTriggerManager.WORK_TAG)

        // Continuar com backup normal...
        return backupUseCase()
            .map { Result.success() }
            .getOrElse { Result.retry() }
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
