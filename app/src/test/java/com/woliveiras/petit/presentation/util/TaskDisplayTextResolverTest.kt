package com.woliveiras.petit.presentation.util

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDisplayTextResolverTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun explicitLanguageOverridesAnUnchangedBaseContextBidirectionally() = runTest {
    val fixtures = Fixtures(pets = listOf(pet("pet-1", "Mimi")))
    val resolver = fixtures.resolver(localizedContext(Locale.ENGLISH))
    val task = autoWeightTask("pet-1")

    assertThat(resolver.resolve(task, AppLanguage.ENGLISH))
      .isEqualTo(TaskDisplayText("Mimi - Weigh", "Reminder to check weight"))
    assertThat(resolver.resolve(task, AppLanguage.PORTUGUESE_BR))
      .isEqualTo(TaskDisplayText("Mimi - Pesar", "Lembrete para verificar o peso"))
    assertThat(resolver.resolve(task, AppLanguage.ENGLISH))
      .isEqualTo(TaskDisplayText("Mimi - Weigh", "Reminder to check weight"))
  }

  @Test
  fun automaticTemplatesResolveFromStructuredDataInEnglishAndBrazilianPortuguese() = runTest {
    val pet = pet("pet-1", "Mimi")
    val vaccination = vaccination("vacc-1", pet.id, VaccineType.RABIES)
    val customVaccination = vaccination("vacc-custom", pet.id, VaccineType.OTHER, "Nobivac® Custom")
    val deworming = deworming("deworm-1", pet.id, DewormingType.BOTH, "Marca X")
    val fixtures =
      Fixtures(
        pets = listOf(pet),
        vaccinations = listOf(vaccination, customVaccination),
        dewormings = listOf(deworming),
      )

    val expectations =
      listOf(
        Locale.ENGLISH to
          listOf(
            TaskDisplayText("Mimi - Rabies", "Automatic reminder for upcoming care"),
            TaskDisplayText("Mimi - Nobivac® Custom", "Automatic reminder for upcoming care"),
            TaskDisplayText(
              "Mimi - Combo (internal + external)",
              "Automatic reminder for upcoming care",
            ),
            TaskDisplayText("Mimi - Weigh", "Reminder to check weight"),
            TaskDisplayText("Mimi - Vaccination", "Automatic reminder for upcoming care"),
            TaskDisplayText("Pet - Rabies", "Automatic reminder for upcoming care"),
          ),
        Locale.forLanguageTag("pt-BR") to
          listOf(
            TaskDisplayText("Mimi - Antirrábica", "Lembrete automático para o próximo cuidado"),
            TaskDisplayText("Mimi - Nobivac® Custom", "Lembrete automático para o próximo cuidado"),
            TaskDisplayText(
              "Mimi - Combo (interno + externo)",
              "Lembrete automático para o próximo cuidado",
            ),
            TaskDisplayText("Mimi - Pesar", "Lembrete para verificar o peso"),
            TaskDisplayText("Mimi - Vacinação", "Lembrete automático para o próximo cuidado"),
            TaskDisplayText("Pet - Antirrábica", "Lembrete automático para o próximo cuidado"),
          ),
      )

    expectations.forEach { (locale, expected) ->
      val resolver = fixtures.resolver(localizedContext(locale))
      val actual =
        listOf(
          resolver.resolve(
            autoCareTask("auto_vacc_vacc-1", TaskKind.VACCINATION, pet.id, "vacc-1")
          ),
          resolver.resolve(
            autoCareTask("auto_vacc_vacc-custom", TaskKind.VACCINATION, pet.id, "vacc-custom")
          ),
          resolver.resolve(
            autoCareTask("auto_deworm_deworm-1", TaskKind.DEWORMING, pet.id, "deworm-1")
          ),
          resolver.resolve(autoWeightTask(pet.id)),
          resolver.resolve(
            autoCareTask("auto_vacc_missing", TaskKind.VACCINATION, pet.id, "missing")
          ),
          resolver.resolve(
            autoCareTask(
              "auto_vacc_vacc-orphan",
              TaskKind.VACCINATION,
              "missing-pet",
              "vacc-orphan",
            )
          ),
        )

      assertThat(actual).containsExactlyElementsIn(expected).inOrder()
    }
  }

  @Test
  fun caregiverTextUnknownIdsAndInconsistentShapesRemainByteExact() = runTest {
    val fixtures =
      Fixtures(
        pets = listOf(pet("pet-1", "Mimi")),
        vaccinations = listOf(vaccination("vacc-1", "pet-1", VaccineType.RABIES)),
      )
    val resolver = fixtures.resolver(localizedContext(Locale.forLanguageTag("pt-BR")))
    val persisted =
      listOf(
        task(
          id = "custom-1",
          kind = TaskKind.CUSTOM,
          title = "Nobivac® / DO NOT TRANSLATE",
          description = "  Cuidador escreveu isto\nexatamente.  ",
        ),
        task(
          id = "future_auto_kind_42",
          kind = TaskKind.MEDICATION,
          title = "Legacy title",
          description = "Legacy description",
        ),
        autoCareTask(
          id = "auto_vacc_wrong-id",
          kind = TaskKind.VACCINATION,
          petId = "pet-1",
          referenceId = "vacc-1",
          title = "Persisted title",
          description = "Persisted description",
        ),
        autoCareTask(
          id = "auto_vacc_vacc-1",
          kind = TaskKind.VACCINATION,
          petId = "other-pet",
          referenceId = "vacc-1",
          title = "Mismatched persisted title",
          description = "Mismatched persisted description",
        ),
      )

    val actual = persisted.map { resolver.resolve(it) }

    assertThat(actual)
      .containsExactlyElementsIn(persisted.map { TaskDisplayText(it.title, it.description) })
      .inOrder()
  }

  private fun localizedContext(locale: Locale): Context {
    val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
    return context.createConfigurationContext(configuration)
  }

  private fun pet(id: String, name: String) =
    Pet(id = id, name = name, petType = PetType.CAT, createdAt = 1L, updatedAt = 1L)

  private fun vaccination(
    id: String,
    petId: String,
    type: VaccineType,
    customName: String? = null,
  ) =
    VaccinationEntry(
      id = id,
      petId = petId,
      vaccineType = type,
      customVaccineTypeName = customName,
      applicationDate = LocalDate.of(2026, 7, 1),
      createdAt = 1L,
      updatedAt = 1L,
    )

  private fun deworming(id: String, petId: String, type: DewormingType, medication: String) =
    DewormingEntry(
      id = id,
      petId = petId,
      type = type,
      medication = medication,
      applicationDate = LocalDate.of(2026, 7, 1),
      createdAt = 1L,
      updatedAt = 1L,
    )

  private fun autoCareTask(
    id: String,
    kind: TaskKind,
    petId: String,
    referenceId: String,
    title: String = "Persisted English title",
    description: String? = "Persisted English description",
  ) = task(id, kind, title, description, petId, referenceId)

  private fun autoWeightTask(petId: String) =
    task(
      id = "auto_weight_$petId",
      kind = TaskKind.WEIGHT,
      title = "Persisted English weight title",
      description = "Persisted English weight description",
      petId = petId,
    )

  private fun task(
    id: String,
    kind: TaskKind,
    title: String,
    description: String?,
    petId: String? = null,
    referenceId: String? = null,
  ) =
    Task(
      id = id,
      petId = petId,
      kind = kind,
      referenceEntityId = referenceId,
      title = title,
      description = description,
      scheduledFor = LocalDateTime.of(2026, 7, 20, 9, 0),
      createdAt = 1L,
      updatedAt = 1L,
    )

  private class Fixtures(
    pets: List<Pet> = emptyList(),
    vaccinations: List<VaccinationEntry> = emptyList(),
    dewormings: List<DewormingEntry> = emptyList(),
  ) {
    private val petRepository = FakePetRepository(pets)
    private val vaccinationRepository = FakeVaccinationRepository(vaccinations)
    private val dewormingRepository = FakeDewormingRepository(dewormings)

    init {
      vaccinationRepository.entries["vacc-orphan"] =
        VaccinationEntry(
          id = "vacc-orphan",
          petId = "missing-pet",
          vaccineType = VaccineType.RABIES,
          applicationDate = LocalDate.of(2026, 7, 1),
          createdAt = 1L,
          updatedAt = 1L,
        )
    }

    fun resolver(context: Context) =
      LocalizedTaskDisplayTextResolver(
        context,
        petRepository,
        vaccinationRepository,
        dewormingRepository,
      )
  }

  private class FakePetRepository(pets: List<Pet>) : PetRepository {
    private val state = MutableStateFlow(pets)

    override fun getAllPets(): Flow<List<Pet>> = state

    override suspend fun getPetById(id: String): Pet? = state.value.find { it.id == id }

    override fun getPetByIdFlow(id: String): Flow<Pet?> =
      MutableStateFlow(state.value.find { it.id == id })

    override fun getPetCount(): Flow<Int> = MutableStateFlow(state.value.size)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class FakeVaccinationRepository(vaccinations: List<VaccinationEntry>) :
    VaccinationEntryRepository {
    val entries = vaccinations.associateByTo(mutableMapOf()) { it.id }

    override fun getVaccinationEntriesForPet(petId: String): Flow<List<VaccinationEntry>> =
      MutableStateFlow(entries.values.filter { it.petId == petId })

    override fun getLatestVaccinationsForPet(petId: String): Flow<List<VaccinationEntry>> =
      getVaccinationEntriesForPet(petId)

    override suspend fun getVaccinationEntryById(id: String): VaccinationEntry? = entries[id]

    override fun getOverdueVaccinations(): Flow<List<VaccinationEntry>> =
      MutableStateFlow(emptyList())

    override fun getUpcomingVaccinations(days: Int): Flow<List<VaccinationEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun saveVaccinationEntry(entry: VaccinationEntry) = Unit

    override suspend fun deleteVaccinationEntry(id: String) = Unit

    override suspend fun countEntriesForPet(petId: String): Int =
      entries.values.count { it.petId == petId }
  }

  private class FakeDewormingRepository(dewormings: List<DewormingEntry>) :
    DewormingEntryRepository {
    private val entries = dewormings.associateBy { it.id }

    override fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntry>> =
      MutableStateFlow(entries.values.filter { it.petId == petId })

    override fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntry>> =
      getDewormingEntriesForPet(petId)

    override suspend fun getDewormingEntryById(id: String): DewormingEntry? = entries[id]

    override fun getOverdueDewormings(): Flow<List<DewormingEntry>> = MutableStateFlow(emptyList())

    override fun getUpcomingDewormings(days: Int): Flow<List<DewormingEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun saveDewormingEntry(entry: DewormingEntry) = Unit

    override suspend fun deleteDewormingEntry(id: String) = Unit

    override suspend fun countEntriesForPet(petId: String): Int =
      entries.values.count { it.petId == petId }
  }
}
