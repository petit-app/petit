package com.woliveiras.petit.presentation.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.VaccineType
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Localized text to use only at a presentation boundary; the stored [Task] is never changed. */
data class TaskDisplayText(val title: String, val description: String?)

/** Resolves strings in the language the caregiver chose, not the one the process started with. */
fun Context.forLanguage(language: AppLanguage?): Context {
  val locale =
    when (language) {
      AppLanguage.ENGLISH -> Locale.ENGLISH
      AppLanguage.PORTUGUESE_BR -> Locale.forLanguageTag(AppLanguage.PORTUGUESE_BR.code)
      AppLanguage.SYSTEM ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          getSystemService(LocaleManager::class.java)?.systemLocales?.get(0)
            ?: Resources.getSystem().configuration.locales[0]
        } else {
          return this
        }
      null -> return this
    }
  val configuration = Configuration(resources.configuration)
  configuration.setLocales(LocaleList(locale))
  return createConfigurationContext(configuration)
}

/**
 * Rebuilds text for structurally valid automatic tasks in the active locale.
 *
 * Care-task descriptions intentionally remain generic: the persisted model has no immutable
 * snapshot of the advance-notice days, so parsing the old description would be locale-dependent and
 * unsafe.
 */
fun interface TaskDisplayTextResolver {
  suspend fun resolve(task: Task, language: AppLanguage?): TaskDisplayText

  suspend fun resolve(task: Task): TaskDisplayText = resolve(task, null)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TaskDisplayTextModule {
  @Binds
  @Singleton
  abstract fun bindTaskDisplayTextResolver(
    implementation: LocalizedTaskDisplayTextResolver
  ): TaskDisplayTextResolver
}

@Singleton
class LocalizedTaskDisplayTextResolver
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val vaccinationEntryRepository: VaccinationEntryRepository,
  private val dewormingEntryRepository: DewormingEntryRepository,
) : TaskDisplayTextResolver {
  override suspend fun resolve(task: Task, language: AppLanguage?): TaskDisplayText {
    val displayContext = displayContext(language)
    return when {
      task.isAutomaticVaccination() -> resolveVaccination(task, displayContext)
      task.isAutomaticDeworming() -> resolveDeworming(task, displayContext)
      task.isAutomaticWeight() -> resolveWeight(task, displayContext)
      else -> task.persistedText()
    }
  }

  private suspend fun resolveVaccination(task: Task, displayContext: Context): TaskDisplayText {
    val referenceId = task.referenceEntityId ?: return task.persistedText()
    val entry =
      vaccinationEntryRepository.getVaccinationEntryById(referenceId)
        ?: return localizedCareFallback(task, R.string.task_kind_vaccination, displayContext)
    if (entry.petId != task.petId) return task.persistedText()

    val petName =
      petRepository.getPetById(entry.petId)?.name
        ?: displayContext.getString(R.string.default_pet_name)
    val vaccineName =
      if (entry.vaccineType == VaccineType.OTHER && !entry.customVaccineTypeName.isNullOrBlank()) {
        entry.customVaccineTypeName
      } else {
        displayContext.getString(entry.vaccineType.resourceId())
      }
    return TaskDisplayText(
      displayContext.getString(R.string.task_auto_care_title, petName, vaccineName),
      displayContext.getString(R.string.task_auto_care_description),
    )
  }

  private suspend fun resolveDeworming(task: Task, displayContext: Context): TaskDisplayText {
    val referenceId = task.referenceEntityId ?: return task.persistedText()
    val entry =
      dewormingEntryRepository.getDewormingEntryById(referenceId)
        ?: return localizedCareFallback(task, R.string.task_kind_deworming, displayContext)
    if (entry.petId != task.petId) return task.persistedText()

    val petName =
      petRepository.getPetById(entry.petId)?.name
        ?: displayContext.getString(R.string.default_pet_name)
    return TaskDisplayText(
      displayContext.getString(
        R.string.task_auto_care_title,
        petName,
        displayContext.getString(entry.type.resourceId()),
      ),
      displayContext.getString(R.string.task_auto_care_description),
    )
  }

  private suspend fun resolveWeight(task: Task, displayContext: Context): TaskDisplayText {
    val petId = task.petId ?: return task.persistedText()
    val petName =
      petRepository.getPetById(petId)?.name ?: displayContext.getString(R.string.default_pet_name)
    return TaskDisplayText(
      displayContext.getString(R.string.reminder_weight_title, petName),
      displayContext.getString(R.string.reminder_weight_description),
    )
  }

  private suspend fun localizedCareFallback(
    task: Task,
    kindResourceId: Int,
    displayContext: Context,
  ): TaskDisplayText {
    val petName =
      task.petId?.let { petRepository.getPetById(it)?.name }
        ?: displayContext.getString(R.string.default_pet_name)
    return TaskDisplayText(
      displayContext.getString(
        R.string.task_auto_care_title,
        petName,
        displayContext.getString(kindResourceId),
      ),
      displayContext.getString(R.string.task_auto_care_description),
    )
  }

  private fun displayContext(language: AppLanguage?): Context = context.forLanguage(language)

  private fun Task.isAutomaticVaccination(): Boolean =
    kind == TaskKind.VACCINATION &&
      !referenceEntityId.isNullOrBlank() &&
      !petId.isNullOrBlank() &&
      id == "auto_vacc_$referenceEntityId"

  private fun Task.isAutomaticDeworming(): Boolean =
    kind == TaskKind.DEWORMING &&
      !referenceEntityId.isNullOrBlank() &&
      !petId.isNullOrBlank() &&
      id == "auto_deworm_$referenceEntityId"

  private fun Task.isAutomaticWeight(): Boolean =
    kind == TaskKind.WEIGHT &&
      referenceEntityId == null &&
      !petId.isNullOrBlank() &&
      id == "auto_weight_$petId"

  private fun Task.persistedText() = TaskDisplayText(title, description)

  private fun VaccineType.resourceId(): Int = labelResId()

  private fun DewormingType.resourceId(): Int = labelResId()
}
