package com.woliveiras.petit.domain.lan

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.MembershipChangeType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.WeightEntry
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LanChangesetBatcherTest {
  @Test
  fun createsIndependentlyValidBoundedBatchesWithStableIdsAndParentRows() {
    val bundle =
      ExportBundle(
        metadata = ExportMetadata("1", "2026-07-18T00:00:00Z"),
        pets = listOf(Pet(id = "pet-1", name = "Mimi", createdAt = 1L, updatedAt = 5L)),
        weightEntries =
          listOf(
            WeightEntry(
              id = "weight-1",
              petId = "pet-1",
              date = LocalDate.of(2026, 7, 18),
              weightGrams = 4_000,
              createdAt = 1L,
              updatedAt = 7L,
            )
          ),
        vaccinationEntries = emptyList(),
        dewormingEntries = emptyList(),
        tasks = emptyList(),
        membershipChanges =
          listOf(
            MembershipChange(
              groupId = "a".repeat(64),
              memberId = "member-1",
              type = MembershipChangeType.RENAME,
              deviceName = "Kitchen",
              timestamp = 9L,
            )
          ),
      )

    val first = LanChangesetBatcher.create("peer-1", 3L, bundle)
    val retry = LanChangesetBatcher.create("peer-1", 3L, bundle)

    assertThat(first.map { it.batchId }).containsExactlyElementsIn(retry.map { it.batchId })
    assertThat(first.map { it.cursor }).containsExactly(9L)
    assertThat(first.all { it.payload.size <= LanProtocol.MAX_CHANGESET_BYTES }).isTrue()
    assertThat(first.map { ExportBundle.validate(it.bundle) }.flatten()).isEmpty()
    assertThat(first.single { it.bundle.weightEntries.isNotEmpty() }.bundle.pets.single().id)
      .isEqualTo("pet-1")

    val afterParentCursor = LanChangesetBatcher.create("peer-1", 6L, bundle)
    assertThat(afterParentCursor.map { it.cursor }).containsExactly(9L)
    assertThat(afterParentCursor.single { it.bundle.weightEntries.isNotEmpty() }.bundle.pets)
      .hasSize(1)
  }

  @Test
  fun oneBoundedBatchAcknowledgesItsHighestIncludedCursor() {
    val bundle =
      ExportBundle(
        metadata = ExportMetadata("1", "2026-07-18T00:00:00Z"),
        pets = listOf(Pet(id = "pet-1", name = "Mimi", createdAt = 1L, updatedAt = 100L)),
        weightEntries =
          listOf(
            WeightEntry(
              id = "weight-1",
              petId = "pet-1",
              date = LocalDate.of(2026, 7, 18),
              weightGrams = 4_000,
              createdAt = 1L,
              updatedAt = 50L,
            )
          ),
        vaccinationEntries = emptyList(),
        dewormingEntries = emptyList(),
        tasks = emptyList(),
      )

    val batches = LanChangesetBatcher.create("peer", 0L, bundle)
    assertThat(batches).hasSize(1)
    assertThat(batches.single().cursor).isEqualTo(100L)
    assertThat(batches.single().bundle.weightEntries).hasSize(1)
  }
}
