# Plano: Compartilhamento Familiar na Nuvem

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0201`, `0401`
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

### FamilyRepository

```kotlin
interface FamilyRepository {
    val currentFamily: StateFlow<Family?>

    suspend fun createFamily(name: String): Result<Family>
    suspend fun joinFamily(inviteCode: String): Result<Family>
    suspend fun leaveFamily(): Result<Unit>
    suspend fun generateInviteCode(): Result<String>
    suspend fun removeMember(userId: String): Result<Unit>
    suspend fun deleteFamily(): Result<Unit>
    fun getFamilyMembers(): Flow<List<FamilyMember>>
}

data class Family(
    val id: String,
    val name: String,
    val inviteCode: String?,
    val memberCount: Int,
    val isAdmin: Boolean
)

data class FamilyMember(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val role: MemberRole,
    val joinedAt: Long
)

enum class MemberRole {
    ADMIN, MEMBER
}
```

### Firestore Security Rules para Família

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Família: membros podem ler, admin pode escrever
    match /families/{familyId} {
      allow read: if request.auth != null &&
        request.auth.uid in resource.data.memberIds;
      allow write: if request.auth != null &&
        request.auth.uid == resource.data.createdBy;

      match /members/{memberId} {
        allow read: if request.auth != null &&
          request.auth.uid in get(/databases/$(database)/documents/families/$(familyId)).data.memberIds;
        allow write: if request.auth != null &&
          request.auth.uid == get(/databases/$(database)/documents/families/$(familyId)).data.createdBy;
      }
    }

    // Membros podem ler/escrever pets compartilhados
    match /pets/{petId} {
      allow read, write: if request.auth != null && (
        request.auth.uid == resource.data.userId ||
        (resource.data.familyId != null &&
         request.auth.uid in get(/databases/$(database)/documents/families/$(resource.data.familyId)).data.memberIds)
      );
    }

    // Convites: qualquer autenticado pode ler para entrar
    match /invites/{code} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

### Sync com Suporte a Família

```kotlin
class FamilySyncEngine(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val familyRepository: FamilyRepository,
    private val petDao: PetDao
) {
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    fun startSync() {
        val userId = authRepository.getCurrentUser()?.id ?: return

        // Sync dados pessoais
        startPersonalSync(userId)

        // Sync dados da família (se houver)
        familyRepository.currentFamily.value?.let { family ->
            startFamilySync(family.id)
        }
    }

    private fun startFamilySync(familyId: String) {
        // Firestore snapshot listener para pets compartilhados
        val registration = firestore.collection("pets")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // Processar mudanças dos pets da família
            }
        listenerRegistrations.add(registration)
    }
}
```

### Pet com FamilyId

```kotlin
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,
    val familyId: String? = null,  // Se compartilhado com família
    val name: String,
    // ...
)

// Query para listar pets (pessoais + família)
@Query("""
    SELECT * FROM pets
    WHERE (ownerId = :userId OR familyId = :familyId)
    AND deletedAt IS NULL
    ORDER BY name
""")
fun getPetsForUserAndFamily(userId: String, familyId: String?): Flow<List<PetEntity>>
```

---

### Invite Flow

```kotlin
class InviteManager(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun generateInviteCode(familyId: String): String {
        val code = "PETIT-" + UUID.randomUUID().toString().take(6).uppercase()

        val invite = mapOf(
            "familyId" to familyId,
            "expiresAt" to (System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)),
            "createdBy" to firebaseAuth.currentUser?.uid
        )

        firestore.collection("invites").document(code).set(invite).await()

        return code
    }

    suspend fun validateAndJoin(code: String): Result<String> {
        val inviteDoc = firestore.collection("invites")
            .document(code).get().await()

        if (!inviteDoc.exists()) {
            return Result.failure(Exception("Código inválido"))
        }

        val expiresAt = inviteDoc.getLong("expiresAt") ?: 0
        if (System.currentTimeMillis() > expiresAt) {
            return Result.failure(Exception("Código expirado"))
        }

        val familyId = inviteDoc.getString("familyId")
            ?: return Result.failure(Exception("Família não encontrada"))

        // Adicionar membro à família
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(Exception("Não autenticado"))

        val displayName = firebaseAuth.currentUser?.displayName

        firestore.collection("families").document(familyId)
            .collection("members").document(userId)
            .set(mapOf(
                "userId" to userId,
                "role" to "member",
                "joinedAt" to System.currentTimeMillis(),
                "displayName" to displayName
            )).await()

        return Result.success(familyId)
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
