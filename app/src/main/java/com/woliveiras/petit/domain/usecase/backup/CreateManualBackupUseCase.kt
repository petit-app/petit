package com.woliveiras.petit.domain.usecase.backup

import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupContent
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.BackupUploadRequest
import java.util.concurrent.CancellationException

/** Provider-neutral staged archive consumed by the upload orchestrator. */
interface PreparedBackupArchive : AutoCloseable {
  val metadata: BackupMetadata
  val content: BackupContent
}

/** Adapter seam for the archive implementation in `domain.backup.archive`. */
interface BackupArchivePreparer {
  suspend fun prepare(backupId: String, trigger: BackupTrigger): PreparedBackupArchive
}

class CreateManualBackupUseCase(
  private val authorizationGateway: BackupAuthorizationGateway,
  private val storageGateway: BackupStorageGateway,
  private val archivePreparer: BackupArchivePreparer,
) : CreateBackupAction {
  override suspend fun execute(
    backupId: String,
    trigger: BackupTrigger,
    onProgress: (BackupProgress) -> Unit,
  ): BackupCreationResult {
    require(backupId.isNotBlank()) { "Backup ID cannot be blank" }
    val authorization =
      try {
        authorizationGateway.refresh()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: BackupProviderException.AuthorizationRequired) {
        return BackupCreationResult.AuthorizationRequired
      } catch (_: BackupProviderException.QuotaExceeded) {
        return BackupCreationResult.QuotaExceeded
      } catch (error: BackupProviderException.Retryable) {
        return BackupCreationResult.RetryableFailure(
          error.message ?: "Retryable backup provider failure"
        )
      } catch (error: BackupProviderException.Interrupted) {
        return BackupCreationResult.RetryableFailure(
          error.message ?: "Backup authorization refresh was interrupted"
        )
      } catch (error: BackupProviderException.Permanent) {
        return BackupCreationResult.PermanentFailure(
          error.message ?: "Permanent backup provider failure"
        )
      } catch (_: Exception) {
        return BackupCreationResult.PermanentFailure(GENERIC_FAILURE_MESSAGE)
      }
    when (authorization) {
      is BackupAuthorizationState.Authorized -> Unit
      BackupAuthorizationState.Authorizing,
      is BackupAuthorizationState.Unavailable ->
        return BackupCreationResult.RetryableFailure(
          "Backup authorization is temporarily unavailable"
        )
      BackupAuthorizationState.AuthorizationRequired,
      BackupAuthorizationState.Disconnected -> return BackupCreationResult.AuthorizationRequired
    }

    var prepared: PreparedBackupArchive? = null
    return try {
      val currentArchive = archivePreparer.prepare(backupId, trigger)
      prepared = currentArchive
      val expectedSize = currentArchive.content.byteSize
      var lastTransferred = -1L
      val result =
        storageGateway.upload(
          BackupUploadRequest(backupId, currentArchive.content, currentArchive.metadata)
        ) { progress ->
          if (progress.totalBytes != expectedSize || progress.bytesTransferred < lastTransferred) {
            throw InvalidBackupProgressException()
          }
          lastTransferred = progress.bytesTransferred
          onProgress(progress)
        }
      if (
        lastTransferred != expectedSize ||
          result.metadata.backupId != backupId ||
          result.remoteId != result.metadata.remoteId ||
          !result.metadata.isRecognized
      ) {
        BackupCreationResult.PermanentFailure(GENERIC_FAILURE_MESSAGE)
      } else {
        BackupCreationResult.Success(result.metadata)
      }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (_: BackupProviderException.AuthorizationRequired) {
      BackupCreationResult.AuthorizationRequired
    } catch (_: BackupProviderException.QuotaExceeded) {
      BackupCreationResult.QuotaExceeded
    } catch (error: BackupProviderException.Retryable) {
      BackupCreationResult.RetryableFailure(error.message ?: "Retryable backup provider failure")
    } catch (error: BackupProviderException.Interrupted) {
      BackupCreationResult.RetryableFailure(error.message ?: "Backup upload was interrupted")
    } catch (error: BackupProviderException.Permanent) {
      BackupCreationResult.PermanentFailure(error.message ?: "Permanent backup provider failure")
    } catch (_: Exception) {
      BackupCreationResult.PermanentFailure(GENERIC_FAILURE_MESSAGE)
    } finally {
      runCatching { prepared?.close() }
    }
  }

  private class InvalidBackupProgressException : IllegalStateException()

  private companion object {
    const val GENERIC_FAILURE_MESSAGE = "Backup could not be completed"
  }
}
