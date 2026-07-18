package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.woliveiras.petit.data.local.entity.PetEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for Pet entities. */
@Dao
interface PetDao {

  /** Get all active (non-deleted) pets ordered by name. */
  @Query("SELECT * FROM pets WHERE deletedAt IS NULL ORDER BY name ASC")
  fun getAllPets(): Flow<List<PetEntity>>

  @Query("SELECT * FROM pets ORDER BY id") suspend fun getAllIncludingDeleted(): List<PetEntity>

  /** Get a single pet by ID. */
  @Query("SELECT * FROM pets WHERE id = :id AND deletedAt IS NULL")
  suspend fun getPetById(id: String): PetEntity?

  @Query("SELECT * FROM pets WHERE id = :id")
  suspend fun getByIdIncludingDeleted(id: String): PetEntity?

  /** Get a single pet by ID as Flow for reactive updates. */
  @Query("SELECT * FROM pets WHERE id = :id AND deletedAt IS NULL")
  fun getPetByIdFlow(id: String): Flow<PetEntity?>

  /** Get the count of active pets. */
  @Query("SELECT COUNT(*) FROM pets WHERE deletedAt IS NULL") fun getPetCount(): Flow<Int>

  /** Insert a new pet. */
  @Upsert suspend fun insertPet(pet: PetEntity)

  @Upsert suspend fun insertPets(pets: List<PetEntity>)

  /** Update an existing pet. */
  @Update suspend fun updatePet(pet: PetEntity)

  /** Soft delete a pet by setting deletedAt timestamp. */
  @Query("UPDATE pets SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
  suspend fun softDeletePet(id: String, timestamp: Long = System.currentTimeMillis())

  /** Hard delete a pet (for testing or data cleanup). */
  @Query("DELETE FROM pets WHERE id = :id") suspend fun hardDeletePet(id: String)

  /** Get all pets including deleted ones (for sync purposes). */
  @Query("SELECT * FROM pets WHERE updatedAt > :since")
  suspend fun getPetsModifiedSince(since: Long): List<PetEntity>

  /** Delete all pets (for data cleanup). */
  @Query("DELETE FROM pets") suspend fun deleteAll()
}
