# Plan: Device-to-Device Transfer

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. No step authorizes implementation until the spec has been reviewed and approved.

## Dependencies

- Specs: `0101`
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
    // Sender
    suspend fun startAdvertising(): Flow<TransferState>
    suspend fun sendData(endpointId: String, data: ExportBundle): Result<Unit>
    fun stopAdvertising()

    // Receiver
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

### NearbyTransferRepository (Implementation)

```kotlin
class NearbyTransferRepository(
    private val context: Context,
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
) : DeviceTransferRepository {

    override suspend fun startAdvertising(): Flow<TransferState> = callbackFlow {
        val code = generateSecurityCode() // 4 digits

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
        // Export local data
        val exportBundle = exportDataUseCase.exportAll()

        // Send via Nearby Connections
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
        // Receive data
        val result = deviceTransferRepository.receiveData(endpointId)

        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }

        val exportBundle = result.getOrNull()!!

        // Import with merge
        return importDataUseCase.importWithMerge(exportBundle)
    }

    suspend fun receiveAndReplace(endpointId: String): Result<ImportResult> {
        val result = deviceTransferRepository.receiveData(endpointId)

        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }

        val exportBundle = result.getOrNull()!!

        // Import with full replacement
        return importDataUseCase.importWithReplace(exportBundle)
    }
}
```

---

### Permissions

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

### Security

- **4-digit code**: Prevents unauthorized connections
- **Proximity-based**: Works only with nearby devices (< 10 meters)
- **One-shot transfer**: The connection is closed after the transfer
- **No cloud storage**: Data travels directly between devices
- **Encryption**: Nearby Connections uses automatic encryption

---

### Implementation Notes

- **Strategy**: Use `Strategy.P2P_POINT_TO_POINT` (1 sender, 1 receiver)
- **Service ID**: Must be unique per app (`com.woliveiras.petit.DEVICE_TRANSFER`)
- **Payload**: Serialize ExportBundle to JSON and send it as a ByteArray
- **Timeout**: 30 seconds without activity cancels the connection
- **Battery**: Nearby Connections is battery-optimized
- **Reuse**: The flow reuses ExportBundle/ImportDataUseCase from Phase 1

---


## Risks and validation

- Dependency on external services, authentication, quotas, and contractual changes.
- Privacy and the lifecycle of personal and pet health data.
- Database migrations and compatibility with data created offline or in older versions.
- Concurrency, idempotency, conflicts, and recovery after interruptions.
- Accessibility and clarity of error, waiting, and destructive confirmation states.

## Planned verification

- `./gradlew test`
- `./gradlew connectedDebugAndroidTest`
- `./gradlew spotlessCheck`
- When a build is run: `./gradlew assembleDebug` followed by `./gradlew installDebug`
