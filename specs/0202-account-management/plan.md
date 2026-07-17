# Plan: Account Management

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

### AccountViewModel

```kotlin
class AccountViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val accountInfo: StateFlow<AccountInfo?> = authRepository.authState
        .map { state ->
            when (state) {
                is AuthState.Authenticated -> AccountInfo(
                    displayName = state.displayName,
                    email = state.email,
                    photoUrl = state.photoUrl,
                    isPremium = state.isPremium,
                    memberSince = userPreferencesRepository.getMemberSince(),
                    lastLoginAt = userPreferencesRepository.getLastLoginAt()
                )
                else -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            // 1. Mark cloud data to be purged in 30 days (LGPD)
            authRepository.deleteAccount()
            // 3. Clear account-related preferences
            userPreferencesRepository.clearAccountData()
        }
    }
}

data class AccountInfo(
    val displayName: String?,
    val email: String,
    val photoUrl: String?,
    val isPremium: Boolean,
    val memberSince: Long?,
    val lastLoginAt: Long?
)
```

### Delete Account in Firebase

```kotlin
suspend fun deleteAccount(): Result<Unit> {
    return try {
        // 1. Delete account from Firebase Auth
        //    Cloud Function handles cascading deletion of the user's data
        firebaseAuth.currentUser?.delete()?.await()
            ?: return Result.failure(Exception("Not logged in"))

        // Note: effective deletion and purging of cloud data
        // is performed by a Firebase Cloud Function after 30 days

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
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
