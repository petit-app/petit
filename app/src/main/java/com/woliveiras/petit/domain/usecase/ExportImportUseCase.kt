package com.woliveiras.petit.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.woliveiras.petit.BuildConfig
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.conflict.ConflictOutcome
import com.woliveiras.petit.domain.conflict.ConflictResolver
import com.woliveiras.petit.domain.conflict.ConflictVersion
import com.woliveiras.petit.domain.conflict.asConflictVersion
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.ImportAnalysis
import com.woliveiras.petit.domain.model.MergeResult
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.WeightEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** Use case for exporting and importing app data as JSON. */
@Singleton
class ExportImportUseCase
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val database: PetitDatabase,
  private val petRepository: PetRepository,
  private val weightRepository: WeightEntryRepository,
  private val vaccinationRepository: VaccinationEntryRepository,
  private val dewormingRepository: DewormingEntryRepository,
  private val taskRepository: TaskRepository,
) {

  companion object {
    private const val MAX_IMPORT_SIZE_BYTES = 10L * 1024 * 1024 // 10MB
    private val ALLOWED_MIME_TYPES =
      listOf("application/json", "text/plain", "application/octet-stream")
  }

  /** Generate the filename for an export. */
  fun generateExportFilename(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return "petit_backup_$date.json"
  }

  /** Export all data to a JSON bundle. */
  suspend fun exportAll(): ExportBundle =
    withContext(Dispatchers.IO) {
      val pets = petRepository.getAllPets().first()
      val weights = mutableListOf<WeightEntry>()
      val vaccinations = mutableListOf<VaccinationEntry>()
      val dewormings = mutableListOf<DewormingEntry>()
      val tasks = taskRepository.getAllActiveTasks().first()

      pets.forEach { pet ->
        weights.addAll(weightRepository.getWeightEntriesForPet(pet.id).first())
        vaccinations.addAll(vaccinationRepository.getVaccinationEntriesForPet(pet.id).first())
        dewormings.addAll(dewormingRepository.getDewormingEntriesForPet(pet.id).first())
      }

      ExportBundle(
        metadata =
          ExportMetadata(
            appVersion = BuildConfig.VERSION_NAME,
            exportDate = Instant.now().toString(),
            schemaVersion = 1,
          ),
        pets = pets,
        weightEntries = weights,
        vaccinationEntries = vaccinations,
        dewormingEntries = dewormings,
        tasks = tasks,
      )
    }

  /** Export every shareable version, including soft-delete tombstones used for convergence. */
  suspend fun exportShareable(): ExportBundle =
    withContext(Dispatchers.IO) {
      ExportBundle(
        metadata =
          ExportMetadata(
            appVersion = BuildConfig.VERSION_NAME,
            exportDate = Instant.now().toString(),
            schemaVersion = 1,
          ),
        pets = database.petDao().getAllIncludingDeleted().toDomain(),
        weightEntries = database.weightEntryDao().getAllIncludingDeleted().toDomain(),
        vaccinationEntries = database.vaccinationEntryDao().getAllIncludingDeleted().toDomain(),
        dewormingEntries = database.dewormingEntryDao().getAllIncludingDeleted().toDomain(),
        tasks = database.taskDao().getAllIncludingDeleted().toDomain(),
      )
    }

  /** Export data for a specific pet. */
  suspend fun exportForPet(petId: String): ExportBundle =
    withContext(Dispatchers.IO) {
      val pet = petRepository.getPetById(petId) ?: throw IllegalArgumentException("Pet not found")

      val weights = weightRepository.getWeightEntriesForPet(petId).first()
      val vaccinations = vaccinationRepository.getVaccinationEntriesForPet(petId).first()
      val dewormings = dewormingRepository.getDewormingEntriesForPet(petId).first()
      val tasks = taskRepository.getTasksForPet(petId).first()

      ExportBundle(
        metadata =
          ExportMetadata(
            appVersion = BuildConfig.VERSION_NAME,
            exportDate = Instant.now().toString(),
            schemaVersion = 1,
          ),
        pets = listOf(pet),
        weightEntries = weights,
        vaccinationEntries = vaccinations,
        dewormingEntries = dewormings,
        tasks = tasks,
      )
    }

  /** Write an export bundle to a URI. */
  suspend fun writeExportToUri(bundle: ExportBundle, uri: Uri) =
    withContext(Dispatchers.IO) {
      val json = bundle.toJson().toString(2)
      context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(json.toByteArray(Charsets.UTF_8))
      } ?: throw IllegalStateException("Cannot open output stream")
    }

  /** Read and parse a JSON file from a URI. */
  suspend fun readImportFromUri(uri: Uri): ExportBundle =
    withContext(Dispatchers.IO) {
      // Validate MIME type
      val mimeType = context.contentResolver.getType(uri)
      if (mimeType != null && mimeType !in ALLOWED_MIME_TYPES) {
        throw IllegalArgumentException("Tipo de arquivo inválido: $mimeType")
      }

      // Validate file size
      context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
        val fileSize = fd.statSize
        if (fileSize > MAX_IMPORT_SIZE_BYTES) {
          throw IllegalArgumentException(
            "Arquivo muito grande: máximo ${MAX_IMPORT_SIZE_BYTES / 1024 / 1024}MB"
          )
        }
      }

      val json =
        context.contentResolver.openInputStream(uri)?.use { stream ->
          stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Cannot read file")

      try {
        val bundle = ExportBundle.fromJson(JSONObject(json))
        val errors = ExportBundle.validate(bundle)
        if (errors.isNotEmpty()) {
          throw IllegalArgumentException("Dados inválidos: ${errors.first()}")
        }
        bundle
      } catch (e: JSONException) {
        throw IllegalArgumentException("Invalid JSON format: ${e.message}")
      } catch (e: IllegalArgumentException) {
        throw e
      } catch (e: Exception) {
        throw IllegalArgumentException("Error parsing file: ${e.message}")
      }
    }

  /** Analyze an import bundle for conflicts. */
  suspend fun analyzeImport(bundle: ExportBundle): ImportAnalysis =
    withContext(Dispatchers.IO) {
      val existingPets = petRepository.getAllPets().first()
      val existingPetIds = existingPets.map { it.id }.toSet()
      val existingPetNames = existingPets.associate { it.id to it.name }

      val conflictingPetNames =
        bundle.pets.filter { it.id in existingPetIds }.map { existingPetNames[it.id] ?: it.name }

      ImportAnalysis(
        totalPets = bundle.pets.size,
        totalWeightEntries = bundle.weightEntries.size,
        totalVaccinationEntries = bundle.vaccinationEntries.size,
        totalDewormingEntries = bundle.dewormingEntries.size,
        totalTasks = bundle.tasks.size,
        conflictingPetNames = conflictingPetNames,
        schemaVersion = bundle.metadata.schemaVersion,
        exportDate = bundle.metadata.exportDate,
      )
    }

  /** Import data from a bundle with the specified conflict resolution strategy. */
  suspend fun importData(
    bundle: ExportBundle,
    conflictResolution: ConflictResolution,
  ): MergeResult =
    withContext(Dispatchers.IO) {
      database.withTransaction { importDataWithinTransaction(bundle, conflictResolution) }
    }

  /** Applies a validated bundle inside a transaction already owned by the caller. */
  internal suspend fun importDataWithinTransaction(
    bundle: ExportBundle,
    conflictResolution: ConflictResolution,
  ): MergeResult {
    val validationErrors = ExportBundle.validate(bundle)
    require(validationErrors.isEmpty()) { "Dados inválidos: ${validationErrors.first()}" }

    return if (conflictResolution == ConflictResolution.REPLACE) {
      replaceShareableData(bundle)
    } else {
      mergeShareableData(bundle, conflictResolution == ConflictResolution.MERGE)
    }
  }

  private suspend fun mergeShareableData(
    bundle: ExportBundle,
    mergeExisting: Boolean,
  ): MergeResult {
    val resolver = ConflictResolver()
    val localPets = database.petDao().getAllIncludingDeleted().toDomain().associateBy { it.id }
    val localWeights =
      database.weightEntryDao().getAllIncludingDeleted().toDomain().associateBy { it.id }
    val localVaccinations =
      database.vaccinationEntryDao().getAllIncludingDeleted().toDomain().associateBy { it.id }
    val localDewormings =
      database.dewormingEntryDao().getAllIncludingDeleted().toDomain().associateBy { it.id }
    val localTasks = database.taskDao().getAllIncludingDeleted().toDomain().associateBy { it.id }
    val pets =
      applyVersions(
        resolver,
        bundle.pets.map { it.asConflictVersion() },
        { id -> localPets[id]?.asConflictVersion() },
        mergeExisting,
      )
    if (pets.values.isNotEmpty()) {
      database.petDao().insertPets(pets.values.map { it.toEntity() })
    }
    val weights =
      applyVersions(
        resolver,
        bundle.weightEntries.map { it.asConflictVersion() },
        { id -> localWeights[id]?.asConflictVersion() },
        mergeExisting,
      )
    if (weights.values.isNotEmpty()) {
      database.weightEntryDao().insertWeightEntries(weights.values.map { it.toEntity() })
    }
    val vaccinations =
      applyVersions(
        resolver,
        bundle.vaccinationEntries.map { it.asConflictVersion() },
        { id -> localVaccinations[id]?.asConflictVersion() },
        mergeExisting,
      )
    if (vaccinations.values.isNotEmpty()) {
      database
        .vaccinationEntryDao()
        .insertVaccinationEntries(vaccinations.values.map { it.toEntity() })
    }
    val dewormings =
      applyVersions(
        resolver,
        bundle.dewormingEntries.map { it.asConflictVersion() },
        { id -> localDewormings[id]?.asConflictVersion() },
        mergeExisting,
      )
    if (dewormings.values.isNotEmpty()) {
      database.dewormingEntryDao().insertDewormingEntries(dewormings.values.map { it.toEntity() })
    }
    val tasks =
      applyVersions(
        resolver,
        bundle.tasks.map { it.asConflictVersion() },
        { id -> localTasks[id]?.asConflictVersion() },
        mergeExisting,
      )
    if (tasks.values.isNotEmpty()) {
      database.taskDao().insertTasks(tasks.values.map { it.toEntity() })
    }

    return MergeResult(
      petsAdded = pets.added,
      petsUpdated = pets.updated,
      petsRemoved = pets.removed,
      weightsAdded = weights.added,
      weightsUpdated = weights.updated,
      weightsRemoved = weights.removed,
      vaccinationsAdded = vaccinations.added,
      vaccinationsUpdated = vaccinations.updated,
      vaccinationsRemoved = vaccinations.removed,
      dewormingsAdded = dewormings.added,
      dewormingsUpdated = dewormings.updated,
      dewormingsRemoved = dewormings.removed,
      tasksAdded = tasks.added,
      tasksUpdated = tasks.updated,
      tasksRemoved = tasks.removed,
      conflictsResolved =
        pets.conflicts +
          weights.conflicts +
          vaccinations.conflicts +
          dewormings.conflicts +
          tasks.conflicts,
    )
  }

  private data class AppliedCounts(
    var added: Int = 0,
    var updated: Int = 0,
    var removed: Int = 0,
    var conflicts: Int = 0,
  )

  private data class AppliedBatch<T>(val counts: AppliedCounts, val values: List<T>) {
    val added: Int
      get() = counts.added

    val updated: Int
      get() = counts.updated

    val removed: Int
      get() = counts.removed

    val conflicts: Int
      get() = counts.conflicts
  }

  private suspend fun <T> applyVersions(
    resolver: ConflictResolver,
    remoteVersions: List<ConflictVersion<T>>,
    getLocal: (String) -> ConflictVersion<T>?,
    mergeExisting: Boolean,
  ): AppliedBatch<T> {
    val counts = AppliedCounts()
    val selectedValues = mutableListOf<T>()
    for (remote in resolver.normalize(remoteVersions)) {
      val local = getLocal(remote.id)
      if (local != null && !mergeExisting) continue
      val result = resolver.resolve(local, remote)
      when (result.outcome) {
        ConflictOutcome.Inserted -> {
          selectedValues += result.selected.value
          if (result.selected.deletedAt == null) counts.added++ else counts.removed++
        }
        ConflictOutcome.RemoteWon -> {
          selectedValues += result.selected.value
          if (local?.deletedAt == null && result.selected.deletedAt != null) {
            counts.removed++
          } else {
            counts.updated++
          }
          counts.conflicts++
        }
        ConflictOutcome.LocalKept -> counts.conflicts++
        ConflictOutcome.Identical -> Unit
      }
    }
    return AppliedBatch(counts, selectedValues)
  }

  private suspend fun replaceShareableData(bundle: ExportBundle): MergeResult {
    val resolver = ConflictResolver()
    val incomingPets =
      resolver.normalize(bundle.pets.map { it.asConflictVersion() }).map { it.value }
    val incomingWeights =
      resolver.normalize(bundle.weightEntries.map { it.asConflictVersion() }).map { it.value }
    val incomingVaccinations =
      resolver.normalize(bundle.vaccinationEntries.map { it.asConflictVersion() }).map { it.value }
    val incomingDewormings =
      resolver.normalize(bundle.dewormingEntries.map { it.asConflictVersion() }).map { it.value }
    val incomingTasks =
      resolver.normalize(bundle.tasks.map { it.asConflictVersion() }).map { it.value }
    val existingPets = database.petDao().getAllIncludingDeleted().toDomain()
    val existingWeights = database.weightEntryDao().getAllIncludingDeleted().toDomain()
    val existingVaccinations = database.vaccinationEntryDao().getAllIncludingDeleted().toDomain()
    val existingDewormings = database.dewormingEntryDao().getAllIncludingDeleted().toDomain()
    val existingTasks = database.taskDao().getAllIncludingDeleted().toDomain()

    database.taskDao().deleteAll()
    database.dewormingEntryDao().deleteAll()
    database.vaccinationEntryDao().deleteAll()
    database.weightEntryDao().deleteAll()
    database.petDao().deleteAll()

    if (incomingPets.isNotEmpty()) database.petDao().insertPets(incomingPets.map { it.toEntity() })
    if (incomingWeights.isNotEmpty()) {
      database.weightEntryDao().insertWeightEntries(incomingWeights.map { it.toEntity() })
    }
    if (incomingVaccinations.isNotEmpty()) {
      database
        .vaccinationEntryDao()
        .insertVaccinationEntries(incomingVaccinations.map { it.toEntity() })
    }
    if (incomingDewormings.isNotEmpty()) {
      database.dewormingEntryDao().insertDewormingEntries(incomingDewormings.map { it.toEntity() })
    }
    if (incomingTasks.isNotEmpty()) {
      database.taskDao().insertTasks(incomingTasks.map { it.toEntity() })
    }

    val petCounts = replacementCounts(existingPets.map { it.id }, incomingPets.map { it.id })
    val weightCounts =
      replacementCounts(existingWeights.map { it.id }, incomingWeights.map { it.id })
    val vaccinationCounts =
      replacementCounts(existingVaccinations.map { it.id }, incomingVaccinations.map { it.id })
    val dewormingCounts =
      replacementCounts(existingDewormings.map { it.id }, incomingDewormings.map { it.id })
    val taskCounts = replacementCounts(existingTasks.map { it.id }, incomingTasks.map { it.id })

    return MergeResult(
      petsAdded = petCounts.added,
      petsUpdated = petCounts.updated,
      petsRemoved = petCounts.removed,
      weightsAdded = weightCounts.added,
      weightsUpdated = weightCounts.updated,
      weightsRemoved = weightCounts.removed,
      vaccinationsAdded = vaccinationCounts.added,
      vaccinationsUpdated = vaccinationCounts.updated,
      vaccinationsRemoved = vaccinationCounts.removed,
      dewormingsAdded = dewormingCounts.added,
      dewormingsUpdated = dewormingCounts.updated,
      dewormingsRemoved = dewormingCounts.removed,
      tasksAdded = taskCounts.added,
      tasksUpdated = taskCounts.updated,
      tasksRemoved = taskCounts.removed,
      conflictsResolved =
        petCounts.updated +
          weightCounts.updated +
          vaccinationCounts.updated +
          dewormingCounts.updated +
          taskCounts.updated,
    )
  }

  private data class ReplacementCounts(val added: Int, val updated: Int, val removed: Int)

  private fun replacementCounts(existingIds: List<String>, incomingIds: List<String>) =
    ReplacementCounts(
      added = (incomingIds.toSet() - existingIds.toSet()).size,
      updated = (incomingIds.toSet() intersect existingIds.toSet()).size,
      removed = (existingIds.toSet() - incomingIds.toSet()).size,
    )
}
