# Plano: Gerenciamento de Conta

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

### Requisitos Técnicos

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
            // 1. Marcar dados na nuvem para purge em 30 dias (LGPD)
            authRepository.deleteAccount()
            // 3. Limpar preferências relacionadas à conta
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

### Delete Account no Firebase

```kotlin
suspend fun deleteAccount(): Result<Unit> {
    return try {
        // 1. Deletar conta no Firebase Auth
        //    Cloud Function trata a exclusão em cascata dos dados do usuário
        firebaseAuth.currentUser?.delete()?.await()
            ?: return Result.failure(Exception("Not logged in"))

        // Nota: a exclusão efetiva dos dados na nuvem e purge
        // é feita por Cloud Function para Firebase após 30 dias

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
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
