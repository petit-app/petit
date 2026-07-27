package com.woliveiras.petit.catalog

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import org.junit.Assert.assertThrows
import org.junit.Test

class BreedCatalogGeneratorTest {

  @Test
  fun `generate includes configured recognition and is deterministic`() {
    val first = BreedCatalogGenerator.generate(SOURCE_FIXTURE, MANIFEST, PT_BR_OVERRIDES)
    val second = BreedCatalogGenerator.generate(SOURCE_FIXTURE, MANIFEST, PT_BR_OVERRIDES)

    assertThat(second).isEqualTo(first)

    val root = JsonParser.parseString(first).asJsonObject
    assertThat(root["vboRelease"].asString).isEqualTo("2026-04-15")
    assertThat(root["vboSha256"].asString).isEqualTo("full-vbo-sha")
    assertThat(root["license"].asString).isEqualTo("CC BY 4.0")

    val entries = root["entries"].asJsonArray
    assertThat(entries).hasSize(4)
    assertThat(entries.map { it.asJsonObject["id"].asString })
      .containsExactly("VBO:0100000", "VBO:0100096", "VBO:0200038", "VBO:0200338")
      .inOrder()

    val chihuahua = entries.last().asJsonObject
    assertThat(chihuahua["displayNames"].asJsonObject["pt-BR"].asString).isEqualTo("Chihuahua")
    assertThat(chihuahua["registries"].asJsonArray.single().asJsonObject["authority"].asString)
      .isEqualTo("FCI")

    val abyssinian = entries.first().asJsonObject
    assertThat(abyssinian["displayNames"].asJsonObject["pt-BR"].asString).isEqualTo("Abissínio")
    assertThat(abyssinian["registries"].asJsonArray.map { it.asJsonObject["authority"].asString })
      .containsExactly("FIFe", "TICA")
      .inOrder()
  }

  @Test
  fun `generate excludes unrecognized terms and FCI varieties`() {
    val output = JsonParser.parseString(BreedCatalogGenerator.generate(SOURCE_FIXTURE, MANIFEST))
    val ids = output.asJsonObject["entries"].asJsonArray.map { it.asJsonObject["id"].asString }

    assertThat(ids).containsNoneOf("VBO:0200002", "VBO:0200040", "VBO:0100002")
  }

  @Test
  fun `generate rejects malformed included identifiers`() {
    val malformed =
      SOURCE_FIXTURE.replace(
        "http://purl.obolibrary.org/obo/VBO_0200338",
        "http://purl.obolibrary.org/obo/VBO_BAD",
      )

    val error =
      assertThrows(IllegalArgumentException::class.java) {
        BreedCatalogGenerator.generate(malformed, MANIFEST)
      }

    assertThat(error).hasMessageThat().contains("Malformed VBO ID")
  }

  companion object {
    private val MANIFEST =
      SourceManifest(
        vboRelease = "2026-04-15",
        vboUrl = "https://purl.obolibrary.org/obo/vbo/releases/2026-04-15/vbo.json",
        vboSha256 = "full-vbo-sha",
        reviewedAt = "2026-07-26",
      )

    private val PT_BR_OVERRIDES = mapOf("VBO:0100000" to "Abissínio")

    private val SOURCE_FIXTURE =
      """
      {
        "graphs": [{
          "nodes": [
            {
              "id": "http://purl.obolibrary.org/obo/VBO_0200338",
              "lbl": "Chihuahua (Dog)",
              "meta": {
                "synonyms": [{"pred": "hasExactSynonym", "val": "Chihuahueño"}],
                "xrefs": [{"val": "FCI:218"}],
                "basicPropertyValues": [
                  {"pred": "http://purl.obolibrary.org/obo/vbo#breed_recognition_status", "val": "http://purl.obolibrary.org/obo/VBO_0300002"},
                  {"pred": "http://purl.org/dc/terms/source", "val": "https://www.fci.be/en/nomenclature/CHIHUAHUA-218.html"}
                ]
              }
            },
            {
              "id": "http://purl.obolibrary.org/obo/VBO_0200038",
              "lbl": "American Cocker Spaniel (Dog)",
              "meta": {
                "xrefs": [{"val": "FCI:167"}],
                "basicPropertyValues": [
                  {"pred": "http://purl.obolibrary.org/obo/vbo#breed_recognition_status", "val": "http://purl.obolibrary.org/obo/VBO_0300002"},
                  {"pred": "http://purl.org/dc/terms/source", "val": "https://www.fci.be/en/nomenclature/AMERICAN-COCKER-SPANIEL-167.html"}
                ]
              }
            },
            {
              "id": "http://purl.obolibrary.org/obo/VBO_0200040",
              "lbl": "American Cocker Spaniel, Black (Dog)",
              "meta": {
                "xrefs": [{"val": "FCI:167"}],
                "basicPropertyValues": [
                  {"pred": "http://purl.obolibrary.org/obo/vbo#breed_recognition_status", "val": "http://purl.obolibrary.org/obo/VBO_0300002"},
                  {"pred": "http://purl.org/dc/terms/source", "val": "https://www.fci.be/en/nomenclature/AMERICAN-COCKER-SPANIEL-167.html"}
                ]
              }
            },
            {
              "id": "http://purl.obolibrary.org/obo/VBO_0200002",
              "lbl": "American Bulldog (Dog)",
              "meta": {
                "basicPropertyValues": [
                  {"pred": "http://purl.obolibrary.org/obo/vbo#breed_recognition_status", "val": "http://purl.obolibrary.org/obo/VBO_0300002"},
                  {"pred": "http://purl.org/dc/terms/source", "val": "https://www.akc.org/dog-breeds/american-bulldog/"}
                ]
              }
            },
            {
              "id": "http://purl.obolibrary.org/obo/VBO_0100000",
              "lbl": "Abyssinian (Cat)",
              "meta": {
                "synonyms": [{"pred": "hasExactSynonym", "val": "Abyssinian"}],
                "basicPropertyValues": [
                  {"pred": "http://purl.obolibrary.org/obo/vbo#breed_recognition_status", "val": "http://purl.obolibrary.org/obo/VBO_0300002"},
                  {"pred": "http://purl.org/dc/terms/source", "val": "https://fifeweb.org/cats/breeds/"},
                  {"pred": "http://purl.org/dc/terms/source", "val": "https://www.tica.org/breeds/browse-all-breeds"}
                ]
              }
            },
            {
              "id": "http://purl.obolibrary.org/obo/VBO_0100096",
              "lbl": "Toybob (Cat)",
              "meta": {
                "basicPropertyValues": [
                  {"pred": "http://purl.obolibrary.org/obo/vbo#breed_recognition_status", "val": "http://purl.obolibrary.org/obo/VBO_0300003"},
                  {"pred": "http://purl.org/dc/terms/source", "val": "https://www.tica.org/breeds/browse-all-breeds"}
                ]
              }
            },
            {
              "id": "http://purl.obolibrary.org/obo/VBO_0100002",
              "lbl": "Imaginary Cat (Cat)",
              "meta": {
                "basicPropertyValues": [
                  {"pred": "http://purl.obolibrary.org/obo/vbo#breed_recognition_status", "val": "http://purl.obolibrary.org/obo/VBO_0300004"},
                  {"pred": "http://purl.org/dc/terms/source", "val": "https://www.tica.org/breeds/browse-all-breeds"}
                ]
              }
            }
          ],
          "edges": [
            {
              "sub": "http://purl.obolibrary.org/obo/VBO_0200040",
              "pred": "is_a",
              "obj": "http://purl.obolibrary.org/obo/VBO_0200038"
            }
          ]
        }]
      }
      """
        .trimIndent()
  }
}
