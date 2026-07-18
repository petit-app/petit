package com.woliveiras.petit.presentation.feature.familygroup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.SyncLog
import com.woliveiras.petit.ui.theme.PetitTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test

class SyncHistoryComposeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun historyShowsPeerTypeAndCounts() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    val viewModel =
      SyncHistoryViewModel(
        FakeFamilyGroupRepository(
          listOf(
            SyncLog(
              id = "log-1",
              peerId = "peer-1",
              peerName = "Kitchen phone",
              syncTimestamp = 1L,
              entitiesSent = 2,
              entitiesReceived = 3,
              conflictsResolved = 1,
              syncType = "MERGE",
            )
          )
        )
      )

    composeRule.setContent {
      PetitTheme { SyncHistoryScreen(onNavigateBack = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Kitchen phone").assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.family_group_sync_type_merge), substring = true)
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.family_group_sync_history_counts, 2, 3, 1))
      .assertIsDisplayed()
  }

  @Test
  fun emptyHistoryShowsLocalizedEmptyState() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    val viewModel = SyncHistoryViewModel(FakeFamilyGroupRepository(emptyList()))

    composeRule.setContent {
      PetitTheme { SyncHistoryScreen(onNavigateBack = {}, viewModel = viewModel) }
    }

    composeRule
      .onNodeWithText(resources.getString(R.string.family_group_sync_history_empty))
      .assertIsDisplayed()
  }

  @Test
  fun loadingHasALocalizedAccessibleDescription() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    val viewModel = SyncHistoryViewModel(FakeFamilyGroupRepository(emptyFlow()))

    composeRule.setContent {
      PetitTheme { SyncHistoryScreen(onNavigateBack = {}, viewModel = viewModel) }
    }

    composeRule
      .onNodeWithContentDescription(resources.getString(R.string.family_group_sync_history_loading))
      .assertIsDisplayed()
  }

  @Test
  fun repositoryFailureShowsLocalizedErrorState() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    val viewModel =
      SyncHistoryViewModel(
        FakeFamilyGroupRepository(flow { throw IllegalStateException("forced failure") })
      )

    composeRule.setContent {
      PetitTheme { SyncHistoryScreen(onNavigateBack = {}, viewModel = viewModel) }
    }

    composeRule
      .onNodeWithText(resources.getString(R.string.family_group_sync_history_error))
      .assertIsDisplayed()
  }

  private class FakeFamilyGroupRepository(private val history: Flow<List<SyncLog>>) :
    FamilyGroupRepository {
    constructor(logs: List<SyncLog>) : this(MutableStateFlow(logs))

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

    override fun getSyncLogs(): Flow<List<SyncLog>> = history

    override suspend fun getLatestSyncLog(): SyncLog? = null

    override suspend fun resetLocalPreferences() = Unit
  }
}
