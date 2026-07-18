package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R

@Composable
fun RestoreBackupRoute(
  onNavigateBack: () -> Unit,
  viewModel: RestoreBackupViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  RestoreBackupScreen(
    state = state,
    copy = restoreBackupCopy(),
    onNavigateBack = onNavigateBack,
    onModeSelected = viewModel::selectMode,
    onApplyPreferencesChanged = viewModel::setApplyBackupPreferences,
    onRestore = viewModel::requestRestore,
    onConfirmReplace = viewModel::confirmReplace,
    onDismissReplace = viewModel::dismissReplaceConfirmation,
  )
}

@Composable
private fun restoreBackupCopy() =
  RestoreBackupCopy(
    title = stringResource(R.string.restore_backup_title),
    explanation = stringResource(R.string.restore_backup_explanation),
    merge = stringResource(R.string.restore_backup_merge),
    mergeDescription = stringResource(R.string.restore_backup_merge_description),
    replace = stringResource(R.string.restore_backup_replace),
    replaceDescription = stringResource(R.string.restore_backup_replace_description),
    applyPreferences = stringResource(R.string.restore_backup_apply_preferences),
    restore = stringResource(R.string.restore_backup_action),
    restoring = stringResource(R.string.restore_backup_restoring),
    success = stringResource(R.string.restore_backup_success),
    authorizationRequired = stringResource(R.string.restore_backup_authorization_required),
    invalidBackup = stringResource(R.string.restore_backup_invalid),
    failed = stringResource(R.string.restore_backup_failed),
    replaceConfirmationTitle = stringResource(R.string.restore_backup_confirm_title),
    replaceConfirmationMessage = stringResource(R.string.restore_backup_confirm_message),
    confirmReplace = stringResource(R.string.restore_backup_confirm_action),
    cancel = stringResource(R.string.action_cancel),
  )
