package com.woliveiras.petit.presentation.util

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CancellationException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class UiFailureTextContractTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun failureTextUsesAppOwnedCopyInEnglishAndBrazilianPortuguese() {
    val failure = IllegalStateException("Provider diagnostic must stay private")

    assertThat(failure.uiFailureText(context.forLocale(Locale.ENGLISH), R.string.pet_error_save))
      .isEqualTo("Error saving")
    assertThat(
        failure.uiFailureText(
          context.forLocale(Locale.forLanguageTag("pt-BR")),
          R.string.pet_error_save,
        )
      )
      .isEqualTo("Erro ao salvar")
  }

  @Test
  fun failureTextRethrowsTheOriginalCancellationException() {
    val cancellation = CancellationException("cancelled")

    val thrown =
      try {
        cancellation.uiFailureText(context, R.string.pet_error_save)
        null
      } catch (failure: CancellationException) {
        failure
      }

    assertThat(thrown).isSameInstanceAs(cancellation)
  }

  @Test
  fun presentationViewModelsDoNotExposeThrowableMessagesOrLiteralFailureFallbacks() {
    val violations =
      viewModelFiles().flatMap { file ->
        val source = file.readText()
        buildList {
          caughtThrowableNames(source).forEach { name ->
            if (Regex("""\b${Regex.escape(name)}\s*\.\s*message\b""").containsMatchIn(source)) {
              add("${file.relativeTo(projectRoot())}: exposes $name.message")
            }
          }
          USER_VISIBLE_LITERAL_PATTERNS.forEach { pattern ->
            pattern.findAll(source).forEach { match ->
              add(
                "${file.relativeTo(projectRoot())}: user-visible failure literal " +
                  match.value.replace('\n', ' ')
              )
            }
          }
        }
      }

    assertThat(violations).isEmpty()
  }

  private fun Context.forLocale(locale: Locale): Context {
    val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(configuration)
  }

  private fun caughtThrowableNames(source: String): List<String> =
    CATCHED_THROWABLE.findAll(source).map { it.groupValues[1] }.filter { it != "_" }.toList()

  private fun viewModelFiles(): List<File> =
    projectRoot()
      .resolve("src/main/java/com/woliveiras/petit/presentation")
      .walkTopDown()
      .filter { it.isFile && it.name.endsWith("ViewModel.kt") }
      .sortedBy { it.path }
      .toList()

  private fun projectRoot() = File(checkNotNull(System.getProperty("user.dir")))

  private companion object {
    val CATCHED_THROWABLE =
      Regex(
        """catch\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*:\s*""" +
          """(?:[A-Za-z_][A-Za-z0-9_.]*\.)?(?:[A-Za-z0-9_]*Exception|Throwable)\s*\)"""
      )
    val USER_VISIBLE_LITERAL_PATTERNS =
      listOf(
        Regex("""\b[A-Za-z][A-Za-z0-9]*Event\.Error\s*\(\s*"[^"]+""""),
        Regex("""\b(?:error|errorMessage)\s*=\s*"[^"]+""""),
      )
  }
}
