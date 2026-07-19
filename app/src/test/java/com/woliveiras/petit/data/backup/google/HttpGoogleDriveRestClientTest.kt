package com.woliveiras.petit.data.backup.google

import com.google.common.truth.Truth.assertThat
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.woliveiras.petit.data.backup.testing.ByteArrayBackupContent
import com.woliveiras.petit.domain.backup.BackupProgress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class HttpGoogleDriveRestClientTest {
  private lateinit var server: HttpServer
  private lateinit var baseUrl: String

  @Before
  fun setUp() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.start()
    baseUrl = "http://127.0.0.1:${server.address.port}"
  }

  @After
  fun tearDown() {
    server.stop(0)
  }

  @Test
  fun listEncodesAppDataQueryAndBearerToken() = runTest {
    var query: String? = null
    var authorization: String? = null
    server.createContext("/drive/files") { exchange ->
      query = exchange.requestURI.rawQuery
      authorization = exchange.requestHeaders.getFirst("Authorization")
      exchange.respond(200, """{"files":[],"nextPageToken":"next"}""")
    }
    val client = client()

    val result =
      client.list(
        "short-lived-token",
        DriveListRequest(
          spaces = APP_DATA_FOLDER,
          query = "name = 'Petit backup'",
          fields = "nextPageToken,files(id)",
          pageSize = 25,
          pageToken = "page token",
          orderBy = "createdTime desc",
        ),
      )

    assertThat(result.nextPageToken).isEqualTo("next")
    assertThat(query).contains("spaces=appDataFolder")
    assertThat(query).contains("q=name+%3D+%27Petit+backup%27")
    assertThat(query).contains("pageToken=page+token")
    assertThat(authorization).isEqualTo("Bearer short-lived-token")
  }

  @Test
  fun resumableUploadUsesTwoRequestsAndReportsTransferredBytes() = runTest {
    val uploaded = mutableListOf<Byte>()
    server.createContext("/upload/files") { exchange ->
      assertThat(exchange.requestMethod).isEqualTo("POST")
      assertThat(exchange.requestURI.rawQuery).contains("uploadType=resumable")
      exchange.requestBody.use { it.readBytes() }
      exchange.responseHeaders.add("Location", "$baseUrl/upload-session")
      exchange.respond(200, "{}")
    }
    server.createContext("/upload-session") { exchange ->
      assertThat(exchange.requestMethod).isEqualTo("PUT")
      uploaded += exchange.requestBody.use { it.readBytes() }.toList()
      exchange.respond(
        200,
        """{"id":"remote-1","name":"backup.zip","size":"3","createdTime":"2026-07-19T10:00:00Z","appProperties":{"key":"value"}}""",
      )
    }
    val progress = mutableListOf<BackupProgress>()

    val result =
      client()
        .uploadResumable(
          token = "token",
          name = "backup.zip",
          parent = APP_DATA_FOLDER,
          appProperties = mapOf("key" to "value"),
          content = ByteArrayBackupContent("zip".encodeToByteArray()),
          onProgress = progress::add,
        )

    assertThat(result.id).isEqualTo("remote-1")
    assertThat(uploaded.toByteArray()).isEqualTo("zip".encodeToByteArray())
    assertThat(progress).containsExactly(BackupProgress(3, 3))
  }

  @Test
  fun nonSuccessResponsePreservesHttpStatusAndDriveReason() = runTest {
    server.createContext("/drive/files") { exchange ->
      exchange.respond(403, """{"error":{"errors":[{"reason":"storageQuotaExceeded"}]}}""")
    }

    val failure =
      runCatching {
          client()
            .list(
              "token",
              DriveListRequest(APP_DATA_FOLDER, "trashed = false", "files(id)", 10, null, ""),
            )
        }
        .exceptionOrNull()

    assertThat(failure).isInstanceOf(GoogleDriveHttpException::class.java)
    failure as GoogleDriveHttpException
    assertThat(failure.statusCode).isEqualTo(403)
    assertThat(failure.reasons).containsExactly("storageQuotaExceeded")
  }

  private fun client() =
    HttpGoogleDriveRestClient(apiBaseUrl = "$baseUrl/drive", uploadBaseUrl = "$baseUrl/upload")

  private fun HttpExchange.respond(status: Int, body: String) {
    val bytes = body.toByteArray(StandardCharsets.UTF_8)
    responseHeaders.add("Content-Type", "application/json")
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
  }
}
