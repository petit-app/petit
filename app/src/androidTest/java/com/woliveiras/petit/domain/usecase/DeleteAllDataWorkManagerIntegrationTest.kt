package com.woliveiras.petit.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.repository.FamilyGroupRepositoryImpl
import com.woliveiras.petit.data.repository.ReminderPreferencesRepositoryImpl
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.worker.TaskSchedulerImpl
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteAllDataWorkManagerIntegrationTest {

  private lateinit var context: Context
  private lateinit var database: PetitDatabase
  private lateinit var workManager: WorkManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.filesDir.resolve("datastore").deleteRecursively()
    database = Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java).build()
    workManager = WorkManager.getInstance(context)
  }

  @After
  fun tearDown() {
    database.close()
    context.filesDir.resolve("datastore").deleteRecursively()
  }

  @Test
  fun purgeCancelsScheduledWorkBeforeItSucceedsOnAnEmptyDatabase() = runTest {
    val taskId = "delete-all-${System.nanoTime()}"
    val scheduler = TaskSchedulerImpl(context)
    scheduler.scheduleTask(
      Task(
        id = taskId,
        kind = TaskKind.CUSTOM,
        title = "Pending reminder",
        scheduledFor = LocalDateTime.now().plusDays(1),
        createdAt = 1L,
        updatedAt = 1L,
      )
    )
    val uniqueWorkName = "petit_task_$taskId"
    waitForWork(uniqueWorkName) { it.isNotEmpty() }

    val result =
      DeleteAllDataUseCase(
          database = database,
          taskScheduler = scheduler,
          reminderPreferencesRepository = ReminderPreferencesRepositoryImpl(context),
          familyGroupRepository =
            FamilyGroupRepositoryImpl(
              context,
              database.familyGroupMemberDao(),
              database.syncLogDao(),
              database,
            ),
        )
        .execute()

    assertThat(result.isSuccess).isTrue()
    waitForWork(uniqueWorkName) { infos ->
      infos.isNotEmpty() && infos.all { it.state == WorkInfo.State.CANCELLED }
    }
  }

  private fun waitForWork(uniqueWorkName: String, condition: (List<WorkInfo>) -> Boolean) {
    repeat(20) {
      val work = workManager.getWorkInfosForUniqueWork(uniqueWorkName).get(5, TimeUnit.SECONDS)
      if (condition(work)) return
      Thread.sleep(100)
    }
    val work = workManager.getWorkInfosForUniqueWork(uniqueWorkName).get(5, TimeUnit.SECONDS)
    assertThat(condition(work)).isTrue()
  }
}
