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
    ],
  version = 2,
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
  }
}
