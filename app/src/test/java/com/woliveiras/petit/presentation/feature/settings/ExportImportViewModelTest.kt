package com.woliveiras.petit.presentation.feature.settings

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.ImportAnalysis
import com.woliveiras.petit.domain.model.MergeResult
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExportImportViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun exportAllFailureUsesTheEffectiveLocalizedMessageWithoutProviderDetails() =
    runTest(dispatcher) {
      for ((locale, expected) in exportErrorMessages) {
        val operations = FailingOperations(FailurePoint.EXPORT_ALL, exportAllFailureDetail)
        val actual = errorFrom(localizedContext(locale), operations) { it.startExportAll() }

        assertLocalizedMessage(actual, expected, exportAllFailureDetail)
      }
    }

  @Test
  fun exportForPetFailureUsesTheEffectiveLocalizedMessageWithoutProviderDetails() =
    runTest(dispatcher) {
      for ((locale, expected) in exportErrorMessages) {
        val operations = FailingOperations(FailurePoint.EXPORT_FOR_PET, exportForPetFailureDetail)
        val actual =
          errorFrom(localizedContext(locale), operations) { it.startExportForPet("pet-123") }

        assertLocalizedMessage(actual, expected, exportForPetFailureDetail)
      }
    }

  @Test
  fun writeExportFailureUsesTheEffectiveLocalizedMessageWithoutProviderDetails() =
    runTest(dispatcher) {
      for ((locale, expected) in exportSaveFileErrorMessages) {
        val operations = FailingOperations(FailurePoint.WRITE_EXPORT, writeExportFailureDetail)
        val viewModel = ExportImportViewModel(localizedContext(locale), operations)

        viewModel.startExportAll()
        advanceUntilIdle()

        val actual = errorFrom(viewModel) { it.writeExportToUri(testUri) }
        assertLocalizedMessage(actual, expected, writeExportFailureDetail)
      }
    }

  @Test
  fun readImportFailureUsesTheEffectiveLocalizedMessageWithoutParserDetails() =
    runTest(dispatcher) {
      for ((locale, expected) in importInvalidFileErrorMessages) {
        val operations = FailingOperations(FailurePoint.READ_IMPORT, readImportFailureDetail)
        val actual = errorFrom(localizedContext(locale), operations) { it.startImport(testUri) }

        assertLocalizedMessage(actual, expected, readImportFailureDetail)
      }
    }

  @Test
  fun confirmedImportFailureUsesTheEffectiveLocalizedMessageWithoutProviderDetails() =
    runTest(dispatcher) {
      for ((locale, expected) in importFailedErrorMessages) {
        val operations = FailingOperations(FailurePoint.IMPORT_DATA, importFailureDetail)
        val viewModel = ExportImportViewModel(localizedContext(locale), operations)

        viewModel.startImport(testUri)
        advanceUntilIdle()

        val actual = errorFrom(viewModel) { it.confirmImport() }
        assertLocalizedMessage(actual, expected, importFailureDetail)
      }
    }

  private suspend fun TestScope.errorFrom(
    context: Context,
    operations: ExportImportOperations,
    action: (ExportImportViewModel) -> Unit,
  ): String = errorFrom(ExportImportViewModel(context, operations), action)

  private suspend fun TestScope.errorFrom(
    viewModel: ExportImportViewModel,
    action: (ExportImportViewModel) -> Unit,
  ): String {
    val event = async { viewModel.events.first() }
    runCurrent()

    action(viewModel)
    advanceUntilIdle()

    return (event.await() as ExportImportEvent.Error).message
  }

  private fun assertLocalizedMessage(actual: String, expected: String, technicalDetail: String) {
    assertThat(actual).isEqualTo(expected)
    assertThat(actual).isNotEqualTo(technicalDetail)
  }

  private fun localizedContext(locale: Locale): Context {
    val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
    return context.createConfigurationContext(configuration)
  }

  private class FailingOperations(
    private val failurePoint: FailurePoint,
    private val failureDetail: String,
  ) : ExportImportOperations {
    override suspend fun exportAll(): ExportBundle {
      failAt(FailurePoint.EXPORT_ALL)
      return testBundle
    }

    override suspend fun exportForPet(petId: String): ExportBundle {
      failAt(FailurePoint.EXPORT_FOR_PET)
      return testBundle
    }

    override fun generateExportFilename(): String = "petit-export.json"

    override suspend fun writeExportToUri(bundle: ExportBundle, uri: Uri) {
      failAt(FailurePoint.WRITE_EXPORT)
    }

    override suspend fun readImportFromUri(uri: Uri): ExportBundle {
      failAt(FailurePoint.READ_IMPORT)
      return testBundle
    }

    override suspend fun analyzeImport(bundle: ExportBundle): ImportAnalysis = testImportAnalysis

    override suspend fun importData(
      bundle: ExportBundle,
      resolution: ConflictResolution,
    ): MergeResult {
      failAt(FailurePoint.IMPORT_DATA)
      return MergeResult(0, 0, 0, 0, 0)
    }

    private fun failAt(point: FailurePoint) {
      if (failurePoint == point) throw IllegalStateException(failureDetail)
    }
  }

  private enum class FailurePoint {
    EXPORT_ALL,
    EXPORT_FOR_PET,
    WRITE_EXPORT,
    READ_IMPORT,
    IMPORT_DATA,
  }

  private companion object {
    val testUri: Uri = Uri.parse("content://petit/export.json")
    const val exportAllFailureDetail = "Provider response 503: internal export detail"
    const val exportForPetFailureDetail = "Provider response 503: pet export detail"
    const val writeExportFailureDetail = "Provider output detail: cannot open target"
    const val readImportFailureDetail = "Parser detail: unexpected token at byte 19"
    const val importFailureDetail = "Import provider detail: conflict resolver unavailable"

    val exportErrorMessages =
      listOf(
        Locale.ENGLISH to "Export failed",
        Locale.forLanguageTag("pt-BR") to "Erro na exportação",
      )
    val exportSaveFileErrorMessages =
      listOf(
        Locale.ENGLISH to "Failed to save file",
        Locale.forLanguageTag("pt-BR") to "Erro ao salvar arquivo",
      )
    val importInvalidFileErrorMessages =
      listOf(Locale.ENGLISH to "Invalid file", Locale.forLanguageTag("pt-BR") to "Arquivo inválido")
    val importFailedErrorMessages =
      listOf(
        Locale.ENGLISH to "Import failed",
        Locale.forLanguageTag("pt-BR") to "Erro na importação",
      )

    val testBundle =
      ExportBundle(
        metadata = ExportMetadata("1.0", "2026-07-20T00:00:00Z"),
        pets = emptyList(),
        weightEntries = emptyList(),
        vaccinationEntries = emptyList(),
        dewormingEntries = emptyList(),
        tasks = emptyList(),
      )
    val testImportAnalysis =
      ImportAnalysis(
        totalPets = 0,
        totalWeightEntries = 0,
        totalVaccinationEntries = 0,
        totalDewormingEntries = 0,
        totalTasks = 0,
        conflictingPetNames = emptyList(),
        schemaVersion = 1,
        exportDate = "2026-07-20T00:00:00Z",
      )
  }
}
