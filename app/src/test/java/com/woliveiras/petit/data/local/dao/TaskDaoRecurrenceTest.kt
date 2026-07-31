package com.woliveiras.petit.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.DailyWindow
import com.woliveiras.petit.domain.model.RecurrenceEnd
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskRecurrence
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDaoRecurrenceTest {
  private lateinit var database: PetitDatabase
  private lateinit var taskDao: TaskDao

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    taskDao = database.taskDao()
  }

  @After fun tearDown() = database.close()

  @Test
  fun aRepeatingTaskKeepsItsRuleSeriesAndPositionAcrossASaveAndRead() = runTest {
    val task =
      task(
        id = "task-1",
        recurrence =
          TaskRecurrence(
            interval = 8,
            unit = RecurrenceUnit.HOURS,
            dailyWindow = DailyWindow(LocalTime.of(7, 0), LocalTime.of(23, 0)),
            end = RecurrenceEnd.AfterOccurrences(6),
          ),
        seriesId = "series-1",
        occurrenceIndex = 3,
      )

    taskDao.insertTask(task.toEntity())
    val stored = taskDao.getTaskById("task-1")?.toDomain()

    assertThat(stored?.recurrence).isEqualTo(task.recurrence)
    assertThat(stored?.seriesId).isEqualTo("series-1")
    assertThat(stored?.occurrenceIndex).isEqualTo(3)
    assertThat(stored?.isRecurring).isTrue()
  }

  @Test
  fun aTaskWithoutARuleComesBackAsASingleOccurrence() = runTest {
    taskDao.insertTask(task(id = "task-2").toEntity())

    val stored = taskDao.getTaskById("task-2")?.toDomain()

    assertThat(stored?.recurrence).isNull()
    assertThat(stored?.seriesId).isNull()
    assertThat(stored?.occurrenceIndex).isEqualTo(0)
    assertThat(stored?.effectiveSeriesId).isEqualTo("task-2")
  }

  private fun task(
    id: String,
    recurrence: TaskRecurrence? = null,
    seriesId: String? = null,
    occurrenceIndex: Int = 0,
  ) =
    Task(
      id = id,
      kind = TaskKind.MEDICATION,
      title = "Remédio",
      scheduledFor = LocalDateTime.of(2026, 3, 10, 8, 0),
      recurrence = recurrence,
      seriesId = seriesId,
      occurrenceIndex = occurrenceIndex,
      createdAt = 1L,
      updatedAt = 1L,
    )
}
