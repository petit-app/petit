# Plano: Sincronização Offline-First

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0401`
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

### Network Listener

```kotlin
class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Estado inicial
        val isCurrentlyOnline = connectivityManager.activeNetwork != null
        trySend(isCurrentlyOnline)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
}
```

### Auto-Sync on Reconnect

```kotlin
class SyncOnReconnectManager(
    private val networkMonitor: NetworkMonitor,
    private val syncEngine: SyncEngine,
    private val premiumRepository: PremiumRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .filter { isOnline -> isOnline }  // Apenas quando volta online
                .collect {
                    if (premiumRepository.isPremium()) {
                        syncEngine.uploadPending()
                    }
                }
        }
    }
}
```

### Contagem de Pendentes

```kotlin
class PendingSyncCounter(
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao
) {
    data class PendingCount(
        val pets: Int = 0,
        val weights: Int = 0,
        val vaccinations: Int = 0,
        val dewormings: Int = 0
    ) {
        val total: Int get() = pets + weights + vaccinations + dewormings
        val isEmpty: Boolean get() = total == 0
    }

    fun getPendingCount(): Flow<PendingCount> = combine(
        petDao.countPendingSync(),
        weightDao.countPendingSync(),
        vaccinationDao.countPendingSync(),
        dewormingDao.countPendingSync()
    ) { pets, weights, vaccinations, dewormings ->
        PendingCount(pets, weights, vaccinations, dewormings)
    }
}

// DAOs
@Query("SELECT COUNT(*) FROM pets WHERE syncStatus = 'PENDING_SYNC' AND deletedAt IS NULL")
fun countPendingSync(): Flow<Int>
```

### Firestore Offline Handling

```kotlin
// Firestore tem persistência offline nativa (isPersistenceEnabled = true).
// Room continua sendo a fonte de verdade para queries locais.
// Dados pendentes ficam em Room com syncStatus = PENDING_SYNC
// e são enviados via WorkManager quando a conexão volta.
```

### WorkManager para Sync Pendente

```kotlin
class UploadPendingWorker(
    context: Context,
    params: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val premiumRepository: PremiumRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!premiumRepository.isPremium()) {
            return Result.success()
        }

        return syncEngine.uploadPending()
            .map { Result.success() }
            .getOrElse { Result.retry() }
    }

    companion object {
        fun scheduleIfPending(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<UploadPendingWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniqueWork(
                "upload_pending",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
```

### Marcar Como Pending ao Salvar

```kotlin
class PetRepositoryImpl(
    private val petDao: PetDao,
    private val premiumRepository: PremiumRepository,
    private val syncEngine: SyncEngine
) : PetRepository {

    override suspend fun insertPet(pet: PetEntity) {
        val syncStatus = if (premiumRepository.isPremium()) {
            "PENDING_SYNC"
        } else {
            "LOCAL_ONLY"
        }

        petDao.insertPet(pet.copy(syncStatus = syncStatus))

        // Tentar sync imediato se online
        if (syncStatus == "PENDING_SYNC") {
            syncEngine.uploadPending()
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
