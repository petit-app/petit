package com.woliveiras.petit.presentation.util

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppDisplayFormatterTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun formatsEverySupportedDisplayShapeForEnglishAndBrazilianPortuguese() {
    val date = LocalDate.of(2026, 7, 5)
    val instant = Instant.parse("2026-07-05T14:30:00Z")
    val time = LocalTime.of(14, 30)
    val english = AppDisplayFormatter(context.forLocale(Locale.ENGLISH))
    val portuguese = AppDisplayFormatter(context.forLocale(Locale.forLanguageTag("pt-BR")))

    assertThat(english.shortDate(date)).isEqualTo("7/5/2026")
    assertThat(portuguese.shortDate(date)).isEqualTo("05/07/2026")
    assertThat(english.dayMonth(date)).isEqualTo("Jul 5")
    assertThat(portuguese.dayMonth(date)).isEqualTo("5 de jul.")
    assertThat(english.monthYear(date)).isEqualTo("July 2026")
    assertThat(portuguese.monthYear(date)).isEqualTo("julho de 2026")
    assertThat(english.time(time)).isEqualTo("2:30\u202FPM")
    assertThat(portuguese.time(time)).isEqualTo("14:30")
    assertThat(english.dateTime(instant, java.time.ZoneOffset.UTC)).contains("Jul 5, 2026")
    assertThat(portuguese.dateTime(instant, java.time.ZoneOffset.UTC)).contains("5 de jul. de 2026")
    assertThat(english.decimal(3.5, fractionDigits = 1)).isEqualTo("3.5")
    assertThat(portuguese.decimal(3.5, fractionDigits = 1)).isEqualTo("3,5")
    assertThat(english.weight(3_500)).isEqualTo("3.5 kg")
    assertThat(portuguese.weight(3_500)).isEqualTo("3,5 kg")
    assertThat(english.weight(350)).isEqualTo("350 g")
    assertThat(portuguese.weight(350)).isEqualTo("350 g")
    assertThat(
        AppDisplayFormatter(context.forLocale(Locale.forLanguageTag("es-ES"))).shortDate(date)
      )
      .isEqualTo("5/7/2026")
  }

  @Test
  fun parsesCurrentAndLocalizedDecimalInputWithoutChangingTheStoredNumber() {
    val english = AppDisplayFormatter(context.forLocale(Locale.ENGLISH))
    val portuguese = AppDisplayFormatter(context.forLocale(Locale.forLanguageTag("pt-BR")))

    assertThat(english.parseDecimal("3.5")).isEqualTo(3.5)
    assertThat(english.parseDecimal("3,5")).isEqualTo(3.5)
    assertThat(portuguese.parseDecimal("3,5")).isEqualTo(3.5)
    assertThat(portuguese.parseDecimal("3.5")).isEqualTo(3.5)
    assertThat(portuguese.parseDecimal(" 3,50 ")).isEqualTo(3.5)
    assertThat(portuguese.parseDecimal("3 kg")).isNull()
  }

  @Test
  fun auditedPresentationFilesHaveNoProcessLocaleOrAdHocFormatting() {
    val violations =
      AUDITED_FILES.flatMap { relativePath ->
        val file = projectRoot().resolve(relativePath)
        val source = file.readText()
        FORBIDDEN_PATTERNS.mapNotNull { pattern ->
          pattern.find(source)?.let { match -> "$relativePath: ${match.value}" }
        }
      }

    assertThat(violations).isEmpty()
  }

  private fun Context.forLocale(locale: Locale): Context {
    val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(configuration)
  }

  private fun projectRoot() = File(checkNotNull(System.getProperty("user.dir")))

  private companion object {
    val FORBIDDEN_PATTERNS =
      listOf(
        Regex("""DateTimeFormatter\.ofPattern"""),
        Regex("""String\.format\s*\("""),
        Regex("""DateFormat\.get(?:Date|Time|DateTime)Instance\s*\("""),
        Regex("""Locale\.getDefault\s*\("""),
        Regex("""\.formattedWeight\b"""),
        Regex("""\.replace\(" kg""""),
        Regex("""text\s*=\s*"kg""""),
      )
    val AUDITED_FILES =
      listOf(
        "src/main/java/com/woliveiras/petit/presentation/feature/home/HomeScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/tasks/TaskListScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/tasks/CompletedTasksScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/tasks/TaskFormScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/tasks/TaskSettingsViewModel.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/pets/PetFormScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/pets/PetDetailScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/pets/PetListScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/components/PetCard.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/vaccination/VaccinationFormScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/vaccination/VaccinationRecordsScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/deworming/DewormingFormScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/deworming/DewormingRecordsScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/weight/WeightFormScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/weight/WeightEntryScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/weight/WeightFormViewModel.kt",
        "src/main/java/com/woliveiras/petit/presentation/components/WeightChart.kt",
        "src/main/java/com/woliveiras/petit/presentation/components/TimelineEventCard.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/backup/BackupSettingsRoute.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/backup/BackupRoutes.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/familygroup/SyncHistoryScreen.kt",
        "src/main/java/com/woliveiras/petit/presentation/feature/familygroup/FamilyGroupScreen.kt",
      )
  }
}
