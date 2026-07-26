package com.woliveiras.petit.presentation.feature.backup

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.backup.BackupCompatibility
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.presentation.util.AppDisplayFormatter

@Composable
fun SavedBackupsRoute(
  onNavigateBack: () -> Unit,
  onCreateBackup: () -> Unit,
  onRestore: (String) -> Unit,
  viewModel: SavedBackupsViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  SavedBackupsScreen(
    state = state,
    copy = savedBackupsCopy(),
    onNavigateBack = onNavigateBack,
    onRefresh = viewModel::refresh,
    onReconnect = viewModel::authorizeAndRefresh,
    onCreateBackup = onCreateBackup,
    onDisconnect = viewModel::disconnect,
    onToggleSelection = viewModel::toggleSelection,
    onShowDetails = viewModel::showDetails,
    onDismissDetails = viewModel::dismissDetails,
    onRestore = onRestore,
    onRequestDeleteOne = viewModel::requestDeleteOne,
    onRequestDeleteSelected = viewModel::requestDeleteSelected,
    onRequestDeleteAll = viewModel::requestDeleteAll,
    onDismissDeletion = viewModel::dismissDeletion,
    onConfirmDeletion = viewModel::confirmDeletion,
    onRetryFailedDeletion = viewModel::retryFailedDeletion,
  )
}

@Composable
private fun savedBackupsCopy(): SavedBackupsCopy {
  val context = LocalContext.current
  val displayFormatter = AppDisplayFormatter(context)
  fun date(metadata: BackupMetadata): String = displayFormatter.dateTime(metadata.createdAt)
  fun size(bytes: Long): String = Formatter.formatFileSize(context, bytes)
  fun trigger(trigger: BackupTrigger): String =
    context.getString(
      when (trigger) {
        BackupTrigger.MANUAL -> R.string.backup_trigger_manual
        BackupTrigger.AUTOMATIC -> R.string.backup_trigger_automatic
        BackupTrigger.DATA_CHANGE -> R.string.backup_trigger_data_change
      }
    )
  fun compatibility(compatibility: BackupCompatibility): String =
    context.getString(
      when (compatibility) {
        BackupCompatibility.COMPATIBLE -> R.string.backup_compatibility_compatible
        BackupCompatibility.ARCHIVE_VERSION_TOO_NEW -> R.string.backup_compatibility_archive_new
        BackupCompatibility.SCHEMA_VERSION_TOO_NEW -> R.string.backup_compatibility_schema_new
        BackupCompatibility.INVALID -> R.string.backup_compatibility_invalid
      }
    )
  return SavedBackupsCopy(
    title = stringResource(R.string.backup_saved_title),
    loading = stringResource(R.string.backup_saved_loading),
    authorizationRequired = stringResource(R.string.backup_saved_authorization_required),
    reconnect = stringResource(R.string.backup_saved_reconnect),
    unavailable = stringResource(R.string.backup_saved_unavailable),
    empty = stringResource(R.string.backup_saved_empty),
    createBackup = stringResource(R.string.backup_saved_create),
    retry = stringResource(R.string.backup_saved_retry),
    deleteSelected = stringResource(R.string.backup_saved_delete_selected),
    deleteAll = stringResource(R.string.backup_saved_delete_all),
    disconnect = stringResource(R.string.backup_saved_disconnect),
    partialDeletion = stringResource(R.string.backup_saved_partial_deletion),
    retryFailedDeletion = stringResource(R.string.backup_saved_retry_deletion),
    cancel = stringResource(R.string.action_cancel),
    confirmDeletionTitle = stringResource(R.string.backup_saved_confirm_title),
    confirmSelectedDeletion = stringResource(R.string.backup_saved_confirm_selected),
    confirmAllDeletion = stringResource(R.string.backup_saved_confirm_all),
    confirmDelete = stringResource(R.string.backup_saved_confirm_delete),
    close = stringResource(R.string.backup_saved_close),
    restore = stringResource(R.string.backup_saved_restore),
    delete = stringResource(R.string.backup_saved_delete),
    backupTitle = { context.getString(R.string.backup_saved_item_title, date(it)) },
    backupSummary = {
      context.getString(
        R.string.backup_saved_item_summary,
        trigger(it.trigger),
        it.appVersion,
        it.contentCounts.pets,
        size(it.archiveSizeBytes),
      )
    },
    selectBackup = { context.getString(R.string.backup_saved_select_item, date(it)) },
    collectionTotal = { count, bytes ->
      context.getString(R.string.backup_saved_collection_total, count, size(bytes))
    },
    backupDetails = {
      val counts = it.contentCounts
      context.getString(
        R.string.backup_saved_details,
        date(it),
        trigger(it.trigger),
        it.appVersion,
        it.archiveFormatVersion,
        it.schemaVersion,
        compatibility(it.compatibility),
        counts.pets,
        counts.weights,
        counts.vaccinations,
        counts.dewormingRecords,
        counts.tasks,
        counts.assets,
        size(it.archiveSizeBytes),
      )
    },
    error = {
      context.getString(
        when (it) {
          SavedBackupsErrorCategory.AUTHORIZATION_REQUIRED ->
            R.string.backup_saved_error_authorization
          SavedBackupsErrorCategory.QUOTA_EXCEEDED -> R.string.backup_saved_error_quota
          SavedBackupsErrorCategory.RETRYABLE -> R.string.backup_saved_error_retryable
          SavedBackupsErrorCategory.PERMANENT -> R.string.backup_saved_error_permanent
        }
      )
    },
  )
}
