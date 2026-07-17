# Plano: Vinculação de Dados

Spec: [spec.md](./spec.md)

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

### Modelo de Dados

### Adição de ownerId nas entidades

```kotlin
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,  // Novo campo
    val name: String,
    // ... outros campos
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,  // Novo campo
    val petId: String,
    // ... outros campos
)

// Similar para VaccinationEntry, DewormingEntry, Reminder
```

---

### Requisitos Técnicos

### Migration do Room

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Adicionar coluna ownerId em todas as tabelas
        database.execSQL("ALTER TABLE pets ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE vaccination_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE deworming_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE reminders ADD COLUMN ownerId TEXT DEFAULT NULL")
    }
}
```

### DataOwnershipManager

```kotlin
class DataOwnershipManager(
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val reminderDao: ReminderDao,
    private val database: PetitDatabase
) {
    /**
     * Vincula todos os dados locais sem owner ao userId atual
     * Chamado após primeiro login
     */
    suspend fun claimOrphanedData(userId: String) {
        database.withTransaction {
            petDao.claimOrphanedPets(userId)
            weightDao.claimOrphanedEntries(userId)
            vaccinationDao.claimOrphanedEntries(userId)
            dewormingDao.claimOrphanedEntries(userId)
            reminderDao.claimOrphanedEntries(userId)
        }
    }
}
```

### DAOs atualizados

```kotlin
@Dao
interface PetDao {
    // Queries existentes...

    @Query("UPDATE pets SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun claimOrphanedPets(userId: String)

    @Query("SELECT * FROM pets WHERE (ownerId = :userId OR ownerId IS NULL) AND deletedAt IS NULL ORDER BY name")
    fun getPetsForUser(userId: String?): Flow<List<PetEntity>>
}
```

### Integração no Login Flow

```kotlin
class AuthRepositoryImpl(...) {

    override suspend fun signInWithGoogle(idToken: String): Result<UserInfo> {
        return try {
            // ... código de login existente ...

            val user = firebaseAuth.currentUser!!

            // Verificar se é primeiro login
            val isFirstLogin = userPreferencesRepository.getFirstLoginDate() == null

            if (isFirstLogin) {
                // Vincular dados órfãos ao novo usuário
                dataOwnershipManager.claimOrphanedData(user.id)
                userPreferencesRepository.setFirstLoginDate(System.currentTimeMillis())
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Criar entidades com owner

```kotlin
class CreatePetUseCase(
    private val petRepository: PetRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(pet: Pet): Result<Pet> {
        val ownerId = authRepository.getCurrentUser()?.id

        val petWithOwner = pet.copy(ownerId = ownerId)

        return petRepository.insertPet(petWithOwner)
    }
}
```

---

### Visualização de Dados

### Fase 2: Mostrar todos os dados locais

```kotlin
// Por enquanto, mostrar todos os dados locais independente do owner
fun getAllPets(): Flow<List<PetEntity>> {
    return petDao.getAllPets()  // Sem filtro por owner
}
```

### Fase futura (5): Filtrar por owner para sync

```kotlin
// Quando implementar sync, filtrar por owner
fun getPetsForSync(userId: String): Flow<List<PetEntity>> {
    return petDao.getPetsForUser(userId)
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
