package com.woliveiras.petit.presentation.feature.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.backup.testing.DeterministicBackupStorageGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupCompatibility
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupDeletionFailureCategory
import com.woliveiras.petit.domain.usecase.backup.ManageBackupsUseCase
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
class SavedBackupsViewModelTest {
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
  fun authorizedLoadShowsSortedBackupsAndDetailsCompatibility() =
    runTest(dispatcher) {
      val gateway = DeterministicBackupStorageGateway()
      gateway.seed(metadata("older", "2026-07-17T10:00:00Z"), ByteArray(1))
      gateway.seed(metadata("newer", "2026-07-18T10:00:00Z").copy(schemaVersion = 2), ByteArray(1))
      val viewModel = viewModel(gateway)
      advanceUntilIdle()

      val ready = viewModel.uiState.value.content as SavedBackupsContent.Ready
      assertThat(ready.collection.backups.map { it.backupId })
        .containsExactly("newer", "older")
        .inOrder()

      viewModel.showDetails("remote-newer")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.details?.compatibility)
        .isEqualTo(BackupCompatibility.SCHEMA_VERSION_TOO_NEW)
    }

  @Test
  fun authorizationRequiredAndProviderErrorAreDistinctStates() =
    runTest(dispatcher) {
      val gateway =
        DeterministicBackupStorageGateway(
          initialAuthorization = BackupAuthorizationState.AuthorizationRequired
        )
      val viewModel = viewModel(gateway)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.content)
        .isEqualTo(SavedBackupsContent.AuthorizationRequired)
      assertThat(gateway.listCalls).isEqualTo(0)

      gateway.setAuthorization(BackupAuthorizationState.Authorized())
      gateway.failNext(
        DeterministicBackupStorageGateway.Operation.LIST,
        BackupProviderException.Retryable("offline"),
      )
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.content)
        .isEqualTo(SavedBackupsContent.Error(SavedBackupsErrorCategory.RETRYABLE))
    }

  @Test
  fun deletionRequiresConfirmationAndPartialFailureCanRetry() =
    runTest(dispatcher) {
      val gateway = DeterministicBackupStorageGateway()
      gateway.seed(metadata("one"), ByteArray(1))
      gateway.seed(metadata("two"), ByteArray(1))
      gateway.failDeleteForRemoteIdOnce("remote-two", BackupProviderException.Retryable("offline"))
      val viewModel = viewModel(gateway)
      advanceUntilIdle()

      viewModel.toggleSelection("remote-one")
      viewModel.toggleSelection("remote-two")
      viewModel.requestDeleteSelected()
      assertThat(gateway.deleteRequests).isEmpty()

      viewModel.confirmDeletion()
      advanceUntilIdle()

      val partial = viewModel.uiState.value.content as SavedBackupsContent.PartialDeletion
      assertThat(partial.result.deletedRemoteIds).containsExactly("remote-one")
      assertThat(partial.result.failures.single().category)
        .isEqualTo(BackupDeletionFailureCategory.RETRYABLE)
      assertThat(viewModel.uiState.value.selectedRemoteIds).containsExactly("remote-two")

      viewModel.retryFailedDeletion()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.content).isEqualTo(SavedBackupsContent.Empty)
      assertThat(gateway.deleteRequests)
        .containsExactly("remote-one", "remote-two", "remote-two")
        .inOrder()
    }

  @Test
  fun requestingDeletionFromDetailsClosesDetailsBeforeConfirmation() =
    runTest(dispatcher) {
      val gateway = DeterministicBackupStorageGateway()
      gateway.seed(metadata("one"), ByteArray(1))
      val viewModel = viewModel(gateway)
      advanceUntilIdle()
      viewModel.showDetails("remote-one")
      advanceUntilIdle()

      viewModel.requestDeleteOne("remote-one")

      assertThat(viewModel.uiState.value.details).isNull()
      assertThat(viewModel.uiState.value.deletionConfirmation)
        .isEqualTo(BackupDeletionConfirmation.Selected(setOf("remote-one")))
    }

  @Test
  fun revokedAuthorizationDuringMetadataLookupShowsReconnectState() =
    runTest(dispatcher) {
      val gateway = DeterministicBackupStorageGateway()
      gateway.seed(metadata("one"), ByteArray(1))
      val viewModel = viewModel(gateway)
      advanceUntilIdle()
      gateway.failNext(
        DeterministicBackupStorageGateway.Operation.GET,
        BackupProviderException.AuthorizationRequired(),
      )

      viewModel.showDetails("remote-one")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.content)
        .isEqualTo(SavedBackupsContent.AuthorizationRequired)
      assertThat(viewModel.uiState.value.details).isNull()
    }

  @Test
  fun disconnectPreservesRemoteFilesAndReconnectRestoresList() =
    runTest(dispatcher) {
      val gateway = DeterministicBackupStorageGateway()
      gateway.seed(metadata("one"), ByteArray(1))
      val viewModel = viewModel(gateway)
      advanceUntilIdle()

      viewModel.disconnect()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.content)
        .isEqualTo(SavedBackupsContent.AuthorizationRequired)
      assertThat(gateway.completedBackups().map { it.backupId }).containsExactly("one")
      assertThat(gateway.deleteRequests).isEmpty()

      viewModel.authorizeAndRefresh()
      advanceUntilIdle()

      val ready = viewModel.uiState.value.content as SavedBackupsContent.Ready
      assertThat(ready.collection.backups.map { it.backupId }).containsExactly("one")
    }

  private fun viewModel(gateway: DeterministicBackupStorageGateway) =
    SavedBackupsViewModel(
      gateway,
      ManageBackupsUseCase(
        storageGateway = gateway,
        supportedArchiveFormatVersion = 1,
        supportedSchemaVersion = 1,
      ),
    )

  private fun metadata(id: String, createdAt: String = "2026-07-18T10:00:00Z") =
    BackupMetadata(
      remoteId = "remote-$id",
      backupId = id,
      createdAt = Instant.parse(createdAt),
      trigger = BackupTrigger.MANUAL,
      appVersion = "1.0.0",
      archiveFormatVersion = 1,
      schemaVersion = 1,
      contentCounts = BackupContentCounts(pets = 1),
      archiveSizeBytes = 1,
      archiveSha256 = "sha256-$id",
    )
}
