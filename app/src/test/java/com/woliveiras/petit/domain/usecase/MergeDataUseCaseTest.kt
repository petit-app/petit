package com.woliveiras.petit.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.Pet
import java.time.Clock
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
  private lateinit var useCase: MergeDataUseCase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val exportImport =
      ExportImportUseCase(
        context,
        database,
        PetRepositoryImpl(database.petDao()),
        WeightEntryRepositoryImpl(database.weightEntryDao()),
        VaccinationEntryRepositoryImpl(database.vaccinationEntryDao(), Clock.systemUTC()),
        DewormingEntryRepositoryImpl(database.dewormingEntryDao(), Clock.systemUTC()),
        TaskRepositoryImpl(database.taskDao()),
      )
    useCase = MergeDataUseCase(exportImport, database)
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
}
