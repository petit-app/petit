package com.woliveiras.petit.data.repository

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.dao.DewormingEntryDao
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DewormingEntryRepositoryImplTest {

  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)
  private val dao = RecordingDewormingEntryDao()
  private val repository = DewormingEntryRepositoryImpl(dao, clock)

  @Test
  fun overdueQueryUsesStartOfControlledToday() = runTest {
    repository.getOverdueDewormings().first()

    assertThat(dao.overdueToday)
      .isEqualTo(LocalDate.of(2026, 7, 17).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
  }

  @Test
  fun upcomingQueryUsesControlledDateRange() = runTest {
    repository.getUpcomingDewormings(days = 30).first()

    assertThat(dao.upcomingToday)
      .isEqualTo(LocalDate.of(2026, 7, 17).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    assertThat(dao.upcomingFutureDate)
      .isEqualTo(LocalDate.of(2026, 8, 16).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
  }

  @Test
  fun softDeleteUsesControlledTimestamp() = runTest {
    repository.deleteDewormingEntry("entry-1")

    assertThat(dao.deletedId).isEqualTo("entry-1")
    assertThat(dao.deletedAt).isEqualTo(clock.millis())
  }

  private class RecordingDewormingEntryDao : DewormingEntryDao {
    var overdueToday: Long? = null
    var upcomingToday: Long? = null
    var upcomingFutureDate: Long? = null
    var deletedId: String? = null
    var deletedAt: Long? = null

    override fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntryEntity>> =
      MutableStateFlow(emptyList())

    override suspend fun getAllIncludingDeleted(): List<DewormingEntryEntity> = emptyList()

    override fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntryEntity>> =
      MutableStateFlow(emptyList())

    override suspend fun getDewormingEntryById(id: String): DewormingEntryEntity? = null

    override suspend fun getByIdIncludingDeleted(id: String): DewormingEntryEntity? = null

    override fun getOverdueDewormings(today: Long): Flow<List<DewormingEntryEntity>> {
      overdueToday = today
      return MutableStateFlow(emptyList())
    }

    override fun getUpcomingDewormings(
      today: Long,
      futureDate: Long,
    ): Flow<List<DewormingEntryEntity>> {
      upcomingToday = today
      upcomingFutureDate = futureDate
      return MutableStateFlow(emptyList())
    }

    override suspend fun insertDewormingEntry(entry: DewormingEntryEntity) = Unit

    override suspend fun insertDewormingEntries(entries: List<DewormingEntryEntity>) = Unit

    override suspend fun updateDewormingEntry(entry: DewormingEntryEntity) = Unit

    override suspend fun softDeleteDewormingEntry(id: String, timestamp: Long) {
      deletedId = id
      deletedAt = timestamp
    }

    override suspend fun countDewormingEntriesForPet(petId: String): Int = 0

    override suspend fun deleteAll() = Unit
  }
}
