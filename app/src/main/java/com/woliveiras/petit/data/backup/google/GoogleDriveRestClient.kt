package com.woliveiras.petit.data.backup.google

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.woliveiras.petit.domain.backup.BackupContent
import com.woliveiras.petit.domain.backup.BackupDownloadTarget
import com.woliveiras.petit.domain.backup.BackupProgress
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal const val APP_DATA_FOLDER = "appDataFolder"
internal const val BACKUP_MIME_TYPE = "application/vnd.petit.backup+zip"

internal data class DriveFileResource(
  val id: String,
  val name: String?,
  val size: Long?,
  val createdTime: String?,
  val appProperties: Map<String, String>,
)

internal data class DriveFilePage(val files: List<DriveFileResource>, val nextPageToken: String?)

internal data class DriveListRequest(
  val spaces: String,
  val query: String,
  val fields: String,
  val pageSize: Int,
  val pageToken: String?,
  val orderBy: String,
)

internal interface GoogleDriveRestClient {
  suspend fun list(token: String, request: DriveListRequest): DriveFilePage

  suspend fun get(token: String, remoteId: String, fields: String): DriveFileResource?

  suspend fun uploadResumable(
    token: String,
    name: String,
    parent: String,
    appProperties: Map<String, String>,
    content: BackupContent,
    onProgress: (BackupProgress) -> Unit,
  ): DriveFileResource

  suspend fun download(
    token: String,
    remoteId: String,
    target: BackupDownloadTarget,
    totalBytes: Long,
    onProgress: (BackupProgress) -> Unit,
  ): Long

  suspend fun delete(token: String, remoteId: String)
}

internal class GoogleDriveHttpException(
  val statusCode: Int,
  val reasons: Set<String> = emptySet(),
  cause: Throwable? = null,
) : IOException("Google Drive request failed with HTTP $statusCode", cause)

internal class HttpGoogleDriveRestClient(
  private val gson: Gson = Gson(),
  private val apiBaseUrl: String = "https://www.googleapis.com/drive/v3",
  private val uploadBaseUrl: String = "https://www.googleapis.com/upload/drive/v3",
) : GoogleDriveRestClient {
  override suspend fun list(token: String, request: DriveListRequest): DriveFilePage {
    val parameters =
      linkedMapOf(
        "spaces" to request.spaces,
        "q" to request.query,
        "fields" to request.fields,
        "pageSize" to request.pageSize.toString(),
        "orderBy" to request.orderBy,
      )
    request.pageToken?.let { parameters["pageToken"] = it }
    val response = executeJson(token, "GET", "$apiBaseUrl/files?${parameters.queryString()}")
    val files =
      response
        .getAsJsonArray("files")
        ?.map { gson.fromJson(it, DriveFileJson::class.java).resource() }
        .orEmpty()
    return DriveFilePage(files, response.stringOrNull("nextPageToken"))
  }

  override suspend fun get(token: String, remoteId: String, fields: String): DriveFileResource? =
    try {
      gson
        .fromJson(
          executeJson(token, "GET", "$apiBaseUrl/files/${remoteId.path()}?fields=${fields.url()}"),
          DriveFileJson::class.java,
        )
        .resource()
    } catch (error: GoogleDriveHttpException) {
      if (error.statusCode == HttpURLConnection.HTTP_NOT_FOUND) null else throw error
    }

  override suspend fun uploadResumable(
    token: String,
    name: String,
    parent: String,
    appProperties: Map<String, String>,
    content: BackupContent,
    onProgress: (BackupProgress) -> Unit,
  ): DriveFileResource =
    withContext(Dispatchers.IO) {
      val metadata =
        gson.toJson(
          mapOf(
            "name" to name,
            "parents" to listOf(parent),
            "mimeType" to BACKUP_MIME_TYPE,
            "appProperties" to appProperties,
          )
        )
      val initiation =
        open("$uploadBaseUrl/files?uploadType=resumable&fields=${FILE_FIELDS.url()}", "POST", token)
      try {
        initiation.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        initiation.setRequestProperty("X-Upload-Content-Type", BACKUP_MIME_TYPE)
        initiation.setRequestProperty("X-Upload-Content-Length", content.byteSize.toString())
        initiation.doOutput = true
        initiation.outputStream.use { it.write(metadata.toByteArray(StandardCharsets.UTF_8)) }
        ensureSuccess(initiation)
        val sessionUrl =
          initiation.getHeaderField("Location") ?: throw IOException("Missing upload session")
        val upload = open(sessionUrl, "PUT", token)
        try {
          upload.setRequestProperty("Content-Type", BACKUP_MIME_TYPE)
          upload.setRequestProperty(
            "Content-Range",
            "bytes 0-${content.byteSize - 1}/${content.byteSize}",
          )
          upload.setFixedLengthStreamingMode(content.byteSize)
          upload.doOutput = true
          var transferred = 0L
          content.openInputStream().use { source ->
            BufferedOutputStream(upload.outputStream).use { target ->
              val buffer = ByteArray(256 * 1024)
              while (true) {
                coroutineContext.ensureActive()
                val count = source.read(buffer)
                if (count < 0) break
                target.write(buffer, 0, count)
                transferred += count
                onProgress(BackupProgress(transferred, content.byteSize))
              }
            }
          }
          ensureSuccess(upload)
          gson.fromJson(upload.inputStream.reader(), DriveFileJson::class.java).resource()
        } finally {
          upload.disconnect()
        }
      } finally {
        initiation.disconnect()
      }
    }

  override suspend fun download(
    token: String,
    remoteId: String,
    target: BackupDownloadTarget,
    totalBytes: Long,
    onProgress: (BackupProgress) -> Unit,
  ): Long =
    withContext(Dispatchers.IO) {
      val connection = open("$apiBaseUrl/files/${remoteId.path()}?alt=media", "GET", token)
      try {
        ensureSuccess(connection)
        var transferred = 0L
        BufferedInputStream(connection.inputStream).use { source ->
          target.openOutputStream().use { output ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
              coroutineContext.ensureActive()
              val count = source.read(buffer)
              if (count < 0) break
              output.write(buffer, 0, count)
              transferred += count
              onProgress(BackupProgress(transferred, totalBytes))
            }
          }
        }
        transferred
      } finally {
        connection.disconnect()
      }
    }

  override suspend fun delete(token: String, remoteId: String) {
    withContext(Dispatchers.IO) {
      val connection = open("$apiBaseUrl/files/${remoteId.path()}", "DELETE", token)
      try {
        if (connection.responseCode != HttpURLConnection.HTTP_NOT_FOUND) ensureSuccess(connection)
      } finally {
        connection.disconnect()
      }
    }
  }

  private suspend fun executeJson(token: String, method: String, url: String): JsonObject =
    withContext(Dispatchers.IO) {
      val connection = open(url, method, token)
      try {
        ensureSuccess(connection)
        gson.fromJson(connection.inputStream.reader(), JsonObject::class.java)
      } finally {
        connection.disconnect()
      }
    }

  private fun open(url: String, method: String, token: String): HttpURLConnection =
    (URI(url).toURL().openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 30_000
      readTimeout = 60_000
      setRequestProperty("Authorization", "Bearer $token")
      setRequestProperty("Accept", "application/json")
    }

  private fun ensureSuccess(connection: HttpURLConnection) {
    val status = connection.responseCode
    if (status in 200..299) return
    val errorBody =
      runCatching { connection.errorStream?.reader()?.readText().orEmpty() }.getOrDefault("")
    val reasons =
      runCatching {
          val root = gson.fromJson(errorBody, JsonObject::class.java)
          root
            .getAsJsonObject("error")
            ?.getAsJsonArray("errors")
            ?.mapNotNull { it.asJsonObject.stringOrNull("reason") }
            ?.toSet()
            .orEmpty()
        }
        .getOrDefault(emptySet())
    throw GoogleDriveHttpException(status, reasons)
  }

  private data class DriveFileJson(
    val id: String = "",
    val name: String? = null,
    val size: String? = null,
    val createdTime: String? = null,
    val appProperties: Map<String, String>? = null,
  ) {
    fun resource() =
      DriveFileResource(id, name, size?.toLongOrNull(), createdTime, appProperties.orEmpty())
  }

  private companion object {
    const val FILE_FIELDS = "id,name,size,createdTime,appProperties"
  }
}

private fun Map<String, String>.queryString(): String =
  entries.joinToString("&") { (key, value) -> "${key.url()}=${value.url()}" }

private fun String.url(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

private fun String.path(): String = url().replace("+", "%20")

private fun JsonObject.stringOrNull(name: String): String? =
  get(name)?.takeUnless { it.isJsonNull }?.asString
