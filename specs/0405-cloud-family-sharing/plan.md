# Plan: Cloud Family Sharing

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0201`, `0401`
- Revalidate demand, privacy, costs, provider terms, and the availability model.

## Proposed sequence

1. Revalidate the spec scenarios against the current product and update obsolete decisions.
2. Create contract tests and domain rules for the first vertical slice.
3. Implement the minimum integration behind repository abstractions, keeping Room as the local source of truth.
4. Deliver UI states and error recovery for the same slice.
5. Repeat the cycle for each task, including migration and compatibility work when necessary.
6. Run the focused tests and relevant Android suites before updating the status.

## Historical technical notes

The class names, APIs, dependencies, and code snippets below came from the original proposal and must be reconciled with the current code and versions before use.

### Technical Requirements

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

### Firestore Security Rules for Families

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Family: members can read, admin can write
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

    // Members can read/write shared pets
    match /pets/{petId} {
      allow read, write: if request.auth != null && (
        request.auth.uid == resource.data.userId ||
        (resource.data.familyId != null &&
         request.auth.uid in get(/databases/$(database)/documents/families/$(resource.data.familyId)).data.memberIds)
      );
    }

    // Invitations: any authenticated user can read them to join
    match /invites/{code} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

### Sync with Family Support

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

        // Sync personal data
        startPersonalSync(userId)

        // Sync family data (if any)
        familyRepository.currentFamily.value?.let { family ->
            startFamilySync(family.id)
        }
    }

    private fun startFamilySync(familyId: String) {
        // Firestore snapshot listener for shared pets
        val registration = firestore.collection("pets")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // Process changes to the family's pets
            }
        listenerRegistrations.add(registration)
    }
}
```

### Pet with FamilyId

```kotlin
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,
    val familyId: String? = null,  // Set when shared with a family
    val name: String,
    // ...
)

// Query to list pets (personal + family)
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
            return Result.failure(Exception("Invalid code"))
        }

        val expiresAt = inviteDoc.getLong("expiresAt") ?: 0
        if (System.currentTimeMillis() > expiresAt) {
            return Result.failure(Exception("Expired code"))
        }

        val familyId = inviteDoc.getString("familyId")
            ?: return Result.failure(Exception("Family not found"))

        // Add member to the family
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(Exception("Not authenticated"))

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

## Risks and validations

- Dependency on external services, authentication, quotas, and contractual changes.
- Privacy and lifecycle of personal and pet health data.
- Database migrations and compatibility with data created offline or by older versions.
- Concurrency, idempotency, conflicts, and recovery after interruptions.
- Accessibility and clarity of error, waiting, and destructive confirmation states.

## Planned verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When a build is run: `./gradlew assembleDebug` followed by `./gradlew installDebug`
