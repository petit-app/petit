package com.woliveiras.petit.domain.backup.archive

import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.ExportBundle
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

/** Creates and validates Petit backup archives without depending on a storage provider. */
class BackupArchiveCodec(private val limits: BackupArchiveLimits = BackupArchiveLimits()) {
  val supportedArchiveFormatVersion: Int
    get() = ARCHIVE_FORMAT_VERSION

  val supportedDataSchemaVersion: Int
    get() = SUPPORTED_DATA_SCHEMA_VERSION

  fun create(request: BackupArchiveRequest): BackupArchiveResult {
    validateComponent(request.backupId, "backup ID")
    val outputDirectory = request.outputDirectory.absoluteFile
    requireArchive(outputDirectory.mkdirs() || outputDirectory.isDirectory) {
      "Backup output directory is unavailable"
    }
    val finalFile = outputDirectory.resolve("petit-backup-${request.backupId}.zip")
    requireArchive(!finalFile.exists()) { "A backup with this ID already exists" }
    val workDirectory = outputDirectory.resolve(".backup-${request.backupId}-${UUID.randomUUID()}")
    val partialFile = outputDirectory.resolve(".${finalFile.name}.${UUID.randomUUID()}.partial")

    try {
      requireArchive(workDirectory.mkdir()) { "Could not create backup staging directory" }
      val portableSnapshot = makePortable(request.snapshot)
      val dataFile = workDirectory.resolve("export.json")
      writeBounded(dataFile, limits.maxEntryBytes) { output ->
        output.writer(Charsets.UTF_8).use { writer ->
          writer.write(snapshotToJson(portableSnapshot).toString())
        }
      }
      val entries = buildList {
        add(payloadEntry(DATA_PATH, DATA_MEDIA_TYPE, dataFile))
        portableSnapshot.assets.sortedBy(::assetPath).forEach { asset ->
          validateAsset(asset)
          add(payloadEntry(assetPath(asset), asset.mediaType, asset.source))
        }
      }
      requireArchive(entries.size <= limits.maxEntryCount) { "Backup has too many payload entries" }
      val totalSize = entries.fold(0L) { total, entry -> checkedAdd(total, entry.uncompressedSize) }
      requireArchive(totalSize <= limits.maxTotalUncompressedBytes) {
        "Backup payload exceeds the uncompressed size limit"
      }
      val manifest = manifest(request, portableSnapshot, entries, totalSize)
      val manifestBytes = manifestToJson(manifest).toString().toByteArray(Charsets.UTF_8)
      requireArchive(manifestBytes.size <= limits.maxManifestBytes) {
        "Backup manifest is too large"
      }

      ZipOutputStream(partialFile.outputStream().buffered()).use { zip ->
        writeZipEntry(zip, MANIFEST_PATH, manifestBytes.inputStream())
        entries.forEach { entry ->
          val source =
            if (entry.path == DATA_PATH) dataFile
            else portableSnapshot.assets.single { assetPath(it) == entry.path }.source
          writePayloadZipEntry(zip, entry, source)
        }
      }
      requireArchive(partialFile.length() <= limits.maxCompressedArchiveBytes) {
        "Backup archive exceeds the compressed size limit"
      }
      requireArchive(partialFile.renameTo(finalFile)) {
        "Could not publish completed backup archive"
      }
      return BackupArchiveResult(
        file = finalFile,
        backupId = request.backupId,
        byteSize = finalFile.length(),
        sha256 = sha256(finalFile),
        manifest = manifest,
      )
    } catch (error: BackupArchiveException) {
      throw error
    } catch (error: Exception) {
      throw BackupArchiveException("Could not create backup archive", error)
    } finally {
      workDirectory.deleteRecursively()
      partialFile.delete()
    }
  }

  /** Validates every payload before returning parsed data or extracted assets. */
  fun validate(archive: File): ValidatedBackupArchive {
    requireArchive(archive.isFile) { "Backup archive is unavailable" }
    requireArchive(archive.length() in 1..limits.maxCompressedArchiveBytes) {
      "Backup archive has an invalid compressed size"
    }
    val archiveParent =
      archive.absoluteFile.parentFile
        ?: throw BackupArchiveException("Backup archive has no private staging parent")
    val stagingRoot = archiveParent.resolve(".restore-${UUID.randomUUID()}")
    try {
      requireArchive(stagingRoot.mkdir()) { "Could not create restore staging directory" }
      ZipFile(archive).use { zip ->
        val zipEntries = zip.entries().asSequence().toList()
        requireArchive(zipEntries.size <= limits.maxEntryCount + 1) {
          "Backup archive has too many entries"
        }
        val paths = zipEntries.map { it.name }
        paths.forEach(::validateArchivePath)
        requireArchive(paths.size == paths.toSet().size) { "Backup archive has duplicate paths" }
        val manifestZipEntry =
          zip.getEntry(MANIFEST_PATH) ?: throw BackupArchiveException("Backup manifest is missing")
        val manifest =
          zip.getInputStream(manifestZipEntry).use { input ->
            val bytes = readBounded(input, limits.maxManifestBytes, "Backup manifest is too large")
            parseManifest(JSONObject(bytes.toString(Charsets.UTF_8)))
          }
        validateManifest(manifest)
        val declaredPaths = manifest.entries.map { it.path }
        requireArchive(paths.toSet() == (declaredPaths + MANIFEST_PATH).toSet()) {
          "Backup archive contains missing or undeclared files"
        }

        var actualTotal = 0L
        manifest.entries.forEach { declared ->
          validateMediaType(declared.path, declared.mediaType)
          val entry =
            zip.getEntry(declared.path)
              ?: throw BackupArchiveException("Declared backup payload is missing")
          val actual =
            zip.getInputStream(entry).use { input ->
              hashAndCopyBounded(input, null, limits.maxEntryBytes)
            }
          validateCompressionRatio(entry.compressedSize, actual.size)
          requireArchive(actual.size == declared.uncompressedSize) {
            "Backup payload size does not match its manifest"
          }
          requireArchive(actual.sha256 == declared.sha256) {
            "Backup payload checksum does not match its manifest"
          }
          actualTotal = checkedAdd(actualTotal, actual.size)
          requireArchive(actualTotal <= limits.maxTotalUncompressedBytes) {
            "Backup payload exceeds the uncompressed size limit"
          }
        }
        requireArchive(actualTotal == manifest.totalUncompressedPayloadSize) {
          "Backup total size does not match its manifest"
        }

        val dataEntry =
          zip.getEntry(DATA_PATH) ?: throw BackupArchiveException("Backup data payload is missing")
        val snapshot =
          zip.getInputStream(dataEntry).use { input ->
            parseSnapshot(
              JSONObject(
                readBounded(input, limits.maxEntryBytes, "Backup data is too large")
                  .toString(Charsets.UTF_8)
              )
            )
          }
        val stagedAssets =
          manifest.entries
            .filter { it.path.startsWith(ASSET_PREFIX) }
            .map { declared ->
              val destination = safeDestination(stagingRoot, declared.path)
              val destinationParent = checkNotNull(destination.parentFile)
              requireArchive(destinationParent.mkdirs() || destinationParent.isDirectory) {
                "Could not stage backup asset"
              }
              zip.getInputStream(zip.getEntry(declared.path)).use { input ->
                destination.outputStream().use { output ->
                  val actual = hashAndCopyBounded(input, output, limits.maxEntryBytes)
                  requireArchive(
                    actual.size == declared.uncompressedSize && actual.sha256 == declared.sha256
                  ) {
                    "Backup asset changed during validation"
                  }
                }
              }
              val petId = declared.path.removePrefix(ASSET_PREFIX).substringBefore('/')
              BackupAsset(petId, destination.name, declared.mediaType, destination)
            }
        val completeSnapshot = snapshot.copy(assets = stagedAssets)
        stagedAssets.forEach(::validateAsset)
        validatePortableReferences(completeSnapshot)
        validateEntryCounts(manifest, completeSnapshot)
        return ValidatedBackupArchive(manifest, completeSnapshot, stagingRoot)
      }
    } catch (error: BackupArchiveException) {
      stagingRoot.deleteRecursively()
      throw error
    } catch (error: Exception) {
      stagingRoot.deleteRecursively()
      throw BackupArchiveException("Backup archive is invalid", error)
    }
  }

  private fun makePortable(snapshot: BackupSnapshot): BackupSnapshot {
    requireArchive(snapshot.exportBundle.membershipChanges.isEmpty()) {
      "Device and family authorization data cannot be backed up"
    }
    val assetsByPet = snapshot.assets.associateBy { it.petId }
    requireArchive(assetsByPet.size == snapshot.assets.size) { "A pet has duplicate backup assets" }
    val petIds = snapshot.exportBundle.pets.map { it.id }.toSet()
    requireArchive(assetsByPet.keys.all { it in petIds }) { "A backup asset has no matching pet" }
    val portablePets =
      snapshot.exportBundle.pets.map { pet ->
        val asset = assetsByPet[pet.id]
        when {
          pet.photoUri != null && asset == null ->
            throw BackupArchiveException("A pet photo is not available as a portable asset")
          asset != null -> pet.copy(photoUri = assetPath(asset))
          else -> pet
        }
      }
    return snapshot.copy(
      exportBundle =
        snapshot.exportBundle.copy(pets = portablePets, membershipChanges = emptyList())
    )
  }

  private fun validatePortableReferences(snapshot: BackupSnapshot) {
    requireArchive(snapshot.exportBundle.membershipChanges.isEmpty()) {
      "Device and family authorization data cannot be restored from a backup"
    }
    val assetsByPet = snapshot.assets.associateBy { it.petId }
    requireArchive(assetsByPet.size == snapshot.assets.size) { "A pet has duplicate backup assets" }
    val references = snapshot.exportBundle.pets.mapNotNull { it.photoUri }.toSet()
    val assetPaths = snapshot.assets.map(::assetPath).toSet()
    requireArchive(references == assetPaths) {
      "Backup asset references are incomplete or undeclared"
    }
    snapshot.exportBundle.pets.forEach { pet ->
      val expected = assetsByPet[pet.id]?.let(::assetPath)
      requireArchive(pet.photoUri == expected) {
        "A backup asset is associated with the wrong pet"
      }
    }
    val validationErrors = ExportBundle.validate(snapshot.exportBundle)
    requireArchive(validationErrors.isEmpty()) {
      "Backup data is invalid: ${validationErrors.first()}"
    }
    validatePreferences(snapshot)
  }

  private fun validatePreferences(snapshot: BackupSnapshot) {
    val reminder = snapshot.reminderPreferences
    requireArchive(reminder.vaccinationDaysBefore in 0..365) {
      "Invalid vaccination reminder setting"
    }
    requireArchive(reminder.dewormingDaysBefore in 0..365) { "Invalid deworming reminder setting" }
    requireArchive(reminder.weightReminderIntervalDays in 1..365) {
      "Invalid weight reminder setting"
    }
    requireArchive(reminder.defaultNotificationHour in 0..23) { "Invalid notification hour" }
    requireArchive(reminder.defaultNotificationMinute in 0..59) { "Invalid notification minute" }
  }

  private fun validateAsset(asset: BackupAsset) {
    validateComponent(asset.petId, "pet ID")
    validateComponent(asset.fileName, "asset filename")
    validateMediaType(assetPath(asset), asset.mediaType)
    requireArchive(asset.source.isFile && asset.source.length() in 1..limits.maxEntryBytes) {
      "Backup asset is unavailable or too large"
    }
    val header = ByteArray(8)
    val read = asset.source.inputStream().use { it.read(header) }
    val validSignature =
      when (asset.mediaType) {
        JPEG_MEDIA_TYPE -> read >= 3 && header.take(3) == JPEG_SIGNATURE
        PNG_MEDIA_TYPE -> read >= 8 && header.toList() == PNG_SIGNATURE
        else -> false
      }
    requireArchive(validSignature) { "Backup asset content does not match its media type" }
  }

  private fun manifest(
    request: BackupArchiveRequest,
    snapshot: BackupSnapshot,
    entries: List<BackupPayloadEntry>,
    totalSize: Long,
  ) =
    BackupManifest(
      archiveFormatVersion = ARCHIVE_FORMAT_VERSION,
      dataSchemaVersion = snapshot.exportBundle.metadata.schemaVersion,
      backupId = request.backupId,
      createdAt = request.createdAt,
      appVersion = request.appVersion,
      trigger = request.trigger,
      entryCounts =
        linkedMapOf(
          "pets" to snapshot.exportBundle.pets.size,
          "weights" to snapshot.exportBundle.weightEntries.size,
          "vaccinations" to snapshot.exportBundle.vaccinationEntries.size,
          "dewormings" to snapshot.exportBundle.dewormingEntries.size,
          "tasks" to snapshot.exportBundle.tasks.size,
          "assets" to snapshot.assets.size,
        ),
      entries = entries,
      payloadCount = entries.size,
      totalUncompressedPayloadSize = totalSize,
    )

  private fun payloadEntry(path: String, mediaType: String, file: File): BackupPayloadEntry {
    requireArchive(file.length() <= limits.maxEntryBytes) { "Backup payload entry is too large" }
    return BackupPayloadEntry(path, file.length(), mediaType, sha256(file))
  }

  private fun validateManifest(manifest: BackupManifest) {
    requireArchive(manifest.archiveFormatVersion == ARCHIVE_FORMAT_VERSION) {
      "Unsupported backup archive format"
    }
    requireArchive(manifest.dataSchemaVersion == SUPPORTED_DATA_SCHEMA_VERSION) {
      "Unsupported backup data schema"
    }
    validateComponent(manifest.backupId, "backup ID")
    requireArchive(manifest.payloadCount == manifest.entries.size) {
      "Backup payload count does not match its manifest"
    }
    requireArchive(manifest.entries.size <= limits.maxEntryCount) { "Backup has too many entries" }
    requireArchive(manifest.entries.map { it.path }.toSet().size == manifest.entries.size) {
      "Backup manifest contains duplicate paths"
    }
    requireArchive(manifest.entryCounts.keys == ENTRY_COUNT_KEYS.toSet()) {
      "Backup manifest has unknown or missing entry counts"
    }
    requireArchive(manifest.entryCounts.values.all { it >= 0 }) {
      "Backup manifest contains a negative entry count"
    }
    manifest.entries.forEach { entry ->
      validateArchivePath(entry.path)
      requireArchive(entry.uncompressedSize in 0..limits.maxEntryBytes) {
        "Backup entry exceeds its size limit"
      }
      requireArchive(entry.sha256.matches(SHA256_PATTERN)) { "Backup checksum is malformed" }
    }
    requireArchive(manifest.entries.count { it.path == DATA_PATH } == 1) {
      "Backup must declare one data payload"
    }
    val declaredTotal =
      manifest.entries.fold(0L) { total, entry -> checkedAdd(total, entry.uncompressedSize) }
    requireArchive(declaredTotal == manifest.totalUncompressedPayloadSize) {
      "Backup manifest total is inconsistent"
    }
  }

  private fun validateEntryCounts(manifest: BackupManifest, snapshot: BackupSnapshot) {
    val actual =
      mapOf(
        "pets" to snapshot.exportBundle.pets.size,
        "weights" to snapshot.exportBundle.weightEntries.size,
        "vaccinations" to snapshot.exportBundle.vaccinationEntries.size,
        "dewormings" to snapshot.exportBundle.dewormingEntries.size,
        "tasks" to snapshot.exportBundle.tasks.size,
        "assets" to snapshot.assets.size,
      )
    requireArchive(manifest.entryCounts == actual) {
      "Backup entry counts do not match the validated payload"
    }
  }

  private fun snapshotToJson(snapshot: BackupSnapshot): JSONObject =
    snapshot.exportBundle.toJson().apply {
      put(
        "appPreferences",
        JSONObject()
          .put("theme", snapshot.appPreferences.theme.name)
          .put("language", snapshot.appPreferences.language.name)
          .put("hasCompletedOnboarding", snapshot.appPreferences.hasCompletedOnboarding),
      )
      put(
        "reminderPreferences",
        JSONObject()
          .put(
            "vaccinationRemindersEnabled",
            snapshot.reminderPreferences.vaccinationRemindersEnabled,
          )
          .put("vaccinationDaysBefore", snapshot.reminderPreferences.vaccinationDaysBefore)
          .put("dewormingRemindersEnabled", snapshot.reminderPreferences.dewormingRemindersEnabled)
          .put("dewormingDaysBefore", snapshot.reminderPreferences.dewormingDaysBefore)
          .put("weightRemindersEnabled", snapshot.reminderPreferences.weightRemindersEnabled)
          .put(
            "weightReminderIntervalDays",
            snapshot.reminderPreferences.weightReminderIntervalDays,
          )
          .put("defaultNotificationHour", snapshot.reminderPreferences.defaultNotificationHour)
          .put("defaultNotificationMinute", snapshot.reminderPreferences.defaultNotificationMinute),
      )
    }

  private fun parseSnapshot(json: JSONObject): BackupSnapshot {
    val app = json.getJSONObject("appPreferences")
    val reminder = json.getJSONObject("reminderPreferences")
    val snapshot =
      BackupSnapshot(
        exportBundle = ExportBundle.fromJson(json),
        appPreferences =
          BackupAppPreferences(
            theme = AppTheme.valueOf(app.getString("theme")),
            language = AppLanguage.valueOf(app.getString("language")),
            hasCompletedOnboarding = app.getBoolean("hasCompletedOnboarding"),
          ),
        reminderPreferences =
          BackupReminderPreferences(
            vaccinationRemindersEnabled = reminder.getBoolean("vaccinationRemindersEnabled"),
            vaccinationDaysBefore = reminder.getInt("vaccinationDaysBefore"),
            dewormingRemindersEnabled = reminder.getBoolean("dewormingRemindersEnabled"),
            dewormingDaysBefore = reminder.getInt("dewormingDaysBefore"),
            weightRemindersEnabled = reminder.getBoolean("weightRemindersEnabled"),
            weightReminderIntervalDays = reminder.getInt("weightReminderIntervalDays"),
            defaultNotificationHour = reminder.getInt("defaultNotificationHour"),
            defaultNotificationMinute = reminder.getInt("defaultNotificationMinute"),
          ),
        assets = emptyList(),
      )
    validatePreferences(snapshot)
    return snapshot
  }

  private fun manifestToJson(manifest: BackupManifest): JSONObject =
    JSONObject()
      .put("archiveFormatVersion", manifest.archiveFormatVersion)
      .put("dataSchemaVersion", manifest.dataSchemaVersion)
      .put("backupId", manifest.backupId)
      .put("createdAt", manifest.createdAt.toString())
      .put("appVersion", manifest.appVersion)
      .put("trigger", manifest.trigger.name)
      .put("entryCounts", JSONObject(manifest.entryCounts))
      .put(
        "entries",
        JSONArray(
          manifest.entries.map { entry ->
            JSONObject()
              .put("path", entry.path)
              .put("uncompressedSize", entry.uncompressedSize)
              .put("mediaType", entry.mediaType)
              .put("sha256", entry.sha256)
          }
        ),
      )
      .put("payloadCount", manifest.payloadCount)
      .put("totalUncompressedPayloadSize", manifest.totalUncompressedPayloadSize)

  private fun parseManifest(json: JSONObject): BackupManifest {
    val counts = json.getJSONObject("entryCounts")
    val countKeys = counts.keys().asSequence().toSet()
    requireArchive(countKeys == ENTRY_COUNT_KEYS.toSet()) {
      "Backup manifest has unknown or missing entry counts"
    }
    val entries = json.getJSONArray("entries")
    return BackupManifest(
      archiveFormatVersion = json.getInt("archiveFormatVersion"),
      dataSchemaVersion = json.getInt("dataSchemaVersion"),
      backupId = json.getString("backupId"),
      createdAt = java.time.Instant.parse(json.getString("createdAt")),
      appVersion = json.getString("appVersion"),
      trigger = BackupTrigger.valueOf(json.getString("trigger")),
      entryCounts =
        ENTRY_COUNT_KEYS.associateWith { key ->
          requireArchive(counts.has(key)) { "Backup entry count is missing" }
          counts.getInt(key)
        },
      entries =
        (0 until entries.length()).map { index ->
          entries.getJSONObject(index).let { entry ->
            BackupPayloadEntry(
              path = entry.getString("path"),
              uncompressedSize = entry.getLong("uncompressedSize"),
              mediaType = entry.getString("mediaType"),
              sha256 = entry.getString("sha256"),
            )
          }
        },
      payloadCount = json.getInt("payloadCount"),
      totalUncompressedPayloadSize = json.getLong("totalUncompressedPayloadSize"),
    )
  }

  private fun validateMediaType(path: String, mediaType: String) {
    val valid =
      when {
        path == DATA_PATH -> mediaType == DATA_MEDIA_TYPE
        path.startsWith(ASSET_PREFIX) && path.endsWith(".jpg", true) -> mediaType == JPEG_MEDIA_TYPE
        path.startsWith(ASSET_PREFIX) && path.endsWith(".jpeg", true) ->
          mediaType == JPEG_MEDIA_TYPE
        path.startsWith(ASSET_PREFIX) && path.endsWith(".png", true) -> mediaType == PNG_MEDIA_TYPE
        else -> false
      }
    requireArchive(valid) { "Backup payload has an invalid media type or path" }
  }

  private fun assetPath(asset: BackupAsset) = "$ASSET_PREFIX${asset.petId}/${asset.fileName}"

  private fun validateArchivePath(path: String) {
    requireArchive(path.length in 1..limits.maxPathLength) { "Backup path has an invalid length" }
    requireArchive(!path.startsWith('/') && !path.startsWith('\\')) {
      "Absolute backup paths are forbidden"
    }
    requireArchive(!WINDOWS_DRIVE_PATTERN.containsMatchIn(path)) {
      "Absolute backup paths are forbidden"
    }
    requireArchive(path.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
      "Backup path traversal is forbidden"
    }
    requireArchive('\\' !in path && '\u0000' !in path) {
      "Backup path contains forbidden characters"
    }
  }

  private fun validateComponent(value: String, label: String) {
    requireArchive(value.matches(SAFE_COMPONENT_PATTERN)) { "Invalid $label" }
  }

  private fun safeDestination(root: File, path: String): File {
    validateArchivePath(path)
    val rootPath = root.canonicalFile.toPath()
    val destination = root.resolve(path).canonicalFile
    requireArchive(destination.toPath().startsWith(rootPath)) { "Backup path escapes staging" }
    return destination
  }

  private fun writeZipEntry(zip: ZipOutputStream, path: String, input: InputStream) {
    validateArchivePath(path)
    zip.putNextEntry(ZipEntry(path).apply { time = 0L })
    input.use { it.copyTo(zip) }
    zip.closeEntry()
  }

  private fun writePayloadZipEntry(
    zip: ZipOutputStream,
    declared: BackupPayloadEntry,
    source: File,
  ) {
    validateArchivePath(declared.path)
    zip.putNextEntry(ZipEntry(declared.path).apply { time = 0L })
    val actual =
      source.inputStream().use { input -> hashAndCopyBounded(input, zip, limits.maxEntryBytes) }
    zip.closeEntry()
    requireArchive(actual.size == declared.uncompressedSize && actual.sha256 == declared.sha256) {
      "Backup payload changed while the archive was written"
    }
  }

  private fun validateCompressionRatio(compressedSize: Long, uncompressedSize: Long) {
    requireArchive(compressedSize >= 0) { "Backup entry has an unknown compressed size" }
    if (uncompressedSize == 0L) return
    requireArchive(
      compressedSize > 0 && uncompressedSize <= compressedSize * limits.maxCompressionRatio
    ) {
      "Backup entry exceeds the compression ratio limit"
    }
  }

  private fun writeBounded(file: File, maxBytes: Long, write: (OutputStream) -> Unit) {
    val bounded = BoundedOutputStream(file.outputStream(), maxBytes)
    bounded.use(write)
  }

  private fun readBounded(input: InputStream, maxBytes: Long, message: String): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val actual = hashAndCopyBounded(input, output, maxBytes)
    requireArchive(actual.size <= maxBytes) { message }
    return output.toByteArray()
  }

  private fun hashAndCopyBounded(
    input: InputStream,
    output: OutputStream?,
    maxBytes: Long,
  ): HashAndSize {
    val digest = MessageDigest.getInstance(SHA256)
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
      val read = input.read(buffer)
      if (read < 0) break
      total = checkedAdd(total, read.toLong())
      requireArchive(total <= maxBytes) { "Backup entry exceeds its size limit" }
      digest.update(buffer, 0, read)
      output?.write(buffer, 0, read)
    }
    return HashAndSize(total, digest.digest().toHex())
  }

  private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance(SHA256)
    DigestInputStream(FileInputStream(file), digest).use { input ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      while (input.read(buffer) >= 0) Unit
    }
    return digest.digest().toHex()
  }

  private fun checkedAdd(first: Long, second: Long): Long {
    requireArchive(second >= 0 && first <= Long.MAX_VALUE - second) { "Backup size overflow" }
    return first + second
  }

  private data class HashAndSize(val size: Long, val sha256: String)

  private class BoundedOutputStream(
    private val delegate: OutputStream,
    private val maxBytes: Long,
  ) : OutputStream() {
    private var count = 0L

    override fun write(value: Int) {
      ensureCapacity(1)
      delegate.write(value)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
      ensureCapacity(length)
      delegate.write(bytes, offset, length)
    }

    override fun close() = delegate.close()

    private fun ensureCapacity(length: Int) {
      if (length < 0 || count > maxBytes - length) {
        throw BackupArchiveException("Backup data is too large")
      }
      count += length
    }
  }

  private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

  private inline fun requireArchive(condition: Boolean, message: () -> String) {
    if (!condition) throw BackupArchiveException(message())
  }

  companion object {
    private const val ARCHIVE_FORMAT_VERSION = 1
    private const val SUPPORTED_DATA_SCHEMA_VERSION = 1
    private const val MANIFEST_PATH = "manifest.json"
    private const val DATA_PATH = "data/export.json"
    private const val ASSET_PREFIX = "assets/pets/"
    private const val DATA_MEDIA_TYPE = "application/json"
    private const val JPEG_MEDIA_TYPE = "image/jpeg"
    private const val PNG_MEDIA_TYPE = "image/png"
    private const val SHA256 = "SHA-256"
    private val ENTRY_COUNT_KEYS =
      listOf("pets", "weights", "vaccinations", "dewormings", "tasks", "assets")
    private val SAFE_COMPONENT_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
    private val WINDOWS_DRIVE_PATTERN = Regex("^[A-Za-z]:")
    private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    private val JPEG_SIGNATURE = listOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_SIGNATURE =
      listOf(
        0x89.toByte(),
        0x50.toByte(),
        0x4E.toByte(),
        0x47.toByte(),
        0x0D.toByte(),
        0x0A.toByte(),
        0x1A.toByte(),
        0x0A.toByte(),
      )
  }
}
