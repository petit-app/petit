package com.woliveiras.petit.data.backup.google

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupContent
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.BackupUploadRequest
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GoogleDriveBackupStorageGatewayTest {
  @Test
  fun listIsRestrictedToPetitFilesInsideAppDataFolderWithMinimalFields() = runTest {
    val rest = FakeRestClient()
    val gateway = GoogleDriveBackupStorageGateway(FakeTokens(), rest)

    gateway.list(pageToken = "next", pageSize = 25)

    val request = rest.listRequests.single()
    assertThat(request.spaces).isEqualTo(APP_DATA_FOLDER)
    assertThat(request.query).contains("petitContractId")
    assertThat(request.query).contains("com.woliveiras.petit.backup")
    assertThat(request.query).contains("trashed = false")
    assertThat(request.fields)
      .isEqualTo("nextPageToken,files(id,name,size,createdTime,appProperties)")
    assertThat(request.pageToken).isEqualTo("next")
  }

  @Test
  fun retryWithSameBackupIdReturnsExistingRemoteFileWithoutAnotherUpload() = runTest {
    val existing = resource("remote-1", metadata("backup-1"))
    val rest = FakeRestClient(files = mutableListOf(existing))
    val gateway = GoogleDriveBackupStorageGateway(FakeTokens(), rest)
    val metadata = metadata("backup-1")
    val progress = mutableListOf<BackupProgress>()

    val result =
      gateway.upload(BackupUploadRequest("backup-1", Bytes("zip".toByteArray()), metadata)) {
        progress += it
      }

    assertThat(result.remoteId).isEqualTo("remote-1")
    assertThat(rest.uploadCalls).isEqualTo(0)
    assertThat(progress).containsExactly(BackupProgress(3, 3))
  }

  @Test
  fun idempotencySearchContinuesPastMalformedDuplicatePages() = runTest {
    val valid = resource("remote-valid", metadata("backup-1"))
    val malformed =
      resource("remote-malformed", metadata("backup-1"))
        .copy(appProperties = valid.appProperties - "petitArchiveSha256")
    val rest =
      FakeRestClient(
        pages =
          mapOf(
            null to DriveFilePage(listOf(malformed), "page-2"),
            "page-2" to DriveFilePage(listOf(valid), null),
          )
      )
    val gateway = GoogleDriveBackupStorageGateway(FakeTokens(), rest)

    val result =
      gateway.upload(
        BackupUploadRequest("backup-1", Bytes("zip".toByteArray()), metadata("backup-1"))
      )

    assertThat(result.remoteId).isEqualTo("remote-valid")
    assertThat(rest.listRequests.map { it.pageToken }).containsExactly(null, "page-2").inOrder()
    assertThat(rest.uploadCalls).isEqualTo(0)
  }

  @Test
  fun uploadUsesResumableAppDataCreateAndPetitMetadata() = runTest {
    val rest = FakeRestClient()
    val gateway = GoogleDriveBackupStorageGateway(FakeTokens(), rest)
    val metadata = metadata("backup-2")

    gateway.upload(BackupUploadRequest("backup-2", Bytes("zip".toByteArray()), metadata))

    assertThat(rest.uploadCalls).isEqualTo(1)
    assertThat(rest.uploadParent).isEqualTo(APP_DATA_FOLDER)
    assertThat(rest.uploadProperties["petitBackupId"]).isEqualTo("backup-2")
    assertThat(rest.uploadProperties["petitContractId"]).isEqualTo("com.woliveiras.petit.backup")
  }

  @Test
  fun expiredTokenIsClearedAndOperationRetriesOnceWithFreshToken() = runTest {
    val tokens = FakeTokens(mutableListOf("expired", "fresh"))
    val rest = FakeRestClient(failFirstListUnauthorized = true)
    val gateway = GoogleDriveBackupStorageGateway(tokens, rest)

    gateway.list()

    assertThat(tokens.invalidated).containsExactly("expired")
    assertThat(rest.seenTokens).containsExactly("expired", "fresh").inOrder()
  }

  @Test
  fun coroutineCancellationIsNeverTranslatedIntoProviderFailure() = runTest {
    val rest = FakeRestClient(cancelList = true)
    val gateway = GoogleDriveBackupStorageGateway(FakeTokens(), rest)

    val failure = runCatching { gateway.list() }.exceptionOrNull()

    assertThat(failure).isInstanceOf(CancellationException::class.java)
  }

  private fun metadata(backupId: String) =
    BackupMetadata(
      remoteId = "pending",
      backupId = backupId,
      createdAt = Instant.parse("2026-07-19T10:00:00Z"),
      trigger = BackupTrigger.MANUAL,
      appVersion = "1.0.0",
      archiveFormatVersion = 1,
      schemaVersion = 1,
      contentCounts = BackupContentCounts(pets = 1),
      archiveSizeBytes = 3,
      archiveSha256 = "abc123",
    )

  private fun resource(remoteId: String, metadata: BackupMetadata) =
    DriveFileResource(
      id = remoteId,
      name = "backup.zip",
      size = metadata.archiveSizeBytes,
      createdTime = metadata.createdAt.toString(),
      appProperties =
        mapOf(
          "petitContractId" to metadata.contractId,
          "petitBackupId" to metadata.backupId,
          "petitCreatedAt" to metadata.createdAt.toString(),
          "petitTrigger" to metadata.trigger.name,
          "petitAppVersion" to metadata.appVersion,
          "petitArchiveVersion" to "1",
          "petitSchemaVersion" to "1",
          "petitArchiveSize" to "3",
          "petitArchiveSha256" to metadata.archiveSha256,
          "petitContentCounts" to "1,0,0,0,0,0",
        ),
    )

  private class Bytes(private val bytes: ByteArray) : BackupContent {
    override val byteSize = bytes.size.toLong()

    override fun openInputStream(): InputStream = ByteArrayInputStream(bytes)
  }

  private class FakeTokens(private val tokens: MutableList<String> = mutableListOf("token")) :
    GoogleDriveAccessTokenProvider {
    val invalidated = mutableListOf<String>()
    private var current = tokens.first()

    override suspend fun acquireToken(): String {
      if (tokens.isNotEmpty()) current = tokens.removeFirst()
      return current
    }

    override suspend fun invalidateToken(token: String) {
      invalidated += token
    }
  }

  private inner class FakeRestClient(
    private val files: MutableList<DriveFileResource> = mutableListOf(),
    private val failFirstListUnauthorized: Boolean = false,
    private val cancelList: Boolean = false,
    private val pages: Map<String?, DriveFilePage>? = null,
  ) : GoogleDriveRestClient {
    val listRequests = mutableListOf<DriveListRequest>()
    val seenTokens = mutableListOf<String>()
    var uploadCalls = 0
    var uploadParent: String? = null
    var uploadProperties: Map<String, String> = emptyMap()

    override suspend fun list(token: String, request: DriveListRequest): DriveFilePage {
      seenTokens += token
      listRequests += request
      if (cancelList) throw CancellationException("cancelled")
      if (failFirstListUnauthorized && seenTokens.size == 1) throw GoogleDriveHttpException(401)
      return pages?.get(request.pageToken) ?: DriveFilePage(files.toList(), null)
    }

    override suspend fun get(token: String, remoteId: String, fields: String) =
      files.firstOrNull { it.id == remoteId }

    override suspend fun uploadResumable(
      token: String,
      name: String,
      parent: String,
      appProperties: Map<String, String>,
      content: BackupContent,
      onProgress: (BackupProgress) -> Unit,
    ): DriveFileResource {
      uploadCalls++
      uploadParent = parent
      uploadProperties = appProperties
      return resource("remote-upload", metadata(appProperties.getValue("petitBackupId")))
    }

    override suspend fun download(
      token: String,
      remoteId: String,
      target: com.woliveiras.petit.domain.backup.BackupDownloadTarget,
      totalBytes: Long,
      onProgress: (BackupProgress) -> Unit,
    ) = totalBytes

    override suspend fun delete(token: String, remoteId: String) = Unit
  }
}
