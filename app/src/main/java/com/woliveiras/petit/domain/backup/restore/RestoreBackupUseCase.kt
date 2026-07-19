package com.woliveiras.petit.domain.backup.restore

import com.woliveiras.petit.data.repository.ReminderPreferences
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.backup.BackupDownloadTarget
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import com.woliveiras.petit.domain.backup.archive.BackupAppPreferences
import com.woliveiras.petit.domain.backup.archive.BackupArchiveCodec
import com.woliveiras.petit.domain.backup.archive.BackupAsset
import com.woliveiras.petit.domain.backup.archive.BackupReminderPreferences
import com.woliveiras.petit.domain.backup.archive.ValidatedBackupArchive
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.MergeResult
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.flow.first

enum class RestoreMode {
  MERGE,
  REPLACE,
}

data class RestoreBackupRequest(
  val remoteId: String,
  val mode: RestoreMode,
  val applyBackupPreferences: Boolean = mode == RestoreMode.REPLACE,
)

data class RestoreBackupResult(val metadata: BackupMetadata, val mergeResult: MergeResult)

interface RestoreBackupAction {
  suspend fun execute(
    request: RestoreBackupRequest,
    onProgress: (BackupProgress) -> Unit = {},
  ): RestoreBackupResult
}

interface PreparedRestoreAssets {
  val operationId: String
  val referencesByArchivePath: Map<String, String>

  fun promote()
}

interface RestoreAssetInstaller {
  fun prepare(assets: List<BackupAsset>): PreparedRestoreAssets

  fun rollback(operationId: String)

  fun commit(operationId: String, activeReferences: Set<String>, previousReferences: Set<String>)

  fun cleanupOrphans(activeOperationId: String?) = Unit
}

interface RestoreReminderScheduler {
  suspend fun rescheduleCurrentTasks()
}

enum class RestoreRecoveryPhase {
  PREPARED,
  ROOM_COMMITTED,
  PREFERENCES_COMMITTED,
}

data class RestoreRecoveryState(
  val phase: RestoreRecoveryPhase,
  val assetOperationId: String,
  val oldBundle: ExportBundle,
  val oldUserPreferences: UserPreferences,
  val oldReminderPreferences: ReminderPreferences,
  val targetUserPreferences: UserPreferences,
  val targetReminderPreferences: ReminderPreferences,
  val previousAssetReferences: Set<String>,
)

interface RestoreRecoveryJournal {
  fun read(): RestoreRecoveryState?

  fun write(state: RestoreRecoveryState)

  fun clear()
}

/** Downloads to a unique private file and validates the complete archive before returning it. */
class DownloadAndValidateBackupUseCase(
  private val storageGateway: BackupStorageGateway,
  private val codec: BackupArchiveCodec,
  private val stagingRoot: File,
) {
  fun cleanupOrphans() {
    if (!stagingRoot.isDirectory) return
    stagingRoot
      .listFiles()
      .orEmpty()
      .filter { it.name.startsWith("download-") || it.name.startsWith(".restore-") }
      .forEach(File::deleteRecursively)
  }

  suspend fun execute(
    remoteId: String,
    onProgress: (BackupProgress) -> Unit = {},
  ): DownloadedValidatedBackup {
    require(remoteId.isNotBlank()) { "Remote backup ID cannot be blank" }
    require(stagingRoot.mkdirs() || stagingRoot.isDirectory) { "Restore staging is unavailable" }
    val operationDirectory = stagingRoot.resolve("download-${UUID.randomUUID()}")
    try {
      check(operationDirectory.mkdir()) { "Could not create download staging" }
      val archive = operationDirectory.resolve("backup.zip")
      val target = FileDownloadTarget(archive)
      var lastProgress = -1L
      val download =
        storageGateway.download(remoteId, target) { progress ->
          if (progress.bytesTransferred >= lastProgress) {
            lastProgress = progress.bytesTransferred
            onProgress(progress)
          }
        }
      require(download.metadata.isRecognized) { "Remote file is not a recognized Petit backup" }
      require(download.bytesDownloaded == archive.length()) { "Downloaded size is inconsistent" }
      require(download.metadata.archiveSizeBytes == archive.length()) {
        "Downloaded archive size does not match remote metadata"
      }
      require(download.metadata.archiveSha256 == sha256(archive)) {
        "Downloaded archive checksum does not match remote metadata"
      }
      val validated = codec.validate(archive)
      require(validated.manifest.backupId == download.metadata.backupId) {
        "Downloaded backup ID does not match its archive"
      }
      return DownloadedValidatedBackup(download.metadata, validated, operationDirectory)
    } catch (error: Exception) {
      operationDirectory.deleteRecursively()
      throw error
    }
  }

  private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    DigestInputStream(file.inputStream(), digest).use { input ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      while (input.read(buffer) >= 0) Unit
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private class FileDownloadTarget(private val destination: File) : BackupDownloadTarget {
    override fun openOutputStream(): OutputStream = destination.outputStream()
  }
}

class DownloadedValidatedBackup
internal constructor(
  val metadata: BackupMetadata,
  val validated: ValidatedBackupArchive,
  private val operationDirectory: File,
) : Closeable {
  override fun close() {
    validated.close()
    operationDirectory.deleteRecursively()
  }
}

/** Coordinates validated data, compensating rollback, and crash-recoverable two-phase assets. */
class RestoreBackupUseCase(
  private val downloader: DownloadAndValidateBackupUseCase,
  private val exportImportUseCase: ExportImportUseCase,
  private val assetInstaller: RestoreAssetInstaller,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val reminderPreferencesRepository: ReminderPreferencesRepository,
  private val reminderScheduler: RestoreReminderScheduler,
  private val recoveryJournal: RestoreRecoveryJournal,
) : RestoreBackupAction {
  override suspend fun execute(
    request: RestoreBackupRequest,
    onProgress: (BackupProgress) -> Unit,
  ): RestoreBackupResult {
    recoverInterruptedRestore()
    downloader.execute(request.remoteId, onProgress).use { downloaded ->
      val incoming = downloaded.validated.snapshot
      val oldBundle = exportImportUseCase.exportBackupSnapshot()
      val oldUserPreferences = userPreferencesRepository.userPreferences.first()
      val oldReminderPreferences = reminderPreferencesRepository.getPreferences()
      val targetUserPreferences =
        if (request.mode == RestoreMode.REPLACE || request.applyBackupPreferences) {
          incoming.appPreferences.toRepositoryModel()
        } else {
          oldUserPreferences
        }
      val targetReminderPreferences =
        if (request.mode == RestoreMode.REPLACE || request.applyBackupPreferences) {
          incoming.reminderPreferences.toRepositoryModel()
        } else {
          oldReminderPreferences
        }
      val preparedAssets = assetInstaller.prepare(incoming.assets)
      val oldReferences = oldBundle.pets.mapNotNull { it.photoUri }.toSet()
      var state =
        RestoreRecoveryState(
          phase = RestoreRecoveryPhase.PREPARED,
          assetOperationId = preparedAssets.operationId,
          oldBundle = oldBundle,
          oldUserPreferences = oldUserPreferences,
          oldReminderPreferences = oldReminderPreferences,
          targetUserPreferences = targetUserPreferences,
          targetReminderPreferences = targetReminderPreferences,
          previousAssetReferences = oldReferences,
        )
      try {
        recoveryJournal.write(state)
        preparedAssets.promote()
        val mergeResult =
          exportImportUseCase.restoreData(
            incoming.exportBundle,
            if (request.mode == RestoreMode.REPLACE) {
              ConflictResolution.REPLACE
            } else {
              ConflictResolution.MERGE
            },
            preparedAssets.referencesByArchivePath,
          )
        state = state.copy(phase = RestoreRecoveryPhase.ROOM_COMMITTED)
        recoveryJournal.write(state)
        applyPreferences(targetUserPreferences, targetReminderPreferences)
        state = state.copy(phase = RestoreRecoveryPhase.PREFERENCES_COMMITTED)
        recoveryJournal.write(state)
        reminderScheduler.rescheduleCurrentTasks()
        val activeReferences = activeAssetReferences()
        assetInstaller.commit(preparedAssets.operationId, activeReferences, oldReferences)
        recoveryJournal.clear()
        return RestoreBackupResult(downloaded.metadata, mergeResult)
      } catch (error: Exception) {
        compensateToOldState(state)
        throw error
      }
    }
  }

  suspend fun recoverInterruptedRestore() {
    val state = recoveryJournal.read()
    downloader.cleanupOrphans()
    assetInstaller.cleanupOrphans(state?.assetOperationId)
    state ?: return
    if (state.phase == RestoreRecoveryPhase.PREPARED) {
      compensateToOldState(state)
      return
    }
    applyPreferences(state.targetUserPreferences, state.targetReminderPreferences)
    reminderScheduler.rescheduleCurrentTasks()
    assetInstaller.commit(
      state.assetOperationId,
      activeAssetReferences(),
      state.previousAssetReferences,
    )
    recoveryJournal.clear()
  }

  private suspend fun compensateToOldState(state: RestoreRecoveryState) {
    try {
      exportImportUseCase.importData(state.oldBundle, ConflictResolution.REPLACE)
      applyPreferences(state.oldUserPreferences, state.oldReminderPreferences)
      assetInstaller.rollback(state.assetOperationId)
      reminderScheduler.rescheduleCurrentTasks()
      recoveryJournal.clear()
    } catch (compensationFailure: Exception) {
      throw IllegalStateException(
        "Restore failed and recovery remains pending",
        compensationFailure,
      )
    }
  }

  private suspend fun applyPreferences(user: UserPreferences, reminders: ReminderPreferences) {
    userPreferencesRepository.replaceRestorablePreferences(user)
    reminderPreferencesRepository.replaceRestorablePreferences(reminders)
  }

  private suspend fun activeAssetReferences(): Set<String> =
    exportImportUseCase.exportBackupSnapshot().pets.mapNotNull { it.photoUri }.toSet()

  private fun BackupAppPreferences.toRepositoryModel() =
    UserPreferences(theme, language, hasCompletedOnboarding)

  private fun BackupReminderPreferences.toRepositoryModel() =
    ReminderPreferences(
      vaccinationRemindersEnabled = vaccinationRemindersEnabled,
      vaccinationDaysBefore = vaccinationDaysBefore,
      dewormingRemindersEnabled = dewormingRemindersEnabled,
      dewormingDaysBefore = dewormingDaysBefore,
      weightRemindersEnabled = weightRemindersEnabled,
      weightReminderIntervalDays = weightReminderIntervalDays,
      defaultNotificationHour = defaultNotificationHour,
      defaultNotificationMinute = defaultNotificationMinute,
    )
}
