# Plan: Premium Gate

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0201`
- Revalidate demand, privacy, costs, provider terms, and the availability model.

## Proposed sequence

1. Revalidate the spec scenarios against the current product and update obsolete decisions.
2. Create contract tests and domain rules for the first vertical slice.
3. Implement the minimum integration behind repository abstractions, keeping Room as the local source.
4. Deliver UI states and error recovery for the same slice.
5. Repeat the cycle for each task, including migration and compatibility where necessary.
6. Run focused tests and the relevant Android suites before updating the status.

## Historical technical notes

The class names, APIs, dependencies, and code snippets below came from the original proposal and must be reconciled with the current code and versions before use.

### Technical Requirements

### PremiumStatus

```kotlin
enum class PremiumTier {
    FREE,
    PREMIUM_MONTHLY,
    PREMIUM_YEARLY
}

data class PremiumStatus(
    val tier: PremiumTier,
    val expiresAt: Long?,
    val isActive: Boolean
) {
    companion object {
        val FREE = PremiumStatus(PremiumTier.FREE, null, false)
    }
}
```

### PremiumRepository

```kotlin
interface PremiumRepository {
    val premiumStatus: StateFlow<PremiumStatus>

    suspend fun checkPremiumStatus(): PremiumStatus
    fun isPremium(): Boolean
}

class PremiumRepositoryImpl(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore  // or Billing client
) : PremiumRepository {

    private val _premiumStatus = MutableStateFlow(PremiumStatus.FREE)
    override val premiumStatus: StateFlow<PremiumStatus> = _premiumStatus.asStateFlow()

    override suspend fun checkPremiumStatus(): PremiumStatus {
        val userId = authRepository.getCurrentUser()?.id ?: return PremiumStatus.FREE

        // Check in Firebase Firestore or through Google Play Billing
        val snapshot = firestore.collection("users")
            .document(userId).get().await()
        val userProfile = snapshot.toObject(UserProfile::class.java)

        val premiumUntil = userProfile?.premiumUntil ?: 0

        val status = when {
            premiumUntil > System.currentTimeMillis() -> PremiumStatus(
                tier = PremiumTier.PREMIUM_MONTHLY,  // or check which plan
                expiresAt = premiumUntil,
                isActive = true
            )
            else -> PremiumStatus.FREE
        }

        _premiumStatus.value = status
        return status
    }

    override fun isPremium(): Boolean = _premiumStatus.value.isActive
}
```

### Feature Gate Composable

```kotlin
@Composable
fun PremiumFeatureGate(
    feature: PremiumFeature,
    premiumStatus: PremiumStatus,
    onShowPremiumInfo: () -> Unit,
    content: @Composable () -> Unit
) {
    if (premiumStatus.isActive) {
        content()
    } else {
        Box(
            modifier = Modifier
                .clickable { onShowPremiumInfo() }
                .alpha(0.6f)
        ) {
            content()
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Premium",
                modifier = Modifier.align(Alignment.TopEnd),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

enum class PremiumFeature {
    CLOUD_BACKUP,
    CLOUD_SYNC,
    FAMILY_SHARING,
    PDF_EXPORT
}
```

### Check before action

```kotlin
class BackupUseCase(
    private val premiumRepository: PremiumRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        if (!premiumRepository.isPremium()) {
            return Result.failure(PremiumRequiredException("Backup requires a premium plan"))
        }

        // Perform backup...
        return Result.success(Unit)
    }
}

class PremiumRequiredException(message: String) : Exception(message)
```

---


## Risks and validations

- Dependency on external services, authentication, quotas, and contractual changes.
- Privacy and the lifecycle of personal and pet health data.
- Database migrations and compatibility with data created offline or in older versions.
- Concurrency, idempotency, conflicts, and recovery after interruptions.
- Accessibility and clarity of error, waiting, and destructive-confirmation states.

## Planned verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When there is a build: `./gradlew assembleDebug` followed by `./gradlew installDebug`
