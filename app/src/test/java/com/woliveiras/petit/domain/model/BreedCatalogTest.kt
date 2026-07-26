package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BreedCatalogTest {

  @Test
  fun `search matches locale name aliases and accents within one species`() {
    val catalog = BreedCatalog.fromJson(FIXTURE)

    assertThat(catalog.search(PetType.DOG, "pt-BR", "pastor alemao").map { it.id })
      .containsExactly("VBO:0200577")
    assertThat(catalog.search(PetType.DOG, "en", "alsatian").map { it.id })
      .containsExactly("VBO:0200577")
    assertThat(catalog.search(PetType.CAT, "pt-BR", "pastor")).isEmpty()
  }

  @Test
  fun `search orders by locale display name and preserves ambiguous aliases`() {
    val catalog = BreedCatalog.fromJson(FIXTURE)

    assertThat(catalog.search(PetType.CAT, "pt-BR", "").map { it.displayName })
      .containsExactly("Abissínio", "Siamês")
      .inOrder()
    assertThat(catalog.search(PetType.CAT, "en", "oriental").map { it.id })
      .containsExactly("VBO:0100000", "VBO:0100221")
      .inOrder()
  }

  @Test
  fun `resolve uses locale and unknown id falls back to caller`() {
    val catalog = BreedCatalog.fromJson(FIXTURE)

    assertThat(catalog.resolve("VBO:0200577", "pt-BR")?.displayName).isEqualTo("Pastor-alemão")
    assertThat(catalog.resolve("VBO:9999999", "pt-BR")).isNull()
  }

  @Test
  fun `malformed asset yields manual-safe empty catalog`() {
    val catalog = BreedCatalog.fromJsonOrEmpty("""{"schemaVersion":99}""")

    assertThat(catalog.search(PetType.DOG, "en", "")).isEmpty()
    assertThat(catalog.metadata).isNull()
  }

  @Test
  fun `legacy mapping is exact and species compatible`() {
    assertThat(BreedIdentity.legacyId(PetType.DOG, "GERMAN_SHEPHERD")).isEqualTo("VBO:0200577")
    assertThat(BreedIdentity.legacyId(PetType.CAT, "GERMAN_SHEPHERD")).isNull()
    assertThat(BreedIdentity.legacyId(PetType.DOG, "German Shepherd")).isNull()
    assertThat(BreedIdentity.legacyId(PetType.CAT, "MIXED_BREED"))
      .isEqualTo(BreedIdentity.MIXED_BREED_ID)
  }

  @Test
  fun `Petit categories remain separate from registry breeds`() {
    assertThat(BreedIdentity.isPetitCategory(BreedIdentity.MIXED_BREED_ID)).isTrue()
    assertThat(BreedIdentity.isPetitCategory(BreedIdentity.UNKNOWN_BREED_ID)).isTrue()
    assertThat(BreedIdentity.isPetitCategory("VBO:0200577")).isFalse()
  }

  companion object {
    private val FIXTURE =
      """
      {
        "schemaVersion": 1,
        "vboRelease": "2026-04-15",
        "vboUrl": "https://example.test/vbo.json",
        "vboSha256": "abc",
        "reviewedAt": "2026-07-26",
        "license": "CC BY 4.0",
        "licenseUrl": "https://creativecommons.org/licenses/by/4.0/",
        "sources": [],
        "entries": [
          {
            "id": "VBO:0100221",
            "species": "CAT",
            "canonicalName": "Siamese",
            "displayNames": {"en": "Siamese", "pt-BR": "Siamês"},
            "aliases": ["Oriental"],
            "registries": [{"authority": "FIFe", "status": "FULLY_RECOGNIZED", "sourceUrl": "https://fifeweb.org/cats/breeds/"}]
          },
          {
            "id": "VBO:0200577",
            "species": "DOG",
            "canonicalName": "German Shepherd Dog",
            "displayNames": {"en": "German Shepherd Dog", "pt-BR": "Pastor-alemão"},
            "aliases": ["Alsatian"],
            "registries": [{"authority": "FCI", "code": "166", "status": "FULLY_RECOGNIZED", "sourceUrl": "https://fci.be/"}]
          },
          {
            "id": "VBO:0100000",
            "species": "CAT",
            "canonicalName": "Abyssinian",
            "displayNames": {"en": "Abyssinian", "pt-BR": "Abissínio"},
            "aliases": ["Oriental"],
            "registries": [{"authority": "TICA", "status": "FULLY_RECOGNIZED", "sourceUrl": "https://tica.org/"}]
          }
        ]
      }
      """
        .trimIndent()
  }
}
