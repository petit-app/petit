package com.woliveiras.petit.domain.model

object BreedIdentity {
  const val MIXED_BREED_ID = "PETIT:MIXED_BREED"
  const val UNKNOWN_BREED_ID = "PETIT:UNKNOWN_BREED"

  private val legacyIds =
    mapOf(
      PetType.CAT to
        mapOf(
          "PERSIAN" to "VBO:0100188",
          "SIAMESE" to "VBO:0100221",
          "MAINE_COON" to "VBO:0100154",
          "RAGDOLL" to "VBO:0100196",
          "BRITISH_SHORTHAIR" to "VBO:0100052",
          "BENGAL" to "VBO:0100040",
          "ABYSSINIAN" to "VBO:0100000",
          "SPHYNX" to "VBO:0100230",
          "SCOTTISH_FOLD" to "VBO:0100209",
          "BURMESE" to "VBO:0100053",
          "RUSSIAN_BLUE" to "VBO:0100200",
          "NORWEGIAN_FOREST" to "VBO:0100178",
          "TURKISH_ANGORA" to "VBO:0100249",
          "MIXED_BREED" to MIXED_BREED_ID,
        ),
      PetType.DOG to
        mapOf(
          "LABRADOR" to "VBO:0200800",
          "GOLDEN_RETRIEVER" to "VBO:0200610",
          "GERMAN_SHEPHERD" to "VBO:0200577",
          "POODLE" to "VBO:0201048",
          "BULLDOG" to "VBO:0200258",
          "BEAGLE" to "VBO:0200131",
          "SHIH_TZU" to "VBO:0201223",
          "YORKSHIRE" to "VBO:0201448",
          "MIXED_BREED" to MIXED_BREED_ID,
        ),
    )

  fun legacyId(species: PetType, legacyBreed: String): String? =
    legacyIds[species]?.get(legacyBreed)

  fun isPetitCategory(id: String): Boolean = id == MIXED_BREED_ID || id == UNKNOWN_BREED_ID
}
