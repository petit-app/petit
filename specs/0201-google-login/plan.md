# Plano: Login com Google

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: Nenhuma dependência entre specs.
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

### Implementação com Credential Manager + Firebase Auth

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
            // Ignorar erro de clear
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
                        _loginResult.emit(LoginResult.Error(error.message ?: "Erro desconhecido"))
                    }
            } catch (e: Exception) {
                _loginResult.emit(LoginResult.Error(e.message ?: "Erro desconhecido"))
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

### DataStore para Persistir Info Local

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

### Configuração do Projeto

### Firebase

Adicionar o arquivo `google-services.json` (baixado do Firebase Console) ao módulo app.

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

## Contexto agregado da proposta original

O conteúdo abaixo veio do README histórico da família. Ele é referência para reavaliação, não uma arquitetura aprovada.

### Visão histórica — Firebase Auth (antiga Fase N)


> **Status**: Em holding — poderá ser reavaliada se houver demanda validada por login Google e backup na nuvem.

## Motivo do Holding

Firebase Auth e serviços cloud foram adiados porque:
1. O app funciona 100% offline e atende às necessidades atuais
2. A demanda imediata é compartilhamento local entre dispositivos da casa
3. Firebase poderá ser reavaliado se houver demanda validada por backup na nuvem ou sync remoto

## Specs Preservadas

As specs abaixo foram migradas da Fase 2 original e serão adaptadas/atualizadas quando esta fase for retomada.

- [US-N01: Login com Google](../0201-google-login/spec.md)
- [US-N02: Gerenciamento de Conta](../0202-account-management/spec.md)
- [US-N03: Vinculação de Dados](../0203-data-ownership/spec.md)
- [US-N04: Gate Premium](../0204-premium-gate/spec.md)


## Pré-requisitos

- Fase 1 completa
- Google Cloud Console com OAuth configurado
- Firebase project configurado (google-services.json)


## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-101](../0201-google-login/spec.md) | Login com Google | P0 |
| [US-102](../0202-account-management/spec.md) | Gerenciamento de Conta | P0 |
| [US-103](../0203-data-ownership/spec.md) | Vinculação de Dados | P1 |
| [US-104](../0204-premium-gate/spec.md) | Gate Premium | P1 |


## Arquitetura

### Fluxo de Autenticação

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

### Estados de Autenticação

```kotlin
sealed class AuthState {
    object Loading : AuthState()
    object Anonymous : AuthState()  // Usando sem login (free)
    data class Authenticated(
        val uid: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?,
        val isPremium: Boolean
    ) : AuthState()
}
```


## Configuração Firebase

### 1. Firebase Console

1. Criar projeto no Firebase Console
2. Habilitar Authentication > Google como provider
3. Baixar e adicionar `google-services.json` ao módulo app
4. Configurar Google Client ID para Credential Manager

### 2. Dependências

```kotlin
// build.gradle.kts (app)
dependencies {
    // Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:VERSION"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // Credential Manager (continua sendo usado para obter ID Token do Google)
    implementation("androidx.credentials:credentials:1.2.x")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.x")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.x.x")
}
```


## Critérios de Aceite Globais

- [ ] Login com Google funciona via Credential Manager + Firebase Auth
- [ ] Usuário pode usar o app sem login (modo anônimo)
- [ ] Logout limpa estado de autenticação mas mantém dados locais
- [ ] Token Firebase Auth é renovado automaticamente
- [ ] UI reflete estado de autenticação corretamente
- [ ] Preparação para verificação de premium status


## Firebase Crashlytics (Complementar)

Junto com a implementação do Firebase Auth, adicionar Firebase Crashlytics para monitoramento de crashes (serviço Firebase complementar gratuito).

### Motivação

- Detectar crashes em produção
- Entender padrões de erros
- Priorizar correções com base em impacto real
- Ter visibilidade antes que usuários reportem

### Dependências Adicionais

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

### Configuração ProGuard/R8

Para que os stack traces sejam legíveis, configurar mapeamento no build:

```kotlin
// build.gradle.kts (app)
android {
    buildTypes {
        release {
            // Já existente
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Adicionar para Crashlytics
            firebaseCrashlytics {
                mappingFileUploadEnabled = true
            }
        }
    }
}
```

### Inicialização

Crashlytics inicia automaticamente. Para desabilitar coleta em debug:

```kotlin
// Application.kt
class PetitApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Desabilitar Crashlytics em debug
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
```

### Critérios de Aceite

- [ ] Crashlytics configurado e recebendo eventos
- [ ] Stack traces são legíveis (não ofuscados)
- [ ] Coleta desabilitada em build de debug
- [ ] Dashboard Firebase mostra crashes corretamente


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
