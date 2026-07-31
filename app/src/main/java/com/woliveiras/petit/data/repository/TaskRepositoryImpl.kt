package com.woliveiras.petit.data.repository

import com.woliveiras.petit.data.local.dao.TaskDao
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of TaskRepository using Room database. */
@Singleton
class TaskRepositoryImpl @Inject constructor(private val taskDao: TaskDao) : TaskRepository {

  override fun getPendingTasks(): Flow<List<Task>> {
    return taskDao.getPendingTasks().map { entities -> entities.toDomain() }
  }

  override fun getAllActiveTasks(): Flow<List<Task>> {
    return taskDao.getAllTasks().map { entities -> entities.toDomain() }
  }

  override fun getTasksForPet(petId: String): Flow<List<Task>> {
    return taskDao.getTasksForPet(petId).map { entities -> entities.toDomain() }
  }

  override suspend fun getTaskById(id: String): Task? {
    return taskDao.getTaskById(id)?.toDomain()
  }

  override fun getTasksDueToday(): Flow<List<Task>> {
    val today = LocalDate.now()
    val dayStart = today.atStartOfDay().toMillis()
    val dayEnd = today.atTime(LocalTime.MAX).toMillis()
    return taskDao.getTasksDueToday(dayStart, dayEnd).map { entities -> entities.toDomain() }
  }

  override fun getTasksDueThisWeek(): Flow<List<Task>> {
    val today = LocalDate.now()
    val weekFields = WeekFields.of(Locale.getDefault())
    val weekStart = today.with(weekFields.dayOfWeek(), 1).atStartOfDay().toMillis()
    val weekEnd = today.with(weekFields.dayOfWeek(), 7).atTime(LocalTime.MAX).toMillis()
    return taskDao.getTasksDueInRange(weekStart, weekEnd).map { entities -> entities.toDomain() }
  }

  override fun getTasksDueThisMonth(): Flow<List<Task>> {
    val today = LocalDate.now()
    val yearMonth = YearMonth.from(today)
    val monthStart = yearMonth.atDay(1).atStartOfDay().toMillis()
    val monthEnd = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).toMillis()
    return taskDao.getTasksDueInRange(monthStart, monthEnd).map { entities -> entities.toDomain() }
  }

  override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> {
    return taskDao.getTasksDueInRange(fromMillis, toMillis).map { entities -> entities.toDomain() }
  }

  override fun getNextTasks(limit: Int): Flow<List<Task>> {
    val now = System.currentTimeMillis()
    return taskDao.getNextTasks(now, limit).map { entities -> entities.toDomain() }
  }

  override suspend fun getPastDueTasks(): List<Task> {
    val now = System.currentTimeMillis()
    return taskDao.getPastDueTasks(now).toDomain()
  }

  override suspend fun getPendingRecurringTasks(): List<Task> =
    taskDao.getPendingRecurringTasks().toDomain()

  override fun getCompletedTasks(): Flow<List<Task>> {
    return taskDao.getCompletedTasks().map { entities -> entities.toDomain() }
  }

  override suspend fun getUsedSubjectNames(kind: TaskKind, petId: String?): List<String> {
    return taskDao.getUsedSubjectNames(kind.name, petId)
  }

  override suspend fun saveTask(task: Task) {
    val updatedTask = task.copy(updatedAt = System.currentTimeMillis())
    val existingTask = taskDao.getTaskById(task.id)
    if (existingTask != null) {
      taskDao.updateTask(updatedTask.toEntity())
    } else {
      taskDao.insertTask(updatedTask.toEntity())
    }
  }

  override suspend fun updateTaskStatus(id: String, status: TaskStatus) {
    taskDao.updateTaskStatus(id, status.name)
  }

  override suspend fun deleteTask(id: String) {
    taskDao.softDeleteTask(id)
  }

  override suspend fun deleteTasksByReferenceEntity(entityId: String) {
    taskDao.softDeleteTasksByReferenceEntity(entityId)
  }

  private fun java.time.LocalDateTime.toMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
