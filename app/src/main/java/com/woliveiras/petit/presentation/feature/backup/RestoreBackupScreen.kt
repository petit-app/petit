package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.domain.backup.restore.RestoreMode
import com.woliveiras.petit.presentation.components.PetitTopAppBar

data class RestoreBackupCopy(
  val title: String,
  val explanation: String,
  val merge: String,
  val mergeDescription: String,
  val replace: String,
  val replaceDescription: String,
  val applyPreferences: String,
  val restore: String,
  val restoring: String,
  val success: String,
  val authorizationRequired: String,
  val invalidBackup: String,
  val failed: String,
  val replaceConfirmationTitle: String,
  val replaceConfirmationMessage: String,
  val confirmReplace: String,
  val cancel: String,
)

@Composable
fun RestoreBackupScreen(
  state: RestoreBackupUiState,
  copy: RestoreBackupCopy,
  onNavigateBack: () -> Unit,
  onModeSelected: (RestoreMode) -> Unit,
  onApplyPreferencesChanged: (Boolean) -> Unit,
  onRestore: () -> Unit,
  onConfirmReplace: () -> Unit,
  onDismissReplace: () -> Unit,
) {
  Scaffold(
    topBar = { PetitTopAppBar(title = { Text(copy.title) }, onNavigateBack = onNavigateBack) }
  ) { padding ->
    RestoreBackupContent(
      state,
      copy,
      Modifier.fillMaxSize().padding(padding),
      onModeSelected,
      onApplyPreferencesChanged,
      onRestore,
    )
  }
  if (state.showReplaceConfirmation) {
    AlertDialog(
      onDismissRequest = onDismissReplace,
      title = { Text(copy.replaceConfirmationTitle) },
      text = { Text(copy.replaceConfirmationMessage) },
      confirmButton = { Button(onClick = onConfirmReplace) { Text(copy.confirmReplace) } },
      dismissButton = { TextButton(onClick = onDismissReplace) { Text(copy.cancel) } },
    )
  }
}

@Composable
fun RestoreBackupContent(
  state: RestoreBackupUiState,
  copy: RestoreBackupCopy,
  modifier: Modifier = Modifier,
  onModeSelected: (RestoreMode) -> Unit,
  onApplyPreferencesChanged: (Boolean) -> Unit,
  onRestore: () -> Unit,
) {
  val busy = state.operation is RestoreOperation.Restoring
  Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(copy.explanation)
    ModeRow(copy.merge, copy.mergeDescription, state.mode == RestoreMode.MERGE, !busy) {
      onModeSelected(RestoreMode.MERGE)
    }
    ModeRow(copy.replace, copy.replaceDescription, state.mode == RestoreMode.REPLACE, !busy) {
      onModeSelected(RestoreMode.REPLACE)
    }
    if (state.mode == RestoreMode.MERGE) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .toggleable(
              value = state.applyBackupPreferences,
              enabled = !busy,
              role = Role.Checkbox,
              onValueChange = onApplyPreferencesChanged,
            ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(state.applyBackupPreferences, onCheckedChange = null, enabled = !busy)
        Text(copy.applyPreferences)
      }
    }
    Button(onClick = onRestore, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
      Text(copy.restore)
    }
    when (state.operation) {
      RestoreOperation.Idle -> Unit
      is RestoreOperation.Restoring -> {
        val progress = state.operation.progress
        if (progress == null || progress.totalBytes == 0L) {
          LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
          LinearProgressIndicator(
            progress = { progress.bytesTransferred.toFloat() / progress.totalBytes.toFloat() },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        Status(copy.restoring)
      }
      is RestoreOperation.Success -> Status(copy.success)
      RestoreOperation.AuthorizationRequired -> Status(copy.authorizationRequired, true)
      RestoreOperation.InvalidBackup -> Status(copy.invalidBackup, true)
      RestoreOperation.Failed -> Status(copy.failed, true)
    }
  }
}

@Composable
private fun ModeRow(
  title: String,
  description: String,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .selectable(
          selected = selected,
          enabled = enabled,
          role = Role.RadioButton,
          onClick = onClick,
        )
        .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected, onClick = null, enabled = enabled)
    Column {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(description, style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun Status(message: String, error: Boolean = false) {
  Text(
    message,
    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
  )
}
