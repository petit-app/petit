# Plano: Resolução de Conflitos na Nuvem

Spec: [spec.md](./spec.md)

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0401`
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

### ConflictResolver

```kotlin
class ConflictResolver {

    sealed class Resolution {
        object KeepLocal : Resolution()
        object UseRemote : Resolution()
        data class Merge(val merged: Any) : Resolution()
    }

    fun <T : SyncableEntity> resolve(local: T?, remote: T): Resolution {
        // Não existe localmente: usar remoto
        if (local == null) {
            return Resolution.UseRemote
        }

        // Verificar deletions
        val localDeleted = local.deletedAt != null
        val remoteDeleted = remote.deletedAt != null

        return when {
            // Ambos deletados: usar mais recente
            localDeleted && remoteDeleted -> {
                if (remote.updatedAt >= local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }

            // Apenas remoto deletado
            remoteDeleted -> {
                // Se delete é mais recente que local update, aceitar delete
                if (remote.deletedAt!! >= local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }

            // Apenas local deletado
            localDeleted -> {
                // Se remote update é mais recente que local delete, restaurar
                if (remote.updatedAt > local.deletedAt!!) Resolution.UseRemote
                else Resolution.KeepLocal
            }

            // Nenhum deletado: comparar updatedAt
            else -> {
                if (remote.updatedAt > local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }
        }
    }
}
```

### Aplicando Resolução

```kotlin
class SyncProcessor(
    private val conflictResolver: ConflictResolver,
    private val petDao: PetDao
) {
    suspend fun processRemotePet(remote: PetFirestoreModel) {
        val local = petDao.getPetById(remote.id)

        when (val resolution = conflictResolver.resolve(local, remote.toEntity())) {
            is ConflictResolver.Resolution.UseRemote -> {
                petDao.insertPet(remote.toEntity().copy(syncStatus = "SYNCED"))
            }
            is ConflictResolver.Resolution.KeepLocal -> {
                // Local é mais recente, precisa re-upload
                if (local != null && local.syncStatus != "SYNCED") {
                    // Será enviado no próximo upload cycle
                }
            }
            is ConflictResolver.Resolution.Merge -> {
                // Implementação futura para field-level merge
            }
        }
    }
}
```

### Clock Synchronization

Para garantir que `updatedAt` seja confiável entre dispositivos:

```kotlin
object SyncClock {
    /**
     * Retorna timestamp para uso em updatedAt
     * Considera possível diferença de relógio entre dispositivos
     */
    fun now(): Long {
        // Por simplicidade, usar System.currentTimeMillis()
        // Em produção, considerar usar Firestore server timestamps
        // ou NTP para sincronizar relógios
        return System.currentTimeMillis()
    }
}

// No Firestore, usar FieldValue.serverTimestamp() para updated_at
// A coluna updated_at pode usar FieldValue.serverTimestamp()
// Mas para Last-Write-Wins, o client envia o timestamp local
```

### Logging de Conflitos (Debug)

```kotlin
class ConflictLogger(
    private val analyticsTracker: AnalyticsTracker
) {
    fun logConflict(
        entityType: String,
        entityId: String,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long,
        resolution: String
    ) {
        if (BuildConfig.DEBUG) {
            Log.d("ConflictResolver", """
                Conflict detected:
                  Entity: $entityType/$entityId
                  Local updatedAt: $localUpdatedAt
                  Remote updatedAt: $remoteUpdatedAt
                  Resolution: $resolution
            """.trimIndent())
        }

        // Analytics para monitorar frequência de conflitos
        analyticsTracker.trackEvent("sync_conflict", mapOf(
            "entity_type" to entityType,
            "resolution" to resolution
        ))
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
