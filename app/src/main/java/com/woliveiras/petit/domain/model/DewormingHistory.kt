package com.woliveiras.petit.domain.model

enum class DewormingCategory {
  INTERNAL,
  EXTERNAL,
}

data class DewormingCategorySummary(val category: DewormingCategory, val latest: DewormingEntry)

private val dewormingRecencyComparator =
  compareByDescending<DewormingEntry> { it.applicationDate }
    .thenByDescending { it.updatedAt }
    .thenByDescending { it.id }

fun List<DewormingEntry>.sortedDewormingHistory(): List<DewormingEntry> =
  sortedWith(dewormingRecencyComparator)

fun List<DewormingEntry>.dewormingCategorySummaries(): List<DewormingCategorySummary> =
  DewormingCategory.entries.mapNotNull { category ->
    filter { entry -> entry.type.appliesTo(category) }
      .sortedWith(dewormingRecencyComparator)
      .firstOrNull()
      ?.let { latest -> DewormingCategorySummary(category = category, latest = latest) }
  }

fun DewormingType.appliesTo(category: DewormingCategory): Boolean =
  this == DewormingType.BOTH ||
    when (category) {
      DewormingCategory.INTERNAL -> this == DewormingType.INTERNAL
      DewormingCategory.EXTERNAL -> this == DewormingType.EXTERNAL
    }
