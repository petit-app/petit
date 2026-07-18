package com.woliveiras.petit.presentation.feature.familygroup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.SyncLog
import com.woliveiras.petit.ui.theme.PetitTheme
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

class FamilyGroupComposeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun membersShowLocalIdentityKnownSyncAndNeverSyncedStates() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    val syncedAt = 20L
    val repository =
      FakeRepository(
        FamilyGroupInfo(
          "group-key",
          listOf(
            member("local", "This tablet", true, null),
            member("remote", "Kitchen phone", false, syncedAt),
          ),
          1L,
        )
      )

    composeRule.setContent {
      PetitTheme { FamilyGroupScreen({}, {}, {}, {}, {}, FamilyGroupViewModel(repository)) }
    }

    composeRule.onNodeWithText("This tablet").assertIsDisplayed()
    composeRule
      .onNodeWithText(resources.getString(R.string.family_group_never_synced))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        resources.getString(
          R.string.family_group_member_last_sync,
          DateFormat.getDateTimeInstance().format(Date(syncedAt)),
        )
      )
      .assertIsDisplayed()
  }

  @Test
  fun localDeviceCanBeRenamedFromAnAccessibleAction() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    val repository =
      FakeRepository(
        FamilyGroupInfo("group-key", listOf(member("local", "Old name", true, null)), 1L)
      )
    composeRule.setContent {
      PetitTheme { FamilyGroupScreen({}, {}, {}, {}, {}, FamilyGroupViewModel(repository)) }
    }

    composeRule
      .onNodeWithContentDescription(resources.getString(R.string.family_group_rename_device))
      .performClick()
    val nameField = composeRule.onAllNodes(hasSetTextAction())[0]
    nameField.performTextClearance()
    nameField.performTextInput("Kitchen tablet")
    composeRule.onNodeWithText(resources.getString(R.string.action_confirm)).performClick()

    composeRule.waitUntil { repository.renamedTo == "Kitchen tablet" }
    assertThat(repository.renamedTo).isEqualTo("Kitchen tablet")
  }

  @Test
  fun missingGroupShowsTheLocalizedEmptyState() {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    composeRule.setContent {
      PetitTheme {
        FamilyGroupScreen({}, {}, {}, {}, {}, FamilyGroupViewModel(FakeRepository(null)))
      }
    }

    composeRule
      .onNodeWithText(resources.getString(R.string.family_group_no_devices))
      .assertIsDisplayed()
  }

  private fun member(id: String, name: String, local: Boolean, lastSyncAt: Long?) =
    FamilyGroupMember(id, name, "group-key", local, lastSyncAt, 1L, 1L)

  private class FakeRepository(info: FamilyGroupInfo?) : FamilyGroupRepository {
    override val familyGroupInfo: Flow<FamilyGroupInfo?> = MutableStateFlow(info)
    override val localDevice: Flow<FamilyGroupMember?> = MutableStateFlow(null)
    override val isSyncEnabled: Flow<Boolean> = MutableStateFlow(true)
    @Volatile var renamedTo: String? = null

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
