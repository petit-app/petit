package com.woliveiras.petit.presentation.feature.backup

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.presentation.util.AppDisplayFormatter

@Composable
fun BackupSettingsRoute(
  onNavigateBack: () -> Unit,
  onNavigateToHistory: () -> Unit,
  viewModel: BackupSettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  BackupSettingsScreen(
    state = state,
    copy = backupSettingsCopy(),
    onNavigateBack = onNavigateBack,
    onAuthorize = viewModel::authorize,
    onAutomaticChanged = viewModel::setAutomaticBackupEnabled,
    onNetworkChanged = viewModel::setNetworkRequirement,
    onNotifyChanged = viewModel::setNotifyAfterSuccess,
    onBackUpNow = viewModel::backUpNow,
    onViewAllHistory = onNavigateToHistory,
    onDisconnect = viewModel::disconnect,
  )
}

@Composable
internal fun backupSettingsCopy(): BackupSettingsCopy {
  val context = LocalContext.current
  val displayFormatter = AppDisplayFormatter(context)
  fun status(status: BackupAttemptStatus): String =
    context.getString(
      when (status) {
        BackupAttemptStatus.RUNNING -> R.string.backup_status_running
        BackupAttemptStatus.SUCCEEDED -> R.string.backup_status_succeeded
        BackupAttemptStatus.RETRYING -> R.string.backup_status_retrying
        BackupAttemptStatus.FAILED -> R.string.backup_status_failed
        BackupAttemptStatus.CANCELLED -> R.string.backup_status_cancelled
        BackupAttemptStatus.AUTHORIZATION_REQUIRED -> R.string.backup_status_authorization_required
      }
    )
  fun instant(value: java.time.Instant?): String = value?.let(displayFormatter::dateTime) ?: "—"
  fun attempt(attempt: BackupAttempt): String =
    context.getString(
      R.string.backup_preferences_attempt,
      when (attempt.trigger) {
        BackupTrigger.MANUAL -> context.getString(R.string.backup_trigger_manual)
        BackupTrigger.AUTOMATIC -> context.getString(R.string.backup_trigger_automatic)
        BackupTrigger.DATA_CHANGE -> context.getString(R.string.backup_trigger_data_change)
      },
      instant(attempt.startedAt),
      instant(attempt.completedAt),
      status(attempt.status),
      Formatter.formatFileSize(context, attempt.archiveSizeBytes ?: 0L),
      attempt.contentCounts?.pets ?: 0,
      attempt.contentCounts?.tasks ?: 0,
    )
  return BackupSettingsCopy(
    title = stringResource(R.string.backup_preferences_title),
    serviceState = stringResource(R.string.backup_preferences_service_state),
    authorizationLabel = {
      when (it) {
        BackupAuthorizationState.Disconnected ->
          context.getString(R.string.backup_preferences_disconnected)
        BackupAuthorizationState.Authorizing ->
          context.getString(R.string.backup_preferences_authorizing)
        is BackupAuthorizationState.Authorized ->
          context.getString(
            R.string.backup_preferences_connected,
            it.accountLabel?.let { label -> " · $label" }.orEmpty(),
          )
        BackupAuthorizationState.AuthorizationRequired ->
          context.getString(R.string.backup_preferences_authorization_required)
        is BackupAuthorizationState.Unavailable ->
          context.getString(R.string.backup_preferences_unavailable)
      }
    },
    connect = stringResource(R.string.backup_preferences_connect),
    automaticBackup = stringResource(R.string.backup_preferences_automatic),
    automaticBackupDescription = stringResource(R.string.backup_preferences_automatic_description),
    inexactSchedule = stringResource(R.string.backup_preferences_inexact),
    unmeteredOnly = stringResource(R.string.backup_preferences_unmetered),
    unmeteredDescription = stringResource(R.string.backup_preferences_unmetered_description),
    notifyAfterSuccess = stringResource(R.string.backup_preferences_notify),
    notifyDescription = stringResource(R.string.backup_preferences_notify_description),
    backUpNow = stringResource(R.string.backup_now),
    history = stringResource(R.string.backup_preferences_history),
    noHistory = stringResource(R.string.backup_preferences_no_history),
    viewAllHistory = stringResource(R.string.backup_preferences_view_all_history),
    attemptSummary = ::attempt,
    manualStatus = ::status,
    disconnect = stringResource(R.string.backup_preferences_disconnect),
    disconnectConfirmationTitle = stringResource(R.string.backup_preferences_disconnect_title),
    disconnectConfirmationMessage = stringResource(R.string.backup_preferences_disconnect_message),
    confirmDisconnect = stringResource(R.string.backup_preferences_disconnect_confirm),
    cancel = stringResource(R.string.action_cancel),
    error = {
      context.getString(
        when (it) {
          BackupSettingsError.SETTINGS_UPDATE_FAILED -> R.string.backup_preferences_error_update
          BackupSettingsError.AUTHORIZATION_UNAVAILABLE ->
            R.string.backup_preferences_error_authorization
          BackupSettingsError.DISCONNECT_FAILED -> R.string.backup_preferences_error_disconnect
        }
      )
    },
  )
}
