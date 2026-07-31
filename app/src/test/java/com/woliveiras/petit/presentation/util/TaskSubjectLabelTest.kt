package com.woliveiras.petit.presentation.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.VaccineType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskSubjectLabelTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun kindsWithoutASubjectHaveNoLabel() {
    assertThat(taskSubjectLabel(context, TaskKind.CUSTOM, null, "Ignored")).isNull()
    assertThat(taskSubjectLabel(context, TaskKind.WEIGHT, "RABIES", "Ignored")).isNull()
  }

  @Test
  fun aKnownVaccineIsNamedFromTheCatalog() {
    assertThat(taskSubjectLabel(context, TaskKind.VACCINATION, VaccineType.RABIES.name, null))
      .isEqualTo(context.getString(VaccineType.RABIES.labelResId()))
  }

  @Test
  fun theOtherVaccineUsesTheFreeTextAndFallsBackToTheCatalogName() {
    assertThat(
        taskSubjectLabel(context, TaskKind.VACCINATION, VaccineType.OTHER.name, "Vacina da clínica")
      )
      .isEqualTo("Vacina da clínica")
    assertThat(taskSubjectLabel(context, TaskKind.VACCINATION, VaccineType.OTHER.name, "  "))
      .isEqualTo(context.getString(VaccineType.OTHER.labelResId()))
  }

  @Test
  fun aCodeFromANewerVersionFallsBackToTheStoredText() {
    assertThat(taskSubjectLabel(context, TaskKind.VACCINATION, "VACCINE_FROM_THE_FUTURE", "V10"))
      .isEqualTo("V10")
    assertThat(taskSubjectLabel(context, TaskKind.VACCINATION, "VACCINE_FROM_THE_FUTURE", null))
      .isNull()
  }

  @Test
  fun antiparasiticCombinesTreatmentTypeAndProduct() {
    val typeLabel = context.getString(DewormingType.EXTERNAL.labelResId())

    assertThat(
        taskSubjectLabel(context, TaskKind.DEWORMING, DewormingType.EXTERNAL.name, "Frontline")
      )
      .isEqualTo("$typeLabel: Frontline")
    assertThat(taskSubjectLabel(context, TaskKind.DEWORMING, DewormingType.EXTERNAL.name, null))
      .isEqualTo(typeLabel)
    assertThat(taskSubjectLabel(context, TaskKind.DEWORMING, null, "Frontline"))
      .isEqualTo("Frontline")
  }

  @Test
  fun medicationUsesTheTrimmedFreeText() {
    assertThat(taskSubjectLabel(context, TaskKind.MEDICATION, null, "  Apoquel  "))
      .isEqualTo("Apoquel")
    assertThat(taskSubjectLabel(context, TaskKind.MEDICATION, null, "   ")).isNull()
  }
}
