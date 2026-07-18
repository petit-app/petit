package com.woliveiras.petit.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

/** Data class representing the complete exportable bundle. */
data class ExportBundle(
  val metadata: ExportMetadata,
  val pets: List<Pet>,
  val weightEntries: List<WeightEntry>,
  val vaccinationEntries: List<VaccinationEntry>,
  val dewormingEntries: List<DewormingEntry>,
  val tasks: List<Task>,
  val membershipChanges: List<MembershipChange> = emptyList(),
) {
  val entityCount: Int
    get() =
      pets.size +
        weightEntries.size +
        vaccinationEntries.size +
        dewormingEntries.size +
        tasks.size +
        membershipChanges.size

  fun toJson(): JSONObject {
    return JSONObject().apply {
      put("metadata", metadata.toJson())
      put("pets", JSONArray(pets.map { it.toExportJson() }))
      put("weightEntries", JSONArray(weightEntries.map { it.toExportJson() }))
      put("vaccinationEntries", JSONArray(vaccinationEntries.map { it.toExportJson() }))
      put("dewormingEntries", JSONArray(dewormingEntries.map { it.toExportJson() }))
      put("tasks", JSONArray(tasks.map { it.toExportJson() }))
      put("membershipChanges", JSONArray(membershipChanges.map { it.toExportJson() }))
    }
  }

  companion object {
    private const val MAX_ENTRIES_PER_LIST = 10_000
    private const val SUPPORTED_SCHEMA_VERSION = 1

    fun fromJson(json: JSONObject): ExportBundle {
      val metadata = ExportMetadata.fromJson(json.getJSONObject("metadata"))

      val pets =
        if (json.has("pets")) {
          json.getJSONArray("pets").let { array ->
            if (array.length() > MAX_ENTRIES_PER_LIST) {
              throw IllegalArgumentException("Too many pets (max $MAX_ENTRIES_PER_LIST)")
            }
            (0 until array.length()).map { Pet.fromExportJson(array.getJSONObject(it)) }
          }
        } else {
          emptyList()
        }

      val weightEntries =
        json.getJSONArray("weightEntries").let { array ->
          if (array.length() > MAX_ENTRIES_PER_LIST)
            throw IllegalArgumentException("Too many weight entries (max $MAX_ENTRIES_PER_LIST)")
          (0 until array.length()).map { WeightEntry.fromExportJson(array.getJSONObject(it)) }
        }

      val vaccinationEntries =
        json.getJSONArray("vaccinationEntries").let { array ->
          if (array.length() > MAX_ENTRIES_PER_LIST)
            throw IllegalArgumentException(
              "Too many vaccination entries (max $MAX_ENTRIES_PER_LIST)"
            )
          (0 until array.length()).map { VaccinationEntry.fromExportJson(array.getJSONObject(it)) }
        }

      val dewormingEntries =
        json.getJSONArray("dewormingEntries").let { array ->
          if (array.length() > MAX_ENTRIES_PER_LIST)
            throw IllegalArgumentException("Too many deworming entries (max $MAX_ENTRIES_PER_LIST)")
          (0 until array.length()).map { DewormingEntry.fromExportJson(array.getJSONObject(it)) }
        }

      val tasks = parseTasks(json)
      val membershipChanges = parseMembershipChanges(json)

      return ExportBundle(
        metadata = metadata,
        pets = pets,
        weightEntries = weightEntries,
        vaccinationEntries = vaccinationEntries,
        dewormingEntries = dewormingEntries,
        tasks = tasks,
        membershipChanges = membershipChanges,
      )
    }

    private fun parseMembershipChanges(json: JSONObject): List<MembershipChange> {
      if (!json.has("membershipChanges")) return emptyList()
      val array = json.getJSONArray("membershipChanges")
      if (array.length() > MAX_ENTRIES_PER_LIST) {
        throw IllegalArgumentException("Too many membership changes (max $MAX_ENTRIES_PER_LIST)")
      }
      return (0 until array.length()).map { index ->
        MembershipChange.fromExportJson(array.getJSONObject(index))
      }
    }

    /**
     * Converts the supported pre-task reminder shape before the bundle is validated or imported.
     */
    private fun parseTasks(json: JSONObject): List<Task> {
      val array =
        when {
          json.has("tasks") -> json.getJSONArray("tasks")
          json.has("reminders") -> json.getJSONArray("reminders")
          else -> return emptyList()
        }
      if (array.length() > MAX_ENTRIES_PER_LIST)
        throw IllegalArgumentException("Too many tasks (max $MAX_ENTRIES_PER_LIST)")

      return (0 until array.length()).map { index ->
        val entry = array.getJSONObject(index)
        if (json.has("tasks")) {
          Task.fromExportJson(entry)
        } else {
          entry.toLegacyReminderTask()
        }
      }
    }

    /** Validate imported data fields. Returns list of validation error messages. */
    fun validate(bundle: ExportBundle): List<String> {
      val errors = mutableListOf<String>()

      fun reportDuplicateIds(label: String, ids: List<String>) {
        if (ids.size != ids.toSet().size) errors.add("Duplicate $label identifiers")
      }

      reportDuplicateIds("pet", bundle.pets.map { it.id })
      reportDuplicateIds("weight", bundle.weightEntries.map { it.id })
      reportDuplicateIds("vaccination", bundle.vaccinationEntries.map { it.id })
      reportDuplicateIds("deworming", bundle.dewormingEntries.map { it.id })
      reportDuplicateIds("task", bundle.tasks.map { it.id })

      if (bundle.metadata.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
        errors.add("Versão de backup não suportada: ${bundle.metadata.schemaVersion}")
      }

      bundle.pets.forEach { pet ->
        if (pet.name.isBlank()) errors.add("Pet com nome vazio (id: ${pet.id})")
        if (pet.name.length > 50) errors.add("Nome do pet muito longo: '${pet.name.take(20)}...'")
        if (pet.notes != null && pet.notes.length > 500)
          errors.add("Notas do pet '${pet.name}' excedem 500 caracteres")
        if (pet.microchipNumber != null && pet.microchipNumber.length > 50)
          errors.add("Microchip do pet '${pet.name}' excede 50 caracteres")
        if (pet.passportNumber != null && pet.passportNumber.length > 50)
          errors.add("Passaporte do pet '${pet.name}' excede 50 caracteres")
      }

      val petIds = bundle.pets.map { it.id }.toSet()

      bundle.weightEntries.forEach { entry ->
        if (entry.weightGrams <= 0)
          errors.add("Peso inválido: ${entry.weightGrams}g (id: ${entry.id})")
        if (entry.weightGrams > 100000)
          errors.add("Peso excede 100kg: ${entry.weightGrams}g (id: ${entry.id})")
        if (entry.petId !in petIds)
          errors.add("Entrada de peso referencia pet inexistente (petId: ${entry.petId})")
      }

      bundle.vaccinationEntries.forEach { entry ->
        if (entry.petId !in petIds)
          errors.add("Vacinação referencia pet inexistente (petId: ${entry.petId})")
        if (entry.customVaccineTypeName != null && entry.customVaccineTypeName.length > 100)
          errors.add("Nome de vacina personalizada muito longo para vacinação (id: ${entry.id})")
      }

      bundle.dewormingEntries.forEach { entry ->
        if (entry.medication != null && entry.medication.length > 100)
          errors.add("Nome de medicamento muito longo na vermifugação (id: ${entry.id})")
        if (entry.petId !in petIds)
          errors.add("Vermifugação referencia pet inexistente (petId: ${entry.petId})")
      }

      bundle.tasks.forEach { entry ->
        if (entry.title.isBlank()) errors.add("Tarefa sem título (id: ${entry.id})")
        if (entry.title.length > 100)
          errors.add("Título de tarefa muito longo: '${entry.title.take(20)}...' (id: ${entry.id})")
        if (entry.description != null && entry.description.length > 500)
          errors.add("Descrição de tarefa muito longa (id: ${entry.id})")
        if (entry.petId != null && entry.petId !in petIds)
          errors.add("Tarefa referencia pet inexistente (petId: ${entry.petId})")
      }

      bundle.membershipChanges.forEach { change ->
        if (!change.groupId.matches(Regex("[0-9a-f]{64}"))) {
          errors.add("Alteração de associação com identificador de grupo inválido")
        }
        if (change.memberId.isBlank() || change.memberId.length > 128) {
          errors.add("Alteração de associação com identidade inválida")
        }
        if (
          change.type == MembershipChangeType.RENAME &&
            (change.deviceName.isNullOrBlank() || change.deviceName.length > 80)
        ) {
          errors.add("Renomeação de dispositivo inválida")
        }
      }

      return errors
    }
  }
}

/** Metadata included in export files. */
data class ExportMetadata(
  val appVersion: String,
  val exportDate: String,
  val schemaVersion: Int = 1,
) {
  fun toJson(): JSONObject {
    return JSONObject().apply {
      put("appVersion", appVersion)
      put("exportDate", exportDate)
      put("schemaVersion", schemaVersion)
    }
  }

  companion object {
    fun fromJson(json: JSONObject): ExportMetadata {
      return ExportMetadata(
        appVersion = json.getString("appVersion"),
        exportDate = json.getString("exportDate"),
        schemaVersion = json.optInt("schemaVersion", 1),
      )
    }
  }
}

/** Analysis of an import bundle. */
data class ImportAnalysis(
  val totalPets: Int,
  val totalWeightEntries: Int,
  val totalVaccinationEntries: Int,
  val totalDewormingEntries: Int,
  val totalTasks: Int,
  val conflictingPetNames: List<String>,
  val schemaVersion: Int,
  val exportDate: String,
) {
  val hasConflicts: Boolean
    get() = conflictingPetNames.isNotEmpty()
}

/** Conflict resolution strategy for imports. */
enum class ConflictResolution {
  REPLACE, // Backup data replaces local
  KEEP, // Local data is kept
  MERGE, // Last-write-wins by updatedAt
}

// Extension functions for JSON serialization

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun Pet.toExportJson(): JSONObject {
  return JSONObject().apply {
    put("id", id)
    put("name", name)
    put("petType", petType.name)
    put("birthDate", birthDate?.format(dateFormatter))
    put("sex", sex.name)
    put("breed", breed)
    put("color", color)
    put("microchipNumber", microchipNumber)
    put("passportNumber", passportNumber)
    put("photoUri", photoUri)
    put("notes", notes)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("deletedAt", deletedAt ?: JSONObject.NULL)
  }
}

fun Pet.Companion.fromExportJson(json: JSONObject): Pet {
  return Pet(
    id = json.getString("id"),
    name = json.getString("name"),
    petType =
      try {
        PetType.valueOf(json.optString("petType", "OTHER"))
      } catch (_: Exception) {
        PetType.OTHER
      },
    birthDate =
      json
        .optString("birthDate", null)
        ?.takeIf { it.isNotEmpty() }
        ?.let { LocalDate.parse(it, dateFormatter) },
    sex =
      try {
        Sex.valueOf(json.optString("sex", "UNKNOWN"))
      } catch (_: Exception) {
        Sex.UNKNOWN
      },
    breed = json.optStringOrNull("breed"),
    color = json.optStringOrNull("color"),
    microchipNumber = json.optStringOrNull("microchipNumber"),
    passportNumber = json.optStringOrNull("passportNumber"),
    photoUri = json.optStringOrNull("photoUri"),
    notes = json.optStringOrNull("notes"),
    createdAt = json.getLong("createdAt"),
    updatedAt = json.getLong("updatedAt"),
    deletedAt = json.optLongOrNull("deletedAt"),
    syncStatus = SyncStatus.LOCAL_ONLY,
  )
}

fun WeightEntry.toExportJson(): JSONObject {
  return JSONObject().apply {
    put("id", id)
    put("petId", petId)
    put("date", date.format(dateFormatter))
    put("weightGrams", weightGrams)
    put("note", note)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("deletedAt", deletedAt ?: JSONObject.NULL)
  }
}

fun WeightEntry.Companion.fromExportJson(json: JSONObject): WeightEntry {
  return WeightEntry(
    id = json.getString("id"),
    petId = json.getString("petId"),
    date = LocalDate.parse(json.getString("date"), dateFormatter),
    weightGrams = json.getInt("weightGrams"),
    note = json.optStringOrNull("note"),
    createdAt = json.getLong("createdAt"),
    updatedAt = json.getLong("updatedAt"),
    deletedAt = json.optLongOrNull("deletedAt"),
    syncStatus = SyncStatus.LOCAL_ONLY,
  )
}

fun VaccinationEntry.toExportJson(): JSONObject {
  return JSONObject().apply {
    put("id", id)
    put("petId", petId)
    put("vaccineType", vaccineType.name)
    put("customVaccineTypeName", customVaccineTypeName)
    put("applicationDate", applicationDate.format(dateFormatter))
    put("nextDueDate", nextDueDate?.format(dateFormatter))
    put("veterinarian", veterinarian)
    put("clinic", clinic)
    put("batchNumber", batchNumber)
    put("note", note)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("deletedAt", deletedAt ?: JSONObject.NULL)
  }
}

fun VaccinationEntry.Companion.fromExportJson(json: JSONObject): VaccinationEntry {
  val vaccineTypeStr = json.getString("vaccineType")
  val vaccineType =
    try {
      VaccineType.valueOf(vaccineTypeStr)
    } catch (_: Exception) {
      VaccineType.OTHER
    }
  return VaccinationEntry(
    id = json.getString("id"),
    petId = json.getString("petId"),
    vaccineType = vaccineType,
    customVaccineTypeName =
      json.optStringOrNull("customVaccineTypeName")
        ?: if (vaccineType == VaccineType.OTHER) vaccineTypeStr else null,
    applicationDate = LocalDate.parse(json.getString("applicationDate"), dateFormatter),
    nextDueDate =
      json
        .optString("nextDueDate", null)
        ?.takeIf { it.isNotEmpty() }
        ?.let { LocalDate.parse(it, dateFormatter) },
    veterinarian = json.optStringOrNull("veterinarian"),
    clinic = json.optStringOrNull("clinic"),
    batchNumber = json.optStringOrNull("batchNumber"),
    note = json.optStringOrNull("note"),
    createdAt = json.getLong("createdAt"),
    updatedAt = json.getLong("updatedAt"),
    deletedAt = json.optLongOrNull("deletedAt"),
    syncStatus = SyncStatus.LOCAL_ONLY,
  )
}

fun DewormingEntry.toExportJson(): JSONObject {
  return JSONObject().apply {
    put("id", id)
    put("petId", petId)
    put("type", type.name)
    put("medication", medication)
    put("applicationDate", applicationDate.format(dateFormatter))
    put("nextDueDate", nextDueDate?.format(dateFormatter))
    put("note", note)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("deletedAt", deletedAt ?: JSONObject.NULL)
  }
}

fun DewormingEntry.Companion.fromExportJson(json: JSONObject): DewormingEntry {
  return DewormingEntry(
    id = json.getString("id"),
    petId = json.getString("petId"),
    type = DewormingType.valueOf(json.getString("type")),
    medication = json.optStringOrNull("medication"),
    applicationDate = LocalDate.parse(json.getString("applicationDate"), dateFormatter),
    nextDueDate =
      json
        .optString("nextDueDate", null)
        ?.takeIf { it.isNotEmpty() }
        ?.let { LocalDate.parse(it, dateFormatter) },
    note = json.optStringOrNull("note"),
    createdAt = json.getLong("createdAt"),
    updatedAt = json.getLong("updatedAt"),
    deletedAt = json.optLongOrNull("deletedAt"),
    syncStatus = SyncStatus.LOCAL_ONLY,
  )
}

fun Task.toExportJson(): JSONObject {
  return JSONObject().apply {
    put("id", id)
    put("petId", petId)
    put("kind", kind.name)
    put("referenceEntityId", referenceEntityId)
    put("title", title)
    put("description", description)
    put("scheduledFor", scheduledFor.format(dateTimeFormatter))
    put("status", status.name)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("deletedAt", deletedAt ?: JSONObject.NULL)
  }
}

fun Task.Companion.fromExportJson(json: JSONObject): Task {
  val kind =
    try {
      TaskKind.valueOf(json.getString("kind"))
    } catch (_: IllegalArgumentException) {
      TaskKind.CUSTOM
    }

  val status =
    try {
      TaskStatus.valueOf(json.optString("status", "PENDING"))
    } catch (_: IllegalArgumentException) {
      TaskStatus.PENDING
    }

  return Task(
    id = json.getString("id"),
    petId = json.optStringOrNull("petId"),
    kind = kind,
    referenceEntityId = json.optStringOrNull("referenceEntityId"),
    title = json.getString("title"),
    description = json.optStringOrNull("description"),
    scheduledFor = LocalDateTime.parse(json.getString("scheduledFor"), dateTimeFormatter),
    status = status,
    createdAt = json.getLong("createdAt"),
    updatedAt = json.getLong("updatedAt"),
    deletedAt = json.optLongOrNull("deletedAt"),
  )
}

private fun MembershipChange.toExportJson(): JSONObject =
  JSONObject()
    .put("groupId", groupId)
    .put("memberId", memberId)
    .put("type", type.name)
    .put("deviceName", deviceName ?: JSONObject.NULL)
    .put("timestamp", timestamp)

private fun MembershipChange.Companion.fromExportJson(json: JSONObject): MembershipChange =
  MembershipChange(
    groupId = json.getString("groupId"),
    memberId = json.getString("memberId"),
    type = MembershipChangeType.valueOf(json.getString("type")),
    deviceName = json.optStringOrNull("deviceName"),
    timestamp = json.getLong("timestamp"),
  )

// Helper extension functions
private fun JSONObject.optStringOrNull(key: String): String? {
  return if (isNull(key)) null else optString(key, null)?.takeIf { it.isNotEmpty() && it != "null" }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
  if (!has(key) || isNull(key)) null else getLong(key)

private fun JSONObject.toLegacyReminderTask(): Task {
  val scheduledFor = optStringOrNull("scheduledAt") ?: optStringOrNull("scheduledFor")
  require(!scheduledFor.isNullOrBlank()) { "Legacy reminder is missing scheduledAt" }
  val title = optStringOrNull("title")
  require(!title.isNullOrBlank()) { "Legacy reminder is missing title" }

  return Task(
    id = getString("id"),
    petId = optStringOrNull("petId"),
    kind = TaskKind.CUSTOM,
    title = title,
    description = optStringOrNull("description"),
    scheduledFor = LocalDateTime.parse(scheduledFor, dateTimeFormatter),
    status = if (optBoolean("completed", false)) TaskStatus.COMPLETED else TaskStatus.PENDING,
    createdAt = getLong("createdAt"),
    updatedAt = getLong("updatedAt"),
  )
}
