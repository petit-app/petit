package com.woliveiras.petit.presentation.feature.pets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.media.PendingCameraPhoto
import com.woliveiras.petit.data.media.PetPhotoStore
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PetFormViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)
  private lateinit var repository: FakePetRepository
  private lateinit var photos: FakePetPhotoStore

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repository = FakePetRepository()
    photos = FakePetPhotoStore()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun invalidFieldsDoNotPersist() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      viewModel.updateBirthDate(LocalDate.of(2027, 1, 1))

      viewModel.savePet()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.nameError).isNotNull()
      assertThat(viewModel.uiState.value.birthDateError).isNotNull()
      assertThat(repository.saved).isEmpty()
    }

  @Test
  fun fieldLimitsAndIdentifiersRejectInvalidValuesWithoutPersisting() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      viewModel.updateName("n".repeat(51))
      viewModel.updateBreed("b".repeat(51))
      viewModel.updateColor("c".repeat(51))
      viewModel.updateMicrochipNumber("invalid@")
      viewModel.updatePassportNumber("p".repeat(51))
      viewModel.updateNotes("n".repeat(501))

      viewModel.savePet()
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertThat(state.nameError).isNotNull()
      assertThat(state.breedError).isNotNull()
      assertThat(state.colorError).isNotNull()
      assertThat(state.microchipError).isNotNull()
      assertThat(state.passportError).isNotNull()
      assertThat(state.notesError).isNotNull()
      assertThat(repository.saved).isEmpty()
    }

  @Test
  fun createUsesInjectedClockAndTrimmedValues() =
    runTest(dispatcher) {
      val viewModel = viewModel()
      viewModel.updateName("  Mimi  ")
      viewModel.updatePetType(PetType.CAT)

      viewModel.events.test {
        viewModel.savePet()
        advanceUntilIdle()

        val saved = repository.saved.single()
        assertThat(saved.name).isEqualTo("Mimi")
        assertThat(saved.createdAt).isEqualTo(clock.millis())
        assertThat(saved.updatedAt).isEqualTo(clock.millis())
        assertThat(awaitItem()).isEqualTo(PetFormEvent.PetSaved(saved.id))
      }
    }

  @Test
  fun editPreservesCreationAndAdvancesUpdatedAtWithInjectedClock() =
    runTest(dispatcher) {
      repository.pet = pet(name = "Mimi", createdAt = 10, updatedAt = 20)
      val viewModel = viewModel(repository.pet?.id)
      advanceUntilIdle()
      viewModel.updateName("Mimi updated")

      viewModel.savePet()
      advanceUntilIdle()

      val saved = repository.saved.single()
      assertThat(saved.createdAt).isEqualTo(10)
      assertThat(saved.updatedAt).isEqualTo(clock.millis())
      assertThat(saved.name).isEqualTo("Mimi updated")
    }

  @Test
  fun failedPhotoImportPreservesExistingPhotoAndEmitsError() =
    runTest(dispatcher) {
      repository.pet = pet(photoUri = "content://private/original")
      photos.importResult = Result.failure(SecurityException("Lost permission"))
      val viewModel = viewModel(repository.pet?.id)
      advanceUntilIdle()

      viewModel.events.test {
        viewModel.importPhoto(Uri.parse("content://external/lost"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.photoUri).isEqualTo("content://private/original")
        assertThat(awaitItem()).isInstanceOf(PetFormEvent.Error::class.java)
      }
    }

  @Test
  fun successfulPhotoImportReplacesPhotoWithPrivateUri() =
    runTest(dispatcher) {
      photos.importResult = Result.success("content://private/copied")
      val viewModel = viewModel()

      viewModel.importPhoto(Uri.parse("content://external/photo"))
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.photoUri).isEqualTo("content://private/copied")
    }

  @Test
  fun cameraLaunchAndCompletionStorePrivatePhoto() =
    runTest(dispatcher) {
      val viewModel = viewModel()

      viewModel.events.test {
        viewModel.startCameraCapture()
        val launch = awaitItem() as PetFormEvent.LaunchCamera
        assertThat(launch.uri).isEqualTo(photos.pending.uri)

        viewModel.completeCameraCapture(success = true)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.photoUri).isEqualTo("content://private/camera")
      }
    }

  @Test
  fun failedCameraCompletionPreservesExistingPhotoAndEmitsError() =
    runTest(dispatcher) {
      repository.pet = pet(photoUri = "content://private/original")
      photos.cameraResult = Result.failure(IllegalArgumentException("Invalid JPEG"))
      val viewModel = viewModel(repository.pet?.id)
      advanceUntilIdle()

      viewModel.startCameraCapture()
      advanceUntilIdle()
      viewModel.events.test {
        viewModel.completeCameraCapture(success = true)
        assertThat(awaitItem()).isInstanceOf(PetFormEvent.Error::class.java)
        assertThat(viewModel.uiState.value.photoUri).isEqualTo("content://private/original")
      }
    }

  private fun viewModel(petId: String? = null) =
    PetFormViewModel(
      SavedStateHandle(mapOf("petId" to petId)),
      context,
      repository,
      photos,
      clock,
      dispatcher,
    )

  private fun pet(
    name: String = "Mimi",
    createdAt: Long = 1,
    updatedAt: Long = 1,
    photoUri: String? = null,
  ) =
    Pet(
      id = "pet-1",
      name = name,
      petType = PetType.CAT,
      photoUri = photoUri,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )

  private class FakePetRepository : PetRepository {
    var pet: Pet? = null
    val saved = mutableListOf<Pet>()

    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(listOfNotNull(pet))

    override suspend fun getPetById(id: String): Pet? = pet

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(pet)

    override fun getPetCount(): Flow<Int> = MutableStateFlow(if (pet == null) 0 else 1)

    override suspend fun savePet(pet: Pet) {
      saved += pet
      this.pet = pet
    }

    override suspend fun deletePet(id: String) = Unit
  }

  private class FakePetPhotoStore : PetPhotoStore {
    var importResult: Result<String> = Result.success("content://private/default")
    val pending = PendingCameraPhoto(Uri.parse("content://private/pending"), File("pending"))
    var cameraResult: Result<String> = Result.success("content://private/camera")

    override fun importFromPicker(source: Uri): Result<String> = importResult

    override fun createCameraPhoto(): Result<PendingCameraPhoto> = Result.success(pending)

    override fun completeCameraPhoto(
      pending: PendingCameraPhoto,
      success: Boolean,
    ): Result<String> = cameraResult
  }
}
