package com.woliveiras.petit.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.ReminderPreferences
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoTaskServiceImplTest {

  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:34:56Z"), ZoneOffset.UTC)

  @Test
  fun vaccinationUsesAdvanceNoticeAndPreferredNotificationTime() = runTest {
    val fixture = fixture(vaccinationDaysBefore = 5, hour = 8, minute = 45)

    fixture.service.handleVaccinationSaved(vaccination(nextDueDate = LocalDate.of(2026, 8, 1)))

    val task = fixture.tasks.saved.single()
    assertThat(task.kind).isEqualTo(TaskKind.VACCINATION)
    assertThat(task.referenceEntityId).isEqualTo("vacc-1")
    assertThat(task.scheduledFor).isEqualTo(LocalDateTime.of(2026, 7, 27, 8, 45))
    assertThat(task.status).isEqualTo(TaskStatus.PENDING)
    assertThat(task.createdAt).isEqualTo(clock.millis())
    assertThat(task.updatedAt).isEqualTo(clock.millis())
    assertThat(fixture.scheduler.scheduled.single().scheduledFor)
      .isEqualTo(LocalDateTime.of(2026, 7, 27, 8, 45))
  }

  @Test
  fun dewormingUsesItsOwnAdvanceNoticeAndPreferredNotificationTime() = runTest {
    val fixture = fixture(dewormingDaysBefore = 3, hour = 6, minute = 15)

    fixture.service.handleDewormingSaved(deworming(nextDueDate = LocalDate.of(2026, 7, 23)))

    val task = fixture.tasks.saved.single()
    assertThat(task.kind).isEqualTo(TaskKind.DEWORMING)
    assertThat(task.referenceEntityId).isEqualTo("deworm-1")
    assertThat(task.scheduledFor).isEqualTo(LocalDateTime.of(2026, 7, 20, 6, 15))
    assertThat(fixture.scheduler.scheduled.single()).isEqualTo(task)
  }

  @Test
  fun careDueTodayWithElapsedAdvanceNoticeIsScheduledImmediately() = runTest {
    val fixture = fixture(vaccinationDaysBefore = 7, hour = 9, minute = 0)

    fixture.service.handleVaccinationSaved(vaccination(nextDueDate = LocalDate.of(2026, 7, 17)))

    val expectedNow = LocalDateTime.ofInstant(clock.instant(), clock.zone)
    assertThat(fixture.tasks.saved.single().scheduledFor).isEqualTo(expectedNow)
    assertThat(fixture.scheduler.scheduled.single().scheduledFor).isEqualTo(expectedNow)
  }

  @Test
  fun overdueCareDoesNotCreateAnUpcomingTask() = runTest {
    val fixture = fixture()

    fixture.service.handleVaccinationSaved(vaccination(nextDueDate = LocalDate.of(2026, 7, 16)))
    fixture.service.handleDewormingSaved(deworming(nextDueDate = LocalDate.of(2026, 7, 16)))

    assertThat(fixture.tasks.saved).isEmpty()
    assertThat(fixture.scheduler.scheduled).isEmpty()
  }

  @Test
  fun disabledCarePreferencesCancelAnyPreviousScheduleWithoutCreatingATask() = runTest {
    val fixture = fixture(vaccinationEnabled = false, dewormingEnabled = false)

    fixture.service.handleVaccinationSaved(vaccination(nextDueDate = LocalDate.of(2026, 8, 1)))
    fixture.service.handleDewormingSaved(deworming(nextDueDate = LocalDate.of(2026, 8, 1)))

    assertThat(fixture.tasks.saved).isEmpty()
    assertThat(fixture.scheduler.scheduled).isEmpty()
    assertThat(fixture.scheduler.cancelled)
      .containsExactly("auto_vacc_vacc-1", "auto_deworm_deworm-1")
  }

  @Test
  fun savingLinkedCareReplacesPriorTaskAndUniqueWork() = runTest {
    val fixture = fixture()
    val entry = vaccination(nextDueDate = LocalDate.of(2026, 8, 1))

    fixture.service.handleVaccinationSaved(entry)
    fixture.service.handleVaccinationSaved(entry.copy(nextDueDate = LocalDate.of(2026, 8, 2)))

    assertThat(fixture.tasks.deletedReferences).containsExactly("vacc-1", "vacc-1")
    assertThat(fixture.scheduler.cancelled).containsExactly("auto_vacc_vacc-1", "auto_vacc_vacc-1")
    assertThat(fixture.scheduler.scheduled.map { it.id })
      .containsExactly("auto_vacc_vacc-1", "auto_vacc_vacc-1")
  }

  @Test
  fun deletingLinkedCareIsIdempotentAndCancelsItsWork() = runTest {
    val fixture = fixture()

    fixture.service.handleVaccinationDeleted("vacc-1")
    fixture.service.handleVaccinationDeleted("vacc-1")
    fixture.service.handleDewormingDeleted("deworm-1")
    fixture.service.handleDewormingDeleted("deworm-1")

    assertThat(fixture.tasks.deletedReferences)
      .containsExactly("vacc-1", "vacc-1", "deworm-1", "deworm-1")
    assertThat(fixture.scheduler.cancelled)
      .containsExactly(
        "auto_vacc_vacc-1",
        "auto_vacc_vacc-1",
        "auto_deworm_deworm-1",
        "auto_deworm_deworm-1",
      )
  }

  @Test
  fun enabledWeightReminderUsesConfiguredIntervalAndTime() = runTest {
    val fixture = fixture(weightEnabled = true, weightIntervalDays = 14, hour = 7, minute = 20)

    fixture.service.handleWeightSaved("pet-1", "Mimi")

    val task = fixture.tasks.saved.single()
    assertThat(task.kind).isEqualTo(TaskKind.WEIGHT)
    assertThat(task.scheduledFor).isEqualTo(LocalDateTime.of(2026, 7, 31, 7, 20))
    assertThat(fixture.scheduler.scheduled.single()).isEqualTo(task)
  }

  @Test
  fun disabledWeightReminderDoesNotReplaceOrScheduleATask() = runTest {
    val fixture = fixture(weightEnabled = false)

    fixture.service.handleWeightSaved("pet-1", "Mimi")

    assertThat(fixture.tasks.saved).isEmpty()
    assertThat(fixture.scheduler.cancelled).isEmpty()
    assertThat(fixture.scheduler.scheduled).isEmpty()
  }

  @Test
  fun eachAutomaticTaskUsesOnePreferencesSnapshot() = runTest {
    val fixture = fixture()

    fixture.service.handleVaccinationSaved(vaccination(nextDueDate = LocalDate.of(2026, 8, 1)))

    assertThat(fixture.preferences.readCount).isEqualTo(1)
  }

  private fun fixture(
    vaccinationDaysBefore: Int = 7,
    dewormingDaysBefore: Int = 7,
    vaccinationEnabled: Boolean = true,
    dewormingEnabled: Boolean = true,
    weightEnabled: Boolean = false,
    weightIntervalDays: Int = 30,
    hour: Int = 9,
    minute: Int = 0,
  ): Fixture {
    val taskRepository = RecordingTaskRepository()
    val scheduler = RecordingTaskScheduler()
    val preferences =
      ReminderPreferences(
        vaccinationRemindersEnabled = vaccinationEnabled,
        vaccinationDaysBefore = vaccinationDaysBefore,
        dewormingRemindersEnabled = dewormingEnabled,
        dewormingDaysBefore = dewormingDaysBefore,
        weightRemindersEnabled = weightEnabled,
        weightReminderIntervalDays = weightIntervalDays,
        defaultNotificationHour = hour,
        defaultNotificationMinute = minute,
      )
    val preferencesRepository = FixedReminderPreferencesRepository(preferences)
    return Fixture(
      tasks = taskRepository,
      scheduler = scheduler,
      preferences = preferencesRepository,
      service =
        AutoTaskServiceImpl(
          context = ApplicationProvider.getApplicationContext<Context>(),
          taskRepository = taskRepository,
          reminderPreferencesRepository = preferencesRepository,
          taskScheduler = scheduler,
          petRepository = FixedPetRepository(),
          clock = clock,
        ),
    )
  }

  private fun vaccination(nextDueDate: LocalDate?) =
    VaccinationEntry(
      id = "vacc-1",
      petId = "pet-1",
      vaccineType = VaccineType.RABIES,
      applicationDate = LocalDate.of(2026, 7, 1),
      nextDueDate = nextDueDate,
      createdAt = 1L,
      updatedAt = 1L,
    )

  private fun deworming(nextDueDate: LocalDate?) =
    DewormingEntry(
      id = "deworm-1",
      petId = "pet-1",
      type = DewormingType.INTERNAL,
      medication = "Milbemax",
      applicationDate = LocalDate.of(2026, 7, 1),
      nextDueDate = nextDueDate,
      createdAt = 1L,
      updatedAt = 1L,
    )

  private data class Fixture(
    val tasks: RecordingTaskRepository,
    val scheduler: RecordingTaskScheduler,
    val preferences: FixedReminderPreferencesRepository,
    val service: AutoTaskServiceImpl,
  )

  private class FixedReminderPreferencesRepository(private val value: ReminderPreferences) :
    ReminderPreferencesRepository {
    var readCount = 0

    override val preferences: Flow<ReminderPreferences> = MutableStateFlow(value)

    override suspend fun getPreferences(): ReminderPreferences {
      readCount += 1
      return value
    }

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

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(getPetByIdSync(id))

    override fun getPetCount(): Flow<Int> = MutableStateFlow(1)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit

    private fun getPetByIdSync(id: String): Pet? = pet.takeIf { it.id == id }
  }

  private class RecordingTaskRepository : TaskRepository {
    val saved = mutableListOf<Task>()
    val deletedReferences = mutableListOf<String>()

    override fun getPendingTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksForPet(petId: String): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getTaskById(id: String): Task? = null

    override fun getTasksDueToday(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueThisWeek(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueThisMonth(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override fun getTasksDueInRange(fromMillis: Long, toMillis: Long): Flow<List<Task>> =
      MutableStateFlow(emptyList())

    override fun getNextTasks(limit: Int): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun getPastDueTasks(): List<Task> = emptyList()

    override fun getCompletedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun saveTask(task: Task) {
      saved += task
    }

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) = Unit

    override suspend fun deleteTask(id: String) = Unit

    override suspend fun deleteTasksByReferenceEntity(entityId: String) {
      deletedReferences += entityId
    }
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
