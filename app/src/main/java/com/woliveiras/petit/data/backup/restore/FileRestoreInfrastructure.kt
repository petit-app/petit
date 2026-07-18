package com.woliveiras.petit.data.backup.restore

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.woliveiras.petit.BuildConfig
import com.woliveiras.petit.data.repository.ReminderPreferences
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.domain.backup.archive.BackupAsset
import com.woliveiras.petit.domain.backup.restore.PreparedRestoreAssets
import com.woliveiras.petit.domain.backup.restore.RestoreAssetInstaller
import com.woliveiras.petit.domain.backup.restore.RestoreRecoveryJournal
import com.woliveiras.petit.domain.backup.restore.RestoreRecoveryPhase
import com.woliveiras.petit.domain.backup.restore.RestoreRecoveryState
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.ExportBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

/** Two-phase installer for app-owned photo bytes with an idempotent operation marker. */
class AndroidRestoreAssetInstaller
internal constructor(private val context: Context, private val referenceForFile: (File) -> String) :
  RestoreAssetInstaller {
  @Inject
  constructor(
    @ApplicationContext context: Context
  ) : this(
    context,
    { file ->
      FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        .toString()
    },
  )

  override fun prepare(assets: List<BackupAsset>): PreparedRestoreAssets {
    val operationId = UUID.randomUUID().toString()
    val operationDirectory = operationsDirectory().resolve(operationId)
    val pendingDirectory = operationDirectory.resolve("pending")
    try {
      check(pendingDirectory.mkdirs()) { "Could not create restore asset staging" }
      val entries =
        assets
          .sortedBy { it.petId }
          .map { asset ->
            val extension = extension(asset.mediaType)
            val pending = pendingDirectory.resolve("${UUID.randomUUID()}.$extension")
            val finalFile = photoDirectory().resolve("${UUID.randomUUID()}.$extension")
            copyBounded(asset.source, pending)
            AssetOperationEntry(
              archivePath = "assets/pets/${asset.petId}/${asset.fileName}",
              pendingPath = pending.absolutePath,
              finalPath = finalFile.absolutePath,
              finalReference = referenceForFile(finalFile),
            )
          }
      writeMarker(operationDirectory, entries)
      return Prepared(operationId, entries)
    } catch (error: Exception) {
      operationDirectory.deleteRecursively()
      throw error
    }
  }

  override fun rollback(operationId: String) {
    val operationDirectory = operationDirectory(operationId)
    readMarker(operationDirectory).forEach { entry ->
      File(entry.pendingPath).delete()
      File(entry.finalPath).delete()
    }
    operationDirectory.deleteRecursively()
  }

  override fun commit(
    operationId: String,
    activeReferences: Set<String>,
    previousReferences: Set<String>,
  ) {
    val operationDirectory = operationDirectory(operationId)
    readMarker(operationDirectory).forEach { entry ->
      File(entry.pendingPath).delete()
      if (entry.finalReference !in activeReferences) File(entry.finalPath).delete()
    }
    previousReferences
      .filter { it !in activeReferences }
      .mapNotNull(::ownedPhotoFile)
      .forEach(File::delete)
    operationDirectory.deleteRecursively()
  }

  override fun cleanupOrphans(activeOperationId: String?) {
    if (!operationsDirectory().isDirectory) return
    operationsDirectory()
      .listFiles()
      .orEmpty()
      .filter { it.isDirectory && it.name != activeOperationId }
      .forEach(File::deleteRecursively)
  }

  private inner class Prepared(
    override val operationId: String,
    private val entries: List<AssetOperationEntry>,
  ) : PreparedRestoreAssets {
    override val referencesByArchivePath = entries.associate { it.archivePath to it.finalReference }

    override fun promote() {
      try {
        entries.forEach { entry ->
          val pending = File(entry.pendingPath)
          val destination = File(entry.finalPath)
          check(pending.isFile) { "Staged restore asset is missing" }
          check(pending.renameTo(destination)) { "Could not install restored asset" }
        }
      } catch (error: Exception) {
        rollback(operationId)
        throw error
      }
    }
  }

  private fun writeMarker(directory: File, entries: List<AssetOperationEntry>) {
    val marker = directory.resolve(MARKER_FILE)
    val partial = directory.resolve("$MARKER_FILE.partial")
    val json =
      JSONObject()
        .put(
          "entries",
          JSONArray(
            entries.map { entry ->
              JSONObject()
                .put("archivePath", entry.archivePath)
                .put("pendingPath", entry.pendingPath)
                .put("finalPath", entry.finalPath)
                .put("finalReference", entry.finalReference)
            }
          ),
        )
    FileOutputStream(partial).use { output ->
      output.write(json.toString().toByteArray(Charsets.UTF_8))
      output.fd.sync()
    }
    Files.move(partial.toPath(), marker.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
  }

  private fun readMarker(operationDirectory: File): List<AssetOperationEntry> {
    val marker = operationDirectory.resolve(MARKER_FILE)
    if (!marker.isFile) return emptyList()
    val entries = JSONObject(marker.readText()).getJSONArray("entries")
    return (0 until entries.length()).map { index ->
      entries.getJSONObject(index).let { json ->
        AssetOperationEntry(
          archivePath = json.getString("archivePath"),
          pendingPath = checkedOperationPath(operationDirectory, json.getString("pendingPath")),
          finalPath = checkedPhotoPath(json.getString("finalPath")),
          finalReference = json.getString("finalReference"),
        )
      }
    }
  }

  private fun operationDirectory(operationId: String): File {
    require(operationId.matches(SAFE_ID)) { "Invalid restore operation ID" }
    return operationsDirectory().resolve(operationId)
  }

  private fun checkedOperationPath(operationDirectory: File, path: String): String {
    val candidate = File(path).canonicalFile
    require(candidate.toPath().startsWith(operationDirectory.canonicalFile.toPath())) {
      "Restore marker path escapes its operation directory"
    }
    return candidate.absolutePath
  }

  private fun checkedPhotoPath(path: String): String {
    val candidate = File(path).canonicalFile
    require(candidate.toPath().startsWith(photoDirectory().canonicalFile.toPath())) {
      "Restore marker path escapes app-owned photos"
    }
    return candidate.absolutePath
  }

  private fun ownedPhotoFile(reference: String): File? {
    val uri = Uri.parse(reference)
    if (uri.scheme != "content" || uri.authority != "${BuildConfig.APPLICATION_ID}.fileprovider") {
      return null
    }
    val name = uri.lastPathSegment ?: return null
    if (!name.matches(SAFE_FILE)) return null
    return photoDirectory().resolve(name)
  }

  private fun copyBounded(source: File, destination: File) {
    require(source.isFile) { "Validated restore asset is missing" }
    source.inputStream().use { input ->
      destination.outputStream().use { output ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
          val read = input.read(buffer)
          if (read < 0) break
          total += read
          require(total <= MAX_ASSET_BYTES) { "Restore asset exceeds its size limit" }
          output.write(buffer, 0, read)
        }
        require(total > 0) { "Restore asset is empty" }
      }
    }
  }

  private fun photoDirectory() =
    context.filesDir.resolve(PHOTO_DIRECTORY).also { check(it.mkdirs() || it.isDirectory) }

  private fun operationsDirectory() =
    context.filesDir.resolve(OPERATIONS_DIRECTORY).also { check(it.mkdirs() || it.isDirectory) }

  private fun extension(mediaType: String) =
    when (mediaType) {
      "image/jpeg" -> "jpg"
      "image/png" -> "png"
      else -> throw IllegalArgumentException("Unsupported restore asset media type")
    }

  private data class AssetOperationEntry(
    val archivePath: String,
    val pendingPath: String,
    val finalPath: String,
    val finalReference: String,
  )

  private companion object {
    const val PHOTO_DIRECTORY = "pet_photos"
    const val OPERATIONS_DIRECTORY = "backup_restore/operations"
    const val MARKER_FILE = "assets.json"
    const val MAX_ASSET_BYTES = 16L * 1024L * 1024L
    val SAFE_ID = Regex("[A-Za-z0-9-]{1,64}")
    val SAFE_FILE = Regex("[A-Za-z0-9-]{1,64}\\.(jpg|png)")
  }
}

/** Atomic app-private journal used to finish or roll back a restore after process death. */
private fun atomicReplaceRestoreJournal(partial: File, journal: File) {
  Files.move(partial.toPath(), journal.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
}

class FileRestoreRecoveryJournal(
  private val directory: File,
  private val publish: (partial: File, journal: File) -> Unit = ::atomicReplaceRestoreJournal,
) : RestoreRecoveryJournal {
  override fun read(): RestoreRecoveryState? {
    val file = journalFile()
    if (!file.isFile) return null
    val json = JSONObject(file.readText())
    return RestoreRecoveryState(
      phase = RestoreRecoveryPhase.valueOf(json.getString("phase")),
      assetOperationId = json.getString("assetOperationId"),
      oldBundle = ExportBundle.fromJson(json.getJSONObject("oldBundle")),
      oldUserPreferences = userPreferences(json.getJSONObject("oldUserPreferences")),
      oldReminderPreferences = reminderPreferences(json.getJSONObject("oldReminderPreferences")),
      targetUserPreferences = userPreferences(json.getJSONObject("targetUserPreferences")),
      targetReminderPreferences =
        reminderPreferences(json.getJSONObject("targetReminderPreferences")),
      previousAssetReferences =
        json.getJSONArray("previousAssetReferences").let { array ->
          (0 until array.length()).map(array::getString).toSet()
        },
    )
  }

  override fun write(state: RestoreRecoveryState) {
    require(directory.mkdirs() || directory.isDirectory) { "Restore journal is unavailable" }
    val partial = directory.resolve("$JOURNAL_FILE.partial")
    val journal = journalFile()
    try {
      val bytes = state.toJson().toString().toByteArray(Charsets.UTF_8)
      FileOutputStream(partial).use { output ->
        output.write(bytes)
        output.fd.sync()
      }
      publish(partial, journal)
    } catch (error: Exception) {
      partial.delete()
      throw IllegalStateException("Could not atomically publish restore journal", error)
    }
  }

  override fun clear() {
    journalFile().delete()
    directory.resolve("$JOURNAL_FILE.partial").delete()
  }

  private fun journalFile() = directory.absoluteFile.resolve(JOURNAL_FILE)

  private fun RestoreRecoveryState.toJson() =
    JSONObject()
      .put("phase", phase.name)
      .put("assetOperationId", assetOperationId)
      .put("oldBundle", oldBundle.toJson())
      .put("oldUserPreferences", oldUserPreferences.toJson())
      .put("oldReminderPreferences", oldReminderPreferences.toJson())
      .put("targetUserPreferences", targetUserPreferences.toJson())
      .put("targetReminderPreferences", targetReminderPreferences.toJson())
      .put("previousAssetReferences", JSONArray(previousAssetReferences.sorted()))

  private fun UserPreferences.toJson() =
    JSONObject()
      .put("theme", theme.name)
      .put("language", language.name)
      .put("hasCompletedOnboarding", hasCompletedOnboarding)

  private fun ReminderPreferences.toJson() =
    JSONObject()
      .put("vaccinationRemindersEnabled", vaccinationRemindersEnabled)
      .put("vaccinationDaysBefore", vaccinationDaysBefore)
      .put("dewormingRemindersEnabled", dewormingRemindersEnabled)
      .put("dewormingDaysBefore", dewormingDaysBefore)
      .put("weightRemindersEnabled", weightRemindersEnabled)
      .put("weightReminderIntervalDays", weightReminderIntervalDays)
      .put("defaultNotificationHour", defaultNotificationHour)
      .put("defaultNotificationMinute", defaultNotificationMinute)

  private fun userPreferences(json: JSONObject) =
    UserPreferences(
      theme = AppTheme.valueOf(json.getString("theme")),
      language = AppLanguage.valueOf(json.getString("language")),
      hasCompletedOnboarding = json.getBoolean("hasCompletedOnboarding"),
    )

  private fun reminderPreferences(json: JSONObject) =
    ReminderPreferences(
      vaccinationRemindersEnabled = json.getBoolean("vaccinationRemindersEnabled"),
      vaccinationDaysBefore = json.getInt("vaccinationDaysBefore"),
      dewormingRemindersEnabled = json.getBoolean("dewormingRemindersEnabled"),
      dewormingDaysBefore = json.getInt("dewormingDaysBefore"),
      weightRemindersEnabled = json.getBoolean("weightRemindersEnabled"),
      weightReminderIntervalDays = json.getInt("weightReminderIntervalDays"),
      defaultNotificationHour = json.getInt("defaultNotificationHour"),
      defaultNotificationMinute = json.getInt("defaultNotificationMinute"),
    )

  private companion object {
    const val JOURNAL_FILE = "restore.json"
  }
}
