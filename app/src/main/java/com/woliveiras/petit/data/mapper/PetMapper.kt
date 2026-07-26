package com.woliveiras.petit.data.mapper

import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.SyncStatus
import java.time.Instant
import java.time.ZoneId

/** Mappers for Pet entity <-> domain model conversions. */
fun PetEntity.toDomain(): Pet =
  Pet(
    id = id,
    name = name,
    petType =
      try {
        PetType.valueOf(petType)
      } catch (_: Exception) {
        PetType.OTHER
      },
    birthDate =
      birthDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
    sex =
      try {
        Sex.valueOf(sex)
      } catch (_: Exception) {
        Sex.UNKNOWN
      },
    breed = breed,
    breedId = breedId,
    color = color,
    microchipNumber = microchipNumber,
    passportNumber = passportNumber,
    photoUri = photoUri,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus =
      try {
        SyncStatus.valueOf(syncStatus)
      } catch (_: Exception) {
        SyncStatus.LOCAL_ONLY
      },
  )

fun Pet.toEntity(): PetEntity =
  PetEntity(
    id = id,
    name = name,
    petType = petType.name,
    birthDate = birthDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    sex = sex.name,
    breed = breed,
    breedId = breedId,
    color = color,
    microchipNumber = microchipNumber,
    passportNumber = passportNumber,
    photoUri = photoUri,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus.name,
  )

fun List<PetEntity>.toDomain(): List<Pet> = map { it.toDomain() }
