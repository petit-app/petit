package com.woliveiras.petit.data.backup.restore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.ReminderPreferences
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.domain.backup.archive.BackupAsset
import com.woliveiras.petit.domain.backup.restore.RestoreRecoveryPhase
import com.woliveiras.petit.domain.backup.restore.RestoreRecoveryState
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.Pet
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileRestoreInfrastructureTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  private lateinit var context: Context
  private lateinit var installer: AndroidRestoreAssetInstaller
  private val createdPhotos = mutableListOf<File>()
  private val operationIds = mutableListOf<String>()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    installer =
      AndroidRestoreAssetInstaller(context) { file ->
        "content://${context.packageName}.fileprovider/${file.name}"
      }
  }

  @After
  fun tearDown() {
    operationIds.forEach { operationId -> runCatching { installer.rollback(operationId) } }
    createdPhotos.forEach(File::delete)
  }

  @Test
  fun recoveryJournalRoundTripsAndAtomicallyReplacesThePreviousState() {
    val directory = temporaryFolder.newFolder("journal")
    val journal = FileRestoreRecoveryJournal(directory)
    val prepared = recoveryState(RestoreRecoveryPhase.PREPARED, "operation-1")
    val committed = recoveryState(RestoreRecoveryPhase.ROOM_COMMITTED, "operation-2")

    journal.write(prepared)
    assertThat(journal.read()).isEqualTo(prepared)

    journal.write(committed)

    assertThat(journal.read()).isEqualTo(committed)
    assertThat(directory.resolve("restore.json.partial").exists()).isFalse()
    assertThat(directory.listFiles().orEmpty().map { it.name }).containsExactly("restore.json")
    journal.clear()
    assertThat(journal.read()).isNull()
  }

  @Test
  fun failedJournalPublicationPreservesThePreviousDurableState() {
    val directory = temporaryFolder.newFolder("journal-failure")
    var rejectPublication = false
    val journal =
      FileRestoreRecoveryJournal(directory) { partial, destination ->
        if (rejectPublication) throw java.io.IOException("Simulated atomic move failure")
        Files.move(partial.toPath(), destination.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
      }
    val previous = recoveryState(RestoreRecoveryPhase.PREPARED, "operation-1")
    journal.write(previous)
    rejectPublication = true

    val failure = runCatching {
      journal.write(recoveryState(RestoreRecoveryPhase.ROOM_COMMITTED, "operation-2"))
    }

    assertThat(failure.isFailure).isTrue()
    assertThat(journal.read()).isEqualTo(previous)
    assertThat(directory.resolve("restore.json.partial").exists()).isFalse()
  }

  @Test
  fun assetInstallerPromotesCommitsAndRemovesOldUnreferencedAsset() {
    val oldPhoto = appPhoto("old.jpg", byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 9))
    val oldReference = photoUri(oldPhoto)
    val source = temporaryFolder.newFile("new.jpg")
    source.writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1, 2, 3))
    val prepared =
      installer.prepare(listOf(BackupAsset("pet-1", "photo.jpg", "image/jpeg", source)))
    operationIds += prepared.operationId
    val installedReference =
      prepared.referencesByArchivePath.getValue("assets/pets/pet-1/photo.jpg")

    prepared.promote()
    val installedFile =
      context.filesDir.resolve(
        "pet_photos/${android.net.Uri.parse(installedReference).lastPathSegment}"
      )
    val installedBytes = installedFile.readBytes()
    installer.commit(prepared.operationId, setOf(installedReference), setOf(oldReference))

    assertThat(installedBytes).isEqualTo(source.readBytes())
    assertThat(oldPhoto.exists()).isFalse()
    assertThat(installedFile.isFile).isTrue()
    installer.commit(prepared.operationId, setOf(installedReference), setOf(oldReference))
  }

  @Test
  fun rollbackAndUnreferencedCommitAreIdempotentAndDeletePromotedAssets() {
    val rollbackPrepared = preparedAsset("rollback")
    rollbackPrepared.promote()
    val rollbackReference = rollbackPrepared.referencesByArchivePath.values.single()

    installer.rollback(rollbackPrepared.operationId)
    installer.rollback(rollbackPrepared.operationId)

    assertThat(
        runCatching {
            context.contentResolver.openInputStream(android.net.Uri.parse(rollbackReference))
          }
          .isFailure
      )
      .isTrue()

    val orphanPrepared = preparedAsset("orphan")
    orphanPrepared.promote()
    val orphanReference = orphanPrepared.referencesByArchivePath.values.single()
    installer.commit(orphanPrepared.operationId, emptySet(), emptySet())

    val orphanFile =
      context.filesDir.resolve(
        "pet_photos/${android.net.Uri.parse(orphanReference).lastPathSegment}"
      )
    assertThat(orphanFile.exists()).isFalse()
  }

  @Test
  fun tamperedMarkerCannotDeleteAPathOutsideRestoreOrPhotoDirectories() {
    val prepared = preparedAsset("tampered")
    val operationDirectory =
      context.filesDir.resolve("backup_restore/operations/${prepared.operationId}")
    val marker = operationDirectory.resolve("assets.json")
    val protected = temporaryFolder.newFile("must-survive.txt").apply { writeText("safe") }
    val json = JSONObject(marker.readText())
    json.getJSONArray("entries").getJSONObject(0).put("pendingPath", protected.absolutePath)
    marker.writeText(json.toString())

    val failure = runCatching { installer.rollback(prepared.operationId) }

    assertThat(failure.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    assertThat(protected.readText()).isEqualTo("safe")
    operationDirectory.deleteRecursively()
  }

  private fun preparedAsset(label: String) =
    temporaryFolder.newFile("$label.jpg").let { source ->
      source.writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1))
      installer.prepare(listOf(BackupAsset("pet-$label", "photo.jpg", "image/jpeg", source))).also {
        operationIds += it.operationId
      }
    }

  private fun appPhoto(name: String, bytes: ByteArray): File =
    context.filesDir.resolve("pet_photos").let { directory ->
      check(directory.mkdirs() || directory.isDirectory)
      directory.resolve(name).apply {
        writeBytes(bytes)
        createdPhotos += this
      }
    }

  private fun photoUri(file: File) = "content://${context.packageName}.fileprovider/${file.name}"

  private fun recoveryState(phase: RestoreRecoveryPhase, operationId: String) =
    RestoreRecoveryState(
      phase = phase,
      assetOperationId = operationId,
      oldBundle =
        ExportBundle(
          metadata = ExportMetadata("1.2.3", "2026-07-18T10:15:30Z"),
          pets = listOf(Pet("pet-1", "Mimi", createdAt = 1L, updatedAt = 2L)),
          weightEntries = emptyList(),
          vaccinationEntries = emptyList(),
          dewormingEntries = emptyList(),
          tasks = emptyList(),
        ),
      oldUserPreferences = UserPreferences(AppTheme.LIGHT, AppLanguage.ENGLISH, true),
      oldReminderPreferences = ReminderPreferences(defaultNotificationHour = 9),
      targetUserPreferences = UserPreferences(AppTheme.DARK, AppLanguage.PORTUGUESE_BR, false),
      targetReminderPreferences = ReminderPreferences(defaultNotificationHour = 7),
      previousAssetReferences = setOf("content://old/photo"),
    )
}
