package com.woliveiras.petit.presentation.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.BreedCatalog
import com.woliveiras.petit.domain.model.BreedIdentity
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.presentation.feature.tasks.TaskFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Utility functions for getting localized display names for enums. */
@Composable
fun Sex.localizedName(): String =
  when (this) {
    Sex.MALE -> stringResource(R.string.sex_male)
    Sex.FEMALE -> stringResource(R.string.sex_female)
    Sex.UNKNOWN -> stringResource(R.string.sex_unknown)
  }

@Composable
fun HealthStatus.localizedName(): String =
  when (this) {
    HealthStatus.OK -> stringResource(R.string.health_status_ok)
    HealthStatus.SCHEDULED -> stringResource(R.string.health_status_scheduled)
    HealthStatus.OVERDUE -> stringResource(R.string.health_status_overdue)
  }

@Composable
fun PetType.localizedName(): String =
  when (this) {
    PetType.CAT -> stringResource(R.string.pet_type_cat)
    PetType.DOG -> stringResource(R.string.pet_type_dog)
    PetType.RABBIT -> stringResource(R.string.pet_type_rabbit)
    PetType.BIRD -> stringResource(R.string.pet_type_bird)
    PetType.HAMSTER -> stringResource(R.string.pet_type_hamster)
    PetType.OTHER -> stringResource(R.string.pet_type_other)
  }

@Composable
fun VaccineType.localizedName(): String =
  when (this) {
    VaccineType.V3 -> stringResource(R.string.vaccine_v3)
    VaccineType.V4 -> stringResource(R.string.vaccine_v4)
    VaccineType.V5 -> stringResource(R.string.vaccine_v5)
    VaccineType.RABIES -> stringResource(R.string.vaccine_rabies)
    VaccineType.FELV -> stringResource(R.string.vaccine_felv)
    VaccineType.FIV -> stringResource(R.string.vaccine_fiv)
    VaccineType.DHPP -> stringResource(R.string.vaccine_dhpp)
    VaccineType.BORDETELLA -> stringResource(R.string.vaccine_bordetella)
    VaccineType.LEPTOSPIROSIS -> stringResource(R.string.vaccine_leptospirosis)
    VaccineType.LEISHMANIA -> stringResource(R.string.vaccine_leishmania)
    VaccineType.GRIPE_CANINA -> stringResource(R.string.vaccine_gripe_canina)
    VaccineType.RHDV -> stringResource(R.string.vaccine_rhdv)
    VaccineType.MYXOMATOSIS -> stringResource(R.string.vaccine_myxomatosis)
    VaccineType.POLYOMAVIRUS -> stringResource(R.string.vaccine_polyomavirus)
    VaccineType.OTHER -> stringResource(R.string.vaccine_other)
  }

@Composable
fun DewormingType.localizedName(): String =
  when (this) {
    DewormingType.INTERNAL -> stringResource(R.string.deworming_internal)
    DewormingType.EXTERNAL -> stringResource(R.string.deworming_external)
    DewormingType.BOTH -> stringResource(R.string.deworming_both)
  }

@Composable
fun TaskKind.localizedName(): String =
  when (this) {
    TaskKind.WEIGHT -> stringResource(R.string.task_kind_weight)
    TaskKind.VACCINATION -> stringResource(R.string.task_kind_vaccination)
    TaskKind.DEWORMING -> stringResource(R.string.task_kind_deworming)
    TaskKind.MEDICATION -> stringResource(R.string.task_kind_medication)
    TaskKind.CUSTOM -> stringResource(R.string.task_kind_custom)
  }

@Composable
fun TaskFilter.localizedName(): String =
  when (this) {
    TaskFilter.TODAY -> stringResource(R.string.task_filter_today)
    TaskFilter.THIS_WEEK -> stringResource(R.string.task_filter_this_week)
    TaskFilter.THIS_MONTH -> stringResource(R.string.task_filter_this_month)
    TaskFilter.NEXT_90_DAYS -> stringResource(R.string.task_filter_next_90_days)
    TaskFilter.THIS_YEAR -> stringResource(R.string.task_filter_this_year)
  }

/** Format age for display. Uses string resources for localization. */
@Composable
fun formatAge(months: Int): String {
  return when {
    months < 1 -> stringResource(R.string.age_young)
    months == 1 -> stringResource(R.string.age_months_singular, 1)
    months < 12 -> stringResource(R.string.age_months_plural, months)
    months / 12 == 1 -> stringResource(R.string.age_years_singular, 1)
    else -> stringResource(R.string.age_years_plural, months / 12)
  }
}

/** Format health status with days remaining. */
@Composable
fun formatHealthStatusWithDays(status: HealthStatus, daysUntilNext: Long?): String {
  return when {
    status == HealthStatus.OVERDUE -> stringResource(R.string.health_status_overdue)
    daysUntilNext != null && daysUntilNext <= 0 -> stringResource(R.string.health_status_today)
    daysUntilNext != null && daysUntilNext == 1L -> stringResource(R.string.health_status_tomorrow)
    daysUntilNext != null && daysUntilNext <= 30 ->
      stringResource(R.string.health_status_days, daysUntilNext)
    status == HealthStatus.OK -> stringResource(R.string.health_status_ok)
    else -> stringResource(R.string.pet_detail_no_record)
  }
}

/** Format days text for alerts (today, tomorrow, in Xd). */
@Composable
fun formatDaysText(days: Long?): String {
  return when (days) {
    0L -> stringResource(R.string.health_status_today).lowercase()
    1L -> stringResource(R.string.health_status_tomorrow).lowercase()
    else -> stringResource(R.string.health_status_days, days ?: 0)
  }
}

/** Convert a breed key (e.g. "PERSIAN") to a localized display name. Custom breeds pass through. */
@Composable
fun localizedBreed(breed: String): String =
  when (breed) {
    "MIXED_BREED" -> stringResource(R.string.breed_mixed)
    "PERSIAN" -> stringResource(R.string.breed_persian)
    "SIAMESE" -> stringResource(R.string.breed_siamese)
    "MAINE_COON" -> stringResource(R.string.breed_maine_coon)
    "RAGDOLL" -> stringResource(R.string.breed_ragdoll)
    "BRITISH_SHORTHAIR" -> stringResource(R.string.breed_british_shorthair)
    "BENGAL" -> stringResource(R.string.breed_bengal)
    "ABYSSINIAN" -> stringResource(R.string.breed_abyssinian)
    "SPHYNX" -> stringResource(R.string.breed_sphynx)
    "SCOTTISH_FOLD" -> stringResource(R.string.breed_scottish_fold)
    "BURMESE" -> stringResource(R.string.breed_burmese)
    "RUSSIAN_BLUE" -> stringResource(R.string.breed_russian_blue)
    "NORWEGIAN_FOREST" -> stringResource(R.string.breed_norwegian_forest)
    "TURKISH_ANGORA" -> stringResource(R.string.breed_turkish_angora)
    // Dog breeds
    "LABRADOR" -> stringResource(R.string.breed_labrador)
    "GOLDEN_RETRIEVER" -> stringResource(R.string.breed_golden_retriever)
    "GERMAN_SHEPHERD" -> stringResource(R.string.breed_german_shepherd)
    "POODLE" -> stringResource(R.string.breed_poodle)
    "BULLDOG" -> stringResource(R.string.breed_bulldog)
    "BEAGLE" -> stringResource(R.string.breed_beagle)
    "SHIH_TZU" -> stringResource(R.string.breed_shih_tzu)
    "YORKSHIRE" -> stringResource(R.string.breed_yorkshire)
    else -> breed
  }

/** Resolves a catalog identity in the active locale and keeps persisted text as its fallback. */
@Composable
fun localizedBreed(breedId: String?, fallback: String?): String? {
  val context = LocalContext.current
  val localeTag = LocalConfiguration.current.locales[0].toLanguageTag()
  val fallbackDisplay = fallback
  if (breedId == null) return fallbackDisplay
  if (breedId == BreedIdentity.MIXED_BREED_ID) return stringResource(R.string.breed_mixed)
  if (breedId == BreedIdentity.UNKNOWN_BREED_ID) return stringResource(R.string.breed_unknown)

  val displayName by
    produceState(initialValue = fallbackDisplay, breedId, localeTag) {
      value =
        withContext(Dispatchers.IO) {
          BreedCatalogCache.get(context).resolve(breedId, localeTag)?.displayName
        } ?: fallbackDisplay
    }
  return displayName
}

private object BreedCatalogCache {
  @Volatile private var cached: BreedCatalog? = null

  fun get(context: Context): BreedCatalog =
    cached
      ?: synchronized(this) {
        cached
          ?: runCatching {
              context.applicationContext.assets.open("breed_catalog.json").bufferedReader().use {
                BreedCatalog.fromJsonOrEmpty(it.readText())
              }
            }
            .getOrElse { BreedCatalog.fromJsonOrEmpty("{}") }
            .also { cached = it }
      }
}

/** Convert a color key (e.g. "ORANGE") to a localized display name. Custom colors pass through. */
@Composable
fun localizedColor(color: String): String =
  when (color) {
    "BLACK" -> stringResource(R.string.color_black)
    "WHITE" -> stringResource(R.string.color_white)
    "ORANGE" -> stringResource(R.string.color_orange)
    "GRAY" -> stringResource(R.string.color_gray)
    "TABBY" -> stringResource(R.string.color_tabby)
    "CALICO" -> stringResource(R.string.color_calico)
    "TUXEDO" -> stringResource(R.string.color_tuxedo)
    "TORTOISESHELL" -> stringResource(R.string.color_tortoiseshell)
    "CREAM" -> stringResource(R.string.color_cream)
    "BROWN" -> stringResource(R.string.color_brown)
    "BLUE" -> stringResource(R.string.color_blue)
    "SILVER" -> stringResource(R.string.color_silver)
    "GOLDEN" -> stringResource(R.string.color_golden)
    "BRINDLE" -> stringResource(R.string.color_brindle)
    else -> color
  }
