package com.woliveiras.petit.presentation.feature.familygroup

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.NearbyTransferRepository
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.PairingState
import com.woliveiras.petit.domain.model.TransferError
import com.woliveiras.petit.domain.model.TransferState
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import com.woliveiras.petit.domain.usecase.MergeDataUseCase
import com.woliveiras.petit.domain.usecase.SendDataUseCase
import java.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class TransferViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private lateinit var database: PetitDatabase
  private lateinit var nearby: FakeNearbyTransferRepository
  private lateinit var sendDataUseCase: SendDataUseCase
  private lateinit var viewModel: TransferViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    nearby = FakeNearbyTransferRepository()
    val exportImport =
      ExportImportUseCase(
        context,
        database,
        PetRepositoryImpl(database.petDao()),
        WeightEntryRepositoryImpl(database.weightEntryDao()),
        VaccinationEntryRepositoryImpl(database.vaccinationEntryDao(), Clock.systemUTC()),
        DewormingEntryRepositoryImpl(database.dewormingEntryDao(), Clock.systemUTC()),
        TaskRepositoryImpl(database.taskDao()),
      )
    sendDataUseCase = SendDataUseCase(exportImport, nearby, database)
    viewModel =
      TransferViewModel(
        SavedStateHandle(mapOf("mode" to "send")),
        nearby,
        sendDataUseCase,
        MergeDataUseCase(exportImport, database),
      )
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun senderUsesTheEndpointKeptByAuthorizedPairing() =
    runTest(dispatcher) {
      advanceUntilIdle()
      awaitBackgroundWork { nearby.sentEndpoints.isNotEmpty() }

      assertThat(nearby.sentEndpoints).containsExactly("authorized-endpoint")
    }

  @Test
  fun completedSendRecordsThePeerAndExactBundleEntityCount() =
    runTest(dispatcher) {
      advanceUntilIdle()
      awaitBackgroundWork { nearby.sentEndpoints.isNotEmpty() }
      database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))

      sendDataUseCase("authorized-endpoint", "Kitchen phone")

      val log = database.syncLogDao().getLatestSyncLog()
      assertThat(log?.peerId).isEqualTo("authorized-endpoint")
      assertThat(log?.peerName).isEqualTo("Kitchen phone")
      assertThat(log?.entitiesSent).isEqualTo(1)
      assertThat(log?.entitiesReceived).isEqualTo(0)
      assertThat(log?.syncType).isEqualTo("SEND")
    }

  @Test
  fun sendIsNotLoggedUntilTheTransportConfirmsPayloadSuccess() =
    runTest(dispatcher) {
      advanceUntilIdle()
      awaitBackgroundWork { nearby.sentEndpoints.isNotEmpty() }
      val logsBefore = database.syncLogDao().getAllSyncLogs().first().size
      database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
      nearby.sendGate = CompletableDeferred()

      val send = launch { sendDataUseCase("authorized-endpoint", "Kitchen phone") }
      advanceUntilIdle()

      assertThat(database.syncLogDao().getAllSyncLogs().first()).hasSize(logsBefore)
      nearby.sendGate?.complete(Unit)
      send.join()
      assertThat(database.syncLogDao().getAllSyncLogs().first()).hasSize(logsBefore + 1)
    }

  @Test
  fun interruptedSendCanRetryWhileEndpointRemainsConnected() =
    runTest(dispatcher) {
      nearby.failSend = true
      advanceUntilIdle()
      awaitBackgroundWork {
        viewModel.uiState.value.transferState == TransferState.Error(TransferError.TransferFailed)
      }
      assertThat(viewModel.uiState.value.transferState)
        .isEqualTo(TransferState.Error(TransferError.TransferFailed))

      nearby.failSend = false
      viewModel.retry()
      advanceUntilIdle()
      awaitBackgroundWork { nearby.sendAttempts == 2 }

      assertThat(nearby.sendAttempts).isEqualTo(2)
      assertThat(nearby.sentEndpoints).containsExactly("authorized-endpoint")
    }

  @Test
  fun cancellationDelegatesToTransportCleanup() =
    runTest(dispatcher) {
      viewModel.cancelTransfer()

      assertThat(nearby.cancelCalls).isEqualTo(1)
    }

  @Test
  fun replaceRequiresAnExplicitConfirmation() =
    runTest(dispatcher) {
      nearby.transfer.value =
        TransferState.Complete(
          ExportBundle(
            ExportMetadata("1.0", "2026-07-18T00:00:00Z"),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
          )
        )
      advanceUntilIdle()

      viewModel.requestReplace()
      assertThat(viewModel.uiState.value.showReplaceConfirmation).isTrue()
      viewModel.dismissReplace()
      assertThat(viewModel.uiState.value.showReplaceConfirmation).isFalse()
    }

  private class FakeNearbyTransferRepository : NearbyTransferRepository {
    override val pairingState: Flow<PairingState> = MutableStateFlow(PairingState.Idle)
    val transfer = MutableStateFlow<TransferState>(TransferState.Idle)
    override val transferState: Flow<TransferState> = transfer
    override val connectedPeerName: String = "Peer"
    override val connectedPeerId: String = "authorized-endpoint"
    var failSend = false
    var sendAttempts = 0
    var sendGate: CompletableDeferred<Unit>? = null
    var cancelCalls = 0
    val sentEndpoints = mutableListOf<String>()

    override suspend fun startAdvertising(
      deviceName: String,
      deviceId: String,
      familyGroupKey: String,
    ) = Unit

    override fun stopAdvertising() = Unit

    override suspend fun startDiscovery(deviceName: String, deviceId: String, pairingCode: String) =
      Unit

    override suspend fun requestConnection(endpointId: String) = Unit

    override fun stopDiscovery() = Unit

    override suspend fun acceptConnection(endpointId: String) = Unit

    override suspend fun rejectConnection(endpointId: String) = Unit

    override suspend fun sendData(endpointId: String, bundle: ExportBundle) {
      sendAttempts++
      if (failSend) error("interrupted")
      sendGate?.await()
      sentEndpoints += endpointId
    }

    override fun cancelTransfer() {
      cancelCalls++
    }

    override fun disconnect() = Unit
  }

  private suspend fun TestScope.awaitBackgroundWork(condition: () -> Boolean) {
    repeat(100) {
      advanceUntilIdle()
      if (condition()) return
      Thread.sleep(10)
    }
  }
}
