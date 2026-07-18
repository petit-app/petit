package com.woliveiras.petit.data.backup

import com.woliveiras.petit.domain.backup.BackupDownloadResult
import com.woliveiras.petit.domain.backup.BackupDownloadTarget
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupPage
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import com.woliveiras.petit.domain.backup.BackupUploadRequest
import com.woliveiras.petit.domain.backup.BackupUploadResult
import javax.inject.Inject
import javax.inject.Singleton

/** Fail-closed production boundary used until a real user-owned storage adapter is installed. */
@Singleton
class ProviderUnavailableBackupStorageGateway @Inject constructor() : BackupStorageGateway {
  override suspend fun upload(
    request: BackupUploadRequest,
    onProgress: (BackupProgress) -> Unit,
  ): BackupUploadResult = unavailable()

  override suspend fun list(pageToken: String?, pageSize: Int): BackupPage = unavailable()

  override suspend fun get(remoteId: String): BackupMetadata? = unavailable()

  override suspend fun download(
    remoteId: String,
    target: BackupDownloadTarget,
    onProgress: (BackupProgress) -> Unit,
  ): BackupDownloadResult = unavailable()

  override suspend fun deleteExact(remoteId: String): Unit = unavailable()

  private fun unavailable(): Nothing = throw BackupProviderException.AuthorizationRequired()
}
