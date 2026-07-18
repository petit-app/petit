package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Singleton durable watermark for restorable local state. */
@Entity(tableName = "restorable_revision")
data class RestorableRevisionEntity(
  @PrimaryKey val id: Int = SINGLETON_ID,
  val currentRevision: Long = 0,
  val completedRevision: Long = 0,
) {
  companion object {
    const val SINGLETON_ID = 0
  }
}
