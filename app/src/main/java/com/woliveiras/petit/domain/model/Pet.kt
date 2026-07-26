package com.woliveiras.petit.domain.model

import java.time.LocalDate

/** Domain model representing a pet. */
data class Pet(
  val id: String,
  val name: String,
  val petType: PetType = PetType.OTHER,
  val birthDate: LocalDate? = null,
  val sex: Sex = Sex.UNKNOWN,
  val breed: String? = null,
  val breedId: String? = null,
  val color: String? = null,
  val microchipNumber: String? = null,
  val passportNumber: String? = null,
  val photoUri: String? = null,
  val notes: String? = null,
  val createdAt: Long,
  val updatedAt: Long,
  val deletedAt: Long? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
) {

  /** Calculates the pet's age in months based on birthdate. */
  fun getAgeInMonths(): Int? {
    val birth = birthDate ?: return null
    val today = LocalDate.now()
    val months = (today.year - birth.year) * 12 + (today.monthValue - birth.monthValue)
    return months.coerceAtLeast(0)
  }

  companion object
}
