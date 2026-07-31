package com.woliveiras.petit.domain.model

import java.time.LocalDateTime

/** Domain model representing a scheduled task/reminder. */
data class Task(
  val id: String,
  val petId: String? = null,
  val kind: TaskKind,
  val referenceEntityId: String? = null,
  val subjectCode: String? = null,
  val subjectName: String? = null,
  val title: String,
  val description: String? = null,
  val scheduledFor: LocalDateTime,
  val status: TaskStatus = TaskStatus.PENDING,
  val createdAt: Long,
  val updatedAt: Long,
  val deletedAt: Long? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
) {

  val isPending: Boolean
    get() = status == TaskStatus.PENDING

  val isCompleted: Boolean
    get() = status == TaskStatus.COMPLETED

  /** Whether the task is past its scheduled time and still pending. */
  val isPastDue: Boolean
    get() = scheduledFor.isBefore(LocalDateTime.now()) && status == TaskStatus.PENDING

  companion object
}
