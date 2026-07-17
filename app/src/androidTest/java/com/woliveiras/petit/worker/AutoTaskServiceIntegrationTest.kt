package com.woliveiras.petit.worker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.ReminderPreferences
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoTaskServiceIntegrationTest {

  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)
  private lateinit var database: PetitDatabase
  private lateinit var scheduler: RecordingTaskScheduler
  private lateinit var service: AutoTaskServiceImpl

  @Before
  fun setUp() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java).build()
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
    scheduler = RecordingTaskScheduler()
    service =
      AutoTaskServiceImpl(
        context = context,
        taskRepository = TaskRepositoryImpl(database.taskDao()),
        reminderPreferencesRepository = FixedReminderPreferencesRepository(),
        taskScheduler = scheduler,
        petRepository = FixedPetRepository(),
        clock = clock,
      )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun replacingThenDeletingLinkedVaccinationUpdatesRoomAndCancelsUniqueWork() = runTest {
    service.handleVaccinationSaved(vaccination(nextDueDate = LocalDate.of(2026, 8, 1)))

    assertThat(database.taskDao().getPendingTasks().first().single().scheduledFor)
      .isEqualTo(LocalDateTime.of(2026, 7, 27, 9, 0).toEpochMillis())

    service.handleVaccinationSaved(vaccination(nextDueDate = LocalDate.of(2026, 8, 2)))

    val active = database.taskDao().getPendingTasks().first()
    assertThat(active).hasSize(1)
    assertThat(active.single().scheduledFor)
      .isEqualTo(LocalDateTime.of(2026, 7, 28, 9, 0).toEpochMillis())
    assertThat(scheduler.cancelled).containsExactly("auto_vacc_vacc-1", "auto_vacc_vacc-1")
    assertThat(scheduler.scheduled).hasSize(2)

    service.handleVaccinationDeleted("vacc-1")
    service.handleVaccinationDeleted("vacc-1")

    assertThat(database.taskDao().getPendingTasks().first()).isEmpty()
    assertThat(database.taskDao().getAllTasks().first()).isEmpty()
    assertThat(scheduler.cancelled)
      .containsExactly(
        "auto_vacc_vacc-1",
        "auto_vacc_vacc-1",
        "auto_vacc_vacc-1",
        "auto_vacc_vacc-1",
      )
  }

  private fun vaccination(nextDueDate: LocalDate) =
    VaccinationEntry(
      id = "vacc-1",
      petId = "pet-1",
      vaccineType = VaccineType.RABIES,
      applicationDate = LocalDate.of(2026, 7, 1),
      nextDueDate = nextDueDate,
      createdAt = 1L,
      updatedAt = 1L,
    )

  private fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

  private class FixedReminderPreferencesRepository : ReminderPreferencesRepository {
    private val value = ReminderPreferences(vaccinationDaysBefore = 5)

    override val preferences: Flow<ReminderPreferences> = MutableStateFlow(value)

    override suspend fun getPreferences(): ReminderPreferences = value

    override suspend fun updateVaccinationSettings(enabled: Boolean, daysBefore: Int) = Unit

    override suspend fun updateDewormingSettings(enabled: Boolean, daysBefore: Int) = Unit

    override suspend fun updateWeightSettings(enabled: Boolean, intervalDays: Int) = Unit

    override suspend fun updateNotificationTime(hour: Int, minute: Int) = Unit

    override suspend fun reset() = Unit
  }

  private class FixedPetRepository : PetRepository {
    private val pet = Pet(id = "pet-1", name = "Mimi", createdAt = 1L, updatedAt = 1L)

    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(listOf(pet))

    override suspend fun getPetById(id: String): Pet? = pet.takeIf { it.id == id }

    override fun getPetByIdFlow(id: String): Flow<Pet?> =
      MutableStateFlow(pet.takeIf { it.id == id })

    override fun getPetCount(): Flow<Int> = MutableStateFlow(1)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class RecordingTaskScheduler : TaskScheduler {
    val scheduled = mutableListOf<Task>()
    val cancelled = mutableListOf<String>()

    override fun scheduleTask(task: Task) {
      scheduled += task
    }

    override fun cancelTask(taskId: String) {
      cancelled += taskId
    }

    override fun cancelAllTasks() = Unit
  }
}
