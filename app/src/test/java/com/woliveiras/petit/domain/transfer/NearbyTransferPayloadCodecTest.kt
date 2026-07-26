package com.woliveiras.petit.domain.transfer

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NearbyTransferPayloadCodecTest {

  @Test
  fun implementationPayloadBoundaryPreservesCompatibilityValuesAndExistingWireFormat() {
    val expected = compatibilityBundle()

    val encoded = NearbyTransferPayloadCodec.encode(expected)
    val restored = NearbyTransferPayloadCodec.decode(String(encoded, Charsets.UTF_8))

    assertThat(String(encoded, Charsets.UTF_8)).isEqualTo(expected.toJson().toString())
    assertThat(ExportBundle.validate(restored)).isEmpty()
    assertThat(restored.pets).isEqualTo(expected.pets)
    assertThat(restored.vaccinationEntries).containsExactlyElementsIn(expected.vaccinationEntries)
    assertThat(restored.dewormingEntries).containsExactlyElementsIn(expected.dewormingEntries)
  }

  private fun compatibilityBundle(): ExportBundle {
    val pets =
      PetType.entries.mapIndexed { index, petType ->
        Pet(
          id = "nearby-pet-$index",
          name = "Pet $index",
          petType = petType,
          breed = listOf("PERSIAN", "Custom rescue breed", "legacy_Breed-ç")[index % 3],
          createdAt = 1L,
          updatedAt = 20L,
        )
      }
    return ExportBundle(
      metadata = ExportMetadata("1.0", "2026-07-18T00:00:00Z"),
      pets = pets,
      weightEntries = emptyList(),
      vaccinationEntries =
        listOf(
          VaccinationEntry(
            id = "nearby-vaccination",
            petId = pets.first().id,
            vaccineType = VaccineType.OTHER,
            customVaccineTypeName = "Historical custom vaccine",
            applicationDate = LocalDate.of(2026, 7, 1),
            createdAt = 1L,
            updatedAt = 20L,
          )
        ),
      dewormingEntries =
        listOf(
          DewormingEntry(
            id = "nearby-deworming",
            petId = pets.last().id,
            type = DewormingType.BOTH,
            medication = "Legacy active ingredient",
            applicationDate = LocalDate.of(2026, 7, 1),
            createdAt = 1L,
            updatedAt = 20L,
          )
        ),
      tasks = emptyList(),
    )
  }
}
