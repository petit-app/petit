package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.woliveiras.petit.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for Task entities. */
@Dao
interface TaskDao {

  /** Get all pending tasks ordered by scheduled time. */
  @Query(
    "SELECT * FROM tasks WHERE status = 'PENDING' AND deletedAt IS NULL ORDER BY scheduledFor ASC"
  )
  fun getPendingTasks(): Flow<List<TaskEntity>>

  /** Get all tasks (both pending and completed) ordered by scheduled time. */
  @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY scheduledFor ASC")
  fun getAllTasks(): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks ORDER BY id") suspend fun getAllIncludingDeleted(): List<TaskEntity>

  @Query("SELECT * FROM tasks WHERE updatedAt >= :since ORDER BY id")
  suspend fun getModifiedSince(since: Long): List<TaskEntity>

  /** Get all tasks for a specific pet. */
  @Query("SELECT * FROM tasks WHERE petId = :petId AND deletedAt IS NULL ORDER BY scheduledFor ASC")
  fun getTasksForPet(petId: String): Flow<List<TaskEntity>>

  /** Get a task by ID. */
  @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL")
  suspend fun getTaskById(id: String): TaskEntity?

  @Query("SELECT * FROM tasks WHERE id = :id")
  suspend fun getByIdIncludingDeleted(id: String): TaskEntity?

  /** Get pending tasks due today (scheduledFor between start and end of day). */
  @Query(
    "SELECT * FROM tasks WHERE scheduledFor BETWEEN :dayStart AND :dayEnd AND status = 'PENDING' AND deletedAt IS NULL ORDER BY scheduledFor ASC"
  )
  fun getTasksDueToday(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

  /** Get pending tasks due within a time range. */
  @Query(
    "SELECT * FROM tasks WHERE scheduledFor BETWEEN :from AND :to AND status = 'PENDING' AND deletedAt IS NULL ORDER BY scheduledFor ASC"
  )
  fun getTasksDueInRange(from: Long, to: Long): Flow<List<TaskEntity>>

  /** Get completed tasks ordered by most recently completed. */
  @Query(
    "SELECT * FROM tasks WHERE status = 'COMPLETED' AND deletedAt IS NULL ORDER BY updatedAt DESC"
  )
  fun getCompletedTasks(): Flow<List<TaskEntity>>

  /** Get past due tasks (scheduled before now, still pending). */
  @Query(
    "SELECT * FROM tasks WHERE scheduledFor < :now AND status = 'PENDING' AND deletedAt IS NULL"
  )
  suspend fun getPastDueTasks(now: Long): List<TaskEntity>

  /** Get the next N upcoming pending tasks. */
  @Query(
    "SELECT * FROM tasks WHERE scheduledFor >= :from AND status = 'PENDING' AND deletedAt IS NULL ORDER BY scheduledFor ASC LIMIT :limit"
  )
  fun getNextTasks(from: Long, limit: Int = 5): Flow<List<TaskEntity>>

  /** Insert a new task. */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTask(task: TaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTasks(tasks: List<TaskEntity>)

  /** Update an existing task. */
  @Update suspend fun updateTask(task: TaskEntity)

  /** Update task status. */
  @Query("UPDATE tasks SET status = :status, updatedAt = :timestamp WHERE id = :id")
  suspend fun updateTaskStatus(
    id: String,
    status: String,
    timestamp: Long = System.currentTimeMillis(),
  )

  /** Soft delete a task. */
  @Query("UPDATE tasks SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
  suspend fun softDeleteTask(id: String, timestamp: Long = System.currentTimeMillis())

  /** Soft delete all tasks for a specific entity (when that entity is deleted). */
  @Query(
    "UPDATE tasks SET deletedAt = :timestamp, updatedAt = :timestamp WHERE referenceEntityId = :entityId AND deletedAt IS NULL"
  )
  suspend fun softDeleteTasksByReferenceEntity(
    entityId: String,
    timestamp: Long = System.currentTimeMillis(),
  )

  /** Hard delete all tasks (for data cleanup). */
  @Query("DELETE FROM tasks") suspend fun deleteAll()
}
