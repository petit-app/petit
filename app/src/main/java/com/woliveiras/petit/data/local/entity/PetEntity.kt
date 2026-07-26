package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Room entity representing a pet in the local database. */
@Entity(tableName = "pets")
data class PetEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val name: String,
  val petType: String = "OTHER",
  val birthDate: Long? = null,
  val sex: String = "UNKNOWN",
  val breed: String? = null,
  val breedId: String? = null,
  val color: String? = null,
  val microchipNumber: String? = null,
  val passportNumber: String? = null,
  val photoUri: String? = null,
  val notes: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val deletedAt: Long? = null,
  val syncStatus: String = "LOCAL_ONLY",
)
