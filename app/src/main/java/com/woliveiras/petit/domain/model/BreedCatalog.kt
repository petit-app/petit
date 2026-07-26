package com.woliveiras.petit.domain.model

import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

data class BreedCatalogMetadata(
  val vboRelease: String,
  val vboUrl: String,
  val reviewedAt: String,
  val license: String,
  val licenseUrl: String,
  val sources: List<BreedCatalogSource>,
)

data class BreedCatalogSource(val authority: String, val url: String, val reviewedAt: String)

data class BreedRegistry(
  val authority: String,
  val code: String?,
  val status: String,
  val sourceUrl: String,
)

data class BreedCatalogEntry(
  val id: String,
  val species: PetType,
  val canonicalName: String,
  val displayNames: Map<String, String>,
  val aliases: List<String>,
  val registries: List<BreedRegistry>,
) {
  fun localized(localeTag: String): BreedCatalogItem {
    val locale = Locale.forLanguageTag(localeTag)
    val displayName =
      displayNames[localeTag]
        ?: displayNames.entries
          .firstOrNull { it.key.equals(locale.language, ignoreCase = true) }
          ?.value
        ?: (if (locale.language == "pt") displayNames["pt-BR"] else displayNames["en"])
        ?: canonicalName
    return BreedCatalogItem(id = id, displayName = displayName, canonicalName = canonicalName)
  }
}

data class BreedCatalogItem(val id: String, val displayName: String, val canonicalName: String)

class BreedCatalog
private constructor(
  val metadata: BreedCatalogMetadata?,
  private val entries: List<BreedCatalogEntry>,
) {
  fun search(species: PetType, localeTag: String, query: String): List<BreedCatalogItem> {
    if (species != PetType.CAT && species != PetType.DOG) return emptyList()

    val normalizedQuery = query.normalizedForSearch()
    val locale = Locale.forLanguageTag(localeTag)
    val collator =
      Collator.getInstance(locale).apply {
        strength = Collator.PRIMARY
        decomposition = Collator.CANONICAL_DECOMPOSITION
      }

    return entries
      .asSequence()
      .filter { it.species == species }
      .filter { entry ->
        normalizedQuery.isEmpty() ||
          buildList {
              add(entry.canonicalName)
              addAll(entry.displayNames.values)
              addAll(entry.aliases)
            }
            .any { it.normalizedForSearch().contains(normalizedQuery) }
      }
      .map { it.localized(localeTag) }
      .sortedWith(
        Comparator<BreedCatalogItem> { left, right ->
            collator.compare(left.displayName, right.displayName)
          }
          .thenBy { it.id }
      )
      .toList()
  }

  fun resolve(id: String, localeTag: String): BreedCatalogItem? =
    entries.firstOrNull { it.id == id }?.localized(localeTag)

  companion object {
    private const val SUPPORTED_SCHEMA_VERSION = 1
    private val ID_PATTERN = Regex("""VBO:\d{7}""")

    fun fromJson(json: String): BreedCatalog {
      val document =
        try {
          Gson().fromJson(json, CatalogDocument::class.java)
        } catch (exception: JsonParseException) {
          throw IllegalArgumentException("Invalid breed catalog JSON", exception)
        }
      require(document.schemaVersion == SUPPORTED_SCHEMA_VERSION) {
        "Unsupported breed catalog schema ${document.schemaVersion}"
      }
      require(document.vboRelease.isNotBlank())
      require(document.vboUrl.isNotBlank())
      require(document.reviewedAt.isNotBlank())
      require(document.license.isNotBlank())
      require(document.licenseUrl.isNotBlank())

      val parsedEntries =
        document.entries.map { entry ->
          require(ID_PATTERN.matches(entry.id)) { "Invalid breed id ${entry.id}" }
          val species =
            runCatching { PetType.valueOf(entry.species) }
              .getOrElse { throw IllegalArgumentException("Invalid species ${entry.species}", it) }
          require(species == PetType.CAT || species == PetType.DOG)
          require(entry.canonicalName.isNotBlank())
          require(entry.displayNames["en"].orEmpty().isNotBlank())
          require(entry.displayNames["pt-BR"].orEmpty().isNotBlank())
          BreedCatalogEntry(
            id = entry.id,
            species = species,
            canonicalName = entry.canonicalName,
            displayNames = entry.displayNames,
            aliases = entry.aliases,
            registries =
              entry.registries.map {
                BreedRegistry(
                  authority = it.authority,
                  code = it.code,
                  status = it.status,
                  sourceUrl = it.sourceUrl,
                )
              },
          )
        }
      require(parsedEntries.map { it.id }.distinct().size == parsedEntries.size) {
        "Duplicate breed ids"
      }

      return BreedCatalog(
        metadata =
          BreedCatalogMetadata(
            vboRelease = document.vboRelease,
            vboUrl = document.vboUrl,
            reviewedAt = document.reviewedAt,
            license = document.license,
            licenseUrl = document.licenseUrl,
            sources =
              document.sources.map {
                BreedCatalogSource(
                  authority = it.authority,
                  url = it.url,
                  reviewedAt = it.reviewedAt,
                )
              },
          ),
        entries = parsedEntries,
      )
    }

    fun fromJsonOrEmpty(json: String): BreedCatalog =
      runCatching { fromJson(json) }
        .getOrElse { BreedCatalog(metadata = null, entries = emptyList()) }
  }
}

private fun String.normalizedForSearch(): String =
  Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("""\p{M}+"""), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
    .trim()

private data class CatalogDocument(
  val schemaVersion: Int = 0,
  val vboRelease: String = "",
  val vboUrl: String = "",
  val reviewedAt: String = "",
  val license: String = "",
  val licenseUrl: String = "",
  val sources: List<CatalogSourceDocument> = emptyList(),
  val entries: List<CatalogEntryDocument> = emptyList(),
)

private data class CatalogSourceDocument(
  val authority: String = "",
  val url: String = "",
  val reviewedAt: String = "",
)

private data class CatalogEntryDocument(
  val id: String = "",
  val species: String = "",
  val canonicalName: String = "",
  val displayNames: Map<String, String> = emptyMap(),
  val aliases: List<String> = emptyList(),
  val registries: List<RegistryDocument> = emptyList(),
)

private data class RegistryDocument(
  val authority: String = "",
  val code: String? = null,
  val status: String = "",
  val sourceUrl: String = "",
)
