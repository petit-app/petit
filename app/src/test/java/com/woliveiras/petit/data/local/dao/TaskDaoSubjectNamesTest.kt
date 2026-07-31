package com.woliveiras.petit.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.TaskEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDaoSubjectNamesTest {
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
  fun usedNamesComeBackDeduplicatedAndMostRecentlyUsedFirst() = runTest {
    insertTask(id = "1", subjectName = "Apoquel", updatedAt = 100)
    insertTask(id = "2", subjectName = "Dipirona", updatedAt = 300)
    insertTask(id = "3", subjectName = "Apoquel", updatedAt = 500)

    val names = taskDao.getUsedSubjectNames(kind = "MEDICATION", petId = null)

    assertThat(names).containsExactly("Apoquel", "Dipirona").inOrder()
  }

  @Test
  fun blankAndDeletedSubjectsAreIgnored() = runTest {
    insertTask(id = "1", subjectName = "Apoquel", updatedAt = 500, deletedAt = 600)
    insertTask(id = "2", subjectName = "   ", updatedAt = 400)
    insertTask(id = "3", subjectName = null, updatedAt = 300)
    insertTask(id = "4", subjectName = "Dipirona", updatedAt = 200)

    val names = taskDao.getUsedSubjectNames(kind = "MEDICATION", petId = null)

    assertThat(names).containsExactly("Dipirona")
  }

  @Test
  fun onlyTheRequestedKindIsConsidered() = runTest {
    insertTask(id = "1", kind = "MEDICATION", subjectName = "Apoquel", updatedAt = 100)
    insertTask(id = "2", kind = "VACCINATION", subjectName = "V10", updatedAt = 200)

    val names = taskDao.getUsedSubjectNames(kind = "MEDICATION", petId = null)

    assertThat(names).containsExactly("Apoquel")
  }

  @Test
  fun aPetFilterNarrowsTheNamesWhileNullReturnsAllOfThem() = runTest {
    insertPet("pet-a")
    insertPet("pet-b")
    insertTask(id = "1", petId = "pet-a", subjectName = "Apoquel", updatedAt = 100)
    insertTask(id = "2", petId = "pet-b", subjectName = "Dipirona", updatedAt = 200)

    assertThat(taskDao.getUsedSubjectNames(kind = "MEDICATION", petId = "pet-a"))
      .containsExactly("Apoquel")
    assertThat(taskDao.getUsedSubjectNames(kind = "MEDICATION", petId = null))
      .containsExactly("Apoquel", "Dipirona")
  }

  @Test
  fun theLimitCapsHowManyNamesComeBack() = runTest {
    repeat(5) { index ->
      insertTask(id = "task-$index", subjectName = "Remedio $index", updatedAt = index.toLong())
    }

    val names = taskDao.getUsedSubjectNames(kind = "MEDICATION", petId = null, limit = 2)

    assertThat(names).containsExactly("Remedio 4", "Remedio 3").inOrder()
  }

  private suspend fun insertPet(id: String) {
    database
      .petDao()
      .insertPet(
        PetEntity(id = id, name = "Pet $id", petType = "DOG", createdAt = 0, updatedAt = 0)
      )
  }

  private suspend fun insertTask(
    id: String,
    kind: String = "MEDICATION",
    petId: String? = null,
    subjectName: String?,
    updatedAt: Long,
    deletedAt: Long? = null,
  ) {
    taskDao.insertTask(
      TaskEntity(
        id = id,
        petId = petId,
        kind = kind,
        subjectName = subjectName,
        title = "Task $id",
        scheduledFor = 0,
        createdAt = 0,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
      )
    )
  }
}
