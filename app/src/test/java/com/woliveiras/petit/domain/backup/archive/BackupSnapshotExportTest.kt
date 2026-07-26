package com.woliveiras.petit.domain.backup.archive

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import com.woliveiras.petit.data.local.entity.FamilyGroupMemberEntity
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.TaskEntity
import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import java.time.Clock
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupSnapshotExportTest {
  private lateinit var database: PetitDatabase
  private lateinit var useCase: ExportImportUseCase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    useCase =
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

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun backupSnapshotIncludesRestorableTombstonesAndExcludesDeviceAuthorizationRows() = runTest {
    database
      .petDao()
      .insertPet(PetEntity(id = "pet-1", name = "Mimi", updatedAt = 2L, deletedAt = 2L))
    database
      .taskDao()
      .insertTask(
        TaskEntity(
          id = "task-1",
          petId = "pet-1",
          kind = "CUSTOM",
          title = "Deleted task",
          scheduledFor = 1L,
          updatedAt = 3L,
          deletedAt = 3L,
        )
      )
    database
      .familyGroupMemberDao()
      .insertMember(
        FamilyGroupMemberEntity(
          id = "device-1",
          deviceName = "Private device identity",
          familyGroupKey = "group-1",
          isLocalDevice = true,
        )
      )

    val snapshot = useCase.exportBackupSnapshot()

    assertThat(snapshot.pets.single().deletedAt).isEqualTo(2L)
    assertThat(snapshot.tasks.single().deletedAt).isEqualTo(3L)
    assertThat(snapshot.membershipChanges).isEmpty()
    assertThat(snapshot.toJson().toString()).doesNotContain("Private device identity")
    assertThat(snapshot.toJson().toString()).doesNotContain("group-1")
  }

  @Test
  fun backupSnapshotPreservesAllSpeciesAndRawBreedValues() = runTest {
    PetType.entries.forEachIndexed { index, petType ->
      database
        .petDao()
        .insertPet(
          PetEntity(
            id = "pet-$index",
            name = "Pet $index",
            petType = petType.name,
            breed = listOf("PERSIAN", "Custom rescue breed", "legacy_Breed-ç")[index % 3],
          )
        )
    }
    database
      .vaccinationEntryDao()
      .insertVaccinationEntry(
        VaccinationEntryEntity(
          id = "vaccination-custom",
          petId = "pet-0",
          vaccineType = "OTHER",
          customVaccineTypeName = "Historical custom vaccine",
          applicationDate = 1L,
        )
      )
    database
      .dewormingEntryDao()
      .insertDewormingEntry(
        DewormingEntryEntity(
          id = "deworming-legacy",
          petId = "pet-5",
          type = "BOTH",
          medication = "Legacy active ingredient",
          applicationDate = 1L,
        )
      )

    val snapshot = useCase.exportBackupSnapshot()

    assertThat(snapshot.pets.map { it.petType }).containsExactlyElementsIn(PetType.entries)
    assertThat(snapshot.pets.map { it.breed })
      .containsExactly(
        "PERSIAN",
        "Custom rescue breed",
        "legacy_Breed-ç",
        "PERSIAN",
        "Custom rescue breed",
        "legacy_Breed-ç",
      )
      .inOrder()
    assertThat(snapshot.vaccinationEntries.single().vaccineType.name).isEqualTo("OTHER")
    assertThat(snapshot.vaccinationEntries.single().customVaccineTypeName)
      .isEqualTo("Historical custom vaccine")
    assertThat(snapshot.dewormingEntries.single().medication).isEqualTo("Legacy active ingredient")
  }
}
