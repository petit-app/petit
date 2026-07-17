package com.woliveiras.petit.data.mapper

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.SyncStatus
import java.time.LocalDate
import org.junit.Test

class PetMapperTest {

  @Test
  fun roundTripPreservesPetFieldsAndMetadata() {
    val pet =
      Pet(
        id = "pet-1",
        name = "Mimi",
        petType = PetType.CAT,
        birthDate = LocalDate.of(2020, 2, 3),
        sex = Sex.FEMALE,
        breed = "Mixed",
        photoUri = "content://private/photo",
        createdAt = 10,
        updatedAt = 20,
        deletedAt = 30,
        syncStatus = SyncStatus.SYNCED,
      )

    assertThat(pet.toEntity().toDomain()).isEqualTo(pet)
  }

  @Test
  fun unknownStoredEnumsFallbackSafely() {
    val mapped =
      PetEntity(
          name = "Mimi",
          petType = "FUTURE_TYPE",
          sex = "FUTURE_SEX",
          syncStatus = "FUTURE_SYNC",
        )
        .toDomain()

    assertThat(mapped.petType).isEqualTo(PetType.OTHER)
    assertThat(mapped.sex).isEqualTo(Sex.UNKNOWN)
    assertThat(mapped.syncStatus).isEqualTo(SyncStatus.LOCAL_ONLY)
  }
}
