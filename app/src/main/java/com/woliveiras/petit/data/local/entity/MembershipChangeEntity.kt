package com.woliveiras.petit.data.local.entity

import androidx.room.Entity

@Entity(tableName = "membership_changes", primaryKeys = ["groupId", "memberId"])
data class MembershipChangeEntity(
  val groupId: String,
  val memberId: String,
  val type: String,
  val deviceName: String?,
  val timestamp: Long,
  /** Retained only for a local offline LEAVE until a membership-only ACK is received. */
  val deliveryKey: String? = null,
)
