package com.woliveiras.petit.presentation.feature.weight

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Screen for adding or editing a weight entry. After saving, navigates back to the weight list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightEntryScreen(
  petId: String,
  entryId: String? = null,
  onNavigateBack: () -> Unit,
  viewModel: WeightFormViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showDatePicker by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Load entry for editing if entryId is provided
  LaunchedEffect(entryId) {
    if (entryId != null) {
      viewModel.loadEntryForEdit(entryId)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is WeightFormEvent.WeightSaved -> onNavigateBack()
        is WeightFormEvent.WeightDeleted -> onNavigateBack()
        is WeightFormEvent.Error -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }

  // Date Picker Dialog
  if (showDatePicker) {
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis =
          uiState.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
      )

    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
              if (!date.isAfter(LocalDate.now())) {
                viewModel.updateDate(date)
              }
            }
            showDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    ) {
      DatePicker(state = datePickerState, showModeToggle = false)
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PetitTopAppBar(
        title = {
          Column {
            Text(
              if (uiState.isEditMode) {
                stringResource(R.string.weight_edit_title)
              } else {
                stringResource(R.string.weight_add_title)
              }
            )
            if (uiState.petName.isNotEmpty()) {
              Text(
                text = uiState.petName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        onNavigateBack = onNavigateBack,
        actions = {
          if (uiState.isEditMode) {
            IconButton(onClick = { viewModel.deleteCurrentEntry() }) {
              Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        },
      )
    },
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Weight Input Card
      WeightInputCard(
        weightValue = uiState.weightValue,
        weightUnit = uiState.weightUnit,
        onWeightChange = viewModel::updateWeight,
        onUnitChange = viewModel::updateWeightUnit,
        weightError = uiState.weightError,
      )

      // Date Field
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.weight_field_date),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Card(
          modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text(
            text = rememberAppDisplayFormatter().shortDate(uiState.date),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          )
        }
      }

      // Note Field
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.weight_field_note),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
          value = uiState.note,
          onValueChange = viewModel::updateNote,
          placeholder = { Text(stringResource(R.string.weight_field_note_placeholder)) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 4,
          maxLines = 5,
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      // Save Button
      Button(
        onClick = viewModel::saveWeight,
        enabled = uiState.weightValue.isNotBlank() && !uiState.isSaving,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        if (uiState.isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = stringResource(R.string.action_save),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
internal fun WeightInputCard(
  weightValue: String,
  weightUnit: WeightUnit,
  onWeightChange: (String) -> Unit,
  onUnitChange: (WeightUnit) -> Unit,
  weightError: String?,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // "Peso" Label
      Text(
        text = stringResource(R.string.weight_field_weight).replace(" *", ""),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Weight Display with Input
      Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
        BasicTextField(
          value = weightValue,
          onValueChange = onWeightChange,
          textStyle =
            TextStyle(
              fontSize = 64.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.primary,
            ),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier.width(150.dp),
          decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
              if (weightValue.isEmpty()) {
                Text(
                  text = "0",
                  style =
                    TextStyle(
                      fontSize = 64.sp,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Center,
                      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    ),
                )
              }
              innerTextField()
            }
          },
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
          text = if (weightUnit == WeightUnit.KG) "kg" else "g",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }

      // Error message
      if (weightError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = weightError,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Unit Toggle
      UnitToggle(selectedUnit = weightUnit, onUnitSelected = onUnitChange)
    }
  }
}

@Composable
private fun UnitToggle(selectedUnit: WeightUnit, onUnitSelected: (WeightUnit) -> Unit) {
  Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
    FilterChip(
      selected = selectedUnit == WeightUnit.KG,
      onClick = { onUnitSelected(WeightUnit.KG) },
      label = {
        Text(
          text = stringResource(R.string.weight_unit_kilograms),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
        )
      },
      modifier = Modifier.width(80.dp),
      shape = RoundedCornerShape(24.dp),
    )

    Spacer(modifier = Modifier.width(8.dp))

    FilterChip(
      selected = selectedUnit == WeightUnit.GRAMS,
      onClick = { onUnitSelected(WeightUnit.GRAMS) },
      label = {
        Text(
          text = stringResource(R.string.weight_unit_grams),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
        )
      },
      modifier = Modifier.width(80.dp),
      shape = RoundedCornerShape(24.dp),
    )
  }
}
