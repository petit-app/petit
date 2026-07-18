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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyGroupViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private lateinit var repository: FakeFamilyGroupRepository
  private lateinit var viewModel: FamilyGroupViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repository = FakeFamilyGroupRepository()
    viewModel = FamilyGroupViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun exposesKnownAndNeverSyncedMembersAndRenamesThroughTheRepository() =
    runTest(dispatcher) {
      repository.group.value =
        FamilyGroupInfo(
          "group-key",
          listOf(
            member("local", "This device", true, null),
            member("remote", "Kitchen phone", false, 20L),
          ),
          1L,
        )
      advanceUntilIdle()

      viewModel.renameLocalDevice("Kitchen tablet")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.familyGroupInfo?.members).hasSize(2)
      assertThat(viewModel.uiState.value.familyGroupInfo?.members?.first()?.lastSyncAt).isNull()
      assertThat(viewModel.uiState.value.familyGroupInfo?.members?.last()?.lastSyncAt)
        .isEqualTo(20L)
      assertThat(repository.renamedTo).isEqualTo("Kitchen tablet")
    }

  private fun member(id: String, name: String, local: Boolean, lastSyncAt: Long?) =
    FamilyGroupMember(id, name, "group-key", local, lastSyncAt, 1L, 1L)

  private class FakeFamilyGroupRepository : FamilyGroupRepository {
    val group = MutableStateFlow<FamilyGroupInfo?>(null)
    var renamedTo: String? = null
    override val familyGroupInfo: Flow<FamilyGroupInfo?> = group
    override val localDevice: Flow<FamilyGroupMember?> = MutableStateFlow(null)
    override val isSyncEnabled: Flow<Boolean> = MutableStateFlow(true)

    override suspend fun getFamilyGroupKey(): String? = "group-key"

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

    override suspend fun renameLocalDevice(deviceName: String) {
      renamedTo = deviceName
    }

    override suspend fun updateLastSyncAt(memberId: String) = Unit

    override suspend fun setSyncEnabled(enabled: Boolean) = Unit

    override suspend fun recordSyncLog(syncLog: SyncLog) = Unit

    override fun getSyncLogs(): Flow<List<SyncLog>> = emptyFlow()

    override suspend fun getLatestSyncLog(): SyncLog? = null

    override suspend fun resetLocalPreferences() = Unit
  }
}
