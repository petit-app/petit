package com.woliveiras.petit.di

import android.content.Context
import androidx.work.WorkManager
import com.woliveiras.petit.BuildConfig
import com.woliveiras.petit.data.backup.archive.AndroidBackupPetAssetSource
import com.woliveiras.petit.data.backup.archive.BackupPetAssetSource
import com.woliveiras.petit.data.backup.archive.PortableBackupArchivePreparer
import com.woliveiras.petit.data.backup.google.GoogleAuthorizationResolutionBridge
import com.woliveiras.petit.data.backup.google.GoogleDriveAccessTokenProvider
import com.woliveiras.petit.data.backup.google.GoogleDriveAuthorizationGateway
import com.woliveiras.petit.data.backup.google.GoogleDriveBackupStorageGateway
import com.woliveiras.petit.data.backup.google.GoogleDriveConnectionMarker
import com.woliveiras.petit.data.backup.google.GoogleIdentityAuthorizationClient
import com.woliveiras.petit.data.backup.google.PlayServicesGoogleIdentityAuthorizationClient
import com.woliveiras.petit.data.backup.google.SharedPreferencesGoogleDriveConnectionMarker
import com.woliveiras.petit.data.backup.restore.AndroidRestoreAssetInstaller
import com.woliveiras.petit.data.backup.restore.FileRestoreRecoveryJournal
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import com.woliveiras.petit.domain.backup.archive.BackupArchiveCodec
import com.woliveiras.petit.domain.backup.restore.DownloadAndValidateBackupUseCase
import com.woliveiras.petit.domain.backup.restore.RestoreAssetInstaller
import com.woliveiras.petit.domain.backup.restore.RestoreBackupAction
import com.woliveiras.petit.domain.backup.restore.RestoreBackupUseCase
import com.woliveiras.petit.domain.backup.restore.RestoreRecoveryJournal
import com.woliveiras.petit.domain.backup.restore.RestoreReminderScheduler
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import com.woliveiras.petit.domain.usecase.backup.BackupArchivePreparer
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import com.woliveiras.petit.domain.usecase.backup.CreateManualBackupUseCase
import com.woliveiras.petit.domain.usecase.backup.ManageBackupsUseCase
import com.woliveiras.petit.presentation.feature.backup.ActivityGoogleAuthorizationResolutionBridge
import com.woliveiras.petit.worker.ChangeTriggeredBackupScheduler
import com.woliveiras.petit.worker.ChangeTriggeredBackupWorker
import com.woliveiras.petit.worker.RestoreReminderSchedulerImpl
import com.woliveiras.petit.worker.WorkManagerChangeTriggeredBackupScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Provider-independent archive dependencies shared by manual and background backup flows. */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
  @Provides
  @Singleton
  fun provideGoogleIdentityAuthorizationClient(
    @ApplicationContext context: Context
  ): PlayServicesGoogleIdentityAuthorizationClient =
    PlayServicesGoogleIdentityAuthorizationClient(context)

  @Provides
  @Singleton
  fun provideGoogleIdentityAuthorizationClientInterface(
    client: PlayServicesGoogleIdentityAuthorizationClient
  ): GoogleIdentityAuthorizationClient = client

  @Provides
  @Singleton
  fun provideGoogleDriveConnectionMarker(
    @ApplicationContext context: Context
  ): GoogleDriveConnectionMarker = SharedPreferencesGoogleDriveConnectionMarker(context)

  @Provides
  @Singleton
  fun provideGoogleAuthorizationResolutionBridge(
    bridge: ActivityGoogleAuthorizationResolutionBridge
  ): GoogleAuthorizationResolutionBridge = bridge

  @Provides
  @Singleton
  fun provideGoogleDriveAuthorizationGateway(
    client: GoogleIdentityAuthorizationClient,
    resolutionBridge: GoogleAuthorizationResolutionBridge,
    marker: GoogleDriveConnectionMarker,
  ): GoogleDriveAuthorizationGateway =
    GoogleDriveAuthorizationGateway(client, resolutionBridge, marker)

  @Provides
  @Singleton
  fun provideBackupAuthorizationGateway(
    gateway: GoogleDriveAuthorizationGateway
  ): BackupAuthorizationGateway = gateway

  @Provides
  @Singleton
  fun provideGoogleDriveAccessTokenProvider(
    gateway: GoogleDriveAuthorizationGateway
  ): GoogleDriveAccessTokenProvider = gateway

  @Provides
  @Singleton
  fun provideBackupStorageGateway(
    tokenProvider: GoogleDriveAccessTokenProvider
  ): BackupStorageGateway = GoogleDriveBackupStorageGateway(tokenProvider)

  @Provides
  @Singleton
  fun provideCreateBackupAction(
    authorizationGateway: BackupAuthorizationGateway,
    storageGateway: BackupStorageGateway,
    archivePreparer: BackupArchivePreparer,
  ): CreateBackupAction =
    CreateManualBackupUseCase(authorizationGateway, storageGateway, archivePreparer)

  @Provides
  @Singleton
  fun provideChangeTriggeredBackupScheduler(
    @ApplicationContext context: Context
  ): ChangeTriggeredBackupScheduler =
    WorkManagerChangeTriggeredBackupScheduler(
      WorkManager.getInstance(context),
      ChangeTriggeredBackupWorker::class.java,
    )

  @Provides @Singleton fun provideBackupArchiveCodec(): BackupArchiveCodec = BackupArchiveCodec()

  @Provides
  @Singleton
  fun provideBackupPetAssetSource(source: AndroidBackupPetAssetSource): BackupPetAssetSource =
    source

  @Provides
  @Singleton
  fun provideBackupArchivePreparer(
    @ApplicationContext context: Context,
    exportImportUseCase: ExportImportUseCase,
    userPreferencesRepository: UserPreferencesRepository,
    reminderPreferencesRepository: ReminderPreferencesRepository,
    assetSource: BackupPetAssetSource,
    codec: BackupArchiveCodec,
    clock: Clock,
  ): BackupArchivePreparer =
    PortableBackupArchivePreparer(
      exportImportUseCase = exportImportUseCase,
      userPreferencesRepository = userPreferencesRepository,
      reminderPreferencesRepository = reminderPreferencesRepository,
      assetSource = assetSource,
      codec = codec,
      stagingRoot = context.cacheDir.resolve("backup_staging"),
      appVersion = BuildConfig.VERSION_NAME,
      clock = clock,
    )

  @Provides
  @Singleton
  fun provideManageBackupsUseCase(
    storageGateway: BackupStorageGateway,
    codec: BackupArchiveCodec,
  ): ManageBackupsUseCase =
    ManageBackupsUseCase(
      storageGateway = storageGateway,
      supportedArchiveFormatVersion = codec.supportedArchiveFormatVersion,
      supportedSchemaVersion = codec.supportedDataSchemaVersion,
    )

  @Provides
  @Singleton
  fun provideRestoreAssetInstaller(installer: AndroidRestoreAssetInstaller): RestoreAssetInstaller =
    installer

  @Provides
  @Singleton
  fun provideRestoreRecoveryJournal(@ApplicationContext context: Context): RestoreRecoveryJournal =
    FileRestoreRecoveryJournal(context.filesDir.resolve("backup_restore"))

  @Provides
  @Singleton
  fun provideRestoreReminderScheduler(
    scheduler: RestoreReminderSchedulerImpl
  ): RestoreReminderScheduler = scheduler

  @Provides
  @Singleton
  fun provideDownloadAndValidateBackupUseCase(
    @ApplicationContext context: Context,
    storageGateway: BackupStorageGateway,
    codec: BackupArchiveCodec,
  ): DownloadAndValidateBackupUseCase =
    DownloadAndValidateBackupUseCase(
      storageGateway,
      codec,
      context.cacheDir.resolve("restore_downloads"),
    )

  @Provides
  @Singleton
  fun provideRestoreBackupUseCase(
    downloader: DownloadAndValidateBackupUseCase,
    exportImportUseCase: ExportImportUseCase,
    assetInstaller: RestoreAssetInstaller,
    userPreferencesRepository: UserPreferencesRepository,
    reminderPreferencesRepository: ReminderPreferencesRepository,
    reminderScheduler: RestoreReminderScheduler,
    recoveryJournal: RestoreRecoveryJournal,
  ): RestoreBackupUseCase =
    RestoreBackupUseCase(
      downloader,
      exportImportUseCase,
      assetInstaller,
      userPreferencesRepository,
      reminderPreferencesRepository,
      reminderScheduler,
      recoveryJournal,
    )

  @Provides
  fun provideRestoreBackupAction(useCase: RestoreBackupUseCase): RestoreBackupAction = useCase
}
