package com.woliveiras.petit.presentation.feature.familygroup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.SyncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncHistoryViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun exposesHistoryContentAndEmptyState() =
    runTest(dispatcher) {
      val logs = MutableStateFlow(emptyList<SyncLog>())
      val viewModel = SyncHistoryViewModel(FakeFamilyGroupRepository(logs))
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.isLoading).isFalse()
      assertThat(viewModel.uiState.value.logs).isEmpty()

      logs.value = listOf(syncLog())
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.logs).containsExactly(syncLog())
    }

  @Test
  fun exposesRepositoryFailure() =
    runTest(dispatcher) {
      val viewModel =
        SyncHistoryViewModel(
          FakeFamilyGroupRepository(flow { throw IllegalStateException("database unavailable") })
        )
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.hasError).isTrue()
      assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

  private fun syncLog() =
    SyncLog(
      id = "log-1",
      peerId = "peer-1",
      peerName = "Kitchen phone",
      syncTimestamp = 1L,
      entitiesSent = 2,
      entitiesReceived = 3,
      conflictsResolved = 1,
      syncType = "MERGE",
      createdAt = 1L,
      updatedAt = 1L,
    )

  private class FakeFamilyGroupRepository(private val logs: Flow<List<SyncLog>>) :
    FamilyGroupRepository {
    override val familyGroupInfo: Flow<FamilyGroupInfo?> = MutableStateFlow(null)
    override val localDevice: Flow<FamilyGroupMember?> = MutableStateFlow(null)
    override val isSyncEnabled: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun getFamilyGroupKey(): String? = null

    override suspend fun createFamilyGroup(deviceName: String): String = error("unused")

    override suspend fun joinFamilyGroup(familyGroupKey: String, deviceName: String) = Unit

    override suspend fun persistAuthorizedPairing(
      familyGroupKey: String,
      localMember: FamilyGroupMember,
      remoteMember: FamilyGroupMember,
    ) = Unit

    override suspend fun addRemoteMember(member: FamilyGroupMember) = Unit

    override suspend fun leaveFamilyGroup() = Unit

    override suspend fun removeMember(memberId: String) = Unit

    override suspend fun updateLastSyncAt(memberId: String) = Unit

    override suspend fun setSyncEnabled(enabled: Boolean) = Unit

    override suspend fun recordSyncLog(syncLog: SyncLog) = Unit

    override fun getSyncLogs(): Flow<List<SyncLog>> = logs

    override suspend fun getLatestSyncLog(): SyncLog? = null

    override suspend fun resetLocalPreferences() = Unit
  }
}
