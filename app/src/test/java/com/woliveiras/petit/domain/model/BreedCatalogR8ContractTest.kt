package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import com.google.gson.annotations.SerializedName
import org.junit.Test

class BreedCatalogR8ContractTest {

  @Test
  fun `catalog document fields declare stable serialized names`() {
    EXPECTED_FIELDS.forEach { (className, expectedFields) ->
      val serializedFields =
        Class.forName("$PACKAGE_NAME.$className").declaredFields.associate { field ->
          field.name to field.getAnnotation(SerializedName::class.java)?.value
        }

      assertThat(serializedFields).containsExactlyEntriesIn(expectedFields)
    }
  }

  companion object {
    private const val PACKAGE_NAME = "com.woliveiras.petit.domain.model"

    private val EXPECTED_FIELDS =
      mapOf(
        "CatalogDocument" to
          listOf(
              "schemaVersion",
              "vboRelease",
              "vboUrl",
              "reviewedAt",
              "license",
              "licenseUrl",
              "sources",
              "entries",
            )
            .associateWith { it },
        "CatalogSourceDocument" to listOf("authority", "url", "reviewedAt").associateWith { it },
        "CatalogEntryDocument" to
          listOf("id", "species", "canonicalName", "displayNames", "aliases", "registries")
            .associateWith { it },
        "RegistryDocument" to
          listOf("authority", "code", "status", "sourceUrl").associateWith { it },
      )
  }
}
