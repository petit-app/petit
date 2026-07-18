package com.woliveiras.petit.domain.lan

import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.Pet
import java.nio.charset.StandardCharsets
import java.util.UUID

data class PreparedLanChangeset(
  val batchId: String,
  val cursor: Long,
  val bundle: ExportBundle,
  val payload: LanBytes,
  val constituentBatchIds: Set<String>,
)

/** Creates small independently valid units so every ACK advances a durable boundary. */
object LanChangesetBatcher {
  fun create(
    peerId: String,
    sinceTimestamp: Long,
    bundle: ExportBundle,
    acknowledgedBatchIds: Set<String> = emptySet(),
  ): List<PreparedLanChangeset> {
    require(peerId.isNotBlank())
    require(sinceTimestamp >= 0)
    val petsById = bundle.pets.associateBy(Pet::id)
    val units = buildList {
      bundle.pets
        .filter { it.updatedAt >= sinceTimestamp }
        .forEach { pet -> add(bundle.copyOnly(pets = listOf(pet)) to pet.updatedAt) }
      bundle.weightEntries.forEach { entry ->
        add(
          bundle.copyOnly(
            pets = listOf(requireNotNull(petsById[entry.petId]) { "Missing parent pet" }),
            weights = listOf(entry),
          ) to entry.updatedAt
        )
      }
      bundle.vaccinationEntries.forEach { entry ->
        add(
          bundle.copyOnly(
            pets = listOf(requireNotNull(petsById[entry.petId]) { "Missing parent pet" }),
            vaccinations = listOf(entry),
          ) to entry.updatedAt
        )
      }
      bundle.dewormingEntries.forEach { entry ->
        add(
          bundle.copyOnly(
            pets = listOf(requireNotNull(petsById[entry.petId]) { "Missing parent pet" }),
            dewormings = listOf(entry),
          ) to entry.updatedAt
        )
      }
      bundle.tasks.forEach { task ->
        val parents =
          task.petId?.let { listOf(requireNotNull(petsById[it]) { "Missing parent pet" }) }
            ?: emptyList()
        add(bundle.copyOnly(pets = parents, tasks = listOf(task)) to task.updatedAt)
      }
      bundle.membershipChanges.forEach { change ->
        add(bundle.copyOnly(membershipChanges = listOf(change)) to change.timestamp)
      }
    }
    val ordered =
      units.sortedWith(
        compareBy<Pair<ExportBundle, Long>> { it.second }.thenBy { it.first.toJson().toString() }
      )
    val pendingUnits =
      ordered
        .map { (unit, cursor) -> prepare(peerId, sinceTimestamp, cursor, unit) }
        .filterNot { it.batchId in acknowledgedBatchIds }
    val batches = mutableListOf<PreparedLanChangeset>()
    var accumulated: ExportBundle? = null
    var accumulatedCursor = 0L
    var constituentIds = linkedSetOf<String>()
    for (prepared in pendingUnits) {
      val unit = prepared.bundle
      val cursor = prepared.cursor
      val candidate = accumulated?.merge(unit) ?: unit
      if (encodedSize(candidate) <= LanProtocol.MAX_CHANGESET_BYTES) {
        accumulated = candidate
        accumulatedCursor = maxOf(accumulatedCursor, cursor)
        constituentIds += prepared.constituentBatchIds
      } else {
        if (accumulated == null) {
          batches += prepared
        } else {
          batches +=
            prepare(peerId, sinceTimestamp, accumulatedCursor, accumulated)
              .copy(constituentBatchIds = constituentIds.toSet())
          accumulated = unit
          accumulatedCursor = cursor
          constituentIds = linkedSetOf<String>().apply { addAll(prepared.constituentBatchIds) }
        }
      }
    }
    accumulated?.let {
      batches +=
        prepare(peerId, sinceTimestamp, accumulatedCursor, it)
          .copy(constituentBatchIds = constituentIds.toSet())
    }
    return batches
  }

  private fun prepare(
    peerId: String,
    sinceTimestamp: Long,
    cursor: Long,
    bundle: ExportBundle,
  ): PreparedLanChangeset {
    val errors = ExportBundle.validate(bundle)
    require(errors.isEmpty()) { errors.first() }
    val bytes = bundle.toJson().toString().toByteArray(StandardCharsets.UTF_8)
    if (bytes.size > LanProtocol.MAX_CHANGESET_BYTES) {
      throw LanProtocolException(
        LanProtocolError.CHANGESET_TOO_LARGE,
        "A single shareable entity exceeds the changeset limit",
      )
    }
    val stablePayload =
      bundle
        .copy(metadata = bundle.metadata.copy(exportDate = ""))
        .toJson()
        .toString()
        .toByteArray(StandardCharsets.UTF_8)
    val stableInput =
      buildString {
          append(peerId)
          append('\u0000')
          append(cursor)
          append('\u0000')
        }
        .toByteArray(StandardCharsets.UTF_8) + stablePayload
    val batchId = UUID.nameUUIDFromBytes(stableInput).toString()
    return PreparedLanChangeset(
      batchId = batchId,
      cursor = cursor,
      bundle = bundle,
      payload = LanBytes(bytes),
      constituentBatchIds = setOf(batchId),
    )
  }

  private fun ExportBundle.copyOnly(
    pets: List<Pet> = emptyList(),
    weights: List<com.woliveiras.petit.domain.model.WeightEntry> = emptyList(),
    vaccinations: List<com.woliveiras.petit.domain.model.VaccinationEntry> = emptyList(),
    dewormings: List<com.woliveiras.petit.domain.model.DewormingEntry> = emptyList(),
    tasks: List<com.woliveiras.petit.domain.model.Task> = emptyList(),
    membershipChanges: List<MembershipChange> = emptyList(),
  ) =
    ExportBundle(
      metadata = metadata,
      pets = pets,
      weightEntries = weights,
      vaccinationEntries = vaccinations,
      dewormingEntries = dewormings,
      tasks = tasks,
      membershipChanges = membershipChanges,
    )

  private fun ExportBundle.merge(other: ExportBundle) =
    copy(
      pets = (pets + other.pets).distinctBy(Pet::id),
      weightEntries = (weightEntries + other.weightEntries).distinctBy { it.id },
      vaccinationEntries = (vaccinationEntries + other.vaccinationEntries).distinctBy { it.id },
      dewormingEntries = (dewormingEntries + other.dewormingEntries).distinctBy { it.id },
      tasks = (tasks + other.tasks).distinctBy { it.id },
      membershipChanges =
        (membershipChanges + other.membershipChanges).distinctBy { it.groupId to it.memberId },
    )

  private fun encodedSize(bundle: ExportBundle) =
    bundle.toJson().toString().toByteArray(StandardCharsets.UTF_8).size
}
