package com.woliveiras.petit.worker

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupSettings
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.revision.BackupMutationKind
import com.woliveiras.petit.domain.backup.revision.RestorableRevision
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionRepository
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionState
import com.woliveiras.petit.domain.usecase.backup.BackupCreationResult
import com.woliveiras.petit.domain.usecase.backup.BackupRevisionCompletion
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChangeTriggeredBackupRunnerTest {
  @Test
  fun retryKeepsNewerRevisionPendingAndSuccessCompletesOnlyTheCapturedTarget() = runTest {
    val revisions = FakeRevisions(RestorableRevisionState(RestorableRevision(3)))
    val completion = RecordingCompletion()
    val action =
      FakeAction(
        mutableListOf(
          BackupCreationResult.RetryableFailure("offline"),
          BackupCreationResult.Success(metadata()),
        )
      )
    val attempts = FakeAttempts()
    val runner = runner(action, attempts, revisions, completion)

    assertThat(runner.run("stable-attempt", RestorableRevision(2)))
      .isEqualTo(ChangeTriggeredBackupOutcome.RETRY)
    assertThat(runner.run("stable-attempt", RestorableRevision(2)))
      .isEqualTo(ChangeTriggeredBackupOutcome.SUCCESS)

    assertThat(action.ids).containsExactly("stable-attempt", "stable-attempt").inOrder()
    assertThat(completion.completed).containsExactly(RestorableRevision(2))
    assertThat(revisions.value.current).isEqualTo(RestorableRevision(3))
    assertThat(attempts.current.single().status).isEqualTo(BackupAttemptStatus.SUCCEEDED)
  }

  @Test
  fun disabledAndAuthorizationRequiredNeverStartAnUpload() = runTest {
    val revisions = FakeRevisions(RestorableRevisionState(RestorableRevision(1)))
    val action = FakeAction(mutableListOf())
    val attempts = FakeAttempts()
    val disabled =
      runner(
        action,
        attempts,
        revisions,
        RecordingCompletion(),
        BackupSettings(automaticBackupEnabled = false),
      )
    assertThat(disabled.run("disabled", RestorableRevision(1)))
      .isEqualTo(ChangeTriggeredBackupOutcome.SUCCESS)

    val unauthorized =
      runner(
        action,
        attempts,
        revisions,
        RecordingCompletion(),
        BackupSettings(automaticBackupEnabled = true),
        BackupAuthorizationState.AuthorizationRequired,
      )
    assertThat(unauthorized.run("unauthorized", RestorableRevision(1)))
      .isEqualTo(ChangeTriggeredBackupOutcome.FAILURE)
    assertThat(action.ids).isEmpty()
    assertThat(attempts.current.single().status)
      .isEqualTo(BackupAttemptStatus.AUTHORIZATION_REQUIRED)
  }

  @Test
  fun revisionCoveredWhileWaitingForTheExecutionGateDoesNotLeaveARunningAttempt() = runTest {
    val target = RestorableRevision(2)
    val revisions = CompletesBetweenChecksRevisions(target)
    val action = FakeAction(mutableListOf())
    val attempts = FakeAttempts()
    val runner =
      ChangeTriggeredBackupRunner(
        action,
        attempts,
        FakeSettings(BackupSettings(automaticBackupEnabled = true)),
        FakeAuthorization(BackupAuthorizationState.Authorized()),
        revisions,
        RecordingCompletion(),
        Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneOffset.UTC),
      )

    val outcome = runner.run("coalesced-attempt", target)

    assertThat(outcome).isEqualTo(ChangeTriggeredBackupOutcome.SUCCESS)
    assertThat(action.ids).isEmpty()
    assertThat(attempts.current).isEmpty()
  }

  @Test
  fun silentRefreshDetectsExternalRevocationBeforePreparingABackup() = runTest {
    val revisions = FakeRevisions(RestorableRevisionState(RestorableRevision(1)))
    val action = FakeAction(mutableListOf())
    val attempts = FakeAttempts()
    val authorization =
      FakeAuthorization(
        initial = BackupAuthorizationState.Authorized(),
        refreshed = BackupAuthorizationState.AuthorizationRequired,
      )
    val runner =
      ChangeTriggeredBackupRunner(
        action,
        attempts,
        FakeSettings(BackupSettings(automaticBackupEnabled = true)),
        authorization,
        revisions,
        RecordingCompletion(),
        Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneOffset.UTC),
      )

    val outcome = runner.run("revoked-attempt", RestorableRevision(1))

    assertThat(outcome).isEqualTo(ChangeTriggeredBackupOutcome.FAILURE)
    assertThat(action.ids).isEmpty()
    assertThat(attempts.current.single().status)
      .isEqualTo(BackupAttemptStatus.AUTHORIZATION_REQUIRED)
    assertThat(authorization.refreshCalls).isEqualTo(1)
  }

  @Test
  fun temporarilyUnavailableSilentRefreshRetriesWithoutPreparingABackup() = runTest {
    val revisions = FakeRevisions(RestorableRevisionState(RestorableRevision(1)))
    val action = FakeAction(mutableListOf())
    val attempts = FakeAttempts()
    val authorization =
      FakeAuthorization(
        initial = BackupAuthorizationState.Authorized(),
        refreshed = BackupAuthorizationState.Unavailable(),
      )
    val runner =
      ChangeTriggeredBackupRunner(
        action,
        attempts,
        FakeSettings(BackupSettings(automaticBackupEnabled = true)),
        authorization,
        revisions,
        RecordingCompletion(),
        Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneOffset.UTC),
      )

    val outcome = runner.run("temporarily-unavailable", RestorableRevision(1))

    assertThat(outcome).isEqualTo(ChangeTriggeredBackupOutcome.RETRY)
    assertThat(action.ids).isEmpty()
    assertThat(attempts.current.single().status).isEqualTo(BackupAttemptStatus.RETRYING)
    assertThat(attempts.current.single().completedAt).isNull()
  }

  private fun runner(
    action: FakeAction,
    attempts: FakeAttempts,
    revisions: FakeRevisions,
    completion: RecordingCompletion,
    settings: BackupSettings = BackupSettings(automaticBackupEnabled = true),
    authorization: BackupAuthorizationState = BackupAuthorizationState.Authorized(),
  ) =
    ChangeTriggeredBackupRunner(
      action,
      attempts,
      FakeSettings(settings),
      FakeAuthorization(authorization),
      revisions,
      completion,
      Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneOffset.UTC),
    )

  private fun metadata() =
    BackupMetadata(
      remoteId = "remote-1",
      backupId = "stable-attempt",
      createdAt = Instant.parse("2026-07-18T08:00:00Z"),
      trigger = BackupTrigger.DATA_CHANGE,
      appVersion = "1.0",
      archiveFormatVersion = 1,
      schemaVersion = 1,
      contentCounts = BackupContentCounts(pets = 1),
      archiveSizeBytes = 1,
      archiveSha256 = "checksum",
    )

  private class FakeAction(private val results: MutableList<BackupCreationResult>) :
    CreateBackupAction {
    val ids = mutableListOf<String>()

    override suspend fun execute(
      backupId: String,
      trigger: BackupTrigger,
      onProgress: (BackupProgress) -> Unit,
    ): BackupCreationResult {
      ids += backupId
      return results.removeFirst()
    }
  }

  private class FakeAttempts : BackupAttemptRepository {
    private val mutable = MutableStateFlow<List<BackupAttempt>>(emptyList())
    val current: List<BackupAttempt>
      get() = mutable.value

    override val attempts: Flow<List<BackupAttempt>> = mutable

    override suspend fun getAttempt(id: String) = mutable.value.firstOrNull { it.id == id }

    override suspend fun upsert(attempt: BackupAttempt) {
      mutable.value = mutable.value.filterNot { it.id == attempt.id } + attempt
    }
  }

  private class RecordingCompletion : BackupRevisionCompletion {
    val completed = mutableListOf<RestorableRevision>()

    override suspend fun capture() = RestorableRevision(0)

    override suspend fun completed(revision: RestorableRevision) {
      completed += revision
    }
  }

  private class FakeRevisions(initial: RestorableRevisionState) : RestorableRevisionRepository {
    private val mutable = MutableStateFlow(initial)
    val value: RestorableRevisionState
      get() = mutable.value

    override val state: Flow<RestorableRevisionState> = mutable

    override suspend fun recordCommittedMutation(kind: BackupMutationKind) = mutable.value.current

    override suspend fun markCompleted(revision: RestorableRevision) = mutable.value
  }

  private class CompletesBetweenChecksRevisions(private val target: RestorableRevision) :
    RestorableRevisionRepository {
    private var reads = 0
    override val state: Flow<RestorableRevisionState>
      get() {
        reads += 1
        return flowOf(
          RestorableRevisionState(
            current = target,
            completed = if (reads == 1) RestorableRevision(0) else target,
          )
        )
      }

    override suspend fun recordCommittedMutation(kind: BackupMutationKind) = target

    override suspend fun markCompleted(revision: RestorableRevision) =
      RestorableRevisionState(target, target)
  }

  private class FakeSettings(initial: BackupSettings) : BackupSettingsRepository {
    private val mutable = MutableStateFlow(initial)
    override val settings: Flow<BackupSettings> = mutable

    override suspend fun getSettings() = mutable.value

    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) = Unit

    override suspend fun updateNetworkRequirement(requirement: BackupNetworkRequirement) = Unit

    override suspend fun updateNotifyAfterSuccess(enabled: Boolean) = Unit
  }

  private class FakeAuthorization(
    initial: BackupAuthorizationState,
    private val refreshed: BackupAuthorizationState = initial,
  ) : BackupAuthorizationGateway {
    private val mutable = MutableStateFlow(initial)
    override val state: StateFlow<BackupAuthorizationState> = mutable
    var refreshCalls = 0

    override suspend fun refresh(): BackupAuthorizationState {
      refreshCalls += 1
      mutable.value = refreshed
      return refreshed
    }

    override suspend fun authorize() = BackupAuthorizationResult.Authorized

    override suspend fun disconnect() = Unit
  }
}
