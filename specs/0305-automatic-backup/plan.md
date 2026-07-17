# Plano: Backup Automático

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0301`
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

### AutoBackupWorker

```kotlin
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Verificar premium
        if (!premiumRepository.isPremium()) {
            // Cancelar trabalho periódico
            WorkManager.getInstance(applicationContext)
                .cancelUniqueWork(WORK_NAME)
            return Result.failure()
        }

        // Verificar Wi-Fi se necessário
        val wifiOnly = backupPreferences.isWifiOnly()
        if (wifiOnly && !isOnWifi()) {
            return Result.retry()  // Tentar depois
        }

        // Executar backup
        return backupUseCase()
            .map { backupInfo ->
                // Atualizar timestamp
                backupPreferences.setLastBackupTimestamp(System.currentTimeMillis())

                // Notificar se configurado
                if (backupPreferences.shouldNotifyOnSuccess()) {
                    notificationHelper.showBackupSuccessNotification(backupInfo)
                }

                Result.success()
            }
            .getOrElse { error ->
                backupPreferences.setLastBackupError(error.message)
                Result.retry()
            }
    }

    private fun isOnWifi(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        const val WORK_NAME = "auto_backup_work"
    }
}
```

### Scheduling do Backup Periódico

```kotlin
class BackupScheduler(
    private val workManager: WorkManager,
    private val backupPreferences: BackupPreferencesRepository
) {
    fun scheduleAutoBackup() {
        val intervalHours = backupPreferences.getBackupIntervalHours()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (backupPreferences.isWifiOnly())
                    NetworkType.UNMETERED
                else
                    NetworkType.CONNECTED
            )
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .addTag("auto_backup")
            .build()

        workManager.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelAutoBackup() {
        workManager.cancelUniqueWork(AutoBackupWorker.WORK_NAME)
    }

    fun getNextBackupTime(): Long? {
        val workInfo = workManager.getWorkInfosForUniqueWork(AutoBackupWorker.WORK_NAME)
            .get()
            .firstOrNull()

        return workInfo?.nextScheduleTimeMillis
    }
}
```

### WorkerFactory para Injeção de Dependência

```kotlin
class PetitWorkerFactory(
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            AutoBackupWorker::class.java.name -> AutoBackupWorker(
                appContext,
                workerParameters,
                premiumRepository,
                backupUseCase,
                backupPreferences,
                notificationHelper
            )
            else -> null
        }
    }
}
```

### BackupPreferencesRepository

```kotlin
interface BackupPreferencesRepository {
    fun isAutoBackupEnabled(): Boolean
    fun setAutoBackupEnabled(enabled: Boolean)

    fun getBackupIntervalHours(): Int
    fun setBackupIntervalHours(hours: Int)

    fun isWifiOnly(): Boolean
    fun setWifiOnly(wifiOnly: Boolean)

    fun shouldNotifyOnSuccess(): Boolean
    fun setNotifyOnSuccess(notify: Boolean)

    fun getLastBackupTimestamp(): Long?
    fun setLastBackupTimestamp(timestamp: Long)

    fun getLastBackupError(): String?
    fun setLastBackupError(error: String?)
}

class BackupPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : BackupPreferencesRepository {

    companion object {
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_INTERVAL_HOURS = intPreferencesKey("backup_interval_hours")
        val WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
        val NOTIFY_ON_SUCCESS = booleanPreferencesKey("backup_notify_success")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val LAST_BACKUP_ERROR = stringPreferencesKey("last_backup_error")
    }

    // Implementações...
}
```

---

## Contexto agregado da proposta original

O conteúdo abaixo veio do README histórico da família. Ele é referência para reavaliação, não uma arquitetura aprovada.

### Visão histórica — Backup Automático (antiga Fase 4)


## Objetivo

Implementar **backup automático diário** dos dados para Google Drive (appDataFolder), agendado para 2h da madrugada, como funcionalidade **gratuita** para todos os usuários logados.

## Escopo

- ✅ Backup automático diário (2h da madrugada) - gratuito
- ✅ Habilitar/desabilitar backup automático
- ✅ Sync apenas em Wi-Fi (configurável)
- ✅ Notificação de backup bem-sucedido (opcional)
- ✅ Retenção automática: rolling window de 30 dias
- ❌ Sync em tempo real entre dispositivos (Fase 5 - premium)
- ❌ Resolução de conflitos multi-device (Fase 5 - premium)


## Pré-requisitos

- Fase 3 completa (Backup manual Google Drive)
- WorkManager configurado
- Login Google ativo (backup requer login)


## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-301](../0305-automatic-backup/spec.md) | Backup Automático | P0 |
| [US-302](../0306-backup-settings/spec.md) | Configurações de Backup | P0 |
| [US-303](../0307-backup-triggers/spec.md) | Triggers de Backup | P1 |


## Arquitetura

### WorkManager para Backup Diário

```
┌─────────────────────────────────────────────────────────────┐
│                        WorkManager                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PeriodicWorkRequest (24h, agendado para 2h da madrugada)    │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │ AutoBackupWorker                                      │     │
│  │                                                       │     │
│  │ - Verifica Login Google                               │     │
│  │ - Verifica Wi-Fi (se configurado)                     │     │
│  │ - Executa backup para Google Drive                   │     │
│  │ - Remove backups > 30 dias (rolling window)          │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   Google Drive   │
                    │  (appDataFolder) │
                    └─────────────────┘
```

### Fluxo de Backup Automático

```
┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│  Data Change   │────▶│   Debounce     │────▶│   Check        │
│  (Room write)  │     │   (5 min)      │     │   Conditions   │
└────────────────┘     └────────────────┘     └────────────────┘
                                                      │
                             ┌────────────────────────┼────────┐
                             │                        │        │
                             ▼                        ▼        ▼
                       ┌──────────┐           ┌──────────┐  ┌─────┐
                       │ Premium? │           │ Wi-Fi?   │  │ ... │
                       └──────────┘           └──────────┘  └─────┘
                             │                        │
                             └────────────────────────┘
                                        │
                                        ▼
                              ┌─────────────────┐
                              │ Execute Backup  │
                              └─────────────────┘
```


## Estratégia de Backup

### Backup Diário (2h da madrugada)
- Agendado via WorkManager com PeriodicWorkRequest (24h)
- Horário fixo: 2h da madrugada (horário ideal: usuário dormindo, device carregando, Wi-Fi ativo)
- Executa em background via WorkManager
- Respeita configuração de Wi-Fi only (padrão: ativo)
- Somente executa se Login Google estiver ativo

### Retenção
- Rolling window de 30 dias para backups automáticos
- Backups manuais não contam neste limite (máx 10, gerenciado na Fase 3)
- Auto-cleanup ao criar novo backup: remove automáticos com mais de 30 dias
- Após exclusão de conta: purge permanente em 30 dias (LGPD)


## Critérios de Aceite Globais

- [ ] Backup automático funciona em background
- [ ] Agendamento às 2h da madrugada funciona corretamente
- [ ] Opção Wi-Fi only é respeitada
- [ ] Battery optimization handling (Doze mode)
- [ ] Login Google é verificado antes do backup
- [ ] Retenção de 30 dias funciona (auto-cleanup)
- [ ] Notificações de backup (opcional)
- [ ] Integração com sistema de backups existente


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
