# Plano: Gate Premium

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
    private val firestore: FirebaseFirestore  // ou Billing client
) : PremiumRepository {

    private val _premiumStatus = MutableStateFlow(PremiumStatus.FREE)
    override val premiumStatus: StateFlow<PremiumStatus> = _premiumStatus.asStateFlow()

    override suspend fun checkPremiumStatus(): PremiumStatus {
        val userId = authRepository.getCurrentUser()?.id ?: return PremiumStatus.FREE

        // Verificar no Firebase Firestore ou via Google Play Billing
        val snapshot = firestore.collection("users")
            .document(userId).get().await()
        val userProfile = snapshot.toObject(UserProfile::class.java)

        val premiumUntil = userProfile?.premiumUntil ?: 0

        val status = when {
            premiumUntil > System.currentTimeMillis() -> PremiumStatus(
                tier = PremiumTier.PREMIUM_MONTHLY,  // ou verificar qual plano
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

### Verificação antes de ação

```kotlin
class BackupUseCase(
    private val premiumRepository: PremiumRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        if (!premiumRepository.isPremium()) {
            return Result.failure(PremiumRequiredException("Backup requer plano premium"))
        }

        // Executar backup...
        return Result.success(Unit)
    }
}

class PremiumRequiredException(message: String) : Exception(message)
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
