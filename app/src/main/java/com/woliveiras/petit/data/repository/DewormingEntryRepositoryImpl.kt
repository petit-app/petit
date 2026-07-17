package com.woliveiras.petit.data.repository

import com.woliveiras.petit.data.local.dao.DewormingEntryDao
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.DewormingEntry
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of DewormingEntryRepository using Room database. */
@Singleton
class DewormingEntryRepositoryImpl
@Inject
constructor(private val dewormingEntryDao: DewormingEntryDao, private val clock: Clock) :
  DewormingEntryRepository {

  override fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntry>> {
    return dewormingEntryDao.getDewormingEntriesForPet(petId).map { entities ->
      entities.toDomain()
    }
  }

  override fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntry>> {
    return dewormingEntryDao.getLatestDewormingsForPet(petId).map { entities ->
      entities.toDomain()
    }
  }

  override suspend fun getDewormingEntryById(id: String): DewormingEntry? {
    return dewormingEntryDao.getDewormingEntryById(id)?.toDomain()
  }

  override fun getOverdueDewormings(): Flow<List<DewormingEntry>> {
    val today = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    return dewormingEntryDao.getOverdueDewormings(today).map { entities -> entities.toDomain() }
  }

  override fun getUpcomingDewormings(days: Int): Flow<List<DewormingEntry>> {
    val today = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    val futureDate =
      LocalDate.now(clock)
        .plusDays(days.toLong())
        .atStartOfDay(clock.zone)
        .toInstant()
        .toEpochMilli()
    return dewormingEntryDao.getUpcomingDewormings(today, futureDate).map { entities ->
      entities.toDomain()
    }
  }

  override suspend fun saveDewormingEntry(entry: DewormingEntry) {
    val existingEntry = dewormingEntryDao.getDewormingEntryById(entry.id)
    if (existingEntry != null) {
      dewormingEntryDao.updateDewormingEntry(entry.toEntity())
    } else {
      dewormingEntryDao.insertDewormingEntry(entry.toEntity())
    }
  }

  override suspend fun deleteDewormingEntry(id: String) {
    dewormingEntryDao.softDeleteDewormingEntry(id, clock.millis())
  }

  override suspend fun countEntriesForPet(petId: String): Int {
    return dewormingEntryDao.countDewormingEntriesForPet(petId)
  }
}
