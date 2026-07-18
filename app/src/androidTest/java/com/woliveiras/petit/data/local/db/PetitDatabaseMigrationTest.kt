package com.woliveiras.petit.data.local.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetitDatabaseMigrationTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.deleteDatabase(TEST_DATABASE)
  }

  @After
  fun tearDown() {
    context.deleteDatabase(TEST_DATABASE)
  }

  @Test
  fun migration1To2PreservesVersion1DataAndCreatesUsableMembershipChangesTable() {
    openDatabase(version = 1).use { helper ->
      helper.writableDatabase.execSQL(
        """
        INSERT INTO family_group_members (
          id, deviceName, familyGroupKey, isLocalDevice, lastSyncAt,
          createdAt, updatedAt, deletedAt, syncStatus
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
          .trimIndent(),
        arrayOf<Any?>(
          "member-v1",
          "Living room tablet",
          "GROUP-V1",
          1,
          321L,
          100L,
          200L,
          null,
          "SYNCED",
        ),
      )
    }

    openDatabase(version = 2).use { helper ->
      val database = helper.writableDatabase

      database
        .query(
          "SELECT deviceName, familyGroupKey, lastSyncAt, syncStatus " +
            "FROM family_group_members WHERE id = ?",
          arrayOf("member-v1"),
        )
        .use { cursor ->
          assertThat(cursor.moveToFirst()).isTrue()
          assertThat(cursor.getString(0)).isEqualTo("Living room tablet")
          assertThat(cursor.getString(1)).isEqualTo("GROUP-V1")
          assertThat(cursor.getLong(2)).isEqualTo(321L)
          assertThat(cursor.getString(3)).isEqualTo("SYNCED")
        }

      database.execSQL(
        """
        INSERT INTO membership_changes (
          groupId, memberId, type, deviceName, timestamp, deliveryKey
        ) VALUES (?, ?, ?, ?, ?, ?)
        """
          .trimIndent(),
        arrayOf<Any?>("group-hash", "member-v1", "LEAVE", null, 500L, "restricted-key"),
      )

      database
        .query(
          "SELECT type, timestamp, deliveryKey FROM membership_changes " +
            "WHERE groupId = ? AND memberId = ?",
          arrayOf("group-hash", "member-v1"),
        )
        .use { cursor ->
          assertThat(cursor.moveToFirst()).isTrue()
          assertThat(cursor.getString(0)).isEqualTo("LEAVE")
          assertThat(cursor.getLong(1)).isEqualTo(500L)
          assertThat(cursor.getString(2)).isEqualTo("restricted-key")
        }

      database.execSQL(
        "INSERT INTO lan_sync_peers (peerId, outboundCursor, updatedAt) VALUES (?, ?, ?)",
        arrayOf<Any?>("peer-v1", 42L, 43L),
      )
      database.execSQL(
        "INSERT INTO lan_applied_batches " +
          "(batchId, peerId, acknowledgedCursor, appliedAt) VALUES (?, ?, ?, ?)",
        arrayOf<Any?>("batch-v1", "peer-v1", 42L, 44L),
      )
      database.execSQL(
        "INSERT INTO lan_outbound_acks (peerId, batchId, cursor) VALUES (?, ?, ?)",
        arrayOf<Any?>("peer-v1", "outbound-v1", 42L),
      )
      database
        .query(
          "SELECT p.outboundCursor, b.acknowledgedCursor, a.cursor FROM lan_sync_peers p " +
            "JOIN lan_applied_batches b ON b.peerId = p.peerId " +
            "JOIN lan_outbound_acks a ON a.peerId = p.peerId WHERE p.peerId = ?",
          arrayOf("peer-v1"),
        )
        .use { cursor ->
          assertThat(cursor.moveToFirst()).isTrue()
          assertThat(cursor.getLong(0)).isEqualTo(42L)
          assertThat(cursor.getLong(1)).isEqualTo(42L)
          assertThat(cursor.getLong(2)).isEqualTo(42L)
        }
    }
  }

  private fun openDatabase(version: Int): SupportSQLiteOpenHelper =
    FrameworkSQLiteOpenHelperFactory()
      .create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
          .name(TEST_DATABASE)
          .callback(
            object : SupportSQLiteOpenHelper.Callback(version) {
              override fun onCreate(database: SupportSQLiteDatabase) {
                require(version == 1)
                database.execSQL(VERSION_1_FAMILY_GROUP_MEMBERS_SCHEMA)
              }

              override fun onUpgrade(
                database: SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
              ) {
                assertThat(oldVersion).isEqualTo(1)
                assertThat(newVersion).isEqualTo(2)
                PetitDatabase.MIGRATION_1_2.migrate(database)
              }
            }
          )
          .build()
      )

  companion object {
    private const val TEST_DATABASE = "petit-migration-1-2-test"

    private val VERSION_1_FAMILY_GROUP_MEMBERS_SCHEMA =
      """
      CREATE TABLE IF NOT EXISTS family_group_members (
        id TEXT NOT NULL,
        deviceName TEXT NOT NULL,
        familyGroupKey TEXT NOT NULL,
        isLocalDevice INTEGER NOT NULL,
        lastSyncAt INTEGER,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL,
        deletedAt INTEGER,
        syncStatus TEXT NOT NULL,
        PRIMARY KEY(id)
      )
      """
        .trimIndent()
  }
}
