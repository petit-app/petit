package com.woliveiras.petit.data.repository

import com.woliveiras.petit.data.local.dao.PetDao
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.Pet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of PetRepository using Room database. */
@Singleton
class PetRepositoryImpl @Inject constructor(private val petDao: PetDao) : PetRepository {

  override fun getAllPets(): Flow<List<Pet>> {
    return petDao.getAllPets().map { entities -> entities.toDomain() }
  }

  override suspend fun getPetById(id: String): Pet? {
    return petDao.getPetById(id)?.toDomain()
  }

  override fun getPetByIdFlow(id: String): Flow<Pet?> {
    return petDao.getPetByIdFlow(id).map { entity -> entity?.toDomain() }
  }

  override fun getPetCount(): Flow<Int> {
    return petDao.getPetCount()
  }

  override suspend fun savePet(pet: Pet) {
    petDao.insertPet(pet.toEntity())
  }

  override suspend fun deletePet(id: String) {
    petDao.softDeletePet(id)
  }
}
