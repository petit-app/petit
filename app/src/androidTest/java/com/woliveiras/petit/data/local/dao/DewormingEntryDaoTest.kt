package com.woliveiras.petit.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.SyncStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DewormingEntryDaoTest {

  private lateinit var database: PetitDatabase
  private lateinit var dao: DewormingEntryDao

  @Before
  fun setUp() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java).build()
    dao = database.dewormingEntryDao()
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi", petType = "CAT"))
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun latestSelectionIsDeterministicAndCompleteHistoryIsDescending() = runTest {
    dao.insertDewormingEntry(entry("old", applicationDate = 100L, updatedAt = 100L))
    dao.insertDewormingEntry(entry("older-update", applicationDate = 200L, updatedAt = 200L))
    dao.insertDewormingEntry(entry("tie-a", applicationDate = 200L, updatedAt = 300L))
    dao.insertDewormingEntry(entry("tie-b", applicationDate = 200L, updatedAt = 300L))

    assertThat(dao.getLatestDewormingsForPet("pet-1").first().map { it.id })
      .containsExactly("tie-b")
    assertThat(dao.getDewormingEntriesForPet("pet-1").first().map { it.id })
      .containsExactly("tie-b", "tie-a", "older-update", "old")
      .inOrder()
  }

  @Test
  fun allTypesPersistUpdateAndSoftDeleteHidesOnlyDeletedRecord() = runTest {
    dao.insertDewormingEntry(entry("internal", 300L, 300L).copy(type = "INTERNAL"))
    dao.insertDewormingEntry(entry("external", 200L, 200L).copy(type = "EXTERNAL"))
    dao.insertDewormingEntry(entry("both", 100L, 100L).copy(type = "BOTH"))
    val updated =
      entry("both", 100L, 400L)
        .copy(type = "BOTH", medication = "Updated", nextDueDate = 500L, note = "note")

    dao.updateDewormingEntry(updated)

    assertThat(dao.getDewormingEntryById("both")).isEqualTo(updated)

    dao.softDeleteDewormingEntry("both", timestamp = 600L)

    assertThat(dao.getDewormingEntryById("both")).isNull()
    assertThat(dao.getDewormingEntriesForPet("pet-1").first().map { it.id })
      .containsExactly("internal", "external")
      .inOrder()
  }

  @Test
  fun repositoryPreservesTimestampProvidedByControlledClock() = runTest {
    val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)
    val repository = DewormingEntryRepositoryImpl(dao, clock)
    val entry =
      DewormingEntry(
        id = "entry-clock",
        petId = "pet-1",
        type = DewormingType.BOTH,
        medication = "Broadline",
        applicationDate = LocalDate.of(2026, 7, 1),
        createdAt = clock.millis(),
        updatedAt = clock.millis(),
        syncStatus = SyncStatus.LOCAL_ONLY,
      )

    repository.saveDewormingEntry(entry)

    assertThat(dao.getDewormingEntryById(entry.id)?.updatedAt).isEqualTo(clock.millis())
  }

  private fun entry(id: String, applicationDate: Long, updatedAt: Long) =
    DewormingEntryEntity(
      id = id,
      petId = "pet-1",
      type = "INTERNAL",
      medication = "Milbemax",
      applicationDate = applicationDate,
      createdAt = 10L,
      updatedAt = updatedAt,
    )
}
