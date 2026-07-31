package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

/** Repository interface for Task operations. */
interface TaskRepository {

  /** Get all pending tasks, ordered by scheduled time. */
  fun getPendingTasks(): Flow<List<Task>>

  /** Get complete active task history, including pending and completed tasks. */
  fun getAllActiveTasks(): Flow<List<Task>>

  /** Get all tasks for a specific pet. */
  fun getTasksForPet(petId: String): Flow<List<Task>>

  /** Get a task by ID. */
  suspend fun getTaskById(id: String): Task?

  /** Get pending tasks due today. */
  fun getTasksDueToday(): Flow<List<Task>>

  /** Get pending tasks due this week (from start of week to end of week). */
  fun getTasksDueThisWeek(): Flow<List<Task>>

  /** Get pending tasks due this month (from start of month to end of month). */
  fun getTasksDueThisMonth(): Flow<List<Task>>

  /** Get pending tasks due in a custom range (millis). */
  fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>>

  /** Get the next N upcoming pending tasks. */
  fun getNextTasks(limit: Int = 5): Flow<List<Task>>

  /** Get past due tasks that are still pending. */
  suspend fun getPastDueTasks(): List<Task>

  /** Get completed tasks ordered by completion date. */
  fun getCompletedTasks(): Flow<List<Task>>

  /** Get subject names already used for a kind, most recently used first. */
  suspend fun getUsedSubjectNames(kind: TaskKind, petId: String?): List<String>

  /** Save a task (insert or update). */
  suspend fun saveTask(task: Task)

  /** Update task status. */
  suspend fun updateTaskStatus(id: String, status: TaskStatus)

  /** Delete a task (soft delete). */
  suspend fun deleteTask(id: String)

  /** Delete all tasks for a specific entity (when that entity is deleted). */
  suspend fun deleteTasksByReferenceEntity(entityId: String)
}
