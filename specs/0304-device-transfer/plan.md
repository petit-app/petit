# Plano: Transferência entre Dispositivos

Spec: [spec.md](./spec.md)

## Estado

Este plano está **On Hold**. Nenhuma etapa autoriza implementação até que a spec seja revisada e aprovada.

## Dependências

- Specs: `0101`
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

### Nearby Connections API

```kotlin
dependencies {
    // Google Nearby Connections
    implementation("com.google.android.gms:play-services-nearby:VERSION")
}
```

### DeviceTransferRepository

```kotlin
interface DeviceTransferRepository {
    // Transmissor
    suspend fun startAdvertising(): Flow<TransferState>
    suspend fun sendData(endpointId: String, data: ExportBundle): Result<Unit>
    fun stopAdvertising()

    // Receptor
    suspend fun startDiscovery(code: String): Flow<TransferState>
    suspend fun receiveData(endpointId: String): Result<ExportBundle>
    fun stopDiscovery()
}

sealed class TransferState {
    object AdvertisingStarted : TransferState()
    data class ConnectionRequested(val endpointId: String, val deviceName: String) : TransferState()
    data class Connected(val endpointId: String) : TransferState()
    data class Transferring(val bytesTransferred: Long, val totalBytes: Long) : TransferState()
    data class TransferComplete(val data: ExportBundle) : TransferState()
    data class Error(val message: String) : TransferState()
}
```

### NearbyTransferRepository (Implementação)

```kotlin
class NearbyTransferRepository(
    private val context: Context,
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
) : DeviceTransferRepository {

    override suspend fun startAdvertising(): Flow<TransferState> = callbackFlow {
        val code = generateSecurityCode() // 4 dígitos

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startAdvertising(
            context.packageName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        )

        trySend(TransferState.AdvertisingStarted)

        awaitClose {
            connectionsClient.stopAdvertising()
        }
    }

    private fun generateSecurityCode(): String {
        return (1000..9999).random().toString()
    }

    companion object {
        private const val SERVICE_ID = "com.woliveiras.petit.DEVICE_TRANSFER"
    }
}
```

### TransferDataUseCase

```kotlin
class TransferDataUseCase(
    private val exportDataUseCase: ExportDataUseCase,
    private val deviceTransferRepository: DeviceTransferRepository
) {
    suspend fun sendData(endpointId: String): Result<Unit> {
        // Exportar dados locais
        val exportBundle = exportDataUseCase.exportAll()

        // Enviar via Nearby Connections
        return deviceTransferRepository.sendData(endpointId, exportBundle)
    }
}
```

### ReceiveDataUseCase

```kotlin
class ReceiveDataUseCase(
    private val deviceTransferRepository: DeviceTransferRepository,
    private val importDataUseCase: ImportDataUseCase
) {
    suspend fun receiveAndMerge(endpointId: String): Result<ImportResult> {
        // Receber dados
        val result = deviceTransferRepository.receiveData(endpointId)

        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }

        val exportBundle = result.getOrNull()!!

        // Importar com merge
        return importDataUseCase.importWithMerge(exportBundle)
    }

    suspend fun receiveAndReplace(endpointId: String): Result<ImportResult> {
        val result = deviceTransferRepository.receiveData(endpointId)

        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }

        val exportBundle = result.getOrNull()!!

        // Importar com substituição total
        return importDataUseCase.importWithReplace(exportBundle)
    }
}
```

---

### Permissões

### AndroidManifest.xml

```xml
<!-- Nearby Connections -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

### Segurança

- **Código de 4 dígitos**: Previne conexões não autorizadas
- **Proximity-based**: Funciona apenas com devices próximos (< 10 metros)
- **One-shot transfer**: Conexão é encerrada após transferência
- **No cloud storage**: Dados trafegam diretamente entre devices
- **Encryption**: Nearby Connections usa criptografia automática

---

### Notas de Implementação

- **Strategy**: Usar `Strategy.P2P_POINT_TO_POINT` (1 transmissor, 1 receptor)
- **Service ID**: Deve ser único por app (`com.woliveiras.petit.DEVICE_TRANSFER`)
- **Payload**: Serializar ExportBundle para JSON, enviar como ByteArray
- **Timeout**: 30 segundos sem atividade cancela conexão
- **Battery**: Nearby Connections é otimizado para bateria
- **Reutilização**: Fluxo reutiliza ExportBundle/ImportDataUseCase da Fase 1

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
