package com.woliveiras.petit.domain.model

/** A pure, static catalog of advisory record-entry choices for each supported species. */
object SpeciesCareCatalog {

  fun breedPresets(petType: PetType): List<BreedPreset> =
    when (petType) {
      PetType.CAT -> catBreedPresets
      PetType.DOG -> dogBreedPresets
      PetType.RABBIT -> manualBreedPresets
      PetType.BIRD -> manualBreedPresets
      PetType.HAMSTER -> manualBreedPresets
      PetType.OTHER -> manualBreedPresets
    }

  fun vaccinePresets(petType: PetType): List<VaccinePreset> =
    when (petType) {
      PetType.CAT -> catVaccinePresets
      PetType.DOG -> dogVaccinePresets
      PetType.RABBIT -> rabbitVaccinePresets
      PetType.BIRD -> birdVaccinePresets
      PetType.HAMSTER -> manualVaccinePresets
      PetType.OTHER -> manualVaccinePresets
    }

  fun antiparasiticOptions(petType: PetType, type: DewormingType): AntiparasiticOptions =
    when (petType) {
      PetType.CAT -> manualAntiparasiticOptions(type)
      PetType.DOG -> manualAntiparasiticOptions(type)
      PetType.RABBIT -> manualAntiparasiticOptions(type)
      PetType.BIRD -> manualAntiparasiticOptions(type)
      PetType.HAMSTER -> manualAntiparasiticOptions(type)
      PetType.OTHER -> manualAntiparasiticOptions(type)
    }

  private fun manualAntiparasiticOptions(type: DewormingType): AntiparasiticOptions =
    when (type) {
      DewormingType.INTERNAL -> AntiparasiticOptions.manualOnly()
      DewormingType.EXTERNAL -> AntiparasiticOptions.manualOnly()
      DewormingType.BOTH -> AntiparasiticOptions.manualOnly()
    }

  private val catBreedPresets =
    listOf(
      breed("MIXED_BREED", "breed_mixed"),
      breed("PERSIAN", "breed_persian"),
      breed("SIAMESE", "breed_siamese"),
      breed("MAINE_COON", "breed_maine_coon"),
      breed("RAGDOLL", "breed_ragdoll"),
      breed("BRITISH_SHORTHAIR", "breed_british_shorthair"),
      breed("BENGAL", "breed_bengal"),
      breed("ABYSSINIAN", "breed_abyssinian"),
      breed("SPHYNX", "breed_sphynx"),
      breed("SCOTTISH_FOLD", "breed_scottish_fold"),
      breed("BURMESE", "breed_burmese"),
      breed("RUSSIAN_BLUE", "breed_russian_blue"),
      breed("NORWEGIAN_FOREST", "breed_norwegian_forest"),
      breed("TURKISH_ANGORA", "breed_turkish_angora"),
      BreedPreset.manualEntry(),
    )

  private val dogBreedPresets =
    listOf(
      breed("MIXED_BREED", "breed_mixed"),
      breed("LABRADOR", "breed_labrador"),
      breed("GOLDEN_RETRIEVER", "breed_golden_retriever"),
      breed("GERMAN_SHEPHERD", "breed_german_shepherd"),
      breed("POODLE", "breed_poodle"),
      breed("BULLDOG", "breed_bulldog"),
      breed("BEAGLE", "breed_beagle"),
      breed("SHIH_TZU", "breed_shih_tzu"),
      breed("YORKSHIRE", "breed_yorkshire"),
      BreedPreset.manualEntry(),
    )

  private val manualBreedPresets = listOf(BreedPreset.manualEntry())

  private val vaccineLabelKeys =
    mapOf(
      VaccineType.RABIES to "vaccine_rabies",
      VaccineType.OTHER to "vaccine_other",
      VaccineType.V3 to "vaccine_v3",
      VaccineType.V4 to "vaccine_v4",
      VaccineType.V5 to "vaccine_v5",
      VaccineType.FELV to "vaccine_felv",
      VaccineType.FIV to "vaccine_fiv",
      VaccineType.DHPP to "vaccine_dhpp",
      VaccineType.BORDETELLA to "vaccine_bordetella",
      VaccineType.LEPTOSPIROSIS to "vaccine_leptospirosis",
      VaccineType.LEISHMANIA to "vaccine_leishmania",
      VaccineType.GRIPE_CANINA to "vaccine_gripe_canina",
      VaccineType.RHDV to "vaccine_rhdv",
      VaccineType.MYXOMATOSIS to "vaccine_myxomatosis",
      VaccineType.POLYOMAVIRUS to "vaccine_polyomavirus",
    )

  private val catVaccinePresets =
    listOf(
      vaccine(VaccineType.RABIES),
      vaccine(VaccineType.V3),
      vaccine(VaccineType.V4),
      vaccine(VaccineType.V5),
      vaccine(VaccineType.FELV),
      vaccine(VaccineType.FIV),
      vaccine(VaccineType.OTHER),
    )

  private val dogVaccinePresets =
    listOf(
      vaccine(VaccineType.RABIES),
      vaccine(VaccineType.DHPP),
      vaccine(VaccineType.BORDETELLA),
      vaccine(VaccineType.LEPTOSPIROSIS),
      vaccine(VaccineType.LEISHMANIA),
      vaccine(VaccineType.GRIPE_CANINA),
      vaccine(VaccineType.OTHER),
    )

  private val rabbitVaccinePresets =
    listOf(vaccine(VaccineType.RHDV), vaccine(VaccineType.MYXOMATOSIS), vaccine(VaccineType.OTHER))

  private val birdVaccinePresets =
    listOf(vaccine(VaccineType.POLYOMAVIRUS), vaccine(VaccineType.OTHER))

  private val manualVaccinePresets = listOf(vaccine(VaccineType.OTHER))

  private fun breed(storedValue: String, labelKey: String) = BreedPreset(storedValue, labelKey)

  private fun vaccine(type: VaccineType) =
    VaccinePreset(vaccineType = type, labelKey = vaccineLabelKeys.getValue(type))
}

/** A breed choice that either writes a stable legacy key or opens manual entry. */
data class BreedPreset(
  val storedValue: String?,
  val labelKey: String,
  val isManualEntry: Boolean = false,
) {
  companion object {
    fun manualEntry() =
      BreedPreset(storedValue = null, labelKey = "option_other", isManualEntry = true)
  }
}

/** A vaccine choice that reuses the persistence-safe [VaccineType] enum. */
data class VaccinePreset(
  val vaccineType: VaccineType,
  val labelKey: String,
  val suggestedIntervalMonths: Int? = null,
)

/** Manual-only antiparasitic recording without a named product or a suggested interval. */
data class AntiparasiticOptions(
  val manualEntryLabelKey: String,
  val suggestedIntervalMonths: Int? = null,
) {
  companion object {
    fun manualOnly() =
      AntiparasiticOptions(manualEntryLabelKey = "deworming_field_medication_custom")
  }
}
