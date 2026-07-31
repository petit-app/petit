package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Room entity representing a task (todo item) for pet care. */
@Entity(
  tableName = "tasks",
  foreignKeys =
    [
      ForeignKey(
        entity = PetEntity::class,
        parentColumns = ["id"],
        childColumns = ["petId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("petId")],
)
data class TaskEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val petId: String? = null,
  val kind: String,
  val referenceEntityId: String? = null,
  val subjectCode: String? = null,
  val subjectName: String? = null,
  val title: String,
  val description: String? = null,
  val scheduledFor: Long,
  val status: String = "PENDING",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val deletedAt: Long? = null,
  val syncStatus: String = "LOCAL_ONLY",
)
