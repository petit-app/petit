package com.woliveiras.petit.data.mapper

import com.woliveiras.petit.data.local.entity.TaskEntity
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import java.time.Instant
import java.time.ZoneId

/** Mappers for Task entity <-> domain model conversions. */
fun TaskEntity.toDomain(): Task =
  Task(
    id = id,
    petId = petId,
    kind =
      try {
        TaskKind.valueOf(kind)
      } catch (_: Exception) {
        TaskKind.CUSTOM
      },
    referenceEntityId = referenceEntityId,
    subjectCode = subjectCode,
    subjectName = subjectName,
    title = title,
    description = description,
    scheduledFor =
      Instant.ofEpochMilli(scheduledFor).atZone(ZoneId.systemDefault()).toLocalDateTime(),
    status =
      try {
        TaskStatus.valueOf(status)
      } catch (_: Exception) {
        TaskStatus.PENDING
      },
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus =
      try {
        SyncStatus.valueOf(syncStatus)
      } catch (_: Exception) {
        SyncStatus.LOCAL_ONLY
      },
  )

fun Task.toEntity(): TaskEntity =
  TaskEntity(
    id = id,
    petId = petId,
    kind = kind.name,
    referenceEntityId = referenceEntityId,
    subjectCode = subjectCode,
    subjectName = subjectName,
    title = title,
    description = description,
    scheduledFor = scheduledFor.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus.name,
  )

@JvmName("taskEntitiesToDomain") fun List<TaskEntity>.toDomain(): List<Task> = map { it.toDomain() }
