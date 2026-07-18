package com.woliveiras.petit.presentation.feature.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.restore.RestoreBackupAction
import com.woliveiras.petit.domain.backup.restore.RestoreBackupRequest
import com.woliveiras.petit.domain.backup.restore.RestoreBackupResult
import com.woliveiras.petit.domain.backup.restore.RestoreMode
import com.woliveiras.petit.domain.model.MergeResult
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreBackupViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before fun setUp() = Dispatchers.setMain(dispatcher)

  @After fun tearDown() = Dispatchers.resetMain()

  @Test
  fun replaceRequiresConfirmationAndReportsMonotonicProgressAndSuccess() =
    runTest(dispatcher) {
      val action = RecordingRestoreAction()
      val viewModel = RestoreBackupViewModel("remote-1", action)

      viewModel.selectMode(RestoreMode.REPLACE)
      viewModel.requestRestore()
      assertThat(viewModel.uiState.value.showReplaceConfirmation).isTrue()
      assertThat(action.requests).isEmpty()

      viewModel.confirmReplace()
      advanceUntilIdle()

      assertThat(action.requests.single())
        .isEqualTo(RestoreBackupRequest("remote-1", RestoreMode.REPLACE))
      assertThat(viewModel.uiState.value.operation)
        .isInstanceOf(RestoreOperation.Success::class.java)
      assertThat(viewModel.uiState.value.lastProgress)
        .isEqualTo(BackupProgress(bytesTransferred = 4, totalBytes = 4))
    }

  @Test
  fun mergeCanApplyPreferencesExplicitlyAndAuthorizationFailureIsActionable() =
    runTest(dispatcher) {
      val action = RecordingRestoreAction(failure = BackupProviderException.AuthorizationRequired())
      val viewModel = RestoreBackupViewModel("remote-2", action)

      viewModel.setApplyBackupPreferences(true)
      viewModel.requestRestore()
      advanceUntilIdle()

      assertThat(action.requests.single())
        .isEqualTo(
          RestoreBackupRequest(
            remoteId = "remote-2",
            mode = RestoreMode.MERGE,
            applyBackupPreferences = true,
          )
        )
      assertThat(viewModel.uiState.value.operation)
        .isEqualTo(RestoreOperation.AuthorizationRequired)
    }

  private class RecordingRestoreAction(private val failure: Exception? = null) :
    RestoreBackupAction {
    val requests = mutableListOf<RestoreBackupRequest>()

    override suspend fun execute(
      request: RestoreBackupRequest,
      onProgress: (BackupProgress) -> Unit,
    ): RestoreBackupResult {
      requests += request
      failure?.let { throw it }
      onProgress(BackupProgress(2, 4))
      onProgress(BackupProgress(1, 4))
      onProgress(BackupProgress(4, 4))
      return RestoreBackupResult(
        BackupMetadata(
          remoteId = request.remoteId,
          backupId = "backup-1",
          createdAt = Instant.EPOCH,
          trigger = BackupTrigger.MANUAL,
          appVersion = "1.0",
          archiveFormatVersion = 1,
          schemaVersion = 1,
          contentCounts = BackupContentCounts(pets = 1),
          archiveSizeBytes = 4,
          archiveSha256 = "checksum",
        ),
        MergeResult(),
      )
    }
  }
}
