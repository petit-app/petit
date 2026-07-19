package com.woliveiras.petit.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupSettings
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.worker.BackupScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BackupSettingsCoordinatorTest {
  @Test
  fun enableNetworkChangeAndDisableReconcileOneAuthoritativeSchedule() = runTest {
    val repository = FakeBackupSettingsRepository()
    val scheduler = RecordingBackupScheduler()
    val coordinator = BackupSettingsCoordinator(repository, scheduler)

    coordinator.setAutomaticBackupEnabled(true)
    coordinator.setNetworkRequirement(BackupNetworkRequirement.CONNECTED)
    coordinator.setAutomaticBackupEnabled(false)

    assertThat(scheduler.scheduled)
      .containsExactly(BackupNetworkRequirement.UNMETERED, BackupNetworkRequirement.CONNECTED)
      .inOrder()
    assertThat(scheduler.cancelCalls).isEqualTo(1)
    assertThat(repository.state.value)
      .isEqualTo(
        BackupSettings(
          automaticBackupEnabled = false,
          networkRequirement = BackupNetworkRequirement.CONNECTED,
        )
      )
  }

  @Test
  fun scheduleFailureRollsBackThePersistedPreferenceAndPreviousSchedule() = runTest {
    val repository = FakeBackupSettingsRepository()
    val scheduler = RecordingBackupScheduler(failNextSchedule = true)
    val coordinator = BackupSettingsCoordinator(repository, scheduler)

    val result = runCatching { coordinator.setAutomaticBackupEnabled(true) }

    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    assertThat(repository.state.value).isEqualTo(BackupSettings())
    assertThat(scheduler.cancelCalls).isEqualTo(1)
  }

  @Test
  fun disconnectDisablesAutomaticBackupBeforeRevokingAuthorization() = runTest {
    val events = mutableListOf<String>()
    val repository = FakeBackupSettingsRepository(BackupSettings(automaticBackupEnabled = true))
    val scheduler = RecordingBackupScheduler(onCancel = { events += "schedule-cancelled" })
    val authorization = RecordingAuthorizationGateway(events)
    val coordinator =
      BackupConnectionCoordinator(authorization, BackupSettingsCoordinator(repository, scheduler))

    coordinator.disconnect()

    assertThat(repository.state.value.automaticBackupEnabled).isFalse()
    assertThat(events).containsExactly("schedule-cancelled", "authorization-revoked").inOrder()
    assertThat(authorization.disconnectCalls).isEqualTo(1)
  }

  @Test
  fun refreshDelegatesToTheAuthorizationGateway() = runTest {
    val authorization = RecordingAuthorizationGateway(mutableListOf())
    val coordinator =
      BackupConnectionCoordinator(
        authorization,
        BackupSettingsCoordinator(FakeBackupSettingsRepository(), RecordingBackupScheduler()),
      )

    assertThat(coordinator.refresh()).isInstanceOf(BackupAuthorizationState.Authorized::class.java)
    assertThat(authorization.refreshCalls).isEqualTo(1)
  }

  internal class FakeBackupSettingsRepository(initial: BackupSettings = BackupSettings()) :
    BackupSettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<BackupSettings> = state

    override suspend fun getSettings(): BackupSettings = state.value

    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) {
      state.value = state.value.copy(automaticBackupEnabled = enabled)
    }

    override suspend fun updateNetworkRequirement(requirement: BackupNetworkRequirement) {
      state.value = state.value.copy(networkRequirement = requirement)
    }

    override suspend fun updateNotifyAfterSuccess(enabled: Boolean) {
      state.value = state.value.copy(notifyAfterSuccess = enabled)
    }
  }

  internal class RecordingBackupScheduler(
    private var failNextSchedule: Boolean = false,
    private val onCancel: () -> Unit = {},
  ) : BackupScheduler {
    val scheduled = mutableListOf<BackupNetworkRequirement>()
    var cancelCalls = 0

    override fun schedulePeriodic(networkRequirement: BackupNetworkRequirement) {
      if (failNextSchedule) {
        failNextSchedule = false
        error("WorkManager unavailable")
      }
      scheduled += networkRequirement
    }

    override fun cancelPeriodic() {
      cancelCalls += 1
      onCancel()
    }
  }

  private class RecordingAuthorizationGateway(private val events: MutableList<String>) :
    BackupAuthorizationGateway {
    private val mutableState =
      MutableStateFlow<BackupAuthorizationState>(BackupAuthorizationState.Authorized())
    override val state: StateFlow<BackupAuthorizationState> = mutableState
    var refreshCalls = 0
    var disconnectCalls = 0

    override suspend fun refresh(): BackupAuthorizationState {
      refreshCalls += 1
      return mutableState.value
    }

    override suspend fun authorize(): BackupAuthorizationResult =
      BackupAuthorizationResult.Authorized

    override suspend fun disconnect() {
      disconnectCalls += 1
      events += "authorization-revoked"
      mutableState.value = BackupAuthorizationState.Disconnected
    }
  }
}
