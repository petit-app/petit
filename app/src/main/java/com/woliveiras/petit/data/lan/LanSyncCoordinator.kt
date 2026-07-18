package com.woliveiras.petit.data.lan

import android.content.Context
import android.net.nsd.NsdManager
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.lan.LanSessionScope
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.PendingDeparture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface LanSyncState {
  data object Idle : LanSyncState

  data object Discovering : LanSyncState

  data class Syncing(val peerName: String) : LanSyncState

  data class Synced(val peerName: String, val timestamp: Long) : LanSyncState

  data object PeerUnavailable : LanSyncState

  data class Error(val message: String) : LanSyncState
}

interface LanSyncController {
  val state: StateFlow<LanSyncState>

  suspend fun startForeground()

  suspend fun stopForeground()

  suspend fun attemptNow(): Boolean
}

internal fun selectLanCredential(
  activeKey: String?,
  departures: List<PendingDeparture>,
  index: Int,
): LanSessionCredential? {
  val candidates =
    departures
      .sortedWith(compareBy({ it.change.groupId }, { it.change.timestamp }))
      .map { LanSessionCredential(it.deliveryKey, LanSessionScope.MEMBERSHIP_ONLY, it) }
      .toMutableList()
      .apply {
        if (activeKey != null) add(LanSessionCredential(activeKey, LanSessionScope.CLINICAL))
      }
  if (candidates.isEmpty()) return null
  return candidates[Math.floorMod(index, candidates.size)]
}

/** Owns short-lived NSD/TCP resources; no persistent service or Wi-Fi Direct group is used. */
@Singleton
class LanSyncCoordinator
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val familyGroupRepository: FamilyGroupRepository,
  private val sessionRunner: LanSessionRunner,
) : LanSyncController {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val _state = MutableStateFlow<LanSyncState>(LanSyncState.Idle)
  private val activePeers = mutableSetOf<String>()
  private val lifecycleMutex = Mutex()
  @Volatile private var generation = 0L
  private var server: LanTcpServer? = null
  private var nsd: NsdServiceManager? = null
  private var peersJob: Job? = null
  private var timeoutJob: Job? = null
  private var errorsJob: Job? = null
  private var nextCredentialIndex = 0
  private var rotationBudget = 0

  override val state: StateFlow<LanSyncState> = _state.asStateFlow()

  override suspend fun startForeground() {
    lifecycleMutex.withLock { startLocked(manual = false) }
  }

  override suspend fun stopForeground() {
    lifecycleMutex.withLock {
      generation++
      stopResources()
      _state.value = LanSyncState.Idle
    }
  }

  override suspend fun attemptNow(): Boolean {
    val (alreadyRunning, attemptGeneration) =
      lifecycleMutex.withLock {
        val running = server != null
        if (running) {
          generation++
          stopResources()
        }
        startLocked(manual = true)
        running to generation
      }
    return try {
      delay(NsdServiceManager.DEFAULT_DISCOVERY_TIMEOUT_MILLIS + 250L)
      generation == attemptGeneration && state.value is LanSyncState.Synced
    } finally {
      lifecycleMutex.withLock {
        if (generation == attemptGeneration) {
          generation++
          stopResources()
          if (alreadyRunning) startLocked(manual = false)
        }
      }
    }
  }

  private suspend fun startLocked(manual: Boolean, resetRotationBudget: Boolean = true) {
    if (server != null) return
    val activeKey = familyGroupRepository.getFamilyGroupKey()
    val pending = familyGroupRepository.getPendingDepartures()
    val enabled = familyGroupRepository.isSyncEnabled.first()
    if (activeKey == null && pending.isEmpty()) {
      _state.value = LanSyncState.PeerUnavailable
      return
    }
    if (!manual && !enabled) {
      _state.value = LanSyncState.Idle
      return
    }
    if (resetRotationBudget) {
      rotationBudget = pending.size + if (activeKey != null) 1 else 0
      rotationBudget = (rotationBudget - 1).coerceAtLeast(0)
    }
    val credential = checkNotNull(selectLanCredential(activeKey, pending, nextCredentialIndex))
    val groupId = MembershipChange.groupIdForKey(credential.groupKey)
    val localId = familyGroupRepository.getOrCreateLocalDeviceId()
    val runtimeGeneration = ++generation
    val tcp =
      LanTcpServer(scope) { connection ->
        updateState(runtimeGeneration, LanSyncState.Syncing("peer"))
        try {
          val result = sessionRunner.runServer(connection, credential)
          if (result.authenticated) {
            updateState(runtimeGeneration, LanSyncState.Synced("peer", System.currentTimeMillis()))
          } else {
            updateState(runtimeGeneration, LanSyncState.Discovering)
          }
        } catch (exception: CancellationException) {
          throw exception
        } catch (exception: Exception) {
          updateState(runtimeGeneration, LanSyncState.Error(exception.message.orEmpty()))
        }
      }
    val manager: NsdServiceManager
    try {
      val port = tcp.start()
      server = tcp
      manager =
        NsdServiceManager(
          backend = AndroidNsdBackend(context.getSystemService(NsdManager::class.java)),
          localDeviceId = localId,
          groupId = groupId,
          port = port,
          scope = scope,
        )
      nsd = manager
      _state.value = LanSyncState.Discovering
      errorsJob =
        scope.launch {
          manager.errors.collect { error ->
            lifecycleMutex.withLock {
              if (generation != runtimeGeneration) return@withLock
              generation++
              stopResources()
              _state.value = LanSyncState.Error(error.message.orEmpty())
            }
          }
        }
      manager.start()
    } catch (exception: Exception) {
      stopResources()
      _state.value = LanSyncState.Error(exception.message.orEmpty())
      return
    }
    peersJob =
      scope.launch {
        manager.peers.collect { peers ->
          peers.forEach { peer ->
            synchronized(activePeers) { if (!activePeers.add(peer.deviceId)) return@forEach }
            launch {
              try {
                updateState(runtimeGeneration, LanSyncState.Syncing(peer.serviceName))
                val result = sessionRunner.runClient(peer, credential)
                if (result.batchesSent > 0 || result.batchesReceived > 0) {
                  updateState(
                    runtimeGeneration,
                    LanSyncState.Synced(peer.serviceName, System.currentTimeMillis()),
                  )
                  if (credential.scope == LanSessionScope.MEMBERSHIP_ONLY && !manual) {
                    rotateAfterDeparture(runtimeGeneration)
                  }
                } else if (
                  generation == runtimeGeneration && _state.value is LanSyncState.Syncing
                ) {
                  updateState(runtimeGeneration, LanSyncState.Discovering)
                }
              } catch (exception: CancellationException) {
                throw exception
              } catch (exception: Exception) {
                updateState(runtimeGeneration, LanSyncState.Error(exception.message.orEmpty()))
              } finally {
                synchronized(activePeers) { activePeers.remove(peer.deviceId) }
              }
            }
          }
        }
      }
    timeoutJob =
      scope.launch {
        delay(NsdServiceManager.DEFAULT_DISCOVERY_TIMEOUT_MILLIS)
        if (generation == runtimeGeneration) {
          if (_state.value is LanSyncState.Discovering) {
            _state.value = LanSyncState.PeerUnavailable
          }
          if (_state.value !is LanSyncState.Synced) {
            if (manual) {
              lifecycleMutex.withLock { nextCredentialIndex++ }
            } else if (rotationBudget > 0) {
              rotationBudget--
              rotateCredential(runtimeGeneration)
            }
          }
        }
      }
  }

  private fun stopResources() {
    timeoutJob?.cancel()
    timeoutJob = null
    errorsJob?.cancel()
    errorsJob = null
    peersJob?.cancel()
    peersJob = null
    nsd?.close()
    nsd = null
    server?.close()
    server = null
    synchronized(activePeers) { activePeers.clear() }
  }

  private fun updateState(runtimeGeneration: Long, state: LanSyncState) {
    if (generation == runtimeGeneration) _state.value = state
  }

  private fun rotateAfterDeparture(runtimeGeneration: Long) {
    if (rotationBudget > 0) rotationBudget--
    rotateCredential(runtimeGeneration, advance = false)
  }

  private fun rotateCredential(runtimeGeneration: Long, advance: Boolean = true) {
    scope.launch {
      delay(1L)
      lifecycleMutex.withLock {
        if (generation != runtimeGeneration) return@withLock
        if (advance) nextCredentialIndex++
        generation++
        stopResources()
        startLocked(manual = false, resetRotationBudget = false)
      }
    }
  }

  fun close() {
    generation++
    stopResources()
    scope.cancel()
  }
}
