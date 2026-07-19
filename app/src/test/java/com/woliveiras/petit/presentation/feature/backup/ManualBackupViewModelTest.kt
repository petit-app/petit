package com.woliveiras.petit.presentation.feature.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupCreationResult
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualBackupViewModelTest {
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
  fun foregroundAuthorizationStartsBackupWithoutAnyAccountOrPlanInput() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization()
      val action = FakeCreateBackupAction()
      val viewModel = ManualBackupViewModel(authorization, action) { "backup-1" }

      viewModel.authorizeAndBackUp()
      advanceUntilIdle()

      assertThat(authorization.authorizeCalls).isEqualTo(1)
      assertThat(action.calls).containsExactly("backup-1")
      assertThat(viewModel.uiState.value.operation)
        .isInstanceOf(ManualBackupOperation.Complete::class.java)
    }

  @Test
  fun retryableFailureReusesBackupIdAndProgressIsExposed() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
      val action = FakeCreateBackupAction()
      action.results += BackupCreationResult.RetryableFailure("offline")
      action.results += BackupCreationResult.Success(metadata("backup-1"))
      val viewModel = ManualBackupViewModel(authorization, action) { "backup-1" }

      viewModel.backUpNow()
      advanceUntilIdle()
      viewModel.backUpNow()
      advanceUntilIdle()

      assertThat(action.calls).containsExactly("backup-1", "backup-1").inOrder()
      assertThat(action.progressCallbacks).isEqualTo(2)
      assertThat(viewModel.uiState.value.operation)
        .isInstanceOf(ManualBackupOperation.Complete::class.java)
    }

  @Test
  fun cancelledAuthorizationRemainsActionable() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization()
      authorization.nextResult = BackupAuthorizationResult.Cancelled
      val action = FakeCreateBackupAction()
      val viewModel = ManualBackupViewModel(authorization, action)

      viewModel.authorizeAndBackUp()
      advanceUntilIdle()

      assertThat(action.calls).isEmpty()
      assertThat(viewModel.uiState.value.operation)
        .isEqualTo(ManualBackupOperation.AuthorizationRequired)
    }

  @Test
  fun revokedAuthorizationAfterCompletionReturnsToAnActionableState() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
      val viewModel = ManualBackupViewModel(authorization, FakeCreateBackupAction()) { "backup-1" }
      viewModel.backUpNow()
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.operation)
        .isInstanceOf(ManualBackupOperation.Complete::class.java)

      authorization.setState(BackupAuthorizationState.AuthorizationRequired)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.operation)
        .isEqualTo(ManualBackupOperation.AuthorizationRequired)
    }

  @Test
  fun unexpectedActionFailureLeavesCreatingWithSafeErrorState() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
      val action = FakeCreateBackupAction()
      action.failure = IllegalStateException("private path")
      val viewModel = ManualBackupViewModel(authorization, action) { "backup-1" }

      viewModel.backUpNow()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.operation)
        .isEqualTo(ManualBackupOperation.PermanentFailure("Backup could not be completed"))
      assertThat(viewModel.uiState.value.operation.toString()).doesNotContain("private path")
    }

  private class FakeAuthorization(
    initial: BackupAuthorizationState = BackupAuthorizationState.AuthorizationRequired
  ) : BackupAuthorizationGateway {
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<BackupAuthorizationState> = mutableState
    var authorizeCalls = 0
    var nextResult: BackupAuthorizationResult = BackupAuthorizationResult.Authorized

    override suspend fun authorize(): BackupAuthorizationResult {
      authorizeCalls += 1
      if (nextResult == BackupAuthorizationResult.Authorized) {
        mutableState.value = BackupAuthorizationState.Authorized()
      }
      return nextResult
    }

    override suspend fun disconnect() {
      mutableState.value = BackupAuthorizationState.Disconnected
    }

    fun setState(state: BackupAuthorizationState) {
      mutableState.value = state
    }
  }

  private class FakeCreateBackupAction : CreateBackupAction {
    val calls = mutableListOf<String>()
    val results = ArrayDeque<BackupCreationResult>()
    var progressCallbacks = 0
    var failure: Exception? = null

    override suspend fun execute(
      backupId: String,
      trigger: BackupTrigger,
      onProgress: (BackupProgress) -> Unit,
    ): BackupCreationResult {
      calls += backupId
      failure?.let { throw it }
      onProgress(BackupProgress(1, 1))
      progressCallbacks += 1
      return results.removeFirstOrNull() ?: BackupCreationResult.Success(metadata(backupId))
    }
  }

  companion object {
    private fun metadata(backupId: String) =
      BackupMetadata(
        remoteId = "remote-$backupId",
        backupId = backupId,
        createdAt = Instant.parse("2026-07-18T10:00:00Z"),
        trigger = BackupTrigger.MANUAL,
        appVersion = "1.0.0",
        archiveFormatVersion = 1,
        schemaVersion = 1,
        contentCounts = BackupContentCounts(pets = 1),
        archiveSizeBytes = 1,
        archiveSha256 = "sha256",
      )
  }
}
