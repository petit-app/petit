# Local Sharing Protocols Between Android Devices

> **Last update:** 2026-07-18. The decisions apply to Petit and generic
> Android devices. Petit uses Nearby Connections for pairing/one-shot transfer
> and protected NSD + TCP for local-network sync in
> [spec 0104](../specs/0104-local-network-sync/spec.md).

## Introduction

This document describes the protocols and technologies available on Android for sharing data between devices **without relying on the internet or remote servers**. It focuses on a scenario in which two devices in the same home (Device A + Device B) need to synchronize an app's data.

---

## Technology Overview

| Technology                     | Range         | Throughput            | Requires Internet | Requires Pairing | Complexity | API Level       |
| ------------------------------ | ------------- | --------------------- | ----------------- | ---------------- | ---------- | --------------- |
| **Nearby Connections**         | ~100 m        | High (Wi-Fi Direct)   | No                | No (code)        | Low        | 16+             |
| **Wi-Fi Direct (P2P)**         | ~200 m        | Very high             | No                | Yes (WPS)        | Medium     | 14+             |
| **Wi-Fi Aware (NAN)**          | ~50 m         | High                  | No                | No               | High       | 26+ (Android 8) |
| **NSD (mDNS/DNS-SD)**          | Local network | High (TCP)            | No\*              | No               | Medium     | 16+             |
| **Bluetooth Classic**          | ~10 m         | Medium (~3 Mbps)      | No                | Yes              | Medium     | 5+              |
| **BLE (Bluetooth Low Energy)** | ~50 m         | Low (~1 Mbps)         | No                | No               | High       | 18+             |
| **NFC**                        | ~4 cm         | Very low              | No                | No               | Low        | 14+             |
| **UWB (Ultra-Wideband)**       | ~200 m        | Low                   | No                | Yes              | High       | 31+ (Android 12)|

> \* NSD requires both devices to be on the same local Wi-Fi network, but it does not require internet access.

---

## 1. Nearby Connections API

### What It Is

A Google Play Services API that abstracts Bluetooth, BLE, and Wi-Fi Direct behind a unified interface. Developers do not need to manage the transport technology in use—the system automatically selects the best option.

### How It Works

```
┌──────────────┐                      ┌──────────────┐
│  Advertiser  │                      │  Discoverer  │
│  (Device A)  │                      │  (Device B)  │
└──────┬───────┘                      └──────┬───────┘
       │                                     │
       │  1. startAdvertising()              │
       │◀────────────────────────────────────│  2. startDiscovery()
       │                                     │
       │  3. onEndpointFound()               │
       │────────────────────────────────────▶│
       │                                     │
       │  4. requestConnection()             │
       │◀────────────────────────────────────│
       │                                     │
       │  5. acceptConnection() ←───────────▶  5. acceptConnection()
       │                                     │
       │  6. onConnectionResult(SUCCESS)     │
       │◀───────────────────────────────────▶│
       │                                     │
       │  7. sendPayload() ◀───────────────▶  7. sendPayload()
       │                                     │
```

### Strategies (Topologies)

| Strategy               | Topology | Connections    | Throughput | Best Use Case                                  |
| ---------------------- | -------- | -------------- | ---------- | ---------------------------------------------- |
| **P2P_POINT_TO_POINT** | 1:1      | Maximum 1      | Maximum    | Data transfer between two devices              |
| **P2P_STAR**           | 1:N      | Hub ↔ N spokes | High       | One central device with multiple peripherals   |
| **P2P_CLUSTER**        | M:N      | Mesh           | Medium     | Multiplayer gaming and mesh networks           |

**Petit decision**: use `P2P_STAR`, matching the current implementation and the
local family-group topology. Any strategy change must be validated on two
devices before the implementation or this decision is updated.

### Payload Types

| Type       | Size      | Use                                      |
| ---------- | --------- | ---------------------------------------- |
| **BYTES**  | Up to 32 KB | Metadata, control messages, handshake  |
| **FILE**   | Unlimited | Large files (photos, backups)            |
| **STREAM** | Unlimited | Real-time generated data (audio, video)  |

**Recommendation for Petit**: use `BYTES` for small JSON payloads (< 32 KB). For larger data, use `FILE` with a serialized ExportBundle.

### Underlying Transport

Nearby Connections automatically uses:

1. **Bluetooth** for initial discovery
2. **Wi-Fi Direct** for data transfer (when available)
3. **BLE** as a discovery fallback

The developer **does not control** which transport is used. The API optimizes it automatically.

### Security

- **Automatic encryption**: all communication is encrypted end-to-end
- **Authentication**: both sides can verify an authentication token (the four-digit code)
- **Proximity**: works only with physically nearby devices

### Kotlin Code (Simplified Example)

```kotlin
// Advertiser (Device A — creates the code)
val advertisingOptions = AdvertisingOptions.Builder()
    .setStrategy(Strategy.P2P_STAR)
    .build()

connectionsClient.startAdvertising(
    localDeviceName,      // visible name
    SERVICE_ID,           // "com.woliveiras.petit.familygroup"
    connectionLifecycleCallback,
    advertisingOptions
)

// Discoverer (Device B — enters the code)
val discoveryOptions = DiscoveryOptions.Builder()
    .setStrategy(Strategy.P2P_STAR)
    .build()

connectionsClient.startDiscovery(
    SERVICE_ID,
    endpointDiscoveryCallback,
    discoveryOptions
)

// Send data after connecting
val jsonBytes = exportBundle.toJson().toByteArray()
val payload = Payload.fromBytes(jsonBytes)
connectionsClient.sendPayload(endpointId, payload)
```

### Required Permissions

```xml
<!-- Android 12 (API 31) and later -->
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- Android 13 (API 33) and later -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
                 android:usesPermissionFlags="neverForLocation" />

<!-- Android 12 and earlier -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
                 android:maxSdkVersion="32" />

<!-- Always required -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```

### Pros and Cons

| Pros                                      | Cons                                        |
| ----------------------------------------- | ------------------------------------------- |
| Simple, well-documented API               | Requires Google Play Services               |
| Automatically abstracts Bluetooth/Wi-Fi   | Analytics collected by Google               |
| Built-in encryption                       | Does not work on devices without GMS (Huawei)|
| High throughput with P2P_POINT_TO_POINT   | Limited control over the transport          |
| Works entirely offline                    |                                             |

---

## 2. Wi-Fi Direct (P2P)

### What It Is

A Wi-Fi protocol that allows two devices to connect directly, **without an intermediary access point**. One device acts as the "Group Owner" (a mini access point), and the other connects to it.

### How It Works

```
┌──────────────┐                      ┌──────────────┐
│   Device A   │  Wi-Fi Direct P2P   │   Device B   │
│ (Group Owner)│◀────────────────────▶│   (Client)   │
└──────┬───────┘                      └──────┬───────┘
       │                                     │
       │  1. discoverPeers()                 │
       │────────────────────────────────────▶│
       │                                     │
       │  2. WIFI_P2P_PEERS_CHANGED          │
       │◀────────────────────────────────────│
       │                                     │
       │  3. connect(WifiP2pConfig)          │
       │────────────────────────────────────▶│
       │                                     │
       │  4. WIFI_P2P_CONNECTION_CHANGED     │
       │◀───────────────────────────────────▶│
       │                                     │
       │  5. ServerSocket ←──── Socket       │
       │     (transfer over TCP)             │
       │                                     │
```

### Comparison with Nearby Connections

| Aspect       | Nearby Connections    | Wi-Fi Direct          |
| ------------ | --------------------- | --------------------- |
| Level        | High (Google API)     | Low (Android SDK)     |
| Control      | Limited (automatic)   | Full (manual)         |
| Transport    | Automatic (BT + Wi-Fi)| Wi-Fi only            |
| Setup        | Simple                | Complex               |
| Dependency   | Google Play Services  | None                  |

### When to Use Wi-Fi Direct Directly

- When full control over the transport is required
- When the app must work without Google Play Services
- When a persistent, long-lived connection is required

### Kotlin Code

```kotlin
// Initialize
val manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
val channel = manager.initialize(this, mainLooper, null)

// Discover peers
manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
    override fun onSuccess() { /* discovery started */ }
    override fun onFailure(reason: Int) { /* failed */ }
})

// Connect
val config = WifiP2pConfig().apply {
    deviceAddress = selectedDevice.deviceAddress
}
manager.connect(channel, config, actionListener)

// Transfer data (after connecting)
// Group Owner: ServerSocket(8888).accept()
// Client: Socket(groupOwnerAddress, 8888)
```

### Pros and Cons

| Pros                              | Cons                                      |
| --------------------------------- | ----------------------------------------- |
| Extremely high throughput         | More complex API                          |
| No GMS dependency                 | Manual socket management                  |
| Range of approximately 200 m      | Wi-Fi only (no Bluetooth fallback)        |
| Persistent connections are possible| Requires managing broadcast receivers    |
| Supports WPA2                     | Complex permission handling               |

---

## 3. Network Service Discovery (NSD / mDNS / DNS-SD)

### What It Is

A local-network service discovery mechanism based on **DNS-SD (DNS Service Discovery)** and **mDNS (Multicast DNS)**. It is the same protocol used by Apple's **Bonjour**. It allows an app to advertise a service on the local network so that other apps can discover and connect to it.

### How It Works

```
┌──────────────┐    mDNS broadcast    ┌──────────────┐
│   Device A   │   "_petit._tcp"      │   Device B   │
│   (Server)   │────────────────────▶│   (Client)   │
└──────┬───────┘  on the Wi-Fi network└──────┬───────┘
       │                                     │
       │  1. registerService()               │
       │  (advertises "_petit._tcp"          │
       │   with an IP address and port)      │
       │                                     │
       │                  2. discoverServices()
       │◀────────────────────────────────────│
       │                                     │
       │  3. resolveService()                │
       │  (obtains IP address + port)        │
       │────────────────────────────────────▶│
       │                                     │
       │  4. TCP socket connection           │
       │◀───────────────────────────────────▶│
       │                                     │
       │  5. Data exchange over TCP          │
       │◀───────────────────────────────────▶│
       │                                     │
```

### Comparison with Nearby Connections

| Aspect       | NSD                        | Nearby Connections       |
| ------------ | -------------------------- | ------------------------ |
| Requirement  | Same Wi-Fi network         | Physical proximity       |
| Always on    | Yes (while on the network) | No (must be started)     |
| Background   | Yes (with WorkManager)     | Limited                  |
| Best use     | Continuous sync at home    | Pairing and one-shot     |
| Dependency   | Android SDK (no GMS)       | Google Play Services     |

### Service Type Format

```
_<protocol>._<transport>
_petit._tcp           ← Petit service over TCP
_http._tcp            ← HTTP server
_ipp._tcp             ← printer
```

### Kotlin Code

```kotlin
// Register the service
val serviceInfo = NsdServiceInfo().apply {
    serviceName = "Petit-Device-A"
    serviceType = "_petit._tcp"
    setPort(serverSocket.localPort)
}

val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)

// Discover services
nsdManager.discoverServices("_petit._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)

// Resolve the service (obtain IP address + port)
nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
    override fun onServiceResolved(service: NsdServiceInfo) {
        val host: InetAddress = service.host
        val port: Int = service.port
        // Connect through a TCP socket
    }
    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { }
})
```

### Pros and Cons

| Pros                                      | Cons                                       |
| ----------------------------------------- | ------------------------------------------ |
| Industry standard (Bonjour-compatible)    | Requires the same Wi-Fi network            |
| No GMS dependency                         | Discovery can be slow (~5–15 seconds)      |
| Works in the background                   | Consumes battery if always active          |
| Ideal for continuous sync at home         | Does not work across different networks    |
| Low overhead                              | Requires manual TCP management             |

---

## 4. Bluetooth Classic

### What It Is

A short-range wireless communication protocol (~10 m). It uses RFCOMM (serial port emulation) for stream-based data transfer between paired devices.

### Throughput

- **Bluetooth 2.0+EDR**: ~3 Mbps
- **Bluetooth 3.0+HS**: ~24 Mbps (over Wi-Fi)
- **Bluetooth 4.0+**: ~3 Mbps (Classic) or ~1 Mbps (BLE)
- **Bluetooth 5.0+**: ~2 Mbps (BLE) with greater range

### When to Use It

- For smaller transfers (< 1 MB)
- When Wi-Fi is unavailable
- When a persistent, reliable connection is required

### Pros and Cons

| Pros                              | Cons                                  |
| --------------------------------- | ------------------------------------- |
| Universally available             | Low throughput compared with Wi-Fi    |
| Low power consumption             | Limited range (~10 m)                 |
| Stable connection                 | Requires manual pairing               |
| No Wi-Fi network dependency       | Older, more complex API               |

---

## 5. Bluetooth Low Energy (BLE)

### What It Is

A low-power version of Bluetooth optimized for exchanging small amounts of data. It uses the GATT (Generic Attribute Profile) model with characteristics and services.

### When to Use It

- To exchange small metadata payloads (< 512 bytes per characteristic)
- For continuous low-power presence detection and discovery
- For wearables and sensors

### For Petit: NOT Recommended as the Primary Transport

BLE is not suitable for transferring data in ExportBundles, which can be hundreds of KB or larger. It is useful only as a complementary discovery mechanism.

---

## 6. Wi-Fi Aware (NAN — Neighbor Awareness Networking)

### What It Is

A Wi-Fi protocol for direct discovery and communication between devices **without an access point**, available from Android 8.0 (API 26). Unlike Wi-Fi Direct, it does not require group formation—the devices communicate directly.

### When to Use It

- For discovery and communication without a Wi-Fi network
- When continuous, low-power discovery is required
- For IoT scenarios

### For Petit: Future Alternative

Wi-Fi Aware is more modern than Wi-Fi Direct but has less device support. For the home scenario (same Wi-Fi network), NSD is simpler and more reliable.

---

## 7. NFC (Near Field Communication)

### What It Is

Very-short-range communication (~4 cm), used to exchange small amounts of information quickly by bringing devices close together.

### For Petit: Useful Only for Bootstrapping

NFC could be used to start pairing (by touching the phones together), but not to transfer a significant amount of data.

---

## Comparison for Petit's Use Case

### Scenario: Family with Device A + Device B, Same Home, Same Wi-Fi Network

| Requirement                       | Best Technology     | Rationale                                              |
| --------------------------------- | ------------------- | ------------------------------------------------------ |
| **Initial pairing**               | Nearby Connections  | Simple, encrypted, and works without Wi-Fi             |
| **One-shot transfer**             | Nearby Connections  | High throughput and a simple API                       |
| **Continuous sync at home**       | NSD + TCP           | Works in the background, auto-discovery, no GMS overhead|
| **Sync without Wi-Fi (emergency)**| Nearby Connections  | Automatically uses Bluetooth/Wi-Fi Direct              |

### Recommended Hybrid Architecture

```
┌─────────────────────────────────────────────────────┐
│                  APPLICATION LAYER                  │
│                                                     │
│   FamilyGroupRepository ──── SyncEngine             │
│          │                      │                    │
│          ▼                      ▼                    │
│  ┌───────────────┐    ┌──────────────────┐          │
│  │    Nearby     │    │    NSD + TCP     │          │
│  │  Connections  │    │    (LAN Sync)    │          │
│  │               │    │                  │          │
│  │  • Pairing    │    │  • Discovery     │          │
│  │  • One-shot   │    │  • Auto-sync     │          │
│  │  • Fallback   │    │  • Background    │          │
│  └───────┬───────┘    └────────┬─────────┘          │
│          │                      │                    │
└──────────┼──────────────────────┼────────────────────┘
           │                      │
    ┌──────▼──────┐       ┌──────▼──────┐
    │  Bluetooth  │       │   TCP over  │
    │  Wi-Fi Dir  │       │   Wi-Fi LAN │
    │  (GMS auto) │       │   (Sockets) │
    └─────────────┘       └─────────────┘
```

---

## Sync Protocol for Petit

### Handshake (TCP over NSD)

1. `HELLO` carries protocol version, stable device UUID, acknowledged cursor,
   a 256-bit random nonce, session scope, and HMAC-SHA256 proof. The group key
   is never transmitted.
2. The server validates version, group proof, known active member, and nonce
   replay before returning `HELLO_ACK` with its identity, nonce, cursor, scope,
   and bilateral HMAC proof. Invalid input receives `ERROR` and `CLOSE` before
   any health data.
3. Both sides derive separate client-to-server and server-to-client AES-256-GCM
   keys. Every protected packet has a strict sequence number; tamper, replay,
   and out-of-order packets are rejected.
4. `CHANGESET` carries a deterministic batch UUID, cursor, and bounded
   `ExportBundle` payload (maximum 512 KiB). The receiver validates the entire
   bundle, applies it with the 0105 resolver, writes the replay ledger and
   `SyncLog` in the same Room transaction, then returns a matching `ACK`.
5. A per-group/per-peer ledger retains stable IDs for the constituent entity
   units confirmed by each ACK. A missing ACK leaves them pending; an
   already-applied batch returns its durable ACK without a second log. Selection
   therefore remains complete even if the local clock moves backwards.
6. `CLOSE` ends each outbound phase. The lower stable UUID initiates the normal
   session, preventing duplicate simultaneous sessions.

The `MEMBERSHIP_ONLY` scope is reserved for a queued offline `LEAVE`. It accepts
only the caller's own departure event and cannot carry pets or health records.
Its restricted delivery credential is removed after ACK.

### Changeset Format

The changeset uses the same format as ExportBundle (JSON), includes tombstones,
and contains versions at the acknowledged cursor boundary or newer. Required
parent pets accompany changed child records so validation completes before the
transaction begins:

```json
{
  "type": "CHANGESET",
  "since": 1712345678000,
  "pets": [
    {
      "id": "pet-a-uuid",
      "name": "Pet A",
      "updatedAt": 1712345900000,
      "deletedAt": null,
      "syncStatus": "SYNCED"
    }
  ],
  "weightEntries": [
    {
      "id": "weight-1-uuid",
      "petId": "pet-a-uuid",
      "date": "2026-04-12",
      "weightGrams": 3520,
      "updatedAt": 1712345800000,
      "deletedAt": null
    }
  ]
}
```

### Merge Rules (Last-Write-Wins)

```
For each entity in the remote changeset:

  IF entity.id does NOT exist locally:
    → INSERT (new entity)

  IF entity.id exists locally:
    IF remote.deletedAt != null:
      IF local.updatedAt > remote.deletedAt:
        → KEEP_LOCAL (edit is more recent than the deletion)
      ELSE:
        → APPLY_DELETE (propagate the soft deletion)

    IF remote.updatedAt > local.updatedAt:
      → UPDATE (remote data is more recent)

    ELSE:
      → KEEP_LOCAL (local data is more recent)
```

---

## References

- [Nearby Connections Overview](https://developers.google.com/nearby/connections/overview)
- [Nearby Connections Strategies](https://developers.google.com/nearby/connections/strategies)
- [Wi-Fi Direct (P2P)](https://developer.android.com/develop/connectivity/wifi/wifip2p)
- [Create P2P connections with Wi-Fi Direct](https://developer.android.com/develop/connectivity/wifi/wifi-direct)
- [Network Service Discovery (NSD)](https://developer.android.com/develop/connectivity/wifi/use-nsd)
- [Bluetooth data transfer](https://developer.android.com/develop/connectivity/bluetooth/transfer-data)
- [Wi-Fi Aware](https://developer.android.com/develop/connectivity/wifi/wifi-aware)
- [Android Connectivity overview](https://developer.android.com/develop/connectivity)
