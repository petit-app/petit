package com.woliveiras.petit.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import com.woliveiras.petit.data.local.entity.FamilyGroupMemberEntity
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.SyncLogEntity
import com.woliveiras.petit.data.local.entity.TaskEntity
import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import com.woliveiras.petit.data.local.entity.WeightEntryEntity
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.FamilyGroupRepositoryImpl
import com.woliveiras.petit.data.repository.ReminderPreferences
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.ReminderPreferencesRepositoryImpl
import com.woliveiras.petit.data.repository.UserPreferencesRepositoryImpl
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.worker.TaskScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteAllDataUseCaseIntegrationTest {

  private lateinit var context: Context
  private lateinit var database: PetitDatabase
  private lateinit var scheduler: RecordingTaskScheduler
  private lateinit var reminderPreferences: ReminderPreferencesRepositoryImpl
  private lateinit var familyGroupRepository: FamilyGroupRepositoryImpl
  private lateinit var userPreferences: UserPreferencesRepositoryImpl
  private lateinit var familyGroupKey: String

  @Before
  fun setUp() = runTest {
    context = ApplicationProvider.getApplicationContext()
    context.filesDir.resolve("datastore").deleteRecursively()
    database = Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java).build()
    scheduler = RecordingTaskScheduler()
    reminderPreferences = ReminderPreferencesRepositoryImpl(context)
    familyGroupRepository =
      FamilyGroupRepositoryImpl(context, database.familyGroupMemberDao(), database.syncLogDao())
    userPreferences = UserPreferencesRepositoryImpl(context)
  }

  @After
  fun tearDown() {
    database.close()
    context.filesDir.resolve("datastore").deleteRecursively()
  }

  @Test
  fun deletesEveryRoomDomainResetsCarePreferencesAndRetainsDeviceExperience() = runTest {
    seedEveryStore()
    val useCase = useCase()

    val result = useCase.execute()

    assertThat(result.isSuccess).isTrue()
    assertThat(scheduler.cancelAllCalls).isEqualTo(1)
    assertStoresAreEmpty()
    assertThat(reminderPreferences.getPreferences()).isEqualTo(ReminderPreferences())
    assertThat(familyGroupRepository.getFamilyGroupKey()).isNull()
    assertThat(familyGroupRepository.isSyncEnabled.first()).isFalse()
    assertUserExperiencePreferencesRetained()
  }

  @Test
  fun repeatedDeletionAfterWorkerCancellationIsIdempotent() = runTest {
    seedEveryStore()
    val useCase = useCase()

    assertThat(useCase.execute().isSuccess).isTrue()
    assertThat(useCase.execute().isSuccess).isTrue()

    assertThat(scheduler.cancelAllCalls).isEqualTo(2)
    assertStoresAreEmpty()
  }

  @Test
  fun schedulerFailureLeavesDataIntactAndCanBeRetried() = runTest {
    seedEveryStore()
    scheduler.failCancellation = true

    val firstAttempt = useCase().execute()

    assertThat(firstAttempt.isFailure).isTrue()
    assertThat(database.petDao().getAllPets().first()).isNotEmpty()
    assertThat(familyGroupRepository.getFamilyGroupKey()).isEqualTo(familyGroupKey)

    scheduler.failCancellation = false
    val retry = useCase().execute()

    assertThat(retry.isSuccess).isTrue()
    assertThat(scheduler.cancelAllCalls).isEqualTo(2)
    assertStoresAreEmpty()
  }

  @Test
  fun roomFailureRollsBackEveryTableAndDoesNotReportSuccess() = runTest {
    seedEveryStore()
    database.openHelper.writableDatabase.execSQL(
      """
      CREATE TRIGGER abort_delete_all_data
      BEFORE DELETE ON sync_logs
      BEGIN
        SELECT RAISE(ABORT, 'forced Room failure');
      END
      """
        .trimIndent()
    )

    val result = useCase().execute()

    assertThat(result.isFailure).isTrue()
    assertThat(scheduler.cancelAllCalls).isEqualTo(1)
    assertThat(database.petDao().getAllPets().first()).isNotEmpty()
    assertThat(database.taskDao().getAllTasks().first()).isNotEmpty()
    assertThat(database.familyGroupMemberDao().getMemberCount(familyGroupKey).first()).isEqualTo(2)
    assertThat(database.syncLogDao().getAllSyncLogs().first()).isNotEmpty()
    assertThat(reminderPreferences.getPreferences().vaccinationRemindersEnabled).isFalse()
    assertThat(familyGroupRepository.getFamilyGroupKey()).isEqualTo(familyGroupKey)
  }

  @Test
  fun preferenceFailureIsRecoverableByRetryingTheIdempotentPurge() = runTest {
    seedEveryStore()
    val failingPreferences = ThrowingReminderPreferencesRepository()
    val firstAttempt =
      DeleteAllDataUseCase(database, scheduler, failingPreferences, familyGroupRepository).execute()

    assertThat(firstAttempt.isFailure).isTrue()
    assertStoresAreEmpty()
    assertThat(familyGroupRepository.getFamilyGroupKey()).isEqualTo(familyGroupKey)

    val retry = useCase().execute()

    assertThat(retry.isSuccess).isTrue()
    assertThat(scheduler.cancelAllCalls).isEqualTo(2)
    assertThat(familyGroupRepository.getFamilyGroupKey()).isNull()
  }

  @Test
  fun familyPreferenceFailureDoesNotReportSuccessAndRetryCompletesSafely() = runTest {
    seedEveryStore()
    val failingFamilyGroupRepository = FailingFamilyGroupRepository(familyGroupRepository)

    val firstAttempt =
      DeleteAllDataUseCase(database, scheduler, reminderPreferences, failingFamilyGroupRepository)
        .execute()

    assertThat(firstAttempt.isFailure).isTrue()
    assertStoresAreEmpty()
    assertThat(reminderPreferences.getPreferences()).isEqualTo(ReminderPreferences())
    assertThat(familyGroupRepository.getFamilyGroupKey()).isEqualTo(familyGroupKey)
    assertUserExperiencePreferencesRetained()

    val retry = useCase().execute()

    assertThat(retry.isSuccess).isTrue()
    assertThat(scheduler.cancelAllCalls).isEqualTo(2)
    assertThat(familyGroupRepository.getFamilyGroupKey()).isNull()
    assertUserExperiencePreferencesRetained()
  }

  private suspend fun seedEveryStore() {
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
    database
      .weightEntryDao()
      .insertWeightEntry(
        WeightEntryEntity(id = "weight-1", petId = "pet-1", date = 1L, weightGrams = 4200)
      )
    database
      .vaccinationEntryDao()
      .insertVaccinationEntry(
        VaccinationEntryEntity(
          id = "vaccination-1",
          petId = "pet-1",
          vaccineType = "RABIES",
          applicationDate = 1L,
        )
      )
    database
      .dewormingEntryDao()
      .insertDewormingEntry(
        DewormingEntryEntity(
          id = "deworming-1",
          petId = "pet-1",
          type = "INTERNAL",
          medication = "Medication",
          applicationDate = 1L,
        )
      )
    database
      .taskDao()
      .insertTask(
        TaskEntity(
          id = "task-1",
          petId = "pet-1",
          kind = "VACCINATION",
          title = "Vaccination",
          scheduledFor = 1L,
        )
      )
    familyGroupKey = familyGroupRepository.createFamilyGroup("This phone")
    database
      .familyGroupMemberDao()
      .insertMember(
        FamilyGroupMemberEntity(
          id = "remote-device",
          deviceName = "Other phone",
          familyGroupKey = familyGroupKey,
        )
      )
    database
      .syncLogDao()
      .insertSyncLog(
        SyncLogEntity(
          id = "sync-1",
          peerId = "remote-device",
          peerName = "Other phone",
          syncTimestamp = 1L,
          entitiesSent = 1,
          entitiesReceived = 1,
          conflictsResolved = 0,
          syncType = "MANUAL",
        )
      )
    reminderPreferences.updateVaccinationSettings(enabled = false, daysBefore = 3)
    reminderPreferences.updateDewormingSettings(enabled = false, daysBefore = 4)
    reminderPreferences.updateWeightSettings(enabled = true, intervalDays = 14)
    reminderPreferences.updateNotificationTime(hour = 15, minute = 45)
    familyGroupRepository.setSyncEnabled(true)
    userPreferences.updateTheme(AppTheme.DARK)
    userPreferences.updateLanguage(AppLanguage.PORTUGUESE_BR)
    userPreferences.setOnboardingCompleted()
  }

  private suspend fun assertStoresAreEmpty() {
    assertThat(database.petDao().getAllPets().first()).isEmpty()
    assertThat(database.weightEntryDao().getWeightEntriesForPet("pet-1").first()).isEmpty()
    assertThat(database.vaccinationEntryDao().getVaccinationEntriesForPet("pet-1").first())
      .isEmpty()
    assertThat(database.dewormingEntryDao().getDewormingEntriesForPet("pet-1").first()).isEmpty()
    assertThat(database.taskDao().getAllTasks().first()).isEmpty()
    assertThat(database.familyGroupMemberDao().getMemberCount(familyGroupKey).first()).isEqualTo(0)
    assertThat(database.syncLogDao().getAllSyncLogs().first()).isEmpty()
  }

  private suspend fun assertUserExperiencePreferencesRetained() {
    assertThat(userPreferences.userPreferences.first())
      .isEqualTo(
        com.woliveiras.petit.data.repository.UserPreferences(
          theme = AppTheme.DARK,
          language = AppLanguage.PORTUGUESE_BR,
          hasCompletedOnboarding = true,
        )
      )
  }

  private fun useCase() =
    DeleteAllDataUseCase(database, scheduler, reminderPreferences, familyGroupRepository)

  private class RecordingTaskScheduler : TaskScheduler {
    var cancelAllCalls = 0
    var failCancellation = false

    override fun scheduleTask(task: com.woliveiras.petit.domain.model.Task) = Unit

    override fun cancelTask(taskId: String) = Unit

    override fun cancelAllTasks() {
      cancelAllCalls += 1
      check(!failCancellation) { "WorkManager unavailable" }
    }
  }

  private class ThrowingReminderPreferencesRepository : ReminderPreferencesRepository {
    override val preferences: Flow<ReminderPreferences>
      get() = throw UnsupportedOperationException()

    override suspend fun getPreferences(): ReminderPreferences =
      throw UnsupportedOperationException()

    override suspend fun updateVaccinationSettings(enabled: Boolean, daysBefore: Int) = Unit

    override suspend fun updateDewormingSettings(enabled: Boolean, daysBefore: Int) = Unit

    override suspend fun updateWeightSettings(enabled: Boolean, intervalDays: Int) = Unit

    override suspend fun updateNotificationTime(hour: Int, minute: Int) = Unit

    override suspend fun reset() {
      throw IllegalStateException("DataStore unavailable")
    }
  }

  private class FailingFamilyGroupRepository(private val delegate: FamilyGroupRepository) :
    FamilyGroupRepository by delegate {

    override suspend fun resetLocalPreferences() {
      throw IllegalStateException("Family DataStore unavailable")
    }
  }
}
