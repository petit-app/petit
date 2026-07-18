package com.woliveiras.petit.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupSettings
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.domain.backup.revision.BackupMutationKind
import com.woliveiras.petit.domain.backup.revision.RestorableRevision
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionRepository
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionState
import com.woliveiras.petit.worker.ChangeTriggeredBackupScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupTriggerCoordinatorTest {
  @Test
  fun startupKeepsDurableWorkAndNewerChangesRestartTheFiveMinuteDebounce() = runTest {
    val revisions = FakeRevisions(RestorableRevisionState(RestorableRevision(2)))
    val settings = FakeSettings(BackupSettings(automaticBackupEnabled = true))
    val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
    val scheduler = RecordingScheduler()
    val coordinator = BackupTriggerCoordinator(revisions, settings, authorization, scheduler)

    coordinator.start(backgroundScope)
    runCurrent()
    revisions.emit(RestorableRevisionState(RestorableRevision(3)))
    runCurrent()

    assertThat(scheduler.ensured).containsExactly(RestorableRevision(2))
    assertThat(scheduler.debounced).containsExactly(RestorableRevision(3))
  }

  @Test
  fun disabledOrUnauthorizedStateCancelsAndCompletionOnlyCoversCapturedRevision() = runTest {
    val revisions = FakeRevisions(RestorableRevisionState(RestorableRevision(3)))
    val settings = FakeSettings(BackupSettings(automaticBackupEnabled = true))
    val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
    val scheduler = RecordingScheduler()
    val coordinator = BackupTriggerCoordinator(revisions, settings, authorization, scheduler)
    coordinator.start(backgroundScope)
    runCurrent()

    coordinator.completed(RestorableRevision(2))
    runCurrent()
    assertThat(revisions.value.completed).isEqualTo(RestorableRevision(2))
    assertThat(scheduler.cancelCalls).isEqualTo(0)

    authorization.mutable.value = BackupAuthorizationState.AuthorizationRequired
    runCurrent()
    assertThat(scheduler.cancelCalls).isEqualTo(1)
  }

  @Test
  fun networkConstraintChangeReplacesPendingWorkWithoutADataMutation() = runTest {
    val revisions = FakeRevisions(RestorableRevisionState(RestorableRevision(1)))
    val settings = FakeSettings(BackupSettings(automaticBackupEnabled = true))
    val scheduler = RecordingScheduler()
    val coordinator =
      BackupTriggerCoordinator(
        revisions,
        settings,
        FakeAuthorization(BackupAuthorizationState.Authorized()),
        scheduler,
      )
    coordinator.start(backgroundScope)
    runCurrent()

    settings.updateNetworkRequirement(BackupNetworkRequirement.CONNECTED)
    runCurrent()

    assertThat(scheduler.debounced).containsExactly(RestorableRevision(1))
  }

  private class RecordingScheduler : ChangeTriggeredBackupScheduler {
    val ensured = mutableListOf<RestorableRevision>()
    val debounced = mutableListOf<RestorableRevision>()
    var cancelCalls = 0

    override fun ensureScheduled(
      revision: RestorableRevision,
      networkRequirement: BackupNetworkRequirement,
    ) {
      ensured += revision
    }

    override fun debounce(
      revision: RestorableRevision,
      networkRequirement: BackupNetworkRequirement,
    ) {
      debounced += revision
    }

    override fun cancel() {
      cancelCalls += 1
    }
  }

  private class FakeRevisions(initial: RestorableRevisionState) : RestorableRevisionRepository {
    private val mutable = MutableStateFlow(initial)
    val value: RestorableRevisionState
      get() = mutable.value

    override val state: Flow<RestorableRevisionState> = mutable

    fun emit(state: RestorableRevisionState) {
      mutable.value = state
    }

    override suspend fun recordCommittedMutation(kind: BackupMutationKind): RestorableRevision {
      if (kind.isRestorable) {
        mutable.value =
          mutable.value.copy(current = RestorableRevision(mutable.value.current.value + 1))
      }
      return mutable.value.current
    }

    override suspend fun markCompleted(revision: RestorableRevision): RestorableRevisionState {
      mutable.value = mutable.value.copy(completed = maxOf(mutable.value.completed, revision))
      return mutable.value
    }
  }

  private class FakeSettings(initial: BackupSettings) : BackupSettingsRepository {
    private val mutable = MutableStateFlow(initial)
    override val settings: Flow<BackupSettings> = mutable

    override suspend fun getSettings() = mutable.value

    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) {
      mutable.value = mutable.value.copy(automaticBackupEnabled = enabled)
    }

    override suspend fun updateNetworkRequirement(requirement: BackupNetworkRequirement) {
      mutable.value = mutable.value.copy(networkRequirement = requirement)
    }

    override suspend fun updateNotifyAfterSuccess(enabled: Boolean) {
      mutable.value = mutable.value.copy(notifyAfterSuccess = enabled)
    }
  }

  private class FakeAuthorization(initial: BackupAuthorizationState) : BackupAuthorizationGateway {
    val mutable = MutableStateFlow(initial)
    override val state: StateFlow<BackupAuthorizationState> = mutable

    override suspend fun authorize() = BackupAuthorizationResult.Authorized

    override suspend fun disconnect() {
      mutable.value = BackupAuthorizationState.Disconnected
    }
  }
}
