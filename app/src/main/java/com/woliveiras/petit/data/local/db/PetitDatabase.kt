package com.woliveiras.petit.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.woliveiras.petit.data.local.dao.DewormingEntryDao
import com.woliveiras.petit.data.local.dao.FamilyGroupMemberDao
import com.woliveiras.petit.data.local.dao.LanSyncDao
import com.woliveiras.petit.data.local.dao.MembershipChangeDao
import com.woliveiras.petit.data.local.dao.PetDao
import com.woliveiras.petit.data.local.dao.RestorableRevisionDao
import com.woliveiras.petit.data.local.dao.SyncLogDao
import com.woliveiras.petit.data.local.dao.TaskDao
import com.woliveiras.petit.data.local.dao.TimelineDao
import com.woliveiras.petit.data.local.dao.VaccinationEntryDao
import com.woliveiras.petit.data.local.dao.WeightEntryDao
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import com.woliveiras.petit.data.local.entity.FamilyGroupMemberEntity
import com.woliveiras.petit.data.local.entity.LanAppliedBatchEntity
import com.woliveiras.petit.data.local.entity.LanOutboundAckEntity
import com.woliveiras.petit.data.local.entity.LanSeenNonceEntity
import com.woliveiras.petit.data.local.entity.LanSyncPeerEntity
import com.woliveiras.petit.data.local.entity.MembershipChangeEntity
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.RestorableRevisionEntity
import com.woliveiras.petit.data.local.entity.SyncLogEntity
import com.woliveiras.petit.data.local.entity.TaskEntity
import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import com.woliveiras.petit.data.local.entity.WeightEntryEntity

/** Main Room database for Petit app. */
@Database(
  entities =
    [
      PetEntity::class,
      WeightEntryEntity::class,
      VaccinationEntryEntity::class,
      DewormingEntryEntity::class,
      TaskEntity::class,
      FamilyGroupMemberEntity::class,
      SyncLogEntity::class,
      MembershipChangeEntity::class,
      LanSyncPeerEntity::class,
      LanAppliedBatchEntity::class,
      LanOutboundAckEntity::class,
      LanSeenNonceEntity::class,
      RestorableRevisionEntity::class,
    ],
  version = 6,
  exportSchema = true,
)
abstract class PetitDatabase : RoomDatabase() {

  abstract fun petDao(): PetDao

  abstract fun weightEntryDao(): WeightEntryDao

  abstract fun vaccinationEntryDao(): VaccinationEntryDao

  abstract fun dewormingEntryDao(): DewormingEntryDao

  abstract fun taskDao(): TaskDao

  abstract fun timelineDao(): TimelineDao

  abstract fun familyGroupMemberDao(): FamilyGroupMemberDao

  abstract fun syncLogDao(): SyncLogDao

  abstract fun membershipChangeDao(): MembershipChangeDao

  abstract fun lanSyncDao(): LanSyncDao

  abstract fun restorableRevisionDao(): RestorableRevisionDao

  companion object {
    const val DATABASE_NAME = "petit_database"

    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS membership_changes (
              groupId TEXT NOT NULL,
              memberId TEXT NOT NULL,
              type TEXT NOT NULL,
              deviceName TEXT,
              timestamp INTEGER NOT NULL,
              deliveryKey TEXT,
              PRIMARY KEY(groupId, memberId)
            )
            """
              .trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lan_outbound_acks (
              peerId TEXT NOT NULL,
              batchId TEXT NOT NULL,
              cursor INTEGER NOT NULL,
              PRIMARY KEY(peerId, batchId)
            )
            """
              .trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lan_seen_nonces (
              nonceKey TEXT NOT NULL,
              seenAt INTEGER NOT NULL,
              PRIMARY KEY(nonceKey)
            )
            """
              .trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lan_sync_peers (
              peerId TEXT NOT NULL,
              outboundCursor INTEGER NOT NULL,
              updatedAt INTEGER NOT NULL,
              PRIMARY KEY(peerId)
            )
            """
              .trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lan_applied_batches (
              batchId TEXT NOT NULL,
              peerId TEXT NOT NULL,
              acknowledgedCursor INTEGER NOT NULL,
              appliedAt INTEGER NOT NULL,
              PRIMARY KEY(batchId, peerId)
            )
            """
              .trimIndent()
          )
        }
      }

    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS restorable_revision (
              id INTEGER NOT NULL,
              currentRevision INTEGER NOT NULL,
              completedRevision INTEGER NOT NULL,
              PRIMARY KEY(id)
            )
            """
              .trimIndent()
          )
          installRestorableRevisionTriggers(db)
        }
      }

    val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE pets ADD COLUMN breedId TEXT")
          db.execSQL(
            """
            UPDATE pets
            SET breedId = CASE
              WHEN breed = 'MIXED_BREED' AND petType IN ('CAT', 'DOG')
                THEN 'PETIT:MIXED_BREED'
              WHEN petType = 'CAT' THEN CASE breed
                WHEN 'PERSIAN' THEN 'VBO:0100188'
                WHEN 'SIAMESE' THEN 'VBO:0100221'
                WHEN 'MAINE_COON' THEN 'VBO:0100154'
                WHEN 'RAGDOLL' THEN 'VBO:0100196'
                WHEN 'BRITISH_SHORTHAIR' THEN 'VBO:0100052'
                WHEN 'BENGAL' THEN 'VBO:0100040'
                WHEN 'ABYSSINIAN' THEN 'VBO:0100000'
                WHEN 'SPHYNX' THEN 'VBO:0100230'
                WHEN 'SCOTTISH_FOLD' THEN 'VBO:0100209'
                WHEN 'BURMESE' THEN 'VBO:0100053'
                WHEN 'RUSSIAN_BLUE' THEN 'VBO:0100200'
                WHEN 'NORWEGIAN_FOREST' THEN 'VBO:0100178'
                WHEN 'TURKISH_ANGORA' THEN 'VBO:0100249'
              END
              WHEN petType = 'DOG' THEN CASE breed
                WHEN 'LABRADOR' THEN 'VBO:0200800'
                WHEN 'GOLDEN_RETRIEVER' THEN 'VBO:0200610'
                WHEN 'GERMAN_SHEPHERD' THEN 'VBO:0200577'
                WHEN 'POODLE' THEN 'VBO:0201048'
                WHEN 'BULLDOG' THEN 'VBO:0200258'
                WHEN 'BEAGLE' THEN 'VBO:0200131'
                WHEN 'SHIH_TZU' THEN 'VBO:0201223'
                WHEN 'YORKSHIRE' THEN 'VBO:0201448'
              END
            END
            WHERE breedId IS NULL
            """
              .trimIndent()
          )
        }
      }

    val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE tasks ADD COLUMN subjectCode TEXT")
          db.execSQL("ALTER TABLE tasks ADD COLUMN subjectName TEXT")
        }
      }

    val MIGRATION_5_6 =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE tasks ADD COLUMN repeatRule TEXT")
          db.execSQL("ALTER TABLE tasks ADD COLUMN seriesId TEXT")
          db.execSQL("ALTER TABLE tasks ADD COLUMN occurrenceIndex INTEGER NOT NULL DEFAULT 0")
        }
      }

    fun installRestorableRevisionTriggers(db: SupportSQLiteDatabase) {
      db.execSQL(
        """
        INSERT OR IGNORE INTO restorable_revision(id, currentRevision, completedRevision)
        VALUES (0, 0, 0)
        """
          .trimIndent()
      )
      val restorableTables =
        listOf("pets", "weight_entries", "vaccination_entries", "deworming_entries", "tasks")
      val operations = listOf("INSERT" to "insert", "UPDATE" to "update", "DELETE" to "delete")
      restorableTables.forEach { table ->
        operations.forEach { (operation, suffix) ->
          db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS restorable_revision_${table}_$suffix
            AFTER $operation ON $table
            BEGIN
              UPDATE restorable_revision
              SET currentRevision = currentRevision + 1
              WHERE id = 0;
            END
            """
              .trimIndent()
          )
        }
      }
    }
  }
}
