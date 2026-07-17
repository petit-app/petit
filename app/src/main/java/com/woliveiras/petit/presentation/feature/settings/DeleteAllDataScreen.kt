package com.woliveiras.petit.presentation.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.presentation.components.PetitTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAllDataScreen(
  onNavigateBack: () -> Unit,
  onDataDeleted: () -> Unit,
  viewModel: DeleteAllDataViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  if (uiState.isDeleted) {
    DeleteSuccessScreen(onGoHome = onDataDeleted)
  } else {
    DeleteConfirmationContent(
      uiState = uiState,
      onNavigateBack = onNavigateBack,
      onConfirmTextChanged = viewModel::updateConfirmText,
      onDelete = { confirmWord -> viewModel.deleteAllData(confirmWord) },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteSuccessScreen(onGoHome: () -> Unit) {
  Scaffold { padding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = stringResource(R.string.cd_icon_check),
          modifier = Modifier.size(100.dp),
          tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = stringResource(R.string.delete_all_data_success),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.pet_delete_go_home))
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeleteConfirmationContent(
  uiState: DeleteAllDataUiState,
  onNavigateBack: () -> Unit,
  onConfirmTextChanged: (String) -> Unit,
  onDelete: (String) -> Unit,
) {
  BackHandler(enabled = uiState.isDeleting) {}

  val confirmWord = stringResource(R.string.delete_all_data_confirm_word)
  val isConfirmEnabled = uiState.confirmText == confirmWord && !uiState.isDeleting

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.delete_all_data_title)) },
        onNavigateBack = { if (!uiState.isDeleting) onNavigateBack() },
      )
    }
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
      // Warning card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = stringResource(R.string.cd_icon_warning),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(32.dp),
          )

          Spacer(modifier = Modifier.width(16.dp))

          Column {
            Text(
              text = stringResource(R.string.pet_delete_warning_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = stringResource(R.string.delete_all_data_warning),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Data that will be deleted
      Text(
        text = stringResource(R.string.pet_delete_records_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DataItemRow(text = stringResource(R.string.delete_all_data_pets))
          DataItemRow(text = stringResource(R.string.delete_all_data_weights))
          DataItemRow(text = stringResource(R.string.delete_all_data_vaccinations))
          DataItemRow(text = stringResource(R.string.delete_all_data_dewormings))
          DataItemRow(text = stringResource(R.string.delete_all_data_reminders))
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Confirmation input
      Text(
        text = stringResource(R.string.delete_all_data_confirm_instruction),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
      )

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = uiState.confirmText,
        onValueChange = onConfirmTextChanged,
        modifier = Modifier.fillMaxWidth(),
        enabled = !uiState.isDeleting,
        singleLine = true,
        isError = uiState.confirmText.isNotEmpty() && uiState.confirmText != confirmWord,
        supportingText = {
          if (uiState.confirmText.isNotEmpty() && uiState.confirmText != confirmWord) {
            Text(
              text = stringResource(R.string.delete_all_confirm_hint, confirmWord),
              color = MaterialTheme.colorScheme.error,
            )
          }
        },
      )

      uiState.errorMessage?.let { message ->
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = message,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      Spacer(modifier = Modifier.weight(1f))
      Spacer(modifier = Modifier.height(24.dp))

      // Action buttons
      Button(
        onClick = { onDelete(confirmWord) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = isConfirmEnabled,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
      ) {
        if (uiState.isDeleting) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.onError,
          )
        } else {
          Icon(
            imageVector = Icons.Default.DeleteForever,
            contentDescription = stringResource(R.string.cd_icon_delete),
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              stringResource(
                if (uiState.errorMessage == null) {
                  R.string.pet_delete_confirm_button
                } else {
                  R.string.action_retry
                }
              ),
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = onNavigateBack,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = !uiState.isDeleting,
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    }
  }
}

@Composable
private fun DataItemRow(text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = Icons.Default.DeleteForever,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.error,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
  }
}
