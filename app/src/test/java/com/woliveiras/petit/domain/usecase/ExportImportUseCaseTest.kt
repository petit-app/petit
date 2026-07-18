package com.woliveiras.petit.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.TaskEntity
import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import com.woliveiras.petit.data.local.entity.WeightEntryEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.domain.model.WeightEntry
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExportImportUseCaseTest {

  private lateinit var database: PetitDatabase
  private lateinit var useCase: ExportImportUseCase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    useCase = createUseCase(context, database)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun backupExportExcludesLogicalDeletesWhileShareableExportIncludesTombstones() = runTest {
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
        TaskEntity(id = "global", kind = "CUSTOM", title = "Global", scheduledFor = 3L),
        TaskEntity(
          id = "deleted",
          petId = "pet-1",
          kind = "CUSTOM",
          title = "Deleted",
          scheduledFor = 4L,
          deletedAt = 5L,
        ),
      )
      .forEach { database.taskDao().insertTask(it) }

    val backup = useCase.exportAll()
    val shareable = useCase.exportShareable()

    assertThat(backup.tasks.map { it.id })
      .containsExactly("pending", "completed", "global")
      .inOrder()
    assertThat(backup.tasks.single { it.id == "completed" }.status).isEqualTo(TaskStatus.COMPLETED)
    assertThat(shareable.tasks.single { it.id == "deleted" }.deletedAt).isEqualTo(5L)
  }

  @Test
  fun exportForPetIncludesEveryRelatedDomainAndExcludesOtherPetsAndGlobalTasks() = runTest {
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
    database.petDao().insertPet(PetEntity(id = "pet-2", name = "Luna"))
    database
      .weightEntryDao()
      .insertWeightEntry(
        WeightEntryEntity(id = "weight-1", petId = "pet-1", date = 1L, weightGrams = 4_200)
      )
    database
      .weightEntryDao()
      .insertWeightEntry(
        WeightEntryEntity(id = "weight-2", petId = "pet-2", date = 1L, weightGrams = 5_200)
      )
    database
      .vaccinationEntryDao()
      .insertVaccinationEntry(
        VaccinationEntryEntity(
          id = "vaccination-1",
          petId = "pet-1",
          vaccineType = "RABIES",
          applicationDate = 1L,
        )
      )
    database
      .dewormingEntryDao()
      .insertDewormingEntry(
        DewormingEntryEntity(
          id = "deworming-1",
          petId = "pet-1",
          type = "INTERNAL",
          medication = "Medicine",
          applicationDate = 1L,
        )
      )
    listOf(
        TaskEntity(
          id = "pet-task",
          petId = "pet-1",
          kind = "CUSTOM",
          title = "Pet task",
          scheduledFor = 1L,
          status = TaskStatus.COMPLETED.name,
        ),
        TaskEntity(
          id = "other-task",
          petId = "pet-2",
          kind = "CUSTOM",
          title = "Other task",
          scheduledFor = 1L,
        ),
        TaskEntity(id = "global-task", kind = "CUSTOM", title = "Global task", scheduledFor = 1L),
      )
      .forEach { database.taskDao().insertTask(it) }

    val exported = useCase.exportForPet("pet-1")

    assertThat(exported.pets.map { it.id }).containsExactly("pet-1")
    assertThat(exported.weightEntries.map { it.id }).containsExactly("weight-1")
    assertThat(exported.vaccinationEntries.map { it.id }).containsExactly("vaccination-1")
    assertThat(exported.dewormingEntries.map { it.id }).containsExactly("deworming-1")
    assertThat(exported.tasks.map { it.id }).containsExactly("pet-task")
    assertThat(exported.tasks.single().status).isEqualTo(TaskStatus.COMPLETED)
  }

  @Test
  fun mergeUsesUpdatedAtAndKeepAndReplaceHonorTheirStrategies() = runTest {
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
    database
      .taskDao()
      .insertTask(
        TaskEntity(
          id = "task-1",
          petId = "pet-1",
          kind = "CUSTOM",
          title = "Local newer",
          scheduledFor = 1L,
          updatedAt = 20L,
        )
      )

    useCase.importData(
      bundleWithTask(title = "Older backup", updatedAt = 10L),
      ConflictResolution.MERGE,
    )
    assertThat(database.taskDao().getTaskById("task-1")?.title).isEqualTo("Local newer")

    useCase.importData(
      bundleWithTask(title = "Kept local", updatedAt = 30L),
      ConflictResolution.KEEP,
    )
    assertThat(database.taskDao().getTaskById("task-1")?.title).isEqualTo("Local newer")

    useCase.importData(
      bundleWithTask(title = "Replacement", updatedAt = 5L),
      ConflictResolution.REPLACE,
    )
    assertThat(database.taskDao().getTaskById("task-1")?.title).isEqualTo("Replacement")
  }

  @Test
  fun invalidBundleIsRejectedBeforeAnyRoomMutation() = runTest {
    val invalid =
      ExportBundle(
        metadata = ExportMetadata("1.0", "2026-07-17T00:00:00Z", schemaVersion = 2),
        pets = listOf(Pet(id = "new-pet", name = "Mimi", createdAt = 1L, updatedAt = 1L)),
        weightEntries = emptyList(),
        vaccinationEntries = emptyList(),
        dewormingEntries = emptyList(),
        tasks = emptyList(),
      )

    val failure = runCatching { useCase.importData(invalid, ConflictResolution.REPLACE) }

    assertThat(failure.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    assertThat(database.petDao().getAllPets().first()).isEmpty()
  }

  @Test
  fun replaceRemovesMissingRecordsAndReportsExactCounters() = runTest {
    database.petDao().insertPet(PetEntity(id = "local-pet", name = "Local"))
    database
      .taskDao()
      .insertTask(
        TaskEntity(
          id = "local-task",
          petId = "local-pet",
          kind = "CUSTOM",
          title = "Local only",
          scheduledFor = 1L,
        )
      )

    val result =
      useCase.importData(
        ExportBundle(
          metadata = ExportMetadata("1.0", "2026-07-17T00:00:00Z"),
          pets = listOf(Pet(id = "remote-pet", name = "Remote", createdAt = 1L, updatedAt = 1L)),
          weightEntries = emptyList(),
          vaccinationEntries = emptyList(),
          dewormingEntries = emptyList(),
          tasks = emptyList(),
        ),
        ConflictResolution.REPLACE,
      )

    assertThat(database.petDao().getAllPets().first().map { it.id }).containsExactly("remote-pet")
    assertThat(database.taskDao().getAllTasks().first()).isEmpty()
    assertThat(result.petsAdded).isEqualTo(1)
    assertThat(result.petsRemoved).isEqualTo(1)
    assertThat(result.tasksRemoved).isEqualTo(1)
    assertThat(result.totalRemoved).isEqualTo(2)
  }

  @Test
  fun replaceRollsBackClearsWhenAnInsertFails() = runTest {
    database.petDao().insertPet(PetEntity(id = "local-pet", name = "Local"))
    database.openHelper.writableDatabase.execSQL(
      """
      CREATE TRIGGER reject_remote_pet
      BEFORE INSERT ON pets
      WHEN NEW.id = 'remote-pet'
      BEGIN
        SELECT RAISE(ABORT, 'forced import failure');
      END
      """
        .trimIndent()
    )

    val failure = runCatching {
      useCase.importData(
        ExportBundle(
          metadata = ExportMetadata("1.0", "2026-07-17T00:00:00Z"),
          pets = listOf(Pet(id = "remote-pet", name = "Remote", createdAt = 1L, updatedAt = 1L)),
          weightEntries = emptyList(),
          vaccinationEntries = emptyList(),
          dewormingEntries = emptyList(),
          tasks = emptyList(),
        ),
        ConflictResolution.REPLACE,
      )
    }

    assertThat(failure.isFailure).isTrue()
    assertThat(database.petDao().getAllPets().first().map { it.id }).containsExactly("local-pet")
  }

  @Test
  fun newerTombstoneWinsAndRetryDoesNotChangeCounts() = runTest {
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
    database
      .taskDao()
      .insertTask(
        TaskEntity(
          id = "task-1",
          petId = "pet-1",
          kind = "CUSTOM",
          title = "Active",
          scheduledFor = 1L,
          updatedAt = 10L,
        )
      )
    val tombstone = bundleWithTask("Deleted", updatedAt = 20L, deletedAt = 20L)

    val first = useCase.importData(tombstone, ConflictResolution.MERGE)
    val retry = useCase.importData(tombstone, ConflictResolution.MERGE)

    assertThat(database.taskDao().getTaskById("task-1")).isNull()
    assertThat(database.taskDao().getByIdIncludingDeleted("task-1")?.deletedAt).isEqualTo(20L)
    assertThat(first.tasksRemoved).isEqualTo(1)
    assertThat(retry.totalAdded + retry.totalUpdated + retry.totalRemoved).isEqualTo(0)
  }

  @Test
  fun duplicateVersionsInARealImportAreNormalizedByTheResolver() = runTest {
    val old = bundleWithTask("Old", updatedAt = 10L)
    val newest = old.tasks.single().copy(title = "Newest", updatedAt = 30L)
    val duplicated = old.copy(tasks = listOf(old.tasks.single(), newest))

    val result = useCase.importData(duplicated, ConflictResolution.MERGE)

    assertThat(database.taskDao().getTaskById("task-1")?.title).isEqualTo("Newest")
    assertThat(result.tasksAdded).isEqualTo(1)
  }

  @Test
  fun mergeAppliesTombstonesForEveryShareableEntityType() = runTest {
    val active = completeBundle(updatedAt = 10L)
    useCase.importData(active, ConflictResolution.MERGE)
    val tombstones =
      active.copy(
        pets = active.pets.map { it.copy(updatedAt = 20L, deletedAt = 20L) },
        weightEntries = active.weightEntries.map { it.copy(updatedAt = 20L, deletedAt = 20L) },
        vaccinationEntries =
          active.vaccinationEntries.map { it.copy(updatedAt = 20L, deletedAt = 20L) },
        dewormingEntries =
          active.dewormingEntries.map { it.copy(updatedAt = 20L, deletedAt = 20L) },
        tasks = active.tasks.map { it.copy(updatedAt = 20L, deletedAt = 20L) },
      )

    val result = useCase.importData(tombstones, ConflictResolution.MERGE)

    assertThat(result.petsRemoved).isEqualTo(1)
    assertThat(result.weightsRemoved).isEqualTo(1)
    assertThat(result.vaccinationsRemoved).isEqualTo(1)
    assertThat(result.dewormingsRemoved).isEqualTo(1)
    assertThat(result.tasksRemoved).isEqualTo(1)
    assertThat(database.petDao().getPetById("pet-1")).isNull()
    assertThat(database.weightEntryDao().getWeightEntryById("weight-1")).isNull()
    assertThat(database.vaccinationEntryDao().getVaccinationEntryById("vaccination-1")).isNull()
    assertThat(database.dewormingEntryDao().getDewormingEntryById("deworming-1")).isNull()
    assertThat(database.taskDao().getTaskById("task-1")).isNull()
  }

  @Test
  fun largeChangesetIsBatchAppliedAndAnIdenticalRetryIsIdempotent() = runTest {
    val tasks =
      (1..500).map { index ->
        Task(
          id = "task-$index",
          petId = "pet-1",
          kind = TaskKind.CUSTOM,
          title = "Task $index",
          scheduledFor = LocalDateTime.of(2026, 7, 18, 9, 0),
          createdAt = 1L,
          updatedAt = index.toLong(),
        )
      }
    val bundle = completeBundle(updatedAt = 1L).copy(tasks = tasks)

    val first = useCase.importData(bundle, ConflictResolution.MERGE)
    val retry = useCase.importData(bundle, ConflictResolution.MERGE)

    assertThat(database.taskDao().getAllIncludingDeleted()).hasSize(500)
    assertThat(first.tasksAdded).isEqualTo(500)
    assertThat(retry.totalAdded + retry.totalUpdated + retry.totalRemoved).isEqualTo(0)
    assertThat(retry.conflictsResolved).isEqualTo(0)
  }

  private fun bundleWithTask(title: String, updatedAt: Long, deletedAt: Long? = null) =
    ExportBundle(
      metadata = ExportMetadata("1.0", "2026-07-17T00:00:00Z"),
      pets = listOf(Pet(id = "pet-1", name = "Mimi", createdAt = 1L, updatedAt = 1L)),
      weightEntries = emptyList(),
      vaccinationEntries = emptyList(),
      dewormingEntries = emptyList(),
      tasks =
        listOf(
          Task(
            id = "task-1",
            petId = "pet-1",
            kind = TaskKind.CUSTOM,
            title = title,
            scheduledFor = LocalDateTime.of(2026, 7, 17, 9, 0),
            createdAt = 1L,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
          )
        ),
    )

  private fun completeBundle(updatedAt: Long) =
    ExportBundle(
      metadata = ExportMetadata("1.0", "2026-07-18T00:00:00Z"),
      pets = listOf(Pet("pet-1", "Mimi", createdAt = 1L, updatedAt = updatedAt)),
      weightEntries =
        listOf(
          WeightEntry(
            "weight-1",
            "pet-1",
            LocalDate.of(2026, 7, 18),
            4_200,
            createdAt = 1L,
            updatedAt = updatedAt,
          )
        ),
      vaccinationEntries =
        listOf(
          VaccinationEntry(
            id = "vaccination-1",
            petId = "pet-1",
            vaccineType = VaccineType.RABIES,
            applicationDate = LocalDate.of(2026, 7, 18),
            createdAt = 1L,
            updatedAt = updatedAt,
          )
        ),
      dewormingEntries =
        listOf(
          DewormingEntry(
            id = "deworming-1",
            petId = "pet-1",
            type = DewormingType.INTERNAL,
            applicationDate = LocalDate.of(2026, 7, 18),
            createdAt = 1L,
            updatedAt = updatedAt,
          )
        ),
      tasks =
        listOf(
          Task(
            id = "task-1",
            petId = "pet-1",
            kind = TaskKind.CUSTOM,
            title = "Task",
            scheduledFor = LocalDateTime.of(2026, 7, 18, 9, 0),
            createdAt = 1L,
            updatedAt = updatedAt,
          )
        ),
    )

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
