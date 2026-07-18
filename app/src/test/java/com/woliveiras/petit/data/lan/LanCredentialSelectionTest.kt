package com.woliveiras.petit.data.lan

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.lan.LanSessionScope
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.MembershipChangeType
import com.woliveiras.petit.domain.model.PendingDeparture
import org.junit.Test

class LanCredentialSelectionTest {
  @Test
  fun rotatesUnavailableDeparturesBeforeTheActiveGroupWithoutDiscardingThem() {
    val first = departure("a", 1L)
    val second = departure("b", 2L)

    assertThat(selectLanCredential("active", listOf(second, first), 0)?.departure).isEqualTo(first)
    assertThat(selectLanCredential("active", listOf(second, first), 1)?.departure).isEqualTo(second)
    val active = selectLanCredential("active", listOf(second, first), 2)
    assertThat(active?.scope).isEqualTo(LanSessionScope.CLINICAL)
    assertThat(active?.groupKey).isEqualTo("active")
    assertThat(selectLanCredential("active", listOf(second, first), 3)?.departure).isEqualTo(first)
  }

  private fun departure(groupId: String, timestamp: Long) =
    PendingDeparture(
      MembershipChange(
        groupId = groupId,
        memberId = "member",
        type = MembershipChangeType.LEAVE,
        timestamp = timestamp,
      ),
      deliveryKey = "key-$groupId",
    )
}
