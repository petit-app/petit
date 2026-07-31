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
  val recurrence: TaskRecurrence? = null,
  val seriesId: String? = null,
  val occurrenceIndex: Int = 0,
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

  /** Whether this task belongs to a caregiver-owned repeating series. */
  val isRecurring: Boolean
    get() = recurrence != null

  /**
   * Whether the app created this task from a health record. `AutoTaskService` owns these ids, and
   * their cadence comes from the recorded next due date instead of a repeat rule.
   */
  val isAutomatic: Boolean
    get() = id.startsWith(AUTOMATIC_ID_PREFIX)

  /** Groups every occurrence of the same series; the first occurrence seeds it. */
  val effectiveSeriesId: String
    get() = seriesId ?: id

  companion object {
    const val AUTOMATIC_ID_PREFIX = "auto_"
  }
}
