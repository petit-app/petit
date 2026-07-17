# Plano: Sincronização em Tempo Real

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

### SyncEngine

```kotlin
interface SyncEngine {
    val syncState: StateFlow<SyncState>

    fun startSync()
    fun stopSync()
    suspend fun syncNow(): Result<SyncResult>
    suspend fun uploadPending(): Result<Int>
    suspend fun downloadAll(): Result<Int>
}

sealed class SyncState {
    object Disabled : SyncState()
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}

data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int
)
```

### SyncEngineImpl

```kotlin
class SyncEngineImpl(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val syncPreferences: SyncPreferencesRepository
) : SyncEngine {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Disabled)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun startSync() {
        val userId = authRepository.getCurrentUser()?.id ?: return

        _syncState.value = SyncState.Idle

        // Upload pendentes primeiro
        scope.launch {
            uploadPending()
        }

        // Iniciar Firestore snapshot listeners para cada coleção
        startPetsListener(userId)
        startWeightsListener(userId)
        startVaccinationsListener(userId)
        startDewormingsListener(userId)
    }

    private fun startPetsListener(userId: String) {
        val registration = firestore.collection("pets")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> handlePetUpsert(change.document)
                            DocumentChange.Type.REMOVED -> handlePetDelete(change.document.id)
                        }
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private suspend fun handlePetUpsert(document: DocumentSnapshot) {
        val remotePet = document.toPetEntity() ?: return
        val localPet = petDao.getPetById(remotePet.id)

        // Last-write-wins: atualizar se remoto for mais recente
        if (localPet == null || remotePet.updatedAt > localPet.updatedAt) {
            petDao.insertPet(remotePet.copy(syncStatus = "SYNCED"))
        }
    }

    private suspend fun handlePetDelete(petId: String) {
        val localPet = petDao.getPetById(petId)
        if (localPet != null && localPet.deletedAt == null) {
            petDao.softDeletePet(localPet.id)
        }
    }

    override suspend fun uploadPending(): Result<Int> {
        val userId = authRepository.getCurrentUser()?.id
            ?: return Result.failure(Exception("Not logged in"))

        var uploadedCount = 0

        // Upload pets pendentes
        petDao.getPendingSyncPets().collect { pets ->
            pets.forEach { pet ->
                try {
                    firestore.collection("pets")
                        .document(pet.id)
                        .set(pet.toFirestoreMap(), SetOptions.merge())
                        .await()

                    petDao.updateSyncStatus(pet.id, "SYNCED")
                    uploadedCount++
                } catch (e: Exception) {
                    // Manter como PENDING para tentar depois
                }
            }
        }

        // Similar para outras entidades...

        return Result.success(uploadedCount)
    }

    override fun stopSync() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        _syncState.value = SyncState.Disabled
    }
}
```

### Firestore Models

```kotlin
data class PetFirestoreModel(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val birthDate: Long? = null,
    val sex: String? = null,
    val microchipNumber: String? = null,
    val passportNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null
) {
    fun toEntity() = PetEntity(
        id = id,
        ownerId = userId,
        name = name,
        birthDate = birthDate,
        sex = sex,
        microchipNumber = microchipNumber,
        passportNumber = passportNumber,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = "SYNCED"
    )
}

fun PetEntity.toFirestoreMap() = mapOf(
    "id" to id,
    "userId" to (ownerId ?: ""),
    "name" to name,
    "birthDate" to birthDate,
    "sex" to sex,
    "microchipNumber" to microchipNumber,
    "passportNumber" to passportNumber,
    "notes" to notes,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "deletedAt" to deletedAt
)

fun DocumentSnapshot.toPetEntity(): PetEntity? {
    return try {
        val model = toObject(PetFirestoreModel::class.java) ?: return null
        model.toEntity()
    } catch (e: Exception) {
        null
    }
}
```

### DAO Updates

```kotlin
@Dao
interface PetDao {
    // Existing queries...

    @Query("SELECT * FROM pets WHERE syncStatus = 'PENDING_SYNC' AND deletedAt IS NULL")
    fun getPendingSyncPets(): Flow<List<PetEntity>>

    @Query("UPDATE pets SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
```

---

## Contexto agregado da proposta original

O conteúdo abaixo veio do README histórico da família. Ele é referência para reavaliação, não uma arquitetura aprovada.

### Visão histórica — Cloud Sync (antiga Fase N+2)


> **Status**: Em holding — poderá ser reavaliada se houver demanda validada por sync remoto em tempo real.

## Motivo do Holding

Cloud sync foi adiado porque:
1. A Fase 2 (Compartilhamento Familiar local) atende a demanda atual de compartilhamento
2. Requer Firebase Auth e infraestrutura cloud (custos operacionais)
3. Sync local via NSD na rede Wi-Fi de casa é suficiente para uso doméstico
4. A hipótese de oferecê-lo como recurso premium só poderá ser considerada após validação de demanda e sustentabilidade

## Specs Preservadas (ex-Fase 5)

- [US-N21: Sync em Tempo Real](../0401-realtime-cloud-sync/spec.md)
- [US-N22: Múltiplos Dispositivos](../0402-multi-device-sync/spec.md)
- [US-N23: Resolução de Conflitos Cloud](../0403-cloud-conflict-resolution/spec.md)
- [US-N24: Sync Offline-First](../0404-offline-cloud-sync/spec.md)
- [US-N25: Compartilhamento Família (cloud)](../0405-cloud-family-sharing/spec.md)


## Pré-requisitos

- Fase 2 completa (Firebase Auth)
- Fase 3/4 completas (Backup Google Drive funciona como fallback)
- Coleções configuradas no Firestore com Security Rules
- Usuário premium ativo


## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-401](../0401-realtime-cloud-sync/spec.md) | Sincronização em Tempo Real | P0 |
| [US-402](../0402-multi-device-sync/spec.md) | Múltiplos Dispositivos | P0 |
| [US-403](../0403-cloud-conflict-resolution/spec.md) | Resolução de Conflitos | P0 |
| [US-404](../0404-offline-cloud-sync/spec.md) | Sync Offline-First | P1 |
| [US-405](../0405-cloud-family-sharing/spec.md) | Compartilhamento Família | P2 |


## Arquitetura

### Modelo de Sync

```
┌─────────────────────────────────────────────────────────────┐
│                        Device A                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐   │
│  │    Room     │◀───▶│  SyncEngine │◀───▶│  Firestore  │   │
│  │  (verdade)  │     │             │     │  (Firebase) │   │
│  └─────────────┘     └─────────────┘     └─────────────┘   │
│                                                  │           │
└─────────────────────────────────────────────────────────────┘
                                                   │
                    ┌──────────────────────────────┴──────────┐
                    │     Firebase Firestore (com Security Rules) │
                    │  ┌─────────────────────────────────────┐ │
                    │  │ pets (userId, Security Rules)          │ │
                    │  │ weight_entries (userId, Security Rules)│ │
                    │  │ vaccination_entries (userId, Rules)    │ │
                    │  │ deworming_entries (userId, Rules)      │ │
                    │  └─────────────────────────────────────┘ │
                    └──────────────────────────────────────────┘
                                                   │
┌──────────────────────────────────────────────────┼───────────┐
│                        Device B                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐   │
│  │    Room     │◀───▶│  SyncEngine │◀───▶│  Firestore  │   │
│  │  (verdade)  │     │             │     │  (Firebase) │   │
│  └─────────────┘     └─────────────┘     └─────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Estrutura do Firestore (Firebase)

```
// Coleções no Firestore (cada uma com Security Rules)

// pets/{petId}
{
  "userId": "uid-do-usuario",
  "name": "Luna",
  "birthDate": 1700000000000,
  "sex": "F",
  "microchipNumber": null,
  "passportNumber": null,
  "photoUri": null,
  "notes": null,
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000,
  "deletedAt": null
}

// weight_entries/{entryId}, vaccination_entries/{entryId}, deworming_entries/{entryId}
// (estrutura similar com userId + petId + Security Rules)

// sync_metadata/{userId}
{
  "lastSyncAt": 1700000000000,
  "deviceCount": 1
}
```

### Fluxo de Sync

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Local Write   │────▶│   SyncEngine    │────▶│   Firestore    │
│                 │     │                 │     │                 │
│ 1. Write Room   │     │ 2. Mark PENDING │     │ 3. Upsert       │
│                 │     │    SYNC         │     │                 │
│                 │◀────│                 │◀────│ 4. Confirm      │
│                 │     │ 5. Mark SYNCED  │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Firestore     │────▶│   SyncEngine    │────▶│   Local Room    │
│   Realtime      │     │                 │     │                 │
│                 │     │ 1. Receive      │     │ 2. Compare      │
│                 │     │    change       │     │    updatedAt    │
│                 │     │                 │     │ 3. Update if    │
│                 │     │                 │     │    newer        │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```


## Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /pets/{petId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /weight_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /vaccination_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /deworming_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }

    // Compartilhamento família (futuro)
    match /families/{familyId} {
      allow read: if request.auth != null && request.auth.uid in resource.data.memberIds;
      allow write: if request.auth != null && request.auth.uid == resource.data.createdBy;
    }
    match /pets/{petId} {
      allow read, write: if request.auth != null && (
        request.auth.uid == resource.data.userId ||
        (resource.data.familyId != null &&
         request.auth.uid in get(/databases/$(database)/documents/families/$(resource.data.familyId)).data.memberIds)
      );
    }
  }
}
```


## Critérios de Aceite Globais

- [ ] Dados sincronizam em tempo real entre dispositivos
- [ ] Princípio offline-first mantido (Room é verdade)
- [ ] Conflitos resolvidos automaticamente (last-write-wins)
- [ ] Sync funciona em background
- [ ] Premium gate aplicado
- [ ] Sync não bloqueia UI
- [ ] Erros de sync tratados graciosamente
- [ ] Indicador visual de status de sync
- [ ] Firestore Security Rules garantem isolamento de dados por usuário


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
