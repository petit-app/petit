package com.woliveiras.petit.presentation.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ImportAnalysis
import com.woliveiras.petit.domain.model.MergeResult
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ExportImportOperations {
  suspend fun exportAll(): ExportBundle

  suspend fun exportForPet(petId: String): ExportBundle

  fun generateExportFilename(): String

  suspend fun writeExportToUri(bundle: ExportBundle, uri: Uri)

  suspend fun readImportFromUri(uri: Uri): ExportBundle

  suspend fun analyzeImport(bundle: ExportBundle): ImportAnalysis

  suspend fun importData(bundle: ExportBundle, resolution: ConflictResolution): MergeResult
}

private class UseCaseExportImportOperations(private val useCase: ExportImportUseCase) :
  ExportImportOperations {
  override suspend fun exportAll(): ExportBundle = useCase.exportAll()

  override suspend fun exportForPet(petId: String): ExportBundle = useCase.exportForPet(petId)

  override fun generateExportFilename(): String = useCase.generateExportFilename()

  override suspend fun writeExportToUri(bundle: ExportBundle, uri: Uri) =
    useCase.writeExportToUri(bundle, uri)

  override suspend fun readImportFromUri(uri: Uri): ExportBundle = useCase.readImportFromUri(uri)

  override suspend fun analyzeImport(bundle: ExportBundle): ImportAnalysis =
    useCase.analyzeImport(bundle)

  override suspend fun importData(bundle: ExportBundle, resolution: ConflictResolution) =
    useCase.importData(bundle, resolution)
}

/** UI state for export/import operations. */
data class ExportImportUiState(
  val isExporting: Boolean = false,
  val isImporting: Boolean = false,
  val showImportDialog: Boolean = false,
  val importAnalysis: ImportAnalysis? = null,
  val pendingImportBundle: ExportBundle? = null,
  val pendingExportBundle: ExportBundle? = null,
  val selectedConflictResolution: ConflictResolution = ConflictResolution.MERGE,
)

/** Events emitted by export/import ViewModel. */
sealed class ExportImportEvent {
  data class ExportReady(val filename: String, val bundle: ExportBundle) : ExportImportEvent()

  data class ExportSuccess(val uri: Uri) : ExportImportEvent()

  data object ImportSuccess : ExportImportEvent()

  data class Error(val message: String) : ExportImportEvent()
}

@HiltViewModel
class ExportImportViewModel
internal constructor(
  @ApplicationContext private val context: Context,
  private val operations: ExportImportOperations,
) : ViewModel() {

  @Inject
  constructor(
    @ApplicationContext context: Context,
    exportImportUseCase: ExportImportUseCase,
  ) : this(context, UseCaseExportImportOperations(exportImportUseCase))

  private val _uiState = MutableStateFlow(ExportImportUiState())
  val uiState: StateFlow<ExportImportUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<ExportImportEvent>()
  val events: SharedFlow<ExportImportEvent> = _events.asSharedFlow()

  /** Start export of all data. */
  fun startExportAll() {
    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true) }
      try {
        val bundle = operations.exportAll()
        val filename = operations.generateExportFilename()
        _uiState.update { it.copy(pendingExportBundle = bundle) }
        _events.emit(ExportImportEvent.ExportReady(filename, bundle))
      } catch (_: Exception) {
        _events.emit(ExportImportEvent.Error(context.getString(R.string.export_error_failed)))
      } finally {
        _uiState.update { it.copy(isExporting = false) }
      }
    }
  }

  /** Start export for a specific pet. */
  fun startExportForPet(petId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true) }
      try {
        val bundle = operations.exportForPet(petId)
        val filename = operations.generateExportFilename()
        _uiState.update { it.copy(pendingExportBundle = bundle) }
        _events.emit(ExportImportEvent.ExportReady(filename, bundle))
      } catch (_: Exception) {
        _events.emit(ExportImportEvent.Error(context.getString(R.string.export_error_failed)))
      } finally {
        _uiState.update { it.copy(isExporting = false) }
      }
    }
  }

  /** Write the pending export to a URI. */
  fun writeExportToUri(uri: Uri) {
    val bundle = _uiState.value.pendingExportBundle ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true) }
      try {
        operations.writeExportToUri(bundle, uri)
        _uiState.update { it.copy(pendingExportBundle = null) }
        _events.emit(ExportImportEvent.ExportSuccess(uri))
      } catch (_: Exception) {
        _events.emit(ExportImportEvent.Error(context.getString(R.string.export_error_save_file)))
      } finally {
        _uiState.update { it.copy(isExporting = false) }
      }
    }
  }

  /** Start import from a URI. */
  fun startImport(uri: Uri) {
    viewModelScope.launch {
      _uiState.update { it.copy(isImporting = true) }
      try {
        val bundle = operations.readImportFromUri(uri)
        val analysis = operations.analyzeImport(bundle)

        _uiState.update {
          it.copy(showImportDialog = true, importAnalysis = analysis, pendingImportBundle = bundle)
        }
      } catch (_: Exception) {
        _events.emit(ExportImportEvent.Error(context.getString(R.string.import_error_invalid_file)))
      } finally {
        _uiState.update { it.copy(isImporting = false) }
      }
    }
  }

  /** Update the selected conflict resolution strategy. */
  fun selectConflictResolution(resolution: ConflictResolution) {
    _uiState.update { it.copy(selectedConflictResolution = resolution) }
  }

  /** Confirm and execute the import. */
  fun confirmImport() {
    val bundle = _uiState.value.pendingImportBundle ?: return
    val resolution = _uiState.value.selectedConflictResolution

    viewModelScope.launch {
      _uiState.update { it.copy(isImporting = true, showImportDialog = false) }
      try {
        operations.importData(bundle, resolution)
        _uiState.update { it.copy(pendingImportBundle = null, importAnalysis = null) }
        _events.emit(ExportImportEvent.ImportSuccess)
      } catch (_: Exception) {
        _events.emit(ExportImportEvent.Error(context.getString(R.string.import_error_failed)))
      } finally {
        _uiState.update { it.copy(isImporting = false) }
      }
    }
  }

  /** Cancel the import. */
  fun cancelImport() {
    _uiState.update {
      it.copy(showImportDialog = false, pendingImportBundle = null, importAnalysis = null)
    }
  }
}
