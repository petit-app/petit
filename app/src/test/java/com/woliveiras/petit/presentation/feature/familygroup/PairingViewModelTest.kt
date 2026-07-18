package com.woliveiras.petit.presentation.feature.familygroup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.NearbyTransferRepository
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.PairingError
import com.woliveiras.petit.domain.model.PairingState
import com.woliveiras.petit.domain.model.SyncLog
import com.woliveiras.petit.domain.model.TransferState
import com.woliveiras.petit.domain.pairing.PairingCredentialsGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PairingViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private lateinit var nearby: FakeNearbyTransferRepository
  private lateinit var family: FakeFamilyGroupRepository
  private lateinit var viewModel: PairingViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    nearby = FakeNearbyTransferRepository()
    family = FakeFamilyGroupRepository()
    viewModel = PairingViewModel(nearby, family, PairingCredentialsGenerator())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun advertisingDoesNotPersistAnythingBeforeAuthorization() =
    runTest(dispatcher) {
      viewModel.startAdvertising()
      advanceUntilIdle()

      assertThat(nearby.advertising).isNotNull()
      assertThat(family.persistCalls).isEmpty()
      assertThat(viewModel.uiState.value.familyGroupKey).isNotEmpty()
    }

  @Test
  fun receiverRequiresFourDigitsBeforeDiscovery() =
    runTest(dispatcher) {
      viewModel.onPairingCodeChanged("12")
      viewModel.startDiscovery()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.isPairingCodeInvalid).isTrue()
      assertThat(nearby.discovery).isNull()
    }

  @Test
  fun receiverConnectsOnlyAfterExplicitEndpointSelection() =
    runTest(dispatcher) {
      viewModel.onPairingCodeChanged("0042")
      viewModel.startDiscovery()
      advanceUntilIdle()
      nearby.pairing.value = PairingState.EndpointFound("Sender", "endpoint-1")
      advanceUntilIdle()

      assertThat(nearby.requestedEndpoint).isNull()
      viewModel.requestConnection("endpoint-1")
      advanceUntilIdle()
      assertThat(nearby.requestedEndpoint).isEqualTo("endpoint-1")
    }

  @Test
  fun authorizedPairingPersistsStableLocalAndRemoteIdentities() =
    runTest(dispatcher) {
      viewModel.onPairingCodeChanged("0042")
      viewModel.startDiscovery()
      advanceUntilIdle()
      val localId = checkNotNull(viewModel.uiState.value.localDeviceId)

      nearby.pairing.value = PairingState.Paired("group-key", "Sender", "sender-id", "endpoint-1")
      advanceUntilIdle()

      val call = family.persistCalls.single()
      assertThat(call.localMember.id).isEqualTo(localId)
      assertThat(call.remoteMember.id).isEqualTo("sender-id")
      assertThat(call.localMember.familyGroupKey).isEqualTo("group-key")
      assertThat(call.remoteMember.familyGroupKey).isEqualTo("group-key")
      assertThat(viewModel.uiState.value.pairingPersisted).isTrue()
    }

  @Test
  fun unavailablePlayServicesStopsBeforeAdvertising() =
    runTest(dispatcher) {
      nearby.available = false

      viewModel.startAdvertising()
      advanceUntilIdle()

      assertThat(nearby.advertising).isNull()
      assertThat(viewModel.uiState.value.pairingState)
        .isEqualTo(PairingState.Error(PairingError.PlayServicesUnavailable))
    }

  @Test
  fun cancellationIsIdempotentAndNeverPersistsAPartialGroup() =
    runTest(dispatcher) {
      viewModel.startAdvertising()
      advanceUntilIdle()

      viewModel.cancel()
      viewModel.cancel()
      advanceUntilIdle()

      assertThat(nearby.disconnectCalls).isEqualTo(2)
      assertThat(family.persistCalls).isEmpty()
      assertThat(viewModel.uiState.value.pairingState).isEqualTo(PairingState.Idle)
    }

  @Test
  fun persistenceFailureDisconnectsAndExposesNoSuccessfulPairing() =
    runTest(dispatcher) {
      family.failPersistence = true
      viewModel.onPairingCodeChanged("0042")
      viewModel.startDiscovery()
      advanceUntilIdle()

      nearby.pairing.value = PairingState.Paired("group-key", "Sender", "sender-id", "endpoint-1")
      advanceUntilIdle()

      assertThat(nearby.disconnectCalls).isEqualTo(1)
      assertThat(viewModel.uiState.value.pairingPersisted).isFalse()
      assertThat(viewModel.uiState.value.pairingState)
        .isEqualTo(PairingState.Error(PairingError.PersistenceFailed))
    }

  private class FakeNearbyTransferRepository : NearbyTransferRepository {
    val pairing = MutableStateFlow<PairingState>(PairingState.Idle)
    override val pairingState: Flow<PairingState> = pairing
    override val transferState: Flow<TransferState> = MutableStateFlow(TransferState.Idle)
    override val connectedPeerName: String? = null
    override val connectedPeerId: String? = null
    var available = true
    var advertising: List<String>? = null
    var discovery: List<String>? = null
    var requestedEndpoint: String? = null
    var disconnectCalls = 0

    override fun isAvailable(): Boolean = available

    override suspend fun startAdvertising(
      deviceName: String,
      deviceId: String,
      familyGroupKey: String,
    ) {
      advertising = listOf(deviceName, deviceId, familyGroupKey)
    }

    override fun stopAdvertising() = Unit

    override suspend fun startDiscovery(deviceName: String, deviceId: String, pairingCode: String) {
      discovery = listOf(deviceName, deviceId, pairingCode)
    }

    override fun stopDiscovery() = Unit

    override suspend fun requestConnection(endpointId: String) {
      requestedEndpoint = endpointId
    }

    override suspend fun acceptConnection(endpointId: String) = Unit

    override suspend fun rejectConnection(endpointId: String) = Unit

    override suspend fun sendData(endpointId: String, bundle: ExportBundle) = Unit

    override fun disconnect() {
      disconnectCalls++
    }
  }

  private class FakeFamilyGroupRepository : FamilyGroupRepository {
    data class PersistCall(
      val key: String,
      val localMember: FamilyGroupMember,
      val remoteMember: FamilyGroupMember,
    )

    override val familyGroupInfo: Flow<FamilyGroupInfo?> = MutableStateFlow(null)
    override val localDevice: Flow<FamilyGroupMember?> = MutableStateFlow(null)
    override val isSyncEnabled: Flow<Boolean> = MutableStateFlow(false)
    val persistCalls = mutableListOf<PersistCall>()
    var failPersistence = false

    override suspend fun getFamilyGroupKey(): String? = null

    override suspend fun createFamilyGroup(deviceName: String): String = error("unused")

    override suspend fun joinFamilyGroup(familyGroupKey: String, deviceName: String) = Unit

    override suspend fun persistAuthorizedPairing(
      familyGroupKey: String,
      localMember: FamilyGroupMember,
      remoteMember: FamilyGroupMember,
    ) {
      if (failPersistence) error("persistence failed")
      persistCalls += PersistCall(familyGroupKey, localMember, remoteMember)
    }

    override suspend fun addRemoteMember(member: FamilyGroupMember) = Unit

    override suspend fun leaveFamilyGroup() = Unit

    override suspend fun removeMember(memberId: String) = Unit

    override suspend fun updateLastSyncAt(memberId: String) = Unit

    override suspend fun setSyncEnabled(enabled: Boolean) = Unit

    override suspend fun recordSyncLog(syncLog: SyncLog) = Unit

    override fun getSyncLogs(): Flow<List<SyncLog>> = emptyFlow()

    override suspend fun getLatestSyncLog(): SyncLog? = null

    override suspend fun resetLocalPreferences() = Unit
  }
}
