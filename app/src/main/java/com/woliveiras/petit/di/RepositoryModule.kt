package com.woliveiras.petit.di

import com.woliveiras.petit.data.backup.ProviderUnavailableBackupAuthorizationGateway
import com.woliveiras.petit.data.backup.ProviderUnavailableBackupStorageGateway
import com.woliveiras.petit.data.backup.revision.RoomRevisionStateStore
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptRepositoryImpl
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.data.repository.BackupSettingsRepositoryImpl
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.ReminderPreferencesRepositoryImpl
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.TimelineRepository
import com.woliveiras.petit.data.repository.TimelineRepositoryImpl
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepositoryImpl
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionRepository
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionStore
import com.woliveiras.petit.domain.backup.revision.TransactionalRestorableRevisionRepository
import com.woliveiras.petit.domain.usecase.DeleteAllDataAction
import com.woliveiras.petit.domain.usecase.DeleteAllDataUseCase
import com.woliveiras.petit.domain.usecase.GetPetHealthSummaryAction
import com.woliveiras.petit.domain.usecase.GetPetHealthSummaryUseCase
import com.woliveiras.petit.domain.usecase.backup.BackupRevisionCompletion
import com.woliveiras.petit.domain.usecase.backup.BackupTriggerCoordinator
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import com.woliveiras.petit.domain.usecase.backup.ProviderUnavailableCreateBackupAction
import com.woliveiras.petit.worker.AutoTaskService
import com.woliveiras.petit.worker.AutoTaskServiceImpl
import com.woliveiras.petit.worker.BackupScheduler
import com.woliveiras.petit.worker.TaskScheduler
import com.woliveiras.petit.worker.TaskSchedulerImpl
import com.woliveiras.petit.worker.WorkManagerBackupScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that binds repository interfaces to implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

  @Binds
  @Singleton
  abstract fun bindBackupRevisionCompletion(
    implementation: BackupTriggerCoordinator
  ): BackupRevisionCompletion

  @Binds
  @Singleton
  abstract fun bindRestorableRevisionStore(
    implementation: RoomRevisionStateStore
  ): RestorableRevisionStore

  @Binds
  @Singleton
  abstract fun bindRestorableRevisionRepository(
    implementation: TransactionalRestorableRevisionRepository
  ): RestorableRevisionRepository

  @Binds
  @Singleton
  abstract fun bindBackupAuthorizationGateway(
    providerUnavailableBackupAuthorizationGateway: ProviderUnavailableBackupAuthorizationGateway
  ): BackupAuthorizationGateway

  @Binds
  @Singleton
  abstract fun bindBackupStorageGateway(
    providerUnavailableBackupStorageGateway: ProviderUnavailableBackupStorageGateway
  ): BackupStorageGateway

  @Binds
  @Singleton
  abstract fun bindBackupAttemptRepository(
    backupAttemptRepositoryImpl: BackupAttemptRepositoryImpl
  ): BackupAttemptRepository

  @Binds
  @Singleton
  abstract fun bindBackupSettingsRepository(
    backupSettingsRepositoryImpl: BackupSettingsRepositoryImpl
  ): BackupSettingsRepository

  @Binds
  @Singleton
  abstract fun bindBackupScheduler(
    workManagerBackupScheduler: WorkManagerBackupScheduler
  ): BackupScheduler

  /** This session intentionally has no production cloud provider adapter. */
  @Binds
  @Singleton
  abstract fun bindCreateBackupAction(
    providerUnavailableCreateBackupAction: ProviderUnavailableCreateBackupAction
  ): CreateBackupAction

  @Binds
  @Singleton
  abstract fun bindPetRepository(petRepositoryImpl: PetRepositoryImpl): PetRepository

  @Binds
  @Singleton
  abstract fun bindWeightEntryRepository(
    weightEntryRepositoryImpl: WeightEntryRepositoryImpl
  ): WeightEntryRepository

  @Binds
  @Singleton
  abstract fun bindVaccinationEntryRepository(
    vaccinationEntryRepositoryImpl: VaccinationEntryRepositoryImpl
  ): VaccinationEntryRepository

  @Binds
  @Singleton
  abstract fun bindDewormingEntryRepository(
    dewormingEntryRepositoryImpl: DewormingEntryRepositoryImpl
  ): DewormingEntryRepository

  @Binds
  @Singleton
  abstract fun bindTaskRepository(taskRepositoryImpl: TaskRepositoryImpl): TaskRepository

  @Binds
  @Singleton
  abstract fun bindTimelineRepository(
    timelineRepositoryImpl: TimelineRepositoryImpl
  ): TimelineRepository

  @Binds
  @Singleton
  abstract fun bindTaskScheduler(taskSchedulerImpl: TaskSchedulerImpl): TaskScheduler

  @Binds
  @Singleton
  abstract fun bindAutoTaskService(autoTaskServiceImpl: AutoTaskServiceImpl): AutoTaskService

  @Binds
  @Singleton
  abstract fun bindDeleteAllDataAction(
    deleteAllDataUseCase: DeleteAllDataUseCase
  ): DeleteAllDataAction

  @Binds
  @Singleton
  abstract fun bindGetPetHealthSummaryAction(
    getPetHealthSummaryUseCase: GetPetHealthSummaryUseCase
  ): GetPetHealthSummaryAction

  @Binds
  @Singleton
  abstract fun bindUserPreferencesRepository(
    userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
  ): UserPreferencesRepository

  @Binds
  @Singleton
  abstract fun bindReminderPreferencesRepository(
    reminderPreferencesRepositoryImpl: ReminderPreferencesRepositoryImpl
  ): ReminderPreferencesRepository
}
