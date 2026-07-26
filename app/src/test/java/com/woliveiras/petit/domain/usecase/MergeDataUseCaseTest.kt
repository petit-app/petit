package com.woliveiras.petit.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.FamilyGroupRepositoryImpl
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.MembershipChangeType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MergeDataUseCaseTest {
  private lateinit var database: PetitDatabase
  private lateinit var exportImport: ExportImportUseCase
  private lateinit var useCase: MergeDataUseCase
  private lateinit var familyGroupRepository: FamilyGroupRepositoryImpl

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    exportImport =
      ExportImportUseCase(
        context,
        database,
        PetRepositoryImpl(database.petDao()),
        WeightEntryRepositoryImpl(database.weightEntryDao()),
        VaccinationEntryRepositoryImpl(database.vaccinationEntryDao(), Clock.systemUTC()),
        DewormingEntryRepositoryImpl(database.dewormingEntryDao(), Clock.systemUTC()),
        TaskRepositoryImpl(database.taskDao()),
      )
    familyGroupRepository =
      FamilyGroupRepositoryImpl(
        context,
        database.familyGroupMemberDao(),
        database.syncLogDao(),
        database,
      )
    useCase = MergeDataUseCase(exportImport, database, familyGroupRepository)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun entityAndAuditMetadataAreCommittedTogether() = runTest {
    val result = useCase(bundle("remote-pet"), "peer-1", "Kitchen phone")

    assertThat(database.petDao().getPetById("remote-pet")).isNotNull()
    val log = database.syncLogDao().getAllSyncLogs().first().single()
    assertThat(log.peerId).isEqualTo("peer-1")
    assertThat(log.peerName).isEqualTo("Kitchen phone")
    assertThat(log.syncType).isEqualTo("MERGE")
    assertThat(log.entitiesReceived).isEqualTo(1)
  }

  @Test
  fun receivedCountIncludesAConflictingVersionThatIsKeptLocal() = runTest {
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Local", updatedAt = 20L))

    val result = useCase(bundle("pet-1"), "peer-1", "Kitchen phone")

    assertThat(result.totalAdded + result.totalUpdated + result.totalRemoved).isEqualTo(0)
    assertThat(result.conflictsResolved).isEqualTo(1)
    val log = database.syncLogDao().getLatestSyncLog()
    assertThat(log?.entitiesReceived).isEqualTo(1)
    assertThat(log?.conflictsResolved).isEqualTo(1)
  }

  @Test
  fun winningRemoteConflictsPreserveRawBreedCustomVaccineAndLegacyMedicationValues() = runTest {
    val incoming = compatibilityConflictBundle()
    incoming.pets.forEach { pet ->
      database
        .petDao()
        .insertPet(
          PetEntity(
            id = pet.id,
            name = "Local ${pet.name}",
            petType = PetType.OTHER.name,
            breed = "local-breed",
            createdAt = 1L,
            updatedAt = 10L,
          )
        )
    }
    database
      .vaccinationEntryDao()
      .insertVaccinationEntry(
        VaccinationEntryEntity(
          id = incoming.vaccinationEntries.single().id,
          petId = incoming.pets.first().id,
          vaccineType = VaccineType.RABIES.name,
          applicationDate = 1L,
          createdAt = 1L,
          updatedAt = 10L,
        )
      )
    database
      .dewormingEntryDao()
      .insertDewormingEntry(
        DewormingEntryEntity(
          id = incoming.dewormingEntries.single().id,
          petId = incoming.pets.last().id,
          type = DewormingType.INTERNAL.name,
          medication = "local-medication",
          applicationDate = 1L,
          createdAt = 1L,
          updatedAt = 10L,
        )
      )

    val result = useCase(incoming, "peer-compat", "Kitchen phone")

    val restored = exportImport.exportBackupSnapshot()
    assertThat(restored.pets.map { Triple(it.id, it.petType, it.breed) })
      .containsExactlyElementsIn(incoming.pets.map { Triple(it.id, it.petType, it.breed) })
    assertThat(restored.vaccinationEntries).containsExactlyElementsIn(incoming.vaccinationEntries)
    assertThat(restored.dewormingEntries).containsExactlyElementsIn(incoming.dewormingEntries)
    assertThat(result.conflictsResolved).isEqualTo(incoming.entityCount)
  }

  @Test
  fun receivedMembershipChangeIsAppliedAndCanBeRetried() = runTest {
    familyGroupRepository.persistAuthorizedPairing(
      "group-key",
      member("local-id", "This device", true),
      member("remote-id", "Old peer", false),
    )
    val change =
      MembershipChange(
        MembershipChange.groupIdForKey("group-key"),
        "remote-id",
        MembershipChangeType.RENAME,
        deviceName = "Kitchen tablet",
        timestamp = 20L,
      )
    val incoming = bundle("remote-pet").copy(membershipChanges = listOf(change))

    useCase(incoming, "peer-1", "Kitchen tablet")
    useCase(incoming, "peer-1", "Kitchen tablet")

    val remote =
      familyGroupRepository.familyGroupInfo.first()?.members?.single { it.id == "remote-id" }
    assertThat(remote?.deviceName).isEqualTo("Kitchen tablet")
  }

  @Test
  fun syncLogFailureRollsBackAppliedEntities() = runTest {
    database.petDao().insertPet(PetEntity(id = "local-pet", name = "Local"))
    database.openHelper.writableDatabase.execSQL(
      """
      CREATE TRIGGER reject_sync_log
      BEFORE INSERT ON sync_logs
      BEGIN
        SELECT RAISE(ABORT, 'forced sync log failure');
      END
      """
        .trimIndent()
    )

    val failure = runCatching { useCase(bundle("remote-pet"), "peer-1", "Kitchen phone") }

    assertThat(failure.isFailure).isTrue()
    assertThat(database.petDao().getAllPets().first().map { it.id }).containsExactly("local-pet")
    assertThat(database.syncLogDao().getAllSyncLogs().first()).isEmpty()
  }

  private fun bundle(petId: String) =
    ExportBundle(
      metadata = ExportMetadata("1.0", "2026-07-18T00:00:00Z"),
      pets = listOf(Pet(petId, "Remote", createdAt = 1L, updatedAt = 1L)),
      weightEntries = emptyList(),
      vaccinationEntries = emptyList(),
      dewormingEntries = emptyList(),
      tasks = emptyList(),
    )

  private fun compatibilityConflictBundle(): ExportBundle {
    val pets =
      PetType.entries.mapIndexed { index, petType ->
        Pet(
          id = "compat-pet-$index",
          name = "Remote $index",
          petType = petType,
          breed = listOf("PERSIAN", "Custom rescue breed", "legacy_Breed-ç")[index % 3],
          createdAt = 1L,
          updatedAt = 20L,
        )
      }
    return ExportBundle(
      metadata = ExportMetadata("1.0", "2026-07-18T00:00:00Z"),
      pets = pets,
      weightEntries = emptyList(),
      vaccinationEntries =
        listOf(
          VaccinationEntry(
            id = "compat-vaccination",
            petId = pets.first().id,
            vaccineType = VaccineType.OTHER,
            customVaccineTypeName = "Historical custom vaccine",
            applicationDate = LocalDate.of(2026, 7, 1),
            createdAt = 1L,
            updatedAt = 20L,
          )
        ),
      dewormingEntries =
        listOf(
          DewormingEntry(
            id = "compat-deworming",
            petId = pets.last().id,
            type = DewormingType.BOTH,
            medication = "Legacy active ingredient",
            applicationDate = LocalDate.of(2026, 7, 1),
            createdAt = 1L,
            updatedAt = 20L,
          )
        ),
      tasks = emptyList(),
    )
  }

  private fun member(id: String, name: String, local: Boolean) =
    FamilyGroupMember(id, name, "group-key", local, null, 1L, 1L)
}
