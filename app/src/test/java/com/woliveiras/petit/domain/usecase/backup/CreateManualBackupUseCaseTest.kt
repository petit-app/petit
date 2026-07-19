package com.woliveiras.petit.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.backup.testing.ByteArrayBackupContent
import com.woliveiras.petit.data.backup.testing.DeterministicBackupStorageGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupContent
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Instant
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateManualBackupUseCaseTest {
  @Test
  fun authorizationRequiredDoesNotCreateLocalArchive() = runTest {
    val gateway =
      DeterministicBackupStorageGateway(
        initialAuthorization = BackupAuthorizationState.AuthorizationRequired
      )
    val preparer = RecordingArchivePreparer()
    val action = CreateManualBackupUseCase(gateway, gateway, preparer)

    val result = action.execute("backup-1", BackupTrigger.MANUAL)

    assertThat(result).isEqualTo(BackupCreationResult.AuthorizationRequired)
    assertThat(preparer.prepareCalls).isEqualTo(0)
  }

  @Test
  fun silentRefreshDetectsExternalRevocationBeforeCreatingLocalArchive() = runTest {
    val storage = DeterministicBackupStorageGateway()
    val authorization = RevokedOnRefreshAuthorizationGateway()
    val preparer = RecordingArchivePreparer()
    val action = CreateManualBackupUseCase(authorization, storage, preparer)

    val result = action.execute("backup-1", BackupTrigger.MANUAL)

    assertThat(result).isEqualTo(BackupCreationResult.AuthorizationRequired)
    assertThat(authorization.refreshCalls).isEqualTo(1)
    assertThat(preparer.prepareCalls).isEqualTo(0)
    assertThat(storage.completedBackups()).isEmpty()
  }

  @Test
  fun silentRefreshUnavailabilityIsRetryableWithoutCreatingLocalArchive() = runTest {
    val storage = DeterministicBackupStorageGateway()
    val authorization = FixedRefreshAuthorizationGateway(BackupAuthorizationState.Unavailable())
    val preparer = RecordingArchivePreparer()
    val action = CreateManualBackupUseCase(authorization, storage, preparer)

    val result = action.execute("backup-1", BackupTrigger.AUTOMATIC)

    assertThat(result).isInstanceOf(BackupCreationResult.RetryableFailure::class.java)
    assertThat(preparer.prepareCalls).isEqualTo(0)
    assertThat(storage.completedBackups()).isEmpty()
  }

  @Test
  fun successfulUploadReportsMonotonicProgressAndAlwaysCleansStaging() = runTest {
    val gateway = DeterministicBackupStorageGateway(uploadChunkSize = 2)
    val preparer = RecordingArchivePreparer()
    val progress = mutableListOf<com.woliveiras.petit.domain.backup.BackupProgress>()
    val action = CreateManualBackupUseCase(gateway, gateway, preparer)

    val result = action.execute("backup-1", BackupTrigger.MANUAL) { progress += it }

    assertThat(result).isInstanceOf(BackupCreationResult.Success::class.java)
    assertThat(progress.map { it.bytesTransferred }).isInOrder()
    assertThat(progress.map { it.totalBytes }.distinct()).containsExactly(7L)
    assertThat(progress.last().bytesTransferred).isEqualTo(7L)
    assertThat(preparer.lastPrepared?.closed).isTrue()
  }

  @Test
  fun interruptionIsRetryableAndDoesNotPublishPartialBackup() = runTest {
    val gateway = DeterministicBackupStorageGateway(uploadChunkSize = 2)
    gateway.interruptNextUploadAfter(3)
    val preparer = RecordingArchivePreparer()
    val action = CreateManualBackupUseCase(gateway, gateway, preparer)

    val result = action.execute("backup-1", BackupTrigger.MANUAL)

    assertThat(result).isInstanceOf(BackupCreationResult.RetryableFailure::class.java)
    assertThat(gateway.completedBackups()).isEmpty()
    assertThat(preparer.lastPrepared?.closed).isTrue()
  }

  @Test
  fun providerFailuresAreClassifiedWithoutLeakingProviderTypes() = runTest {
    val cases =
      listOf(
        BackupProviderException.AuthorizationRequired() to
          BackupCreationResult.AuthorizationRequired::class.java,
        BackupProviderException.QuotaExceeded() to BackupCreationResult.QuotaExceeded::class.java,
        BackupProviderException.Retryable("offline") to
          BackupCreationResult.RetryableFailure::class.java,
        BackupProviderException.Permanent("rejected") to
          BackupCreationResult.PermanentFailure::class.java,
      )

    cases.forEachIndexed { index, (failure, expectedType) ->
      val gateway = DeterministicBackupStorageGateway()
      gateway.failNext(DeterministicBackupStorageGateway.Operation.UPLOAD, failure)
      val action = CreateManualBackupUseCase(gateway, gateway, RecordingArchivePreparer())

      assertThat(action.execute("backup-$index", BackupTrigger.MANUAL)).isInstanceOf(expectedType)
    }
  }

  @Test
  fun callerSuppliedBackupIdMakesRetryAfterLostResponseIdempotent() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.failNextUploadAfterCommit(BackupProviderException.Retryable("response lost"))
    val action = CreateManualBackupUseCase(gateway, gateway, RecordingArchivePreparer())

    val first = action.execute("stable-id", BackupTrigger.MANUAL)
    val retry = action.execute("stable-id", BackupTrigger.MANUAL)

    assertThat(first).isInstanceOf(BackupCreationResult.RetryableFailure::class.java)
    assertThat(retry).isInstanceOf(BackupCreationResult.Success::class.java)
    assertThat(gateway.completedBackups().map { it.backupId }).containsExactly("stable-id")
  }

  @Test
  fun retryAfterLostResponseAcceptsTheCompletedBackupForTheStableId() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.failNextUploadAfterCommit(BackupProviderException.Retryable("response lost"))
    val preparer =
      RecordingArchivePreparer(
        bytesByAttempt =
          ArrayDeque(listOf("first".encodeToByteArray(), "changed".encodeToByteArray()))
      )
    val action = CreateManualBackupUseCase(gateway, gateway, preparer)

    val first = action.execute("stable-id", BackupTrigger.MANUAL)
    val retry = action.execute("stable-id", BackupTrigger.MANUAL)

    assertThat(first).isInstanceOf(BackupCreationResult.RetryableFailure::class.java)
    assertThat(retry).isInstanceOf(BackupCreationResult.Success::class.java)
    assertThat((retry as BackupCreationResult.Success).metadata.archiveSizeBytes).isEqualTo(5)
    assertThat(gateway.completedBackups().map { it.backupId }).containsExactly("stable-id")
  }

  @Test
  fun cancellationPropagatesAfterClosingPreparedArchive() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    val preparer = RecordingArchivePreparer(cancelOnRead = true)
    val action = CreateManualBackupUseCase(gateway, gateway, preparer)

    val failure = runCatching { action.execute("backup-1", BackupTrigger.MANUAL) }.exceptionOrNull()

    assertThat(failure).isInstanceOf(CancellationException::class.java)
    assertThat(preparer.lastPrepared?.closed).isTrue()
    assertThat(gateway.completedBackups()).isEmpty()
  }

  @Test
  fun unexpectedPreparationFailureBecomesNonSensitivePermanentFailure() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    val action =
      CreateManualBackupUseCase(
        gateway,
        gateway,
        RecordingArchivePreparer(prepareFailure = IllegalStateException("private path")),
      )

    val result = action.execute("backup-1", BackupTrigger.MANUAL)

    assertThat(result)
      .isEqualTo(BackupCreationResult.PermanentFailure("Backup could not be completed"))
    assertThat(result.toString()).doesNotContain("private path")
    assertThat(gateway.completedBackups()).isEmpty()
  }

  @Test
  fun cancellationDuringPreparationPropagatesWithoutBecomingFailure() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    val action =
      CreateManualBackupUseCase(
        gateway,
        gateway,
        RecordingArchivePreparer(prepareFailure = CancellationException("cancelled")),
      )

    val failure = runCatching { action.execute("backup-1", BackupTrigger.MANUAL) }.exceptionOrNull()

    assertThat(failure).isInstanceOf(CancellationException::class.java)
    assertThat(gateway.completedBackups()).isEmpty()
  }

  @Test
  fun inconsistentProviderProgressCannotBeReportedAsSuccess() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.reportNextUploadTotalBytes(8)
    val preparer = RecordingArchivePreparer()
    val action = CreateManualBackupUseCase(gateway, gateway, preparer)

    val result = action.execute("backup-1", BackupTrigger.MANUAL)

    assertThat(result)
      .isEqualTo(BackupCreationResult.PermanentFailure("Backup could not be completed"))
    assertThat(preparer.lastPrepared?.closed).isTrue()
  }

  private class RecordingArchivePreparer(
    private val cancelOnRead: Boolean = false,
    private val prepareFailure: Exception? = null,
    private val bytesByAttempt: ArrayDeque<ByteArray> = ArrayDeque(),
  ) : BackupArchivePreparer {
    var prepareCalls = 0
    var lastPrepared: Prepared? = null

    override suspend fun prepare(backupId: String, trigger: BackupTrigger): PreparedBackupArchive {
      prepareCalls += 1
      prepareFailure?.let { throw it }
      val bytes = bytesByAttempt.removeFirstOrNull() ?: "archive".encodeToByteArray()
      return Prepared(backupId, trigger, cancelOnRead, bytes).also { lastPrepared = it }
    }
  }

  private class RevokedOnRefreshAuthorizationGateway : BackupAuthorizationGateway {
    private val mutableState =
      MutableStateFlow<BackupAuthorizationState>(BackupAuthorizationState.Authorized())
    override val state: StateFlow<BackupAuthorizationState> = mutableState
    var refreshCalls = 0

    override suspend fun refresh(): BackupAuthorizationState {
      refreshCalls += 1
      return BackupAuthorizationState.AuthorizationRequired.also { mutableState.value = it }
    }

    override suspend fun authorize(): BackupAuthorizationResult =
      BackupAuthorizationResult.Authorized

    override suspend fun disconnect() {
      mutableState.value = BackupAuthorizationState.Disconnected
    }
  }

  private class FixedRefreshAuthorizationGateway(initialState: BackupAuthorizationState) :
    BackupAuthorizationGateway {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<BackupAuthorizationState> = mutableState

    override suspend fun refresh(): BackupAuthorizationState = mutableState.value

    override suspend fun authorize(): BackupAuthorizationResult =
      BackupAuthorizationResult.Unavailable()

    override suspend fun disconnect() {
      mutableState.value = BackupAuthorizationState.Disconnected
    }
  }

  private class Prepared(
    backupId: String,
    trigger: BackupTrigger,
    cancelOnRead: Boolean = false,
    private val bytes: ByteArray,
  ) : PreparedBackupArchive {
    var closed = false
      private set

    override val content: BackupContent =
      if (cancelOnRead) {
        object : BackupContent {
          override val byteSize: Long = bytes.size.toLong()

          override fun openInputStream() = throw CancellationException("cancelled")
        }
      } else {
        ByteArrayBackupContent(bytes)
      }
    override val metadata =
      BackupMetadata(
        remoteId = "",
        backupId = backupId,
        createdAt = Instant.parse("2026-07-18T10:00:00Z"),
        trigger = trigger,
        appVersion = "1.0.0",
        archiveFormatVersion = 1,
        schemaVersion = 1,
        contentCounts = BackupContentCounts(pets = 1),
        archiveSizeBytes = bytes.size.toLong(),
        archiveSha256 = "sha256",
      )

    override fun close() {
      closed = true
    }
  }
}
