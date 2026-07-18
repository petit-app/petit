package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.woliveiras.petit.data.local.entity.FamilyGroupMemberEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for FamilyGroupMember entities. */
@Dao
interface FamilyGroupMemberDao {

  /** Get all active members of a family group. */
  @Query(
    "SELECT * FROM family_group_members WHERE familyGroupKey = :familyGroupKey AND deletedAt IS NULL ORDER BY createdAt ASC"
  )
  fun getMembersByGroupKey(familyGroupKey: String): Flow<List<FamilyGroupMemberEntity>>

  /** Get the local device member. */
  @Query("SELECT * FROM family_group_members WHERE isLocalDevice = 1 AND deletedAt IS NULL LIMIT 1")
  suspend fun getLocalDevice(): FamilyGroupMemberEntity?

  /** Get the local device member as Flow. */
  @Query("SELECT * FROM family_group_members WHERE isLocalDevice = 1 AND deletedAt IS NULL LIMIT 1")
  fun getLocalDeviceFlow(): Flow<FamilyGroupMemberEntity?>

  /** Get a member by ID. */
  @Query("SELECT * FROM family_group_members WHERE id = :id AND deletedAt IS NULL")
  suspend fun getMemberById(id: String): FamilyGroupMemberEntity?

  /** Get a member including a revocation tombstone. */
  @Query("SELECT * FROM family_group_members WHERE id = :id")
  suspend fun getMemberByIdIncludingDeleted(id: String): FamilyGroupMemberEntity?

  /** Get revoked stable identities for transport authorization. */
  @Query(
    "SELECT id FROM family_group_members WHERE familyGroupKey = :familyGroupKey AND deletedAt IS NOT NULL"
  )
  suspend fun getRevokedMemberIds(familyGroupKey: String): List<String>

  @Query("SELECT * FROM family_group_members WHERE deletedAt IS NOT NULL")
  suspend fun getAllRevokedMembers(): List<FamilyGroupMemberEntity>

  @Query("SELECT * FROM family_group_members WHERE familyGroupKey = :familyGroupKey ORDER BY id")
  suspend fun getAllByGroupKeyIncludingDeleted(
    familyGroupKey: String
  ): List<FamilyGroupMemberEntity>

  /** Get the count of active members in a group. */
  @Query(
    "SELECT COUNT(*) FROM family_group_members WHERE familyGroupKey = :familyGroupKey AND deletedAt IS NULL"
  )
  fun getMemberCount(familyGroupKey: String): Flow<Int>

  /** Insert a new member. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMember(member: FamilyGroupMemberEntity)

  @Transaction
  suspend fun insertPairingMembers(members: List<FamilyGroupMemberEntity>) {
    members.forEach { insertMember(it) }
  }

  /** Update an existing member. */
  @Update suspend fun updateMember(member: FamilyGroupMemberEntity)

  /** Update the last sync timestamp for a member. */
  @Query(
    "UPDATE family_group_members SET lastSyncAt = :timestamp WHERE id = :id AND deletedAt IS NULL"
  )
  suspend fun updateLastSyncAt(id: String, timestamp: Long = System.currentTimeMillis())

  /** Soft delete a member by setting deletedAt timestamp. */
  @Query(
    "UPDATE family_group_members SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id"
  )
  suspend fun softDeleteMember(id: String, timestamp: Long = System.currentTimeMillis())

  /** Hard delete a member by ID (for data cleanup). */
  @Query("DELETE FROM family_group_members WHERE id = :id") suspend fun deleteMember(id: String)

  /** Delete all members of a group. */
  @Query("DELETE FROM family_group_members WHERE familyGroupKey = :familyGroupKey")
  suspend fun deleteAllByGroupKey(familyGroupKey: String)

  /**
   * Retain stable peer identities after leaving so a membership-only retry can be authenticated.
   */
  @Query(
    "UPDATE family_group_members SET deletedAt = :timestamp, updatedAt = :timestamp " +
      "WHERE familyGroupKey = :familyGroupKey AND deletedAt IS NULL"
  )
  suspend fun softDeleteAllByGroupKey(familyGroupKey: String, timestamp: Long)

  /** Delete all members. */
  @Query("DELETE FROM family_group_members") suspend fun deleteAll()
}
