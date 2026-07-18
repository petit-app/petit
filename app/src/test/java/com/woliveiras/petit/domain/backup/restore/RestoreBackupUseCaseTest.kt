package com.woliveiras.petit.domain.backup.restore

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.FamilyGroupMemberEntity
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.ReminderPreferences
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupDownloadResult
import com.woliveiras.petit.domain.backup.BackupDownloadTarget
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupPage
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.BackupUploadRequest
import com.woliveiras.petit.domain.backup.BackupUploadResult
import com.woliveiras.petit.domain.backup.archive.BackupAppPreferences
import com.woliveiras.petit.domain.backup.archive.BackupArchiveCodec
import com.woliveiras.petit.domain.backup.archive.BackupArchiveRequest
import com.woliveiras.petit.domain.backup.archive.BackupAsset
import com.woliveiras.petit.domain.backup.archive.BackupReminderPreferences
import com.woliveiras.petit.domain.backup.archive.BackupSnapshot
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import java.io.File
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RestoreBackupUseCaseTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  private lateinit var database: PetitDatabase
  private lateinit var exportImport: ExportImportUseCase
  private lateinit var userPreferences: MutableUserPreferencesRepository
  private lateinit var reminders: MutableReminderPreferencesRepository
  private lateinit var installer: RecordingAssetInstaller
  private lateinit var scheduler: RecordingReminderScheduler
  private lateinit var journal: MemoryRestoreJournal

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    exportImport = createExportImport(context)
    userPreferences =
      MutableUserPreferencesRepository(UserPreferences(AppTheme.LIGHT, AppLanguage.ENGLISH, true))
    reminders =
      MutableReminderPreferencesRepository(ReminderPreferences(defaultNotificationHour = 9))
    installer = RecordingAssetInstaller()
    scheduler = RecordingReminderScheduler()
    journal = MemoryRestoreJournal()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun invalidDownloadIsRejectedAndPrivateStagingIsCleaned() = runTest {
    val archive = createArchive("invalid-download")
    val gateway =
      FileStorageGateway(archive, metadata(archive).copy(archiveSha256 = "0".repeat(64)))
    val staging = temporaryFolder.newFolder("invalid-staging")
    val downloader = DownloadAndValidateBackupUseCase(gateway, BackupArchiveCodec(), staging)

    val failure = runCatching { downloader.execute("remote-1") }

    assertThat(failure.isFailure).isTrue()
    assertThat(staging.listFiles().orEmpty()).isEmpty()
  }

  @Test
  fun downloadCancellationCleansPartialBytesAndProgressRemainsMonotonic() = runTest {
    val archive = createArchive("cancel-download")
    val staging = temporaryFolder.newFolder("cancel-staging")
    val cancelling =
      FileStorageGateway(archive, metadata(archive), cancelAfterBytes = archive.length() / 2)
    val downloader = DownloadAndValidateBackupUseCase(cancelling, BackupArchiveCodec(), staging)

    val cancellation = runCatching { downloader.execute("remote-1") }.exceptionOrNull()

    assertThat(cancellation).isInstanceOf(CancellationException::class.java)
    assertThat(staging.listFiles().orEmpty()).isEmpty()

    val progress = mutableListOf<Long>()
    val successful =
      DownloadAndValidateBackupUseCase(
          FileStorageGateway(
            archive,
            metadata(archive),
            progressScript = listOf(5L, 3L, archive.length()),
          ),
          BackupArchiveCodec(),
          staging,
        )
        .execute("remote-1") { progress += it.bytesTransferred }

    assertThat(progress).containsExactly(5L, archive.length()).inOrder()
    successful.close()
    assertThat(staging.listFiles().orEmpty()).isEmpty()
  }

  @Test
  fun startupCleanupRemovesDownloadAndExtractionDirectoriesLeftByProcessDeath() {
    val archive = createArchive("orphan-cleanup")
    val staging = temporaryFolder.newFolder("orphan-staging")
    staging.resolve("download-dead-process").mkdir()
    staging.resolve(".restore-dead-process").mkdir()
    val unrelated = staging.resolve("keep.txt").apply { writeText("safe") }
    val downloader =
      DownloadAndValidateBackupUseCase(
        FileStorageGateway(archive, metadata(archive)),
        BackupArchiveCodec(),
        staging,
      )

    downloader.cleanupOrphans()

    assertThat(staging.resolve("download-dead-process").exists()).isFalse()
    assertThat(staging.resolve(".restore-dead-process").exists()).isFalse()
    assertThat(unrelated.readText()).isEqualTo("safe")
  }

  @Test
  fun replaceInstallsExactRoomAssetsAndPreferencesWhilePreservingDeviceIdentity() = runTest {
    database.petDao().insertPet(PetEntity(id = "local", name = "Local"))
    database
      .familyGroupMemberDao()
      .insertMember(
        FamilyGroupMemberEntity(
          id = "device-1",
          deviceName = "This device",
          familyGroupKey = "secret-group-key",
          isLocalDevice = true,
        )
      )
    val archive = createArchive("replace", petId = "remote", photo = true)
    val useCase = restoreUseCase(archive)

    val result = useCase.execute(RestoreBackupRequest("remote-1", RestoreMode.REPLACE))

    assertThat(database.petDao().getAllIncludingDeleted().map { it.id }).containsExactly("remote")
    assertThat(database.petDao().getByIdIncludingDeleted("remote")?.photoUri)
      .isEqualTo("content://installed/remote")
    assertThat(database.familyGroupMemberDao().getLocalDevice()?.id).isEqualTo("device-1")
    assertThat(userPreferences.current)
      .isEqualTo(UserPreferences(AppTheme.DARK, AppLanguage.PORTUGUESE_BR, false))
    assertThat(reminders.current.defaultNotificationHour).isEqualTo(7)
    assertThat(installer.committed).isTrue()
    assertThat(scheduler.calls).isEqualTo(1)
    assertThat(journal.state).isNull()
    assertThat(result.mergeResult.petsAdded).isEqualTo(1)
  }

  @Test
  fun mergeUsesSharedResolverKeepsLocalPreferencesAndAssetsFollowWinningPet() = runTest {
    database
      .petDao()
      .insertPet(
        PetEntity(
          id = "pet-1",
          name = "Local newer",
          photoUri = "content://local/photo",
          createdAt = 1L,
          updatedAt = 50L,
        )
      )
    val archive = createArchive("merge", petId = "pet-1", petUpdatedAt = 10L, photo = true)
    val useCase = restoreUseCase(archive)

    useCase.execute(RestoreBackupRequest("remote-1", RestoreMode.MERGE))

    assertThat(database.petDao().getByIdIncludingDeleted("pet-1")?.name).isEqualTo("Local newer")
    assertThat(database.petDao().getByIdIncludingDeleted("pet-1")?.photoUri)
      .isEqualTo("content://local/photo")
    assertThat(userPreferences.current.theme).isEqualTo(AppTheme.LIGHT)
    assertThat(reminders.current.defaultNotificationHour).isEqualTo(9)
    assertThat(installer.activeAtCommit).containsExactly("content://local/photo")
  }

  @Test
  fun postMutationFailureCompensatesRoomPreferencesAndPromotedAssets() = runTest {
    database
      .petDao()
      .insertPet(PetEntity(id = "local", name = "Local", createdAt = 1L, updatedAt = 1L))
    val archive = createArchive("rollback", petId = "remote", photo = true)
    scheduler.failNext = true
    val useCase = restoreUseCase(archive)

    val failure = runCatching {
      useCase.execute(RestoreBackupRequest("remote-1", RestoreMode.REPLACE))
    }

    assertThat(failure.isFailure).isTrue()
    assertThat(database.petDao().getAllIncludingDeleted().map { it.id }).containsExactly("local")
    assertThat(userPreferences.current)
      .isEqualTo(UserPreferences(AppTheme.LIGHT, AppLanguage.ENGLISH, true))
    assertThat(reminders.current.defaultNotificationHour).isEqualTo(9)
    assertThat(installer.rolledBack).isTrue()
    assertThat(journal.state).isNull()
  }

  @Test
  fun initialJournalPublicationFailureRollsBackStagedAssetsBeforeMutation() = runTest {
    database.petDao().insertPet(PetEntity(id = "local", name = "Local"))
    val archive = createArchive("journal-failure", petId = "remote", photo = true)
    journal.failNextWrite = true
    val useCase = restoreUseCase(archive)

    val failure = runCatching {
      useCase.execute(RestoreBackupRequest("remote-1", RestoreMode.REPLACE))
    }

    assertThat(failure.isFailure).isTrue()
    assertThat(database.petDao().getAllIncludingDeleted().map { it.id }).containsExactly("local")
    assertThat(installer.rolledBack).isTrue()
    assertThat(journal.state).isNull()
  }

  @Test
  fun recoveryRollsBackAmbiguousPreparedPhaseAndCompletesCommittedPhase() = runTest {
    val oldBundle = bundle("old", updatedAt = 1L)
    exportImport.importData(oldBundle, com.woliveiras.petit.domain.model.ConflictResolution.REPLACE)
    val oldUser = userPreferences.current
    val oldReminders = reminders.current
    val archive = createArchive("recovery", petId = "new")
    val useCase = restoreUseCase(archive)
    exportImport.importData(
      bundle("new", updatedAt = 2L),
      com.woliveiras.petit.domain.model.ConflictResolution.REPLACE,
    )
    journal.state =
      RestoreRecoveryState(
        phase = RestoreRecoveryPhase.PREPARED,
        assetOperationId = "operation-1",
        oldBundle = oldBundle,
        oldUserPreferences = oldUser,
        oldReminderPreferences = oldReminders,
        targetUserPreferences = UserPreferences(AppTheme.DARK, AppLanguage.PORTUGUESE_BR, false),
        targetReminderPreferences = ReminderPreferences(defaultNotificationHour = 7),
        previousAssetReferences = emptySet(),
      )

    useCase.recoverInterruptedRestore()

    assertThat(database.petDao().getAllIncludingDeleted().map { it.id }).containsExactly("old")
    assertThat(installer.rolledBack).isTrue()

    exportImport.importData(
      bundle("new", updatedAt = 2L),
      com.woliveiras.petit.domain.model.ConflictResolution.REPLACE,
    )
    journal.state =
      RestoreRecoveryState(
        phase = RestoreRecoveryPhase.ROOM_COMMITTED,
        assetOperationId = "operation-2",
        oldBundle = oldBundle,
        oldUserPreferences = oldUser,
        oldReminderPreferences = oldReminders,
        targetUserPreferences = UserPreferences(AppTheme.DARK, AppLanguage.PORTUGUESE_BR, false),
        targetReminderPreferences = ReminderPreferences(defaultNotificationHour = 7),
        previousAssetReferences = emptySet(),
      )

    useCase.recoverInterruptedRestore()

    assertThat(database.petDao().getAllIncludingDeleted().map { it.id }).containsExactly("new")
    assertThat(userPreferences.current.hasCompletedOnboarding).isFalse()
    assertThat(reminders.current.defaultNotificationHour).isEqualTo(7)
    assertThat(installer.committed).isTrue()
    assertThat(journal.state).isNull()
  }

  private fun restoreUseCase(archive: File): RestoreBackupUseCase {
    val gateway = FileStorageGateway(archive, metadata(archive))
    val downloader =
      DownloadAndValidateBackupUseCase(
        gateway,
        BackupArchiveCodec(),
        temporaryFolder.newFolder("download-${archive.nameWithoutExtension}"),
      )
    return RestoreBackupUseCase(
      downloader,
      exportImport,
      installer,
      userPreferences,
      reminders,
      scheduler,
      journal,
    )
  }

  private fun createArchive(
    id: String,
    petId: String = "pet-1",
    petUpdatedAt: Long = 2L,
    photo: Boolean = false,
  ): File {
    val source =
      if (photo) {
        temporaryFolder.newFile("$id.jpg").apply {
          writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1, 2, 3))
        }
      } else {
        null
      }
    return BackupArchiveCodec()
      .create(
        BackupArchiveRequest(
          backupId = id,
          createdAt = Instant.parse("2026-07-18T10:15:30Z"),
          appVersion = "1.2.3",
          trigger = BackupTrigger.MANUAL,
          snapshot =
            BackupSnapshot(
              exportBundle =
                bundle(petId, petUpdatedAt).let { bundle ->
                  if (source == null) bundle
                  else bundle.copy(pets = bundle.pets.map { it.copy(photoUri = "content://old") })
                },
              appPreferences =
                BackupAppPreferences(AppTheme.DARK, AppLanguage.PORTUGUESE_BR, false),
              reminderPreferences = BackupReminderPreferences(true, 5, false, 6, true, 14, 7, 30),
              assets =
                source?.let { listOf(BackupAsset(petId, "photo.jpg", "image/jpeg", it)) }
                  ?: emptyList(),
            ),
          outputDirectory = temporaryFolder.newFolder("archive-$id"),
        )
      )
      .file
  }

  private fun bundle(petId: String, updatedAt: Long) =
    ExportBundle(
      metadata = ExportMetadata("1.2.3", "2026-07-18T10:15:30Z"),
      pets =
        listOf(
          Pet(
            petId,
            if (petId == "pet-1") "Remote older" else petId,
            createdAt = 1L,
            updatedAt = updatedAt,
          )
        ),
      weightEntries = emptyList(),
      vaccinationEntries = emptyList(),
      dewormingEntries = emptyList(),
      tasks = emptyList(),
    )

  private fun metadata(file: File) =
    BackupMetadata(
      remoteId = "remote-1",
      backupId = file.name.removePrefix("petit-backup-").removeSuffix(".zip"),
      createdAt = Instant.parse("2026-07-18T10:15:30Z"),
      trigger = BackupTrigger.MANUAL,
      appVersion = "1.2.3",
      archiveFormatVersion = 1,
      schemaVersion = 1,
      contentCounts = BackupContentCounts(pets = 1),
      archiveSizeBytes = file.length(),
      archiveSha256 = sha256(file.readBytes()),
    )

  private fun createExportImport(context: Context) =
    ExportImportUseCase(
      context,
      database,
      PetRepositoryImpl(database.petDao()),
      WeightEntryRepositoryImpl(database.weightEntryDao()),
      VaccinationEntryRepositoryImpl(database.vaccinationEntryDao(), Clock.systemUTC()),
      DewormingEntryRepositoryImpl(database.dewormingEntryDao(), Clock.systemUTC()),
      TaskRepositoryImpl(database.taskDao()),
    )

  private fun sha256(bytes: ByteArray) =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

  private inner class FileStorageGateway(
    private val file: File,
    private val backupMetadata: BackupMetadata,
    private val cancelAfterBytes: Long? = null,
    private val progressScript: List<Long>? = null,
  ) : BackupStorageGateway {
    override suspend fun download(
      remoteId: String,
      target: BackupDownloadTarget,
      onProgress: (BackupProgress) -> Unit,
    ): BackupDownloadResult {
      if (cancelAfterBytes != null) {
        target.openOutputStream().use { output ->
          file.inputStream().use { input -> input.copyTo(output, limit = cancelAfterBytes) }
        }
        onProgress(BackupProgress(cancelAfterBytes, file.length()))
        throw CancellationException("Simulated cancellation")
      }
      target.openOutputStream().use { output -> file.inputStream().use { it.copyTo(output) } }
      (progressScript ?: listOf(file.length())).forEach { transferred ->
        onProgress(BackupProgress(transferred, file.length()))
      }
      return BackupDownloadResult(backupMetadata, file.length())
    }

    override suspend fun upload(
      request: BackupUploadRequest,
      onProgress: (BackupProgress) -> Unit,
    ): BackupUploadResult = error("Not used")

    override suspend fun list(pageToken: String?, pageSize: Int): BackupPage = error("Not used")

    override suspend fun get(remoteId: String): BackupMetadata? = backupMetadata

    override suspend fun deleteExact(remoteId: String) = Unit
  }

  private fun java.io.InputStream.copyTo(output: java.io.OutputStream, limit: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var remaining = limit
    while (remaining > 0) {
      val read = read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
      if (read < 0) break
      output.write(buffer, 0, read)
      remaining -= read
    }
  }

  private class MutableUserPreferencesRepository(initial: UserPreferences) :
    UserPreferencesRepository {
    private val flow = MutableStateFlow(initial)
    val current: UserPreferences
      get() = flow.value

    override val userPreferences: Flow<UserPreferences> = flow

    override suspend fun updateTheme(theme: AppTheme) {
      flow.value = flow.value.copy(theme = theme)
    }

    override suspend fun updateLanguage(language: AppLanguage) {
      flow.value = flow.value.copy(language = language)
    }

    override suspend fun setOnboardingCompleted() {
      flow.value = flow.value.copy(hasCompletedOnboarding = true)
    }

    override suspend fun replaceRestorablePreferences(preferences: UserPreferences) {
      flow.value = preferences
    }
  }

  private class MutableReminderPreferencesRepository(initial: ReminderPreferences) :
    ReminderPreferencesRepository {
    private val flow = MutableStateFlow(initial)
    val current: ReminderPreferences
      get() = flow.value

    override val preferences: Flow<ReminderPreferences> = flow

    override suspend fun getPreferences() = flow.value

    override suspend fun updateVaccinationSettings(enabled: Boolean, daysBefore: Int) = Unit

    override suspend fun updateDewormingSettings(enabled: Boolean, daysBefore: Int) = Unit

    override suspend fun updateWeightSettings(enabled: Boolean, intervalDays: Int) = Unit

    override suspend fun updateNotificationTime(hour: Int, minute: Int) = Unit

    override suspend fun reset() = Unit

    override suspend fun replaceRestorablePreferences(preferences: ReminderPreferences) {
      flow.value = preferences
    }
  }

  private class RecordingAssetInstaller : RestoreAssetInstaller {
    var rolledBack = false
    var committed = false
    var activeAtCommit: Set<String> = emptySet()

    override fun prepare(assets: List<BackupAsset>): PreparedRestoreAssets =
      object : PreparedRestoreAssets {
        override val operationId = "operation-1"
        override val referencesByArchivePath =
          assets.associate {
            "assets/pets/${it.petId}/${it.fileName}" to "content://installed/${it.petId}"
          }

        override fun promote() = Unit
      }

    override fun rollback(operationId: String) {
      rolledBack = true
    }

    override fun commit(
      operationId: String,
      activeReferences: Set<String>,
      previousReferences: Set<String>,
    ) {
      committed = true
      activeAtCommit = activeReferences
    }
  }

  private class RecordingReminderScheduler : RestoreReminderScheduler {
    var calls = 0
    var failNext = false

    override suspend fun rescheduleCurrentTasks() {
      calls++
      if (failNext) {
        failNext = false
        throw IllegalStateException("Simulated scheduler failure")
      }
    }
  }

  private class MemoryRestoreJournal : RestoreRecoveryJournal {
    var state: RestoreRecoveryState? = null
    var failNextWrite = false

    override fun read() = state

    override fun write(state: RestoreRecoveryState) {
      if (failNextWrite) {
        failNextWrite = false
        throw IllegalStateException("Simulated journal publication failure")
      }
      this.state = state
    }

    override fun clear() {
      state = null
    }
  }
}
