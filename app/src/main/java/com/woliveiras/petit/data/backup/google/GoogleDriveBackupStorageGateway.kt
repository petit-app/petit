package com.woliveiras.petit.data.backup.google

import com.woliveiras.petit.domain.backup.BackupCompatibility
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupDownloadResult
import com.woliveiras.petit.domain.backup.BackupDownloadTarget
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupPage
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.BackupUploadRequest
import com.woliveiras.petit.domain.backup.BackupUploadResult
import com.woliveiras.petit.domain.backup.PETIT_BACKUP_CONTRACT_ID
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GoogleDriveBackupStorageGateway
internal constructor(
  private val tokenProvider: GoogleDriveAccessTokenProvider,
  private val restClient: GoogleDriveRestClient,
) : BackupStorageGateway {
  constructor(
    tokenProvider: GoogleDriveAccessTokenProvider
  ) : this(tokenProvider, HttpGoogleDriveRestClient())

  private val uploadMutex = Mutex()

  override suspend fun upload(
    request: BackupUploadRequest,
    onProgress: (BackupProgress) -> Unit,
  ): BackupUploadResult =
    uploadMutex.withLock {
      val existing = findByBackupId(request.backupId)
      if (existing != null) {
        onProgress(BackupProgress(request.content.byteSize, request.content.byteSize))
        return@withLock BackupUploadResult(existing.remoteId, existing)
      }

      withMappedErrors {
        withTokenRetry { token ->
          restClient
            .uploadResumable(
              token = token,
              name = "petit-backup-${request.backupId}.zip",
              parent = APP_DATA_FOLDER,
              appProperties = request.metadata.toAppProperties(),
              content = request.content,
              onProgress = onProgress,
            )
            .toMetadata()
            .let { metadata -> BackupUploadResult(metadata.remoteId, metadata) }
        }
      }
    }

  override suspend fun list(pageToken: String?, pageSize: Int): BackupPage {
    require(pageSize in 1..100) { "Drive page size must be between 1 and 100" }
    return withMappedErrors {
      withTokenRetry { token ->
        val page =
          restClient.list(
            token,
            DriveListRequest(
              spaces = APP_DATA_FOLDER,
              query = recognizedQuery(),
              fields = "nextPageToken,files($FILE_FIELDS)",
              pageSize = pageSize,
              pageToken = pageToken,
              orderBy = "createdTime desc",
            ),
          )
        BackupPage(
          page.files.mapNotNull { runCatching { it.toMetadata() }.getOrNull() },
          page.nextPageToken,
        )
      }
    }
  }

  override suspend fun get(remoteId: String): BackupMetadata? = withMappedErrors {
    withTokenRetry { token ->
      restClient.get(token, remoteId, FILE_FIELDS)?.toMetadata()?.takeIf { it.isRecognized }
    }
  }

  override suspend fun download(
    remoteId: String,
    target: BackupDownloadTarget,
    onProgress: (BackupProgress) -> Unit,
  ): BackupDownloadResult {
    val metadata = get(remoteId) ?: throw BackupProviderException.Permanent("Backup does not exist")
    return withMappedErrors {
      withTokenRetry { token ->
        val downloaded =
          restClient.download(token, remoteId, target, metadata.archiveSizeBytes, onProgress)
        BackupDownloadResult(metadata, downloaded)
      }
    }
  }

  override suspend fun deleteExact(remoteId: String) {
    withMappedErrors { withTokenRetry { token -> restClient.delete(token, remoteId) } }
  }

  private suspend fun findByBackupId(backupId: String): BackupMetadata? = withMappedErrors {
    withTokenRetry { token ->
      var pageToken: String? = null
      do {
        val page =
          restClient.list(
            token,
            DriveListRequest(
              spaces = APP_DATA_FOLDER,
              query = recognizedQuery(backupId),
              fields = "nextPageToken,files($FILE_FIELDS)",
              pageSize = 100,
              pageToken = pageToken,
              orderBy = "createdTime desc",
            ),
          )
        page.files
          .mapNotNull { runCatching { it.toMetadata() }.getOrNull() }
          .firstOrNull { it.isRecognized && it.backupId == backupId }
          ?.let {
            return@withTokenRetry it
          }
        pageToken = page.nextPageToken
      } while (pageToken != null)
      null
    }
  }

  private suspend fun <T> withTokenRetry(block: suspend (String) -> T): T {
    val first = tokenProvider.acquireToken()
    return try {
      block(first)
    } catch (error: GoogleDriveHttpException) {
      if (error.statusCode != 401) throw error
      tokenProvider.invalidateToken(first)
      val refreshed = tokenProvider.acquireToken()
      block(refreshed)
    }
  }

  private suspend fun <T> withMappedErrors(block: suspend () -> T): T =
    try {
      block()
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (known: BackupProviderException) {
      throw known
    } catch (error: GoogleDriveHttpException) {
      throw error.toDomain()
    } catch (error: IOException) {
      throw BackupProviderException.Retryable("Google Drive transport failed", error)
    } catch (error: Exception) {
      throw BackupProviderException.Permanent("Google Drive rejected the backup operation", error)
    }

  private companion object {
    const val FILE_FIELDS = "id,name,size,createdTime,appProperties"
  }
}

private const val CONTRACT_PROPERTY = "petitContractId"
private const val BACKUP_ID_PROPERTY = "petitBackupId"
private const val CREATED_AT_PROPERTY = "petitCreatedAt"
private const val TRIGGER_PROPERTY = "petitTrigger"
private const val APP_VERSION_PROPERTY = "petitAppVersion"
private const val ARCHIVE_VERSION_PROPERTY = "petitArchiveVersion"
private const val SCHEMA_VERSION_PROPERTY = "petitSchemaVersion"
private const val ARCHIVE_SIZE_PROPERTY = "petitArchiveSize"
private const val ARCHIVE_SHA_PROPERTY = "petitArchiveSha256"
private const val COUNTS_PROPERTY = "petitContentCounts"

private fun recognizedQuery(backupId: String? = null): String =
  buildList {
      add("trashed = false")
      add("appProperties has { key='$CONTRACT_PROPERTY' and value='$PETIT_BACKUP_CONTRACT_ID' }")
      if (backupId != null) {
        add(
          "appProperties has { key='$BACKUP_ID_PROPERTY' and value='${backupId.queryLiteral()}' }"
        )
      }
    }
    .joinToString(" and ")

private fun String.queryLiteral(): String = replace("\\", "\\\\").replace("'", "\\'")

private fun BackupMetadata.toAppProperties(): Map<String, String> =
  mapOf(
    CONTRACT_PROPERTY to contractId,
    BACKUP_ID_PROPERTY to backupId,
    CREATED_AT_PROPERTY to createdAt.toString(),
    TRIGGER_PROPERTY to trigger.name,
    APP_VERSION_PROPERTY to appVersion,
    ARCHIVE_VERSION_PROPERTY to archiveFormatVersion.toString(),
    SCHEMA_VERSION_PROPERTY to schemaVersion.toString(),
    ARCHIVE_SIZE_PROPERTY to archiveSizeBytes.toString(),
    ARCHIVE_SHA_PROPERTY to archiveSha256,
    COUNTS_PROPERTY to
      listOf(
          contentCounts.pets,
          contentCounts.weights,
          contentCounts.vaccinations,
          contentCounts.dewormingRecords,
          contentCounts.tasks,
          contentCounts.assets,
        )
        .joinToString(","),
  )

private fun DriveFileResource.toMetadata(): BackupMetadata {
  val properties = appProperties
  val counts = properties.required(COUNTS_PROPERTY).split(',').map(String::toInt)
  require(counts.size == 6) { "Invalid content-count metadata" }
  val metadataSize = properties.required(ARCHIVE_SIZE_PROPERTY).toLong()
  require(size == null || size == metadataSize) { "Drive size does not match Petit metadata" }
  return BackupMetadata(
    remoteId = id,
    backupId = properties.required(BACKUP_ID_PROPERTY),
    createdAt = Instant.parse(properties.required(CREATED_AT_PROPERTY)),
    trigger = BackupTrigger.valueOf(properties.required(TRIGGER_PROPERTY)),
    appVersion = properties.required(APP_VERSION_PROPERTY),
    archiveFormatVersion = properties.required(ARCHIVE_VERSION_PROPERTY).toInt(),
    schemaVersion = properties.required(SCHEMA_VERSION_PROPERTY).toInt(),
    contentCounts =
      BackupContentCounts(
        pets = counts[0],
        weights = counts[1],
        vaccinations = counts[2],
        dewormingRecords = counts[3],
        tasks = counts[4],
        assets = counts[5],
      ),
    archiveSizeBytes = metadataSize,
    archiveSha256 = properties.required(ARCHIVE_SHA_PROPERTY),
    contractId = properties.required(CONTRACT_PROPERTY),
    compatibility = BackupCompatibility.COMPATIBLE,
  )
}

private fun Map<String, String>.required(key: String): String =
  requireNotNull(this[key]) { "Missing Petit metadata property: $key" }

private fun GoogleDriveHttpException.toDomain(): BackupProviderException =
  when {
    statusCode == 401 || "authError" in reasons || "insufficientPermissions" in reasons ->
      BackupProviderException.AuthorizationRequired(this)
    "storageQuotaExceeded" in reasons || "quotaExceeded" in reasons ->
      BackupProviderException.QuotaExceeded(this)
    statusCode == 408 ||
      statusCode == 429 ||
      statusCode >= 500 ||
      reasons.any { it in RETRYABLE_REASONS } ->
      BackupProviderException.Retryable("Google Drive is temporarily unavailable", this)
    else -> BackupProviderException.Permanent("Google Drive rejected the backup operation", this)
  }

private val RETRYABLE_REASONS =
  setOf("rateLimitExceeded", "userRateLimitExceeded", "backendError", "internalError")
