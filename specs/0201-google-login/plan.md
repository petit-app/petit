# Plan: Google Login

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: No dependencies between specs.
- Revalidate demand, privacy, costs, provider terms, and the availability model.

## Proposed sequence

1. Revalidate the spec scenarios against the current product and update obsolete decisions.
2. Create contract tests and domain rules for the first vertical slice.
3. Implement the minimum integration behind repository abstractions, keeping Room as the local source.
4. Deliver UI states and error recovery for the same slice.
5. Repeat the cycle for each task, including migration and compatibility where necessary.
6. Run focused tests and the relevant Android suites before updating the status.

## Historical technical notes

The class names, APIs, dependencies, and code snippets below must be reviewed against the current code and versions before use.

### Technical Requirements

### AuthRepository

```kotlin
interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun signInWithGoogle(idToken: String): Result<UserInfo>
    suspend fun signOut()
    fun getCurrentUser(): UserInfo?
    fun isLoggedIn(): Boolean
}
```

### Implementation with Credential Manager + Firebase Auth

```kotlin
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val context: Context
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = if (auth.currentUser != null) {
                auth.currentUser!!.toAuthState()
            } else {
                AuthState.Anonymous
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserInfo> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("User not found after sign in"))
            Result.success(user.toUserInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoogleIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential as? CustomCredential
            ?: throw Exception("Invalid credential")

        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
        return googleIdToken.idToken
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Ignore clear error
        }
    }
}
```

### ViewModel

```kotlin
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState = authRepository.authState

    private val _loginResult = MutableSharedFlow<LoginResult>()
    val loginResult = _loginResult.asSharedFlow()

    fun signInWithGoogle() {
        viewModelScope.launch {
            _loginResult.emit(LoginResult.Loading)

            try {
                val idToken = authRepository.getGoogleIdToken()
                authRepository.signInWithGoogle(idToken)
                    .onSuccess { user ->
                        _loginResult.emit(LoginResult.Success(user.userMetadata?.get("full_name")?.toString() ?: ""))
                    }
                    .onFailure { error ->
                        _loginResult.emit(LoginResult.Error(error.message ?: "Unknown error"))
                    }
            } catch (e: Exception) {
                _loginResult.emit(LoginResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

sealed class LoginResult {
    object Loading : LoginResult()
    data class Success(val userName: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object Cancelled : LoginResult()
}
```

### DataStore for Persisting Local Information

```kotlin
data class UserPreferences(
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isPremium: Boolean = false,
    val lastLoginAt: Long? = null
)
```

---

### Project Configuration

### Firebase

Add the `google-services.json` file (downloaded from Firebase Console) to the app module.

### strings.xml

```xml
<resources>
    <string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
</resources>
```

### BuildConfig

```kotlin
// build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${properties["GOOGLE_WEB_CLIENT_ID"]}\"")
    }
}
```

---

## Aggregated context from the original proposal

The content below came from the family's historical README. It is a reference for reassessment, not an approved architecture.

### Historical overview — Firebase Auth


> **Status**: On Hold — may be reassessed if there is validated demand for Google Login and cloud backup.

## Reason for Holding

Firebase Auth and cloud services were postponed because:
1. The app works 100% offline and meets current needs
2. The immediate demand is local sharing between devices in the household
3. Firebase may be reassessed if there is validated demand for cloud backup or remote sync

## Preserved Specs

The specs below will be adapted and updated if work on this family resumes.

- [US-N01: Google Login](../0201-google-login/spec.md)
- [US-N02: Account Management](../0202-account-management/spec.md)
- [US-N03: Data Ownership](../0203-data-ownership/spec.md)
- [US-N04: Petit Cloud Gate](../0204-premium-gate/spec.md)


## Prerequisites

- Core pet-care capabilities implemented
- Google Cloud Console with OAuth configured
- Firebase project configured (google-services.json)


## User Stories

| ID | Feature | Priority |
|----|---------|------------|
| [US-101](../0201-google-login/spec.md) | Google Login | P0 |
| [US-102](../0202-account-management/spec.md) | Account Management | P0 |
| [US-103](../0203-data-ownership/spec.md) | Data Ownership | P1 |
| [US-104](../0204-premium-gate/spec.md) | Petit Cloud Gate | P1 |


## Architecture

### Authentication Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Login     │────▶│  Credential │────▶│  Firebase   │
│   Button    │     │   Manager   │     │    Auth     │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ Firebase    │
                                        │ UserInfo    │
                                        │ id, email   │
                                        └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │  DataStore  │
                                        │  (local)    │
                                        └─────────────┘
```

### Authentication States

```kotlin
sealed class AuthState {
    object Loading : AuthState()
    object Anonymous : AuthState()  // Using without login (free)
    data class Authenticated(
        val uid: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?,
        val isPremium: Boolean
    ) : AuthState()
}
```


## Firebase Configuration

### 1. Firebase Console

1. Create a project in Firebase Console
2. Enable Authentication > Google as a provider
3. Download and add `google-services.json` to the app module
4. Configure the Google Client ID for Credential Manager

### 2. Dependencies

```kotlin
// build.gradle.kts (app)
dependencies {
    // Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:VERSION"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // Credential Manager (still used to obtain the Google ID Token)
    implementation("androidx.credentials:credentials:1.2.x")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.x")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.x.x")
}
```


## Global Acceptance Criteria

- [ ] Google Login works through Credential Manager + Firebase Auth
- [ ] Users can use the app without logging in (anonymous mode)
- [ ] Logging out clears the authentication state but preserves local data
- [ ] The Firebase Auth token is renewed automatically
- [ ] The UI reflects the authentication state correctly
- [ ] Preparation for premium status verification


## Firebase Crashlytics (Supplementary)

Alongside the Firebase Auth implementation, add Firebase Crashlytics for crash monitoring (a free supplementary Firebase service).

### Motivation

- Detect production crashes
- Understand error patterns
- Prioritize fixes based on actual impact
- Gain visibility before users report issues

### Additional Dependencies

```kotlin
// build.gradle.kts (project)
plugins {
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

// build.gradle.kts (app)
plugins {
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```

### ProGuard/R8 Configuration

To make stack traces readable, configure mapping in the build:

```kotlin
// build.gradle.kts (app)
android {
    buildTypes {
        release {
            // Already exists
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Add for Crashlytics
            firebaseCrashlytics {
                mappingFileUploadEnabled = true
            }
        }
    }
}
```

### Initialization

Crashlytics starts automatically. To disable collection in debug builds:

```kotlin
// Application.kt
class PetitApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Disable Crashlytics in debug builds
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
```

### Acceptance Criteria

- [ ] Crashlytics configured and receiving events
- [ ] Stack traces are readable (not obfuscated)
- [ ] Collection disabled in debug builds
- [ ] Firebase dashboard displays crashes correctly


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
