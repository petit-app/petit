package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.woliveiras.petit.data.local.entity.MembershipChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipChangeDao {
  @Query("SELECT * FROM membership_changes ORDER BY groupId, memberId")
  suspend fun getAll(): List<MembershipChangeEntity>

  @Query("SELECT * FROM membership_changes WHERE groupId = :groupId AND memberId = :memberId")
  suspend fun get(groupId: String, memberId: String): MembershipChangeEntity?

  @Query("SELECT * FROM membership_changes WHERE type = 'LEAVE' AND deliveryKey IS NOT NULL")
  suspend fun getPendingDepartures(): List<MembershipChangeEntity>

  @Query("SELECT COUNT(*) FROM membership_changes WHERE type = 'LEAVE' AND deliveryKey IS NOT NULL")
  fun observePendingDepartureCount(): Flow<Int>

  @Upsert suspend fun upsert(change: MembershipChangeEntity)

  @Query(
    "UPDATE membership_changes SET deliveryKey = NULL WHERE groupId = :groupId AND memberId = :memberId"
  )
  suspend fun clearDeliveryKey(groupId: String, memberId: String)

  @Query("DELETE FROM membership_changes") suspend fun deleteAll()
}
