package com.woliveiras.petit.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.TaskEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.presentation.feature.settings.createBackupShareIntent
import java.io.File
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportImportUseCaseIntegrationTest {

  private lateinit var context: Context
  private lateinit var database: PetitDatabase
  private lateinit var useCase: ExportImportUseCase
  private lateinit var backupDirectory: File

  @Before
  fun setUp() {
    context = InstrumentationRegistry.getInstrumentation().targetContext
    database = Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java).build()
    useCase = createUseCase(context, database)
    backupDirectory = File(context.cacheDir, "backups").apply { mkdirs() }
  }

  @After
  fun tearDown() {
    database.close()
    backupDirectory.deleteRecursively()
  }

  @Test
  fun contentResolverRoundTripAndShareIntentPreserveCompletedTaskAndUriGrant() = runTest {
    val uri = backupUri("round-trip.json")
    val completed =
      Task(
        id = "completed",
        petId = "pet-1",
        kind = TaskKind.CUSTOM,
        referenceEntityId = "record-1",
        title = "Completed task",
        scheduledFor = LocalDateTime.of(2026, 7, 17, 9, 0),
        status = TaskStatus.COMPLETED,
        createdAt = 1L,
        updatedAt = 2L,
      )
    val bundle = validBundle(tasks = listOf(completed))

    useCase.writeExportToUri(bundle, uri)
    val restored = useCase.readImportFromUri(uri)
    val shareIntent = createBackupShareIntent(uri)

    assertThat(restored.tasks).containsExactly(completed)
    assertThat(shareIntent.action).isEqualTo(Intent.ACTION_SEND)
    assertThat(shareIntent.type).isEqualTo("application/json")
    assertThat(shareIntent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)).isEqualTo(uri)
    assertThat(shareIntent.clipData?.getItemAt(0)?.uri).isEqualTo(uri)
    assertThat(shareIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
  }

  @Test
  fun roomExportIncludesBothActiveStatesAndFiltersSoftDeletedTasks() = runTest {
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
    listOf(
        TaskEntity(
          id = "pending",
          petId = "pet-1",
          kind = "CUSTOM",
          title = "Pending",
          scheduledFor = 1L,
        ),
        TaskEntity(
          id = "completed",
          petId = "pet-1",
          kind = "CUSTOM",
          title = "Completed",
          scheduledFor = 2L,
          status = TaskStatus.COMPLETED.name,
        ),
        TaskEntity(
          id = "deleted",
          petId = "pet-1",
          kind = "CUSTOM",
          title = "Deleted",
          scheduledFor = 3L,
          deletedAt = 4L,
        ),
      )
      .forEach { database.taskDao().insertTask(it) }

    val exported = useCase.exportAll()

    assertThat(exported.tasks.map { it.id }).containsExactly("pending", "completed").inOrder()
  }

  @Test
  fun legacyReminderIsConvertedBeforeValidationAndMalformedLegacyLeavesRoomUntouched() = runTest {
    val validLegacyUri = backupUri("legacy.json")
    writeJson(
      validLegacyUri,
      emptyBundleJson()
        .put(
          "reminders",
          JSONArray()
            .put(
              JSONObject()
                .put("id", "legacy-task")
                .put("petId", "pet-1")
                .put("title", "Legacy completed task")
                .put("scheduledAt", "2026-07-17T09:00:00")
                .put("completed", true)
                .put("createdAt", 1L)
                .put("updatedAt", 2L)
            ),
        ),
    )
    val converted = useCase.readImportFromUri(validLegacyUri)
    useCase.importData(converted, ConflictResolution.REPLACE)
    assertThat(database.taskDao().getTaskById("legacy-task")?.status)
      .isEqualTo(TaskStatus.COMPLETED.name)

    val malformedUri = backupUri("malformed-legacy.json")
    writeJson(
      malformedUri,
      emptyBundleJson()
        .put("pets", JSONArray())
        .put("reminders", JSONArray().put(JSONObject().put("id", "broken"))),
    )
    val failure = runCatching { useCase.readImportFromUri(malformedUri) }

    assertThat(failure.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    assertThat(database.petDao().getAllPets().first().map { it.id }).containsExactly("pet-1")
    assertThat(database.taskDao().getAllTasks().first().map { it.id })
      .containsExactly("legacy-task")
  }

  @Test
  fun roomFailureRollsBackRecordsWrittenEarlierInTheImportTransaction() = runTest {
    database.openHelper.writableDatabase.execSQL(
      """
      CREATE TRIGGER abort_task_import
      BEFORE INSERT ON tasks
      BEGIN
        SELECT RAISE(ABORT, 'forced task import failure');
      END
      """
        .trimIndent()
    )
    val task =
      Task(
        id = "task-1",
        petId = "pet-1",
        kind = TaskKind.CUSTOM,
        title = "Task",
        scheduledFor = LocalDateTime.of(2026, 7, 17, 9, 0),
        createdAt = 1L,
        updatedAt = 1L,
      )

    val failure = runCatching {
      useCase.importData(validBundle(tasks = listOf(task)), ConflictResolution.REPLACE)
    }

    assertThat(failure.exceptionOrNull()).isNotNull()
    assertThat(database.petDao().getAllPets().first()).isEmpty()
    assertThat(database.taskDao().getAllTasks().first()).isEmpty()
  }

  @Test
  fun syncLogFailureRollsBackTheImportedEntitiesInTheSameRoomTransaction() = runTest {
    database.openHelper.writableDatabase.execSQL(
      """
      CREATE TRIGGER abort_sync_log
      BEFORE INSERT ON sync_logs
      BEGIN
        SELECT RAISE(ABORT, 'forced sync log failure');
      END
      """
        .trimIndent()
    )
    val mergeData = MergeDataUseCase(useCase, database)

    val failure = runCatching {
      mergeData(
        bundle = validBundle(tasks = emptyList()),
        peerId = "peer-1",
        peerName = "Kitchen phone",
      )
    }

    assertThat(failure.exceptionOrNull()).isNotNull()
    assertThat(database.petDao().getAllPets().first()).isEmpty()
    assertThat(database.syncLogDao().getLatestSyncLog()).isNull()
  }

  private fun backupUri(filename: String) =
    FileProvider.getUriForFile(
      context,
      "${context.packageName}.test.backups",
      File(backupDirectory, filename).apply {
        checkNotNull(parentFile).mkdirs()
        createNewFile()
      },
    )

  private fun writeJson(uri: android.net.Uri, json: JSONObject) {
    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
      it.write(json.toString())
    } ?: error("Could not open test backup URI")
  }

  private fun validBundle(tasks: List<Task>) =
    ExportBundle(
      metadata = ExportMetadata("1.0", "2026-07-17T00:00:00Z"),
      pets = listOf(Pet(id = "pet-1", name = "Mimi", createdAt = 1L, updatedAt = 1L)),
      weightEntries = emptyList(),
      vaccinationEntries = emptyList(),
      dewormingEntries = emptyList(),
      tasks = tasks,
    )

  private fun emptyBundleJson() =
    JSONObject()
      .put("metadata", ExportMetadata("1.0", "2026-07-17T00:00:00Z").toJson())
      .put(
        "pets",
        JSONArray()
          .put(
            JSONObject()
              .put("id", "pet-1")
              .put("name", "Mimi")
              .put("petType", "CAT")
              .put("sex", "UNKNOWN")
              .put("createdAt", 1L)
              .put("updatedAt", 1L)
          ),
      )
      .put("weightEntries", JSONArray())
      .put("vaccinationEntries", JSONArray())
      .put("dewormingEntries", JSONArray())

  private fun createUseCase(context: Context, database: PetitDatabase) =
    ExportImportUseCase(
      context = context,
      database = database,
      petRepository = PetRepositoryImpl(database.petDao()),
      weightRepository = WeightEntryRepositoryImpl(database.weightEntryDao()),
      vaccinationRepository =
        VaccinationEntryRepositoryImpl(database.vaccinationEntryDao(), Clock.systemUTC()),
      dewormingRepository =
        DewormingEntryRepositoryImpl(database.dewormingEntryDao(), Clock.systemUTC()),
      taskRepository = TaskRepositoryImpl(database.taskDao()),
    )
}
