package com.woliveiras.petit.presentation.feature.deworming

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.presentation.components.PetitDatePickerDialog
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.localizedName
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter

/** Screen for adding or editing a deworming entry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DewormingFormScreen(
  petId: String,
  entryId: String? = null,
  preselectedDewormingType: String? = null,
  onNavigateBack: () -> Unit,
  viewModel: DewormingViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val form = uiState.form
  var showApplicationDatePicker by remember { mutableStateOf(false) }
  var showNextDueDatePicker by remember { mutableStateOf(false) }
  var dewormingTypeExpanded by remember { mutableStateOf(false) }

  // Track if the user has explicitly selected a deworming type
  var hasSelectedType by rememberSaveable { mutableStateOf(false) }
  // Track if the entry point preselection was already applied, so a later change is kept
  var hasAppliedPreselection by rememberSaveable { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  // In edit mode, consider type as already selected
  LaunchedEffect(form.isEditMode) {
    if (form.isEditMode) {
      hasSelectedType = true
    }
  }

  // Load entry for editing if entryId is provided
  LaunchedEffect(entryId) {
    if (entryId != null) {
      viewModel.loadEntryForEdit(entryId)
    }
  }

  // Pre-select the treatment type when the caregiver arrived from a typed entry point
  LaunchedEffect(preselectedDewormingType) {
    if (preselectedDewormingType != null && entryId == null && !hasAppliedPreselection) {
      runCatching { DewormingType.valueOf(preselectedDewormingType) }
        .getOrNull()
        ?.let { type ->
          viewModel.updateDewormingType(type)
          hasSelectedType = true
        }
      hasAppliedPreselection = true
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is DewormingEvent.DewormingSaved -> onNavigateBack()
        is DewormingEvent.DewormingDeleted -> onNavigateBack()
        is DewormingEvent.Error -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }

  // Application Date Picker Dialog
  if (showApplicationDatePicker) {
    PetitDatePickerDialog(
      selectedDate = form.applicationDate,
      onDateSelected = viewModel::updateApplicationDate,
      onDismissRequest = { showApplicationDatePicker = false },
      maxDate = uiState.today,
    )
  }

  // Next Due Date Picker Dialog
  if (showNextDueDatePicker) {
    PetitDatePickerDialog(
      selectedDate = form.nextDueDate,
      onDateSelected = viewModel::updateNextDueDate,
      onDismissRequest = { showNextDueDatePicker = false },
    )
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PetitTopAppBar(
        title = {
          Column {
            Text(
              if (form.isEditMode) {
                stringResource(R.string.deworming_edit_title)
              } else {
                stringResource(R.string.deworming_add_title)
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
          if (form.isEditMode) {
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
    val displayFormatter = rememberAppDisplayFormatter()

    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Deworming Type Dropdown
      FormField(label = stringResource(R.string.deworming_field_type).replace(" *", "")) {
        val selectTypeLabel = stringResource(R.string.deworming_field_type_select)
        ExposedDropdownMenuBox(
          expanded = dewormingTypeExpanded,
          onExpandedChange = { dewormingTypeExpanded = it },
        ) {
          OutlinedTextField(
            value = if (hasSelectedType) form.dewormingType.localizedName() else selectTypeLabel,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(selectTypeLabel) },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = dewormingTypeExpanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
              ),
          )
          ExposedDropdownMenu(
            expanded = dewormingTypeExpanded,
            onDismissRequest = { dewormingTypeExpanded = false },
          ) {
            DewormingType.entries.forEach { type ->
              DropdownMenuItem(
                text = { Text(type.localizedName()) },
                onClick = {
                  viewModel.updateDewormingType(type)
                  hasSelectedType = true
                  dewormingTypeExpanded = false
                },
              )
            }
          }
        }
      }

      Text(
        text = stringResource(R.string.care_presets_veterinary_advisory),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Medication
      FormField(label = stringResource(R.string.deworming_field_medication).replace(" *", "")) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          DewormingMedicationField(
            medication = form.medication,
            error = form.medicationError,
            onValueChange = viewModel::updateMedication,
          )

          // Show error message if any
          if (form.medicationError != null) {
            Text(
              text = form.medicationError!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(start = 4.dp),
            )
          }
        }
      }

      // Application Date
      FormField(
        label = stringResource(R.string.deworming_field_application_date).replace(" *", "")
      ) {
        Card(
          modifier = Modifier.fillMaxWidth().clickable { showApplicationDatePicker = true },
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text(
            text = displayFormatter.shortDate(form.applicationDate),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          )
        }
        if (form.applicationDateError != null) {
          Text(
            text = form.applicationDateError!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
          )
        }
      }

      // Next Due Date (optional)
      FormField(label = stringResource(R.string.deworming_field_next_due_date)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Card(
            modifier = Modifier.weight(1f).clickable { showNextDueDatePicker = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text(
              text =
                form.nextDueDate?.let(displayFormatter::shortDate)
                  ?: stringResource(R.string.deworming_field_next_due_date_placeholder),
              style = MaterialTheme.typography.bodyLarge,
              color =
                if (form.nextDueDate != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
          }
          if (form.nextDueDate != null) {
            TextButton(onClick = { viewModel.updateNextDueDate(null) }) {
              Text(stringResource(R.string.action_clear))
            }
          }
        }
      }

      // Note
      FormField(label = stringResource(R.string.deworming_field_note)) {
        OutlinedTextField(
          value = form.note,
          onValueChange = viewModel::updateNote,
          placeholder = { Text(stringResource(R.string.deworming_field_note_placeholder)) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          maxLines = 5,
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Save Button
      Button(
        onClick = { viewModel.saveDeworming() },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = !form.isSaving,
        shape = RoundedCornerShape(16.dp),
      ) {
        if (form.isSaving) {
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

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    content()
  }
}

@Composable
internal fun DewormingMedicationField(
  medication: String,
  error: String?,
  onValueChange: (String) -> Unit,
) {
  OutlinedTextField(
    value = medication,
    onValueChange = onValueChange,
    placeholder = { Text(stringResource(R.string.deworming_field_medication_custom)) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    isError = error != null,
    shape = RoundedCornerShape(12.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
      ),
  )
}
