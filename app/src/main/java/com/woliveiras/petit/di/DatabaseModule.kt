package com.woliveiras.petit.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.woliveiras.petit.data.local.dao.DewormingEntryDao
import com.woliveiras.petit.data.local.dao.FamilyGroupMemberDao
import com.woliveiras.petit.data.local.dao.PetDao
import com.woliveiras.petit.data.local.dao.RestorableRevisionDao
import com.woliveiras.petit.data.local.dao.SyncLogDao
import com.woliveiras.petit.data.local.dao.TaskDao
import com.woliveiras.petit.data.local.dao.TimelineDao
import com.woliveiras.petit.data.local.dao.VaccinationEntryDao
import com.woliveiras.petit.data.local.dao.WeightEntryDao
import com.woliveiras.petit.data.local.db.PetitDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that provides database and DAO instances. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun providePetitDatabase(@ApplicationContext context: Context): PetitDatabase {
    return Room.databaseBuilder(context, PetitDatabase::class.java, PetitDatabase.DATABASE_NAME)
      .addMigrations(PetitDatabase.MIGRATION_1_2, PetitDatabase.MIGRATION_2_3)
      .addCallback(
        object : RoomDatabase.Callback() {
          override fun onOpen(db: SupportSQLiteDatabase) {
            PetitDatabase.installRestorableRevisionTriggers(db)
          }
        }
      )
      .build()
  }

  @Provides
  @Singleton
  fun providePetDao(database: PetitDatabase): PetDao {
    return database.petDao()
  }

  @Provides
  @Singleton
  fun provideWeightEntryDao(database: PetitDatabase): WeightEntryDao {
    return database.weightEntryDao()
  }

  @Provides
  @Singleton
  fun provideVaccinationEntryDao(database: PetitDatabase): VaccinationEntryDao {
    return database.vaccinationEntryDao()
  }

  @Provides
  @Singleton
  fun provideDewormingEntryDao(database: PetitDatabase): DewormingEntryDao {
    return database.dewormingEntryDao()
  }

  @Provides
  @Singleton
  fun provideTaskDao(database: PetitDatabase): TaskDao {
    return database.taskDao()
  }

  @Provides
  @Singleton
  fun provideTimelineDao(database: PetitDatabase): TimelineDao {
    return database.timelineDao()
  }

  @Provides
  @Singleton
  fun provideFamilyGroupMemberDao(database: PetitDatabase): FamilyGroupMemberDao {
    return database.familyGroupMemberDao()
  }

  @Provides
  @Singleton
  fun provideSyncLogDao(database: PetitDatabase): SyncLogDao {
    return database.syncLogDao()
  }

  @Provides
  @Singleton
  fun provideRestorableRevisionDao(database: PetitDatabase): RestorableRevisionDao =
    database.restorableRevisionDao()
}
