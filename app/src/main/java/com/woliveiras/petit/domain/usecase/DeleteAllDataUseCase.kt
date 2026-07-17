package com.woliveiras.petit.domain.usecase

import androidx.room.withTransaction
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.worker.TaskScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for deleting all user data from the database. This is used for the "Delete all my data"
 * feature in settings.
 */
interface DeleteAllDataAction {
  suspend fun execute(): Result<Unit>
}

@Singleton
class DeleteAllDataUseCase
@Inject
constructor(
  private val database: PetitDatabase,
  private val taskScheduler: TaskScheduler,
  private val reminderPreferencesRepository: ReminderPreferencesRepository,
  private val familyGroupRepository: FamilyGroupRepository,
) : DeleteAllDataAction {

  override suspend fun execute(): Result<Unit> =
    withContext(Dispatchers.IO) {
      try {
        taskScheduler.cancelAllTasks()
        database.withTransaction {
          // Delete in order: first dependent data, then pets
          database.taskDao().deleteAll()
          database.dewormingEntryDao().deleteAll()
          database.vaccinationEntryDao().deleteAll()
          database.weightEntryDao().deleteAll()
          database.petDao().deleteAll()
          database.familyGroupMemberDao().deleteAll()
          database.syncLogDao().deleteAll()
        }
        reminderPreferencesRepository.reset()
        familyGroupRepository.resetLocalPreferences()
        Result.success(Unit)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
}
