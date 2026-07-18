package com.woliveiras.petit.domain.conflict

import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.WeightEntry

data class ConflictVersion<T>(
  val id: String,
  val updatedAt: Long,
  val deletedAt: Long?,
  val canonicalPayload: String,
  val value: T,
) {
  val effectiveTimestamp: Long
    get() = maxOf(updatedAt, deletedAt ?: Long.MIN_VALUE)
}

enum class ConflictOutcome {
  Inserted,
  RemoteWon,
  LocalKept,
  Identical,
}

data class ConflictResult<T>(val selected: ConflictVersion<T>, val outcome: ConflictOutcome)

/** A pure total-order resolver shared by every local transport. */
class ConflictResolver {
  fun <T> resolve(local: ConflictVersion<T>?, remote: ConflictVersion<T>): ConflictResult<T> {
    if (local == null) return ConflictResult(remote, ConflictOutcome.Inserted)
    require(local.id == remote.id) { "Cannot resolve different UUIDs" }

    val comparison = compare(remote, local)
    return when {
      comparison > 0 -> ConflictResult(remote, ConflictOutcome.RemoteWon)
      comparison < 0 -> ConflictResult(local, ConflictOutcome.LocalKept)
      else -> ConflictResult(local, ConflictOutcome.Identical)
    }
  }

  fun <T> normalize(versions: List<ConflictVersion<T>>): List<ConflictVersion<T>> =
    versions
      .groupBy { it.id }
      .mapValues { (_, duplicates) ->
        duplicates.reduce { selected, candidate -> resolve(selected, candidate).selected }
      }
      .toSortedMap()
      .values
      .toList()

  private fun <T> compare(first: ConflictVersion<T>, second: ConflictVersion<T>): Int {
    val time = first.effectiveTimestamp.compareTo(second.effectiveTimestamp)
    if (time != 0) return time

    // At the same event time a tombstone wins, preventing ambiguous resurrection.
    val deletion = (first.deletedAt != null).compareTo(second.deletedAt != null)
    if (deletion != 0) return deletion

    return first.canonicalPayload.compareTo(second.canonicalPayload)
  }
}

fun Pet.asConflictVersion(): ConflictVersion<Pet> =
  ConflictVersion(
    id,
    updatedAt,
    deletedAt,
    canonical(
      id,
      name,
      petType,
      birthDate,
      sex,
      breed,
      color,
      microchipNumber,
      passportNumber,
      photoUri,
      notes,
      createdAt,
      updatedAt,
      deletedAt,
    ),
    this,
  )

fun WeightEntry.asConflictVersion(): ConflictVersion<WeightEntry> =
  ConflictVersion(
    id,
    updatedAt,
    deletedAt,
    canonical(id, petId, date, weightGrams, note, createdAt, updatedAt, deletedAt),
    this,
  )

fun VaccinationEntry.asConflictVersion(): ConflictVersion<VaccinationEntry> =
  ConflictVersion(
    id,
    updatedAt,
    deletedAt,
    canonical(
      id,
      petId,
      vaccineType,
      customVaccineTypeName,
      applicationDate,
      nextDueDate,
      veterinarian,
      clinic,
      batchNumber,
      note,
      createdAt,
      updatedAt,
      deletedAt,
    ),
    this,
  )

fun DewormingEntry.asConflictVersion(): ConflictVersion<DewormingEntry> =
  ConflictVersion(
    id,
    updatedAt,
    deletedAt,
    canonical(
      id,
      petId,
      type,
      medication,
      applicationDate,
      nextDueDate,
      note,
      createdAt,
      updatedAt,
      deletedAt,
    ),
    this,
  )

fun Task.asConflictVersion(): ConflictVersion<Task> =
  ConflictVersion(
    id,
    updatedAt,
    deletedAt,
    canonical(
      id,
      petId,
      kind,
      referenceEntityId,
      title,
      description,
      scheduledFor,
      status,
      createdAt,
      updatedAt,
      deletedAt,
    ),
    this,
  )

/** Length-prefixing makes user text unambiguous without relying on Android serialization. */
private fun canonical(vararg fields: Any?): String =
  fields.joinToString(separator = "") { field ->
    when (field) {
      null -> "n;"
      is String -> "s${field.length}:$field;"
      is Int -> "i$field;"
      is Long -> "l$field;"
      is Enum<*> -> "e${field.name.length}:${field.name};"
      else -> {
        val value = field.toString()
        "v${value.length}:$value;"
      }
    }
  }
