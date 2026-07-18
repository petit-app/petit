package com.woliveiras.petit.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.SyncLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestorableRevisionRoomTest {
  private lateinit var database: PetitDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .addCallback(
          object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
              PetitDatabase.installRestorableRevisionTriggers(db)
            }
          }
        )
        .allowMainThreadQueries()
        .build()
    database.openHelper.writableDatabase
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun restorableRoomMutationsAdvanceAtomicallyWhileBookkeepingDoesNot() = runBlocking {
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Milo"))
    assertThat(database.restorableRevisionDao().read().currentRevision).isEqualTo(1)

    database.petDao().softDeletePet("pet-1", timestamp = 2)
    assertThat(database.restorableRevisionDao().read().currentRevision).isEqualTo(2)

    database
      .syncLogDao()
      .insertSyncLog(
        SyncLogEntity(
          id = "sync-1",
          peerId = "peer-1",
          peerName = "Other device",
          syncTimestamp = 1,
          entitiesSent = 0,
          entitiesReceived = 0,
          conflictsResolved = 0,
          syncType = "MERGE",
        )
      )
    assertThat(database.restorableRevisionDao().read().currentRevision).isEqualTo(2)
  }
}
