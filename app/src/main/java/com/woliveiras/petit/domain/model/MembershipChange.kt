package com.woliveiras.petit.domain.model

import java.security.MessageDigest

enum class MembershipChangeType {
  RENAME,
  REMOVE,
  LEAVE,
}

/** A terminal event that may authenticate only its own delivery, never clinical data access. */
data class PendingDeparture(val change: MembershipChange, val deliveryKey: String)

/** Minimal, non-clinical group change that can be forwarded idempotently between peers. */
data class MembershipChange(
  val groupId: String,
  val memberId: String,
  val type: MembershipChangeType,
  val deviceName: String? = null,
  val timestamp: Long,
) {
  companion object {
    fun normalize(changes: List<MembershipChange>): List<MembershipChange> =
      changes
        .groupBy { it.groupId to it.memberId }
        .mapValues { (_, versions) -> versions.maxWith(::compare) }
        .toSortedMap(compareBy<Pair<String, String>> { it.first }.thenBy { it.second })
        .values
        .toList()

    /** One-way group identifier lets departure events survive without retaining the access key. */
    fun groupIdForKey(groupKey: String): String =
      MessageDigest.getInstance("SHA-256")
        .digest(groupKey.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun compare(first: MembershipChange, second: MembershipChange): Int {
      val priority = priority(first.type).compareTo(priority(second.type))
      if (priority != 0) return priority
      val timestamp = first.timestamp.compareTo(second.timestamp)
      if (timestamp != 0) return timestamp
      return first.deviceName.orEmpty().compareTo(second.deviceName.orEmpty())
    }

    private fun priority(type: MembershipChangeType): Int =
      when (type) {
        MembershipChangeType.RENAME -> 0
        MembershipChangeType.REMOVE -> 1
        MembershipChangeType.LEAVE -> 2
      }
  }
}
