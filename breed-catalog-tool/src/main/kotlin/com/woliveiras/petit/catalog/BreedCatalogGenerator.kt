package com.woliveiras.petit.catalog

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.Normalizer

data class SourceManifest(
  val vboRelease: String,
  val vboUrl: String,
  val vboSha256: String,
  val reviewedAt: String,
  val selectedSourceSha256: String? = null,
)

object BreedCatalogGenerator {
  private const val ID_PREFIX = "http://purl.obolibrary.org/obo/VBO_"
  private const val STATUS_PROPERTY = "http://purl.obolibrary.org/obo/vbo#breed_recognition_status"
  private const val SOURCE_PROPERTY = "http://purl.org/dc/terms/source"
  private const val BREED_CODE_PROPERTY = "http://purl.obolibrary.org/obo/vbo#breed_code"
  private const val FULLY_RECOGNIZED = "http://purl.obolibrary.org/obo/VBO_0300002"
  private const val PARTIALLY_RECOGNIZED = "http://purl.obolibrary.org/obo/VBO_0300003"
  private val idPattern = Regex("VBO:[0-9]{7}")

  fun generate(
    sourceJson: String,
    manifest: SourceManifest,
    ptBrOverrides: Map<String, String> = emptyMap(),
  ): String {
    require(manifest.vboRelease.isNotBlank()) { "Missing VBO release" }
    require(manifest.vboUrl.isNotBlank()) { "Missing VBO source URL" }
    require(manifest.vboSha256.isNotBlank()) { "Missing VBO checksum" }
    require(manifest.reviewedAt.isNotBlank()) { "Missing source review date" }

    val graph =
      JsonParser.parseString(sourceJson)
        .asJsonObject
        .requiredArray("graphs")
        .firstOrNull()
        ?.asJsonObject ?: throw IllegalArgumentException("VBO source has no graph")
    val candidates =
      graph.requiredArray("nodes").mapNotNull { element ->
        element.asJsonObject.toCandidateOrNull()
      }
    val duplicateIds = candidates.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
    require(duplicateIds.isEmpty()) { "Duplicate VBO IDs: ${duplicateIds.sorted().joinToString()}" }

    val recognized = candidates.filter { it.registries.isNotEmpty() }
    val cats = recognized.filter { it.species == Species.CAT }
    val dogs =
      recognized
        .filter { it.species == Species.DOG }
        .groupBy { requireNotNull(it.fciCode) }
        .values
        .map { sameFciCode ->
          sameFciCode.minWith(
            compareBy<Candidate>(
              { it.name.startsWith("obsolete ", ignoreCase = true) },
              { ',' in it.name },
              { it.name.length },
              { it.id },
            )
          )
        }
    val included = cats + dogs

    require(included.size <= 1_000) { "Catalog exceeds 1000 entries" }
    val entries =
      included
        .sortedBy { it.id }
        .map { candidate ->
          validateIncluded(candidate)
          val ptBrName = ptBrOverrides[candidate.id]?.trim().orEmpty().ifBlank { candidate.name }
          require(ptBrName.isNotBlank()) { "Missing pt-BR display name for ${candidate.id}" }
          candidate.toJson(ptBrName)
        }

    val root =
      JsonObject().apply {
        addProperty("schemaVersion", 1)
        addProperty("vboRelease", manifest.vboRelease)
        addProperty("vboUrl", manifest.vboUrl)
        addProperty("vboSha256", manifest.vboSha256)
        manifest.selectedSourceSha256?.let { addProperty("selectedSourceSha256", it) }
        addProperty("reviewedAt", manifest.reviewedAt)
        addProperty("license", "CC BY 4.0")
        addProperty("licenseUrl", "https://creativecommons.org/licenses/by/4.0/")
        add(
          "sources",
          JsonArray().apply {
            add(source("FCI", "https://fci.be/en/Nomenclature/", manifest.reviewedAt))
            add(source("FIFe", "https://fifeweb.org/cats/breeds/", manifest.reviewedAt))
            add(source("TICA", "https://tica.org/breed-standards/", manifest.reviewedAt))
          },
        )
        add("entries", JsonArray().apply { entries.forEach(::add) })
      }
    return Gson().newBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root) +
      "\n"
  }

  private fun JsonObject.toCandidateOrNull(): Candidate? {
    val label = optionalString("lbl") ?: return null
    val species =
      when {
        label.endsWith("(Cat)") -> Species.CAT
        label.endsWith("(Dog)") -> Species.DOG
        else -> return null
      }
    val iri = optionalString("id") ?: return null
    val id =
      if (iri.startsWith(ID_PREFIX)) "VBO:${iri.removePrefix(ID_PREFIX)}"
      else iri.substringAfterLast('/').replace('_', ':')
    val meta = getAsJsonObject("meta") ?: JsonObject()
    val properties = meta.optionalArray("basicPropertyValues").elements().map { it.asJsonObject }
    val status =
      properties
        .firstOrNull { it.optionalString("pred") == STATUS_PROPERTY }
        ?.optionalString("val")
        ?.toRecognitionStatus() ?: return null
    if (status == RecognitionStatus.NOT_RECOGNIZED) return null
    val sources =
      properties
        .filter { it.optionalString("pred") == SOURCE_PROPERTY }
        .mapNotNull { it.optionalString("val") }
        .distinct()
        .sorted()
    val xrefs =
      meta.optionalArray("xrefs").elements().mapNotNull { it.asJsonObject.optionalString("val") }
    val fciCode = xrefs.firstOrNull { it.startsWith("FCI:") }?.substringAfter("FCI:")
    val breedCodes =
      properties
        .filter { it.optionalString("pred") == BREED_CODE_PROPERTY }
        .mapNotNull { it.optionalString("val") }
        .distinct()
        .sorted()
    val registries = buildRegistries(species, status, sources, fciCode, breedCodes)
    val canonicalName =
      label.removeSuffix(" (Cat)").removeSuffix(" (Dog)").trim().also {
        require(it.isNotBlank()) { "Missing canonical name for $id" }
      }
    val aliases =
      meta
        .optionalArray("synonyms")
        .elements()
        .mapNotNull { it.asJsonObject.optionalString("val")?.trim() }
        .filter { it.isNotBlank() && !it.equals(canonicalName, ignoreCase = true) }
        .distinctBy(::normalized)
        .sorted()
    return Candidate(
      id = id,
      species = species,
      name = canonicalName,
      aliases = aliases,
      registries = registries,
      fciCode = fciCode,
    )
  }

  private fun buildRegistries(
    species: Species,
    status: RecognitionStatus,
    sources: List<String>,
    fciCode: String?,
    breedCodes: List<String>,
  ): List<Registry> =
    when (species) {
      Species.DOG ->
        sources
          .firstOrNull { "fci.be" in it }
          ?.takeIf { fciCode != null }
          ?.let { listOf(Registry("FCI", fciCode, status, it)) }
          .orEmpty()
      Species.CAT ->
        buildList {
            sources
              .firstOrNull { "fifeweb.org" in it }
              ?.let { add(Registry("FIFe", breedCodes.singleOrNull(), status, it)) }
            sources
              .firstOrNull { "tica.org" in it }
              ?.let { add(Registry("TICA", null, status, it)) }
          }
          .sortedBy { it.authority }
    }

  private fun validateIncluded(candidate: Candidate) {
    require(idPattern.matches(candidate.id)) { "Malformed VBO ID: ${candidate.id}" }
    require(candidate.registries.isNotEmpty()) { "Missing registry provenance for ${candidate.id}" }
    candidate.registries.forEach { registry ->
      require(registry.authority in setOf("FCI", "FIFe", "TICA")) {
        "Unsupported authority ${registry.authority}"
      }
      require(registry.sourceUrl.startsWith("https://")) {
        "Invalid registry source for ${candidate.id}"
      }
    }
  }

  private fun Candidate.toJson(ptBrName: String) =
    JsonObject().apply {
      addProperty("id", id)
      addProperty("species", species.name)
      addProperty("canonicalName", name)
      add(
        "displayNames",
        JsonObject().apply {
          addProperty("en", name)
          addProperty("pt-BR", ptBrName)
        },
      )
      add("aliases", JsonArray().apply { aliases.forEach(::add) })
      add(
        "registries",
        JsonArray().apply {
          registries.forEach { registry ->
            add(
              JsonObject().apply {
                addProperty("authority", registry.authority)
                registry.code?.let { addProperty("code", it) }
                addProperty("status", registry.status.serialized)
                addProperty("sourceUrl", registry.sourceUrl)
              }
            )
          }
        },
      )
    }

  private fun source(authority: String, url: String, reviewedAt: String) =
    JsonObject().apply {
      addProperty("authority", authority)
      addProperty("url", url)
      addProperty("reviewedAt", reviewedAt)
    }

  private fun String.toRecognitionStatus() =
    when (this) {
      FULLY_RECOGNIZED -> RecognitionStatus.FULLY_RECOGNIZED
      PARTIALLY_RECOGNIZED -> RecognitionStatus.PARTIALLY_RECOGNIZED
      else -> RecognitionStatus.NOT_RECOGNIZED
    }

  private fun normalized(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

  private fun JsonObject.requiredArray(name: String): JsonArray =
    getAsJsonArray(name) ?: throw IllegalArgumentException("Missing array: $name")

  private fun JsonObject.optionalArray(name: String): JsonArray? =
    get(name)?.takeIf { it.isJsonArray }?.asJsonArray

  private fun JsonArray?.elements() = this?.toList().orEmpty()

  private fun JsonObject.optionalString(name: String): String? =
    get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

  private enum class Species {
    CAT,
    DOG,
  }

  private enum class RecognitionStatus(val serialized: String) {
    FULLY_RECOGNIZED("FULLY_RECOGNIZED"),
    PARTIALLY_RECOGNIZED("PARTIALLY_RECOGNIZED"),
    NOT_RECOGNIZED("NOT_RECOGNIZED"),
  }

  private data class Registry(
    val authority: String,
    val code: String?,
    val status: RecognitionStatus,
    val sourceUrl: String,
  )

  private data class Candidate(
    val id: String,
    val species: Species,
    val name: String,
    val aliases: List<String>,
    val registries: List<Registry>,
    val fciCode: String?,
  )
}
