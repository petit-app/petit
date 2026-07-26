package com.woliveiras.petit.presentation.components

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.ui.theme.PetitTheme
import java.time.LocalDate
import java.util.Locale
import org.junit.Rule
import org.junit.Test

class WeightChartLocalizationComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun chartSemanticsUsesEnglishPluralDescriptionForEntryCount() {
    composeRule.setContent { LocalizedWeightChart(Locale.ENGLISH) }

    composeRule
      .onNodeWithContentDescription("Weight evolution chart with 2 records")
      .assertIsDisplayed()
  }

  @Test
  fun chartSemanticsUsesBrazilianPortuguesePluralDescriptionForEntryCount() {
    composeRule.setContent { LocalizedWeightChart(Locale.forLanguageTag("pt-BR")) }

    composeRule
      .onNodeWithContentDescription("Gráfico de evolução de peso com 2 registros")
      .assertIsDisplayed()
  }

  @androidx.compose.runtime.Composable
  private fun LocalizedWeightChart(locale: Locale) {
    val localizedContext = localizedContext(locale)
    CompositionLocalProvider(LocalContext provides localizedContext) {
      PetitTheme { WeightChart(entries = entries) }
    }
  }

  private fun localizedContext(locale: Locale): Context {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
    return context.createConfigurationContext(configuration)
  }

  private companion object {
    val entries =
      listOf(
        WeightEntry(
          id = "first",
          petId = "pet",
          date = LocalDate.of(2026, 1, 1),
          weightGrams = 4000,
          createdAt = 0,
          updatedAt = 0,
        ),
        WeightEntry(
          id = "second",
          petId = "pet",
          date = LocalDate.of(2026, 2, 1),
          weightGrams = 4200,
          createdAt = 0,
          updatedAt = 0,
        ),
      )
  }
}
