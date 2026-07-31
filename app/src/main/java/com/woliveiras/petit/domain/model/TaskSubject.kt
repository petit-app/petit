package com.woliveiras.petit.domain.model

import java.text.Normalizer
import java.util.Locale

/** Which subject control a task kind needs, if any. */
enum class TaskSubjectControl {
  NONE,
  VACCINE,
  ANTIPARASITIC,
  MEDICATION,
}

/** Advisory subject choices for a task, reusing the species care catalog. */
object TaskSubjectOptions {

  fun controlFor(kind: TaskKind): TaskSubjectControl =
    when (kind) {
      TaskKind.VACCINATION -> TaskSubjectControl.VACCINE
      TaskKind.DEWORMING -> TaskSubjectControl.ANTIPARASITIC
      TaskKind.MEDICATION -> TaskSubjectControl.MEDICATION
      TaskKind.WEIGHT -> TaskSubjectControl.NONE
      TaskKind.CUSTOM -> TaskSubjectControl.NONE
    }

  /** Without a pet there is no species to filter by, so every vaccine stays selectable. */
  fun vaccineOptions(petType: PetType?): List<VaccineType> =
    if (petType == null) {
      VaccineType.entries.filter { it != VaccineType.OTHER } + VaccineType.OTHER
    } else {
      SpeciesCareCatalog.vaccinePresets(petType).map(VaccinePreset::vaccineType)
    }

  fun antiparasiticOptions(): List<DewormingType> = DewormingType.entries

  /** A vaccine subject is only complete once the free text behind "other" is filled. */
  fun requiresFreeText(control: TaskSubjectControl, code: String?): Boolean =
    control == TaskSubjectControl.VACCINE && code == VaccineType.OTHER.name

  /** Matches ignoring case and accents, and drops near-duplicates that only differ by those. */
  fun matchingSuggestions(query: String, used: List<String>, limit: Int = 5): List<String> {
    val normalizedQuery = query.normalizedForSuggestion()
    val seen = mutableSetOf<String>()
    return used
      .asSequence()
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .filter { candidate ->
        val normalized = candidate.normalizedForSuggestion()
        normalized != normalizedQuery &&
          (normalizedQuery.isEmpty() || normalized.contains(normalizedQuery))
      }
      .filter { seen.add(it.normalizedForSuggestion()) }
      .take(limit)
      .toList()
  }
}

private fun String.normalizedForSuggestion(): String =
  Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("""\p{M}+"""), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
    .trim()
