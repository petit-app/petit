package com.woliveiras.petit.presentation.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.data.lan.LanSyncState
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.feature.familygroup.FamilyGroupSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onNavigateBack: () -> Unit,
  onNavigateToDeleteAllData: () -> Unit,
  onNavigateToFamilyGroup: () -> Unit,
  onNavigateToPairing: () -> Unit,
  viewModel: SettingsViewModel = hiltViewModel(),
  exportImportViewModel: ExportImportViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val exportImportState by exportImportViewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val themeSheetState = rememberModalBottomSheetState()
  val languageSheetState = rememberModalBottomSheetState()
  val snackbarHostState = remember { SnackbarHostState() }

  var pendingExportFilename by remember { mutableStateOf<String?>(null) }

  // Export success/error messages
  val exportSuccessMessage = stringResource(R.string.export_success)
  val importSuccessMessage = stringResource(R.string.import_success)

  // File picker for export (save)
  val exportLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
      uri?.let { exportImportViewModel.writeExportToUri(it) }
      pendingExportFilename = null
    }

  // File picker for import (open)
  val importLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri?
      ->
      uri?.let { exportImportViewModel.startImport(it) }
    }

  // Handle export/import events
  LaunchedEffect(Unit) {
    exportImportViewModel.events.collect { event ->
      when (event) {
        is ExportImportEvent.ExportReady -> {
          pendingExportFilename = event.filename
          exportLauncher.launch(event.filename)
        }
        is ExportImportEvent.ExportSuccess -> {
          context.startActivity(Intent.createChooser(createBackupShareIntent(event.uri), null))
          snackbarHostState.showSnackbar(exportSuccessMessage)
        }
        is ExportImportEvent.ImportSuccess -> {
          snackbarHostState.showSnackbar(importSuccessMessage)
        }
        is ExportImportEvent.Error -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }

  // Theme selection bottom sheet
  if (uiState.showThemeDialog) {
    ModalBottomSheet(onDismissRequest = viewModel::hideThemeDialog, sheetState = themeSheetState) {
      ThemeSelectionContent(
        currentTheme = uiState.currentTheme,
        themes = uiState.availableThemes,
        onThemeSelected = viewModel::updateTheme,
        enabled = !uiState.isSavingPreference,
      )
    }
  }

  // Language selection bottom sheet
  if (uiState.showLanguageDialog) {
    ModalBottomSheet(
      onDismissRequest = viewModel::hideLanguageDialog,
      sheetState = languageSheetState,
    ) {
      LanguageSelectionContent(
        currentLanguage = uiState.currentLanguage,
        languages = uiState.availableLanguages,
        onLanguageSelected = viewModel::updateLanguage,
        enabled = !uiState.isSavingPreference,
      )
    }
  }

  // Import confirmation dialog
  if (exportImportState.showImportDialog) {
    ImportConfirmationDialog(
      analysis = exportImportState.importAnalysis,
      selectedResolution = exportImportState.selectedConflictResolution,
      onResolutionSelected = exportImportViewModel::selectConflictResolution,
      onConfirm = exportImportViewModel::confirmImport,
      onDismiss = exportImportViewModel::cancelImport,
    )
  }

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        onNavigateBack = onNavigateBack,
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Family Group section
      SettingsSectionHeader(title = stringResource(R.string.profile_section_family_group))

      FamilyGroupSection(
        familyGroupInfo = uiState.familyGroupInfo,
        lastSyncText = uiState.lastSyncText,
        onPairDevice = onNavigateToPairing,
        onJoinGroup = onNavigateToPairing,
        onManageGroup = onNavigateToFamilyGroup,
      )

      if (uiState.familyGroupInfo != null) {
        LanSyncSettingsCard(
          enabled = uiState.lanSyncEnabled,
          state = uiState.lanSyncState,
          onEnabledChange = viewModel::setLanSyncEnabled,
          onAttemptNow = viewModel::attemptLanSync,
        )
      }

      // Appearance section
      SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column {
          // Theme setting
          SettingsItem(
            icon = getThemeIcon(uiState.currentTheme),
            title = stringResource(R.string.settings_theme),
            subtitle = getThemeDisplayName(uiState.currentTheme),
            onClick = viewModel::showThemeDialog,
          )

          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )

          // Language setting
          SettingsItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.settings_language),
            subtitle = getLanguageDisplayName(uiState.currentLanguage),
            onClick = viewModel::showLanguageDialog,
          )

          LanguagePreferenceFeedback(
            restartRequired = uiState.languageRestartRequired,
            error = uiState.preferenceError,
          )
        }
      }

      // Data section
      SettingsSectionHeader(title = stringResource(R.string.settings_section_data))

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column {
          // Export
          SettingsItem(
            icon = Icons.Default.Upload,
            title = stringResource(R.string.settings_export),
            subtitle = stringResource(R.string.settings_export_description),
            onClick = { exportImportViewModel.startExportAll() },
            isLoading = exportImportState.isExporting,
          )

          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )

          // Import
          SettingsItem(
            icon = Icons.Default.Download,
            title = stringResource(R.string.settings_import),
            subtitle = stringResource(R.string.settings_import_description),
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            isLoading = exportImportState.isImporting,
          )

          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )

          // Delete all data
          SettingsItemDanger(
            icon = Icons.Default.DeleteForever,
            title = stringResource(R.string.settings_delete_all_data),
            subtitle = stringResource(R.string.settings_delete_all_data_description),
            onClick = onNavigateToDeleteAllData,
          )
        }
      }

      // About section
      SettingsSectionHeader(title = stringResource(R.string.settings_section_about))

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
      ) {
        // Version info
        ListItem(
          colors =
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          headlineContent = { Text(stringResource(R.string.settings_version)) },
          supportingContent = { Text("1.0.0") },
        )
      }
    }
  }
}

@Composable
private fun lanSyncStatusText(state: LanSyncState): String =
  when (state) {
    LanSyncState.Idle -> stringResource(R.string.lan_sync_idle)
    LanSyncState.Discovering -> stringResource(R.string.lan_sync_discovering)
    is LanSyncState.Syncing -> stringResource(R.string.lan_sync_syncing, state.peerName)
    is LanSyncState.Synced -> stringResource(R.string.lan_sync_synced, state.peerName)
    LanSyncState.PeerUnavailable -> stringResource(R.string.lan_sync_peer_unavailable)
    is LanSyncState.Error -> stringResource(R.string.lan_sync_error)
  }

@Composable
internal fun LanSyncSettingsCard(
  enabled: Boolean,
  state: LanSyncState,
  onEnabledChange: (Boolean) -> Unit,
  onAttemptNow: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column {
      ListItem(
        colors =
          ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = {
          Icon(
            Icons.Default.Wifi,
            contentDescription = stringResource(R.string.lan_sync_title),
            tint = MaterialTheme.colorScheme.primary,
          )
        },
        headlineContent = { Text(stringResource(R.string.lan_sync_title)) },
        supportingContent = { Text(lanSyncStatusText(state)) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onEnabledChange) },
      )
      TextButton(
        onClick = onAttemptNow,
        modifier = Modifier.align(Alignment.End).padding(horizontal = 8.dp),
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.lan_sync_try_now))
      }
    }
  }
}

@Composable
private fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
  )
}

@Composable
private fun SettingsItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  isLoading: Boolean = false,
) {
  ListItem(
    modifier = Modifier.clickable(enabled = !isLoading, onClick = onClick),
    colors =
      ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    leadingContent = {
      Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
    },
    headlineContent = { Text(title) },
    supportingContent = { Text(subtitle) },
    trailingContent =
      if (isLoading) {
        { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
      } else {
        {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
  )
}

@Composable
internal fun ThemeSelectionContent(
  currentTheme: AppTheme,
  themes: List<AppTheme>,
  onThemeSelected: (AppTheme) -> Unit,
  enabled: Boolean = true,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
    Text(
      text = stringResource(R.string.settings_theme),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )

    themes.forEach { theme ->
      ListItem(
        modifier =
          Modifier.selectable(
            selected = theme == currentTheme,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = { onThemeSelected(theme) },
          ),
        leadingContent = {
          Icon(
            imageVector = getThemeIcon(theme),
            contentDescription = getThemeDisplayName(theme),
            tint = MaterialTheme.colorScheme.primary,
          )
        },
        headlineContent = { Text(getThemeDisplayName(theme)) },
        trailingContent = {
          if (theme == currentTheme) {
            Icon(
              Icons.Default.Check,
              contentDescription = stringResource(R.string.cd_icon_check),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        },
      )
    }
  }
}

@Composable
internal fun LanguagePreferenceFeedback(restartRequired: Boolean, error: String?) {
  if (restartRequired) {
    Text(
      text = stringResource(R.string.settings_language_restart_message),
      color = MaterialTheme.colorScheme.primary,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
  }

  error?.let {
    Text(
      text = it,
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
  }
}

@Composable
internal fun LanguageSelectionContent(
  currentLanguage: AppLanguage,
  languages: List<AppLanguage>,
  onLanguageSelected: (AppLanguage) -> Unit,
  enabled: Boolean = true,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = stringResource(R.string.settings_language),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )

    LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
      items(languages) { language ->
        ListItem(
          modifier =
            Modifier.selectable(
              selected = language == currentLanguage,
              enabled = enabled,
              role = Role.RadioButton,
              onClick = { onLanguageSelected(language) },
            ),
          headlineContent = { Text(getLanguageDisplayName(language)) },
          supportingContent =
            if (language != AppLanguage.SYSTEM) {
              { Text(language.code) }
            } else null,
          trailingContent = {
            if (language == currentLanguage) {
              Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_icon_check),
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          },
        )
      }
    }
  }
}

@Composable
private fun getThemeIcon(theme: AppTheme): ImageVector {
  return when (theme) {
    AppTheme.SYSTEM -> Icons.Default.PhoneAndroid
    AppTheme.LIGHT -> Icons.Default.LightMode
    AppTheme.DARK -> Icons.Default.DarkMode
  }
}

@Composable
private fun getThemeDisplayName(theme: AppTheme): String {
  return when (theme) {
    AppTheme.SYSTEM -> stringResource(R.string.settings_theme_system)
    AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
    AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
  }
}

@Composable
private fun getLanguageDisplayName(language: AppLanguage): String {
  return when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
    AppLanguage.PORTUGUESE_BR -> stringResource(R.string.settings_language_portuguese_br)
  }
}

@Composable
private fun ImportConfirmationDialog(
  analysis: com.woliveiras.petit.domain.model.ImportAnalysis?,
  selectedResolution: ConflictResolution,
  onResolutionSelected: (ConflictResolution) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.import_dialog_title)) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Import summary
        analysis?.let { a ->
          Text(
            text = stringResource(R.string.import_dialog_summary),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.import_pets_count, a.totalPets))
            Text(stringResource(R.string.import_weights_count, a.totalWeightEntries))
            Text(stringResource(R.string.import_vaccinations_count, a.totalVaccinationEntries))
            Text(stringResource(R.string.import_dewormings_count, a.totalDewormingEntries))
            Text(stringResource(R.string.import_tasks_count, a.totalTasks))
          }

          // Conflict resolution options (only if there are conflicts)
          if (a.hasConflicts) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = stringResource(R.string.import_conflict_title, a.conflictingPetNames.size),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error,
            )

            Text(
              text = a.conflictingPetNames.joinToString(", "),
              style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = stringResource(R.string.import_conflict_resolution_title),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )

            ConflictResolution.entries.forEach { resolution ->
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .selectable(
                      selected = selectedResolution == resolution,
                      onClick = { onResolutionSelected(resolution) },
                      role = Role.RadioButton,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(selected = selectedResolution == resolution, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = getConflictResolutionTitle(resolution),
                    style = MaterialTheme.typography.bodyMedium,
                  )
                  Text(
                    text = getConflictResolutionDescription(resolution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_confirm)) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  )
}

@Composable
private fun getConflictResolutionTitle(resolution: ConflictResolution): String {
  return when (resolution) {
    ConflictResolution.REPLACE -> stringResource(R.string.import_conflict_replace)
    ConflictResolution.KEEP -> stringResource(R.string.import_conflict_keep)
    ConflictResolution.MERGE -> stringResource(R.string.import_conflict_merge)
  }
}

@Composable
private fun getConflictResolutionDescription(resolution: ConflictResolution): String {
  return when (resolution) {
    ConflictResolution.REPLACE -> stringResource(R.string.import_conflict_replace_desc)
    ConflictResolution.KEEP -> stringResource(R.string.import_conflict_keep_desc)
    ConflictResolution.MERGE -> stringResource(R.string.import_conflict_merge_desc)
  }
}

@Composable
private fun SettingsItemDanger(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
) {
  ListItem(
    modifier = Modifier.clickable(onClick = onClick),
    colors =
      ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    leadingContent = {
      Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.error)
    },
    headlineContent = { Text(text = title, color = MaterialTheme.colorScheme.error) },
    supportingContent = {
      Text(text = subtitle, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
    },
    trailingContent = {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
      )
    },
  )
}
