package com.woliveiras.petit.domain.backup.archive

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.Pet
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupArchiveCodecTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  private val codec = BackupArchiveCodec()

  @Test
  fun createBuildsPortableArchiveWithManifestChecksumsAndFinalMetadata() {
    val sourcePhoto = temporaryFolder.newFile("source.jpg")
    sourcePhoto.writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1, 2, 3))
    val outputDirectory = temporaryFolder.newFolder("output")
    val snapshot =
      snapshot(
        photoUri = "content://device-only/photo",
        assets =
          listOf(
            BackupAsset(
              petId = "pet-1",
              fileName = "portrait.jpg",
              mediaType = "image/jpeg",
              source = sourcePhoto,
            )
          ),
      )

    val result =
      codec.create(
        BackupArchiveRequest(
          backupId = "backup-1",
          createdAt = Instant.parse("2026-07-18T10:15:30Z"),
          appVersion = "1.2.3",
          trigger = BackupTrigger.MANUAL,
          snapshot = snapshot,
          outputDirectory = outputDirectory,
        )
      )
    val validated = codec.validate(result.file)

    assertThat(result.file.exists()).isTrue()
    assertThat(result.byteSize).isEqualTo(result.file.length())
    assertThat(result.sha256).hasLength(64)
    assertThat(result.manifest.backupId).isEqualTo("backup-1")
    assertThat(result.manifest.payloadCount).isEqualTo(2)
    assertThat(result.manifest.entries.map { it.path })
      .containsExactly("data/export.json", "assets/pets/pet-1/portrait.jpg")
      .inOrder()
    assertThat(result.manifest.entries.map { it.sha256 }).containsNoneOf("", result.sha256)
    assertThat(result.manifest.entryCounts)
      .containsExactly(
        "pets",
        1,
        "weights",
        0,
        "vaccinations",
        0,
        "dewormings",
        0,
        "tasks",
        0,
        "assets",
        1,
      )
    assertThat(validated.snapshot.exportBundle.pets.single().photoUri)
      .isEqualTo("assets/pets/pet-1/portrait.jpg")
    assertThat(validated.snapshot.assets.single().source.readBytes())
      .isEqualTo(sourcePhoto.readBytes())
    assertThat(validated.snapshot.appPreferences.theme).isEqualTo(AppTheme.DARK)
    assertThat(validated.snapshot.reminderPreferences.defaultNotificationHour).isEqualTo(8)
    val extractedAsset = validated.snapshot.assets.single().source
    validated.close()
    assertThat(extractedAsset.exists()).isFalse()
  }

  @Test
  fun createRejectsDeviceLocalPhotoWithoutPortableAssetAndCleansTemporaryFiles() {
    val outputDirectory = temporaryFolder.newFolder("failure-output")

    val failure = runCatching {
      codec.create(
        BackupArchiveRequest(
          backupId = "backup-failure",
          createdAt = Instant.parse("2026-07-18T10:15:30Z"),
          appVersion = "1.2.3",
          trigger = BackupTrigger.MANUAL,
          snapshot = snapshot(photoUri = "content://device-only/photo"),
          outputDirectory = outputDirectory,
        )
      )
    }

    assertThat(failure.exceptionOrNull()).isInstanceOf(BackupArchiveException::class.java)
    assertThat(outputDirectory.listFiles().orEmpty()).isEmpty()
  }

  @Test
  fun validationRejectsTraversalBeforeExtractionAndLeavesNoStagingFiles() {
    val hostile = temporaryFolder.newFile("traversal.zip")
    ZipOutputStream(hostile.outputStream().buffered()).use { zip ->
      zip.putNextEntry(ZipEntry("../escape.jpg"))
      zip.write(byteArrayOf(1))
      zip.closeEntry()
    }

    val failure = runCatching { codec.validate(hostile) }

    assertThat(failure.exceptionOrNull()).isInstanceOf(BackupArchiveException::class.java)
    assertThat(temporaryFolder.root.walk().none { it.name.startsWith(".restore-") }).isTrue()
  }

  @Test
  fun validationRejectsChecksumSchemaCountsDuplicatesAndMissingPayloads() {
    val original = createArchive("manifest-source")
    val mutations =
      listOf<(JSONObject) -> Unit>(
        { manifest ->
          manifest.getJSONArray("entries").getJSONObject(0).put("sha256", "0".repeat(64))
        },
        { manifest -> manifest.put("dataSchemaVersion", 99) },
        { manifest -> manifest.getJSONObject("entryCounts").put("pets", -1) },
        { manifest -> manifest.getJSONObject("entryCounts").put("pets", 2) },
        { manifest ->
          manifest.getJSONArray("entries").getJSONObject(0).put("mediaType", "text/plain")
        },
        { manifest ->
          val entries = manifest.getJSONArray("entries")
          entries.put(JSONObject(entries.getJSONObject(0).toString()))
          manifest.put("payloadCount", entries.length())
        },
      )

    mutations.forEachIndexed { index, mutate ->
      val hostile = temporaryFolder.newFile("manifest-$index.zip")
      rewriteZip(original, hostile) { path, bytes ->
        if (path == "manifest.json") {
          val manifest = JSONObject(bytes.toString(Charsets.UTF_8)).also(mutate)
          listOf(path to manifest.toString().toByteArray())
        } else {
          listOf(path to bytes)
        }
      }
      assertThat(runCatching { codec.validate(hostile) }.exceptionOrNull())
        .isInstanceOf(BackupArchiveException::class.java)
    }

    val missing = temporaryFolder.newFile("missing.zip")
    rewriteZip(original, missing) { path, bytes ->
      if (path == "data/export.json") emptyList() else listOf(path to bytes)
    }
    assertThat(runCatching { codec.validate(missing) }.exceptionOrNull())
      .isInstanceOf(BackupArchiveException::class.java)

    val undeclared = temporaryFolder.newFile("undeclared.zip")
    rewriteZip(original, undeclared) { path, bytes ->
      if (path == "manifest.json") {
        listOf(path to bytes, "extra.bin" to byteArrayOf(1))
      } else {
        listOf(path to bytes)
      }
    }
    assertThat(runCatching { codec.validate(undeclared) }.exceptionOrNull())
      .isInstanceOf(BackupArchiveException::class.java)
  }

  @Test
  fun validationRejectsAssetWithManifestMatchingChecksumButInvalidMediaSignature() {
    val original = createArchive("signature-source", includePhoto = true)
    val hostile = temporaryFolder.newFile("signature.zip")
    val invalidAsset = byteArrayOf(0, 0, 0, 1, 2, 3)
    rewriteZip(original, hostile) { path, bytes ->
      when {
        path.startsWith("assets/") -> listOf(path to invalidAsset)
        path == "manifest.json" -> {
          val manifest = JSONObject(bytes.toString(Charsets.UTF_8))
          val entries = manifest.getJSONArray("entries")
          (0 until entries.length())
            .map(entries::getJSONObject)
            .single { it.getString("path").startsWith("assets/") }
            .put("sha256", sha256(invalidAsset))
            .put("uncompressedSize", invalidAsset.size)
          listOf(path to manifest.toString().toByteArray())
        }
        else -> listOf(path to bytes)
      }
    }

    assertThat(runCatching { codec.validate(hostile) }.exceptionOrNull())
      .isInstanceOf(BackupArchiveException::class.java)
  }

  @Test
  fun validationRejectsProviderArchiveContainingDeviceAuthorizationData() {
    val original = createArchive("authorization-source")
    val originalEntries =
      ZipFile(original).use { zip ->
        zip.entries().asSequence().associate { entry ->
          entry.name to zip.getInputStream(entry).use { it.readBytes() }
        }
      }
    val originalData = originalEntries.getValue("data/export.json")
    val hostileData =
      JSONObject(originalData.toString(Charsets.UTF_8))
        .put(
          "membershipChanges",
          JSONArray()
            .put(
              JSONObject()
                .put("groupId", "a".repeat(64))
                .put("memberId", "device-1")
                .put("type", "LEAVE")
                .put("timestamp", 1L)
            ),
        )
        .toString()
        .toByteArray()
    val hostile = temporaryFolder.newFile("authorization.zip")
    rewriteZip(original, hostile) { path, bytes ->
      when (path) {
        "data/export.json" -> listOf(path to hostileData)
        "manifest.json" -> {
          val manifest = JSONObject(bytes.toString(Charsets.UTF_8))
          val dataEntry =
            manifest
              .getJSONArray("entries")
              .let { entries -> (0 until entries.length()).map(entries::getJSONObject) }
              .single { it.getString("path") == "data/export.json" }
          dataEntry.put("sha256", sha256(hostileData)).put("uncompressedSize", hostileData.size)
          manifest.put(
            "totalUncompressedPayloadSize",
            manifest.getLong("totalUncompressedPayloadSize") - originalData.size + hostileData.size,
          )
          listOf(path to manifest.toString().toByteArray())
        }
        else -> listOf(path to bytes)
      }
    }

    assertThat(runCatching { codec.validate(hostile) }.exceptionOrNull())
      .isInstanceOf(BackupArchiveException::class.java)
  }

  @Test
  fun validationEnforcesPerEntryTotalAndCompressionRatioLimits() {
    val original = createArchive("limits-source")

    assertThat(
        runCatching {
            BackupArchiveCodec(BackupArchiveLimits(maxEntryBytes = 32L)).validate(original)
          }
          .exceptionOrNull()
      )
      .isInstanceOf(BackupArchiveException::class.java)
    assertThat(
        runCatching {
            BackupArchiveCodec(BackupArchiveLimits(maxTotalUncompressedBytes = 32L))
              .validate(original)
          }
          .exceptionOrNull()
      )
      .isInstanceOf(BackupArchiveException::class.java)
    assertThat(
        runCatching {
            BackupArchiveCodec(BackupArchiveLimits(maxCompressionRatio = 1L)).validate(original)
          }
          .exceptionOrNull()
      )
      .isInstanceOf(BackupArchiveException::class.java)
  }

  private fun createArchive(id: String, includePhoto: Boolean = false): File {
    val photo =
      if (includePhoto) {
        temporaryFolder.newFile("$id.jpg").apply {
          writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1, 2, 3))
        }
      } else {
        null
      }
    val assets =
      photo?.let { listOf(BackupAsset("pet-1", "photo.jpg", "image/jpeg", it)) } ?: emptyList()
    return codec
      .create(
        BackupArchiveRequest(
          backupId = id,
          createdAt = Instant.parse("2026-07-18T10:15:30Z"),
          appVersion = "1.2.3",
          trigger = BackupTrigger.MANUAL,
          snapshot = snapshot(photoUri = photo?.let { "content://device/photo" }, assets = assets),
          outputDirectory = temporaryFolder.newFolder("output-$id"),
        )
      )
      .file
  }

  private fun rewriteZip(
    source: File,
    destination: File,
    transform: (String, ByteArray) -> List<Pair<String, ByteArray>>,
  ) {
    val rewritten = mutableListOf<Pair<String, ByteArray>>()
    ZipFile(source).use { zip ->
      zip.entries().asSequence().forEach { entry ->
        rewritten += transform(entry.name, zip.getInputStream(entry).use { it.readBytes() })
      }
    }
    ZipOutputStream(destination.outputStream()).use { output ->
      rewritten.forEach { (path, bytes) ->
        output.putNextEntry(ZipEntry(path))
        output.write(bytes)
        output.closeEntry()
      }
    }
  }

  private fun sha256(bytes: ByteArray) =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

  private fun snapshot(photoUri: String? = null, assets: List<BackupAsset> = emptyList()) =
    BackupSnapshot(
      exportBundle =
        ExportBundle(
          metadata = ExportMetadata("1.2.3", "2026-07-18T10:15:30Z"),
          pets =
            listOf(
              Pet(id = "pet-1", name = "Mimi", photoUri = photoUri, createdAt = 1L, updatedAt = 2L)
            ),
          weightEntries = emptyList(),
          vaccinationEntries = emptyList(),
          dewormingEntries = emptyList(),
          tasks = emptyList(),
        ),
      appPreferences =
        BackupAppPreferences(
          theme = AppTheme.DARK,
          language = AppLanguage.ENGLISH,
          hasCompletedOnboarding = true,
        ),
      reminderPreferences =
        BackupReminderPreferences(
          vaccinationRemindersEnabled = true,
          vaccinationDaysBefore = 5,
          dewormingRemindersEnabled = false,
          dewormingDaysBefore = 6,
          weightRemindersEnabled = true,
          weightReminderIntervalDays = 14,
          defaultNotificationHour = 8,
          defaultNotificationMinute = 30,
        ),
      assets = assets,
    )
}
