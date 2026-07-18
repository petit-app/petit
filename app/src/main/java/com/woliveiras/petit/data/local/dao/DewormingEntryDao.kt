package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for DewormingEntry entities. */
@Dao
interface DewormingEntryDao {

  /** Get all deworming entries for a pet, ordered by application date (newest first). */
  @Query(
    """
      SELECT * FROM deworming_entries
      WHERE petId = :petId AND deletedAt IS NULL
      ORDER BY applicationDate DESC, updatedAt DESC, id DESC
    """
  )
  fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntryEntity>>

  @Query("SELECT * FROM deworming_entries ORDER BY id")
  suspend fun getAllIncludingDeleted(): List<DewormingEntryEntity>

  @Query("SELECT * FROM deworming_entries WHERE updatedAt >= :since ORDER BY id")
  suspend fun getModifiedSince(since: Long): List<DewormingEntryEntity>

  /** Get the latest deworming entry for each type for a pet. */
  @Query(
    """
        SELECT * FROM deworming_entries d1
        WHERE petId = :petId 
        AND deletedAt IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM deworming_entries d2
            WHERE d2.petId = d1.petId
            AND d2.type = d1.type
            AND d2.deletedAt IS NULL
            AND (
              d2.applicationDate > d1.applicationDate
              OR (d2.applicationDate = d1.applicationDate AND d2.updatedAt > d1.updatedAt)
              OR (d2.applicationDate = d1.applicationDate AND d2.updatedAt = d1.updatedAt AND d2.id > d1.id)
            )
        )
        ORDER BY applicationDate DESC, updatedAt DESC, id DESC
    """
  )
  fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntryEntity>>

  /** Get a deworming entry by ID. */
  @Query("SELECT * FROM deworming_entries WHERE id = :id AND deletedAt IS NULL")
  suspend fun getDewormingEntryById(id: String): DewormingEntryEntity?

  @Query("SELECT * FROM deworming_entries WHERE id = :id")
  suspend fun getByIdIncludingDeleted(id: String): DewormingEntryEntity?

  /** Get all overdue dewormings (nextDueDate < today, only latest entry per type per pet). */
  @Query(
    """
        SELECT * FROM deworming_entries d1
        WHERE deletedAt IS NULL
        AND nextDueDate IS NOT NULL
        AND nextDueDate < :today
        AND NOT EXISTS (
            SELECT 1 FROM deworming_entries d2
            WHERE d2.petId = d1.petId
            AND d2.type = d1.type
            AND d2.deletedAt IS NULL
            AND (
              d2.applicationDate > d1.applicationDate
              OR (d2.applicationDate = d1.applicationDate AND d2.updatedAt > d1.updatedAt)
              OR (d2.applicationDate = d1.applicationDate AND d2.updatedAt = d1.updatedAt AND d2.id > d1.id)
            )
        )
    """
  )
  fun getOverdueDewormings(today: Long): Flow<List<DewormingEntryEntity>>

  /** Get dewormings due within a certain number of days (only latest entry per type per pet). */
  @Query(
    """
        SELECT * FROM deworming_entries d1
        WHERE deletedAt IS NULL
        AND nextDueDate IS NOT NULL
        AND nextDueDate BETWEEN :today AND :futureDate
        AND NOT EXISTS (
            SELECT 1 FROM deworming_entries d2
            WHERE d2.petId = d1.petId
            AND d2.type = d1.type
            AND d2.deletedAt IS NULL
            AND (
              d2.applicationDate > d1.applicationDate
              OR (d2.applicationDate = d1.applicationDate AND d2.updatedAt > d1.updatedAt)
              OR (d2.applicationDate = d1.applicationDate AND d2.updatedAt = d1.updatedAt AND d2.id > d1.id)
            )
        )
        ORDER BY nextDueDate ASC
    """
  )
  fun getUpcomingDewormings(today: Long, futureDate: Long): Flow<List<DewormingEntryEntity>>

  /** Insert a new deworming entry. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDewormingEntry(entry: DewormingEntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDewormingEntries(entries: List<DewormingEntryEntity>)

  /** Update an existing deworming entry. */
  @Update suspend fun updateDewormingEntry(entry: DewormingEntryEntity)

  /** Soft delete a deworming entry. */
  @Query(
    "UPDATE deworming_entries SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id"
  )
  suspend fun softDeleteDewormingEntry(id: String, timestamp: Long = System.currentTimeMillis())

  /** Count deworming entries for a pet. */
  @Query("SELECT COUNT(*) FROM deworming_entries WHERE petId = :petId AND deletedAt IS NULL")
  suspend fun countDewormingEntriesForPet(petId: String): Int

  /** Delete all deworming entries (for data cleanup). */
  @Query("DELETE FROM deworming_entries") suspend fun deleteAll()
}
