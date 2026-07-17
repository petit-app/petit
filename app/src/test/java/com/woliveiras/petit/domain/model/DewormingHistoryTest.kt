package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DewormingHistoryTest {

  @Test
  fun bothCountsInInternalAndExternalCategories() {
    val combined = entry("both", DewormingType.BOTH, day = 10)

    val summaries = listOf(combined).dewormingCategorySummaries()

    assertThat(summaries.map { it.category })
      .containsExactly(DewormingCategory.INTERNAL, DewormingCategory.EXTERNAL)
      .inOrder()
    assertThat(summaries.map { it.latest.id }).containsExactly("both", "both").inOrder()
  }

  @Test
  fun latestApplicableRecordWinsCompetitionBetweenSpecificAndBoth() {
    val entries =
      listOf(
        entry("internal-old", DewormingType.INTERNAL, day = 1),
        entry("external-new", DewormingType.EXTERNAL, day = 12),
        entry("both-middle", DewormingType.BOTH, day = 10),
      )

    val summaries = entries.dewormingCategorySummaries().associateBy { it.category }

    assertThat(summaries.getValue(DewormingCategory.INTERNAL).latest.id).isEqualTo("both-middle")
    assertThat(summaries.getValue(DewormingCategory.EXTERNAL).latest.id).isEqualTo("external-new")
  }

  @Test
  fun equalDatesUseUpdatedAtThenIdAsDeterministicTieBreakers() {
    val entries =
      listOf(
        entry("old-update", DewormingType.INTERNAL, day = 10, updatedAt = 100),
        entry("tie-a", DewormingType.BOTH, day = 10, updatedAt = 200),
        entry("tie-b", DewormingType.INTERNAL, day = 10, updatedAt = 200),
      )

    val internal =
      entries.dewormingCategorySummaries().single { it.category == DewormingCategory.INTERNAL }

    assertThat(internal.latest.id).isEqualTo("tie-b")
  }

  @Test
  fun chronologicalHistoryIsDescendingAndDeterministic() {
    val entries =
      listOf(
        entry("old", DewormingType.INTERNAL, day = 1, updatedAt = 300),
        entry("tie-a", DewormingType.EXTERNAL, day = 10, updatedAt = 200),
        entry("older-update", DewormingType.BOTH, day = 10, updatedAt = 100),
        entry("tie-b", DewormingType.INTERNAL, day = 10, updatedAt = 200),
      )

    assertThat(entries.sortedDewormingHistory().map { it.id })
      .containsExactly("tie-b", "tie-a", "older-update", "old")
      .inOrder()
  }

  private fun entry(id: String, type: DewormingType, day: Int, updatedAt: Long = day.toLong()) =
    DewormingEntry(
      id = id,
      petId = "pet-1",
      type = type,
      medication = "Medication",
      applicationDate = LocalDate.of(2026, 7, day),
      createdAt = 1L,
      updatedAt = updatedAt,
    )
}
