# Plano: Sincronização entre Múltiplos Dispositivos

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

### Download Inicial

```kotlin
class InitialSyncUseCase(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val database: PetitDatabase
) {
    sealed class Progress {
        object Starting : Progress()
        data class Downloading(val entity: String, val current: Int, val total: Int) : Progress()
        object Complete : Progress()
        data class Error(val message: String) : Progress()
    }

    suspend fun execute(): Flow<Progress> = flow {
        emit(Progress.Starting)

        val userId = authRepository.getCurrentUser()?.id
            ?: throw Exception("Not logged in")

        database.withTransaction {
            // Download pets
            val petsSnapshot = firestore.collection("pets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("deletedAt", null)
                .get().await()
            val pets = petsSnapshot.documents.mapNotNull { it.toPetEntity() }

            emit(Progress.Downloading("pets", pets.size, pets.size))
            pets.forEach { petDao.insertPet(it) }

            // Download weights
            val weightsSnapshot = firestore.collection("weight_entries")
                .whereEqualTo("userId", userId)
                .get().await()
            val weights = weightsSnapshot.documents.mapNotNull { it.toWeightEntity() }

            emit(Progress.Downloading("pesagens", weights.size, weights.size))
            weights.forEach { weightDao.upsertWeightEntry(it) }

            // Similar para vaccinations, dewormings...
        }

        emit(Progress.Complete)
    }.catch { e ->
        emit(Progress.Error(e.message ?: "Erro no sync"))
    }
}
```

### Tracking de Dispositivos

```kotlin
// No Firestore: coleção devices
// devices (user_id, device_id, device_name, last_seen_at, app_version)
data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val lastSeenAt: Long = 0,
    val appVersion: String = ""
)

class DeviceTracker(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val context: Context
) {
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun registerDevice() {
        val userId = authRepository.getCurrentUser()?.id ?: return

        val deviceInfo = mapOf(
            "userId" to userId,
            "deviceId" to deviceId,
            "deviceName" to Build.MODEL,
            "lastSeenAt" to System.currentTimeMillis(),
            "appVersion" to BuildConfig.VERSION_NAME
        )

        firestore.collection("devices")
            .document("${userId}_${deviceId}")
            .set(deviceInfo, SetOptions.merge())
            .await()
    }

    suspend fun updateLastSeen() {
        val userId = authRepository.getCurrentUser()?.id ?: return

        firestore.collection("devices")
            .document("${userId}_${deviceId}")
            .update("lastSeenAt", System.currentTimeMillis())
            .await()
    }

    fun getConnectedDevices(): Flow<List<DeviceInfo>> {
        val userId = authRepository.getCurrentUser()?.id ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection("devices")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    val devices = snapshot?.documents?.mapNotNull {
                        it.toObject(DeviceInfo::class.java)
                    } ?: emptyList()
                    trySend(devices)
                }
            awaitClose { registration.remove() }
        }
    }
}
```

### Heartbeat para Last Seen

```kotlin
class SyncHeartbeatWorker(
    context: Context,
    params: WorkerParameters,
    private val deviceTracker: DeviceTracker
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        deviceTracker.updateLastSeen()
        return Result.success()
    }

    companion object {
        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncHeartbeatWorker>(
                15, TimeUnit.MINUTES
            ).build()

            workManager.enqueueUniquePeriodicWork(
                "sync_heartbeat",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
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
