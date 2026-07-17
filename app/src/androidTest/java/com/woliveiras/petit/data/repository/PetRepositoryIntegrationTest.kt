package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.WeightEntryEntity
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetRepositoryIntegrationTest {

  private lateinit var database: PetitDatabase
  private lateinit var repository: PetRepositoryImpl

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java).build()
    repository = PetRepositoryImpl(database.petDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun crudSortSoftDeleteAndEditPreservesChildHistory() = runTest {
    repository.savePet(pet("pet-b", "Zelda", updatedAt = 10))
    repository.savePet(pet("pet-a", "Amora", updatedAt = 20))
    database
      .weightEntryDao()
      .insertWeightEntry(
        WeightEntryEntity(id = "weight-1", petId = "pet-a", date = 1, weightGrams = 4500)
      )

    assertThat(repository.getAllPets().first().map { it.name })
      .containsExactly("Amora", "Zelda")
      .inOrder()

    repository.savePet(pet("pet-a", "Amora edited", updatedAt = 30))

    assertThat(repository.getPetById("pet-a")?.name).isEqualTo("Amora edited")
    assertThat(database.weightEntryDao().getWeightEntryById("weight-1")).isNotNull()
    assertThat(database.petDao().getPetsModifiedSince(25).map { it.id }).contains("pet-a")

    repository.deletePet("pet-a")

    assertThat(repository.getPetById("pet-a")).isNull()
    assertThat(repository.getAllPets().first().map { it.id }).containsExactly("pet-b")
    val deleted = database.petDao().getPetsModifiedSince(0).single { it.id == "pet-a" }
    assertThat(deleted.deletedAt).isNotNull()
    assertThat(deleted.updatedAt).isEqualTo(deleted.deletedAt)
  }

  private fun pet(id: String, name: String, updatedAt: Long) =
    Pet(id = id, name = name, petType = PetType.CAT, createdAt = 1, updatedAt = updatedAt)
}
