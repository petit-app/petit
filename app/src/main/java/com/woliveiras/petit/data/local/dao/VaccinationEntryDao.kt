package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for VaccinationEntry entities. */
@Dao
interface VaccinationEntryDao {

  /** Get all vaccination entries for a pet, ordered by application date (newest first). */
  @Query(
    """
      SELECT * FROM vaccination_entries
      WHERE petId = :petId AND deletedAt IS NULL
      ORDER BY applicationDate DESC, updatedAt DESC, id DESC
    """
  )
  fun getVaccinationEntriesForPet(petId: String): Flow<List<VaccinationEntryEntity>>

  @Query("SELECT * FROM vaccination_entries ORDER BY id")
  suspend fun getAllIncludingDeleted(): List<VaccinationEntryEntity>

  @Query("SELECT * FROM vaccination_entries WHERE updatedAt >= :since ORDER BY id")
  suspend fun getModifiedSince(since: Long): List<VaccinationEntryEntity>

  /** Get the latest vaccination entry for each vaccine type for a pet. */
  @Query(
    """
        SELECT * FROM vaccination_entries v1
        WHERE petId = :petId 
        AND deletedAt IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM vaccination_entries v2
            WHERE v2.petId = v1.petId
            AND v2.vaccineType = v1.vaccineType
            AND v2.deletedAt IS NULL
            AND (
              v2.applicationDate > v1.applicationDate
              OR (v2.applicationDate = v1.applicationDate AND v2.updatedAt > v1.updatedAt)
              OR (v2.applicationDate = v1.applicationDate AND v2.updatedAt = v1.updatedAt AND v2.id > v1.id)
            )
        )
        ORDER BY applicationDate DESC, updatedAt DESC, id DESC
    """
  )
  fun getLatestVaccinationsForPet(petId: String): Flow<List<VaccinationEntryEntity>>

  /** Get a vaccination entry by ID. */
  @Query("SELECT * FROM vaccination_entries WHERE id = :id AND deletedAt IS NULL")
  suspend fun getVaccinationEntryById(id: String): VaccinationEntryEntity?

  @Query("SELECT * FROM vaccination_entries WHERE id = :id")
  suspend fun getByIdIncludingDeleted(id: String): VaccinationEntryEntity?

  /**
   * Get all overdue vaccinations (nextDueDate < today, only latest entry per vaccine type per pet).
   */
  @Query(
    """
        SELECT * FROM vaccination_entries v1
        WHERE deletedAt IS NULL
        AND nextDueDate IS NOT NULL
        AND nextDueDate < :today
        AND NOT EXISTS (
            SELECT 1 FROM vaccination_entries v2
            WHERE v2.petId = v1.petId
            AND v2.vaccineType = v1.vaccineType
            AND v2.deletedAt IS NULL
            AND (
              v2.applicationDate > v1.applicationDate
              OR (v2.applicationDate = v1.applicationDate AND v2.updatedAt > v1.updatedAt)
              OR (v2.applicationDate = v1.applicationDate AND v2.updatedAt = v1.updatedAt AND v2.id > v1.id)
            )
        )
    """
  )
  fun getOverdueVaccinations(today: Long): Flow<List<VaccinationEntryEntity>>

  /**
   * Get vaccinations due within a certain number of days (only latest entry per vaccine type per
   * pet).
   */
  @Query(
    """
        SELECT * FROM vaccination_entries v1
        WHERE deletedAt IS NULL
        AND nextDueDate IS NOT NULL
        AND nextDueDate BETWEEN :today AND :futureDate
        AND NOT EXISTS (
            SELECT 1 FROM vaccination_entries v2
            WHERE v2.petId = v1.petId
            AND v2.vaccineType = v1.vaccineType
            AND v2.deletedAt IS NULL
            AND (
              v2.applicationDate > v1.applicationDate
              OR (v2.applicationDate = v1.applicationDate AND v2.updatedAt > v1.updatedAt)
              OR (v2.applicationDate = v1.applicationDate AND v2.updatedAt = v1.updatedAt AND v2.id > v1.id)
            )
        )
        ORDER BY nextDueDate ASC
    """
  )
  fun getUpcomingVaccinations(today: Long, futureDate: Long): Flow<List<VaccinationEntryEntity>>

  /** Insert a new vaccination entry. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVaccinationEntry(entry: VaccinationEntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVaccinationEntries(entries: List<VaccinationEntryEntity>)

  /** Update an existing vaccination entry. */
  @Update suspend fun updateVaccinationEntry(entry: VaccinationEntryEntity)

  /** Soft delete a vaccination entry. */
  @Query(
    "UPDATE vaccination_entries SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id"
  )
  suspend fun softDeleteVaccinationEntry(id: String, timestamp: Long = System.currentTimeMillis())

  /** Count vaccination entries for a pet. */
  @Query("SELECT COUNT(*) FROM vaccination_entries WHERE petId = :petId AND deletedAt IS NULL")
  suspend fun countVaccinationEntriesForPet(petId: String): Int

  /** Delete all vaccination entries (for data cleanup). */
  @Query("DELETE FROM vaccination_entries") suspend fun deleteAll()
}
