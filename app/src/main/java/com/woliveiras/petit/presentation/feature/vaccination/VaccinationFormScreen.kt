package com.woliveiras.petit.presentation.feature.vaccination

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.localizedName
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import java.time.Instant
import java.time.ZoneId

/** Screen for adding or editing a vaccination entry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationFormScreen(
  petId: String,
  entryId: String? = null,
  preselectedVaccineType: String? = null,
  onNavigateBack: () -> Unit,
  viewModel: VaccinationViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val form = uiState.form
  var showApplicationDatePicker by remember { mutableStateOf(false) }
  var showNextDueDatePicker by remember { mutableStateOf(false) }
  var vaccineTypeExpanded by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Load entry for editing if entryId is provided
  LaunchedEffect(entryId) {
    if (entryId != null) {
      viewModel.loadEntryForEdit(entryId)
    }
  }

  // Pre-select vaccine type if provided (for quick apply from home alerts)
  LaunchedEffect(preselectedVaccineType) {
    if (preselectedVaccineType != null && entryId == null) {
      runCatching { VaccineType.valueOf(preselectedVaccineType) }
        .getOrNull()
        ?.let { type -> viewModel.updateVaccineType(type) }
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is VaccinationEvent.VaccinationSaved -> onNavigateBack()
        is VaccinationEvent.VaccinationDeleted -> onNavigateBack()
        is VaccinationEvent.Error -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }

  // Application Date Picker Dialog
  if (showApplicationDatePicker) {
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis =
          form.applicationDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
      )

    DatePickerDialog(
      onDismissRequest = { showApplicationDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
              viewModel.updateApplicationDate(date)
            }
            showApplicationDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { showApplicationDatePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    ) {
      DatePicker(state = datePickerState, showModeToggle = false)
    }
  }

  // Next Due Date Picker Dialog
  if (showNextDueDatePicker) {
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis =
          form.nextDueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
      )

    DatePickerDialog(
      onDismissRequest = { showNextDueDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
              viewModel.updateNextDueDate(date)
            }
            showNextDueDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { showNextDueDatePicker = false }) {
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
              if (form.isEditMode) {
                stringResource(R.string.vaccination_edit_title)
              } else {
                stringResource(R.string.vaccination_add_title)
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
      // Vaccine Type Dropdown
      FormField(label = stringResource(R.string.vaccination_field_type).replace(" *", "")) {
        ExposedDropdownMenuBox(
          expanded = vaccineTypeExpanded,
          onExpandedChange = { vaccineTypeExpanded = it },
        ) {
          OutlinedTextField(
            value = form.vaccineType.localizedName(),
            onValueChange = {},
            readOnly = true,
            isError = form.vaccineTypeError != null,
            supportingText = form.vaccineTypeError?.let { error -> { Text(error) } },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = vaccineTypeExpanded)
            },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
              ),
          )

          ExposedDropdownMenu(
            expanded = vaccineTypeExpanded,
            onDismissRequest = { vaccineTypeExpanded = false },
          ) {
            uiState.availableVaccineTypes.forEach { type ->
              DropdownMenuItem(
                text = { Text(type.localizedName()) },
                onClick = {
                  viewModel.updateVaccineType(type)
                  vaccineTypeExpanded = false
                },
              )
            }
          }
        }
      }

      VaccinationCustomNameField(
        vaccineType = form.vaccineType,
        customName = form.customName,
        error = form.customNameError,
        onValueChange = viewModel::updateCustomName,
      )

      Text(
        text = stringResource(R.string.care_presets_veterinary_advisory),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Application Date
      FormField(
        label = stringResource(R.string.vaccination_field_application_date).replace(" *", "")
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
      FormField(label = stringResource(R.string.vaccination_field_next_due_date)) {
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
                  ?: stringResource(R.string.vaccination_field_next_due_date_placeholder),
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

      // Veterinarian
      FormField(label = stringResource(R.string.vaccination_field_veterinarian)) {
        OutlinedTextField(
          value = form.veterinarian,
          onValueChange = viewModel::updateVeterinarian,
          placeholder = {
            Text(stringResource(R.string.vaccination_field_veterinarian_placeholder))
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          isError = form.veterinarianError != null,
          supportingText = form.veterinarianError?.let { error -> { Text(error) } },
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
        )
      }

      // Clinic
      FormField(label = stringResource(R.string.vaccination_field_clinic)) {
        OutlinedTextField(
          value = form.clinic,
          onValueChange = viewModel::updateClinic,
          placeholder = { Text(stringResource(R.string.vaccination_field_clinic_placeholder)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          isError = form.clinicError != null,
          supportingText = form.clinicError?.let { error -> { Text(error) } },
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
        )
      }

      // Batch Number
      FormField(label = stringResource(R.string.vaccination_field_batch)) {
        OutlinedTextField(
          value = form.batchNumber,
          onValueChange = viewModel::updateBatchNumber,
          placeholder = { Text(stringResource(R.string.vaccination_field_batch_placeholder)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          isError = form.batchNumberError != null,
          supportingText = form.batchNumberError?.let { error -> { Text(error) } },
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
        )
      }

      // Note
      FormField(label = stringResource(R.string.vaccination_field_note)) {
        OutlinedTextField(
          value = form.note,
          onValueChange = viewModel::updateNote,
          placeholder = { Text(stringResource(R.string.vaccination_field_note_placeholder)) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          maxLines = 5,
          isError = form.noteError != null,
          supportingText = form.noteError?.let { error -> { Text(error) } },
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
        onClick = viewModel::saveVaccination,
        enabled = !form.isSaving,
        modifier = Modifier.fillMaxWidth().height(56.dp),
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
internal fun VaccinationCustomNameField(
  vaccineType: VaccineType,
  customName: String,
  error: String?,
  onValueChange: (String) -> Unit,
) {
  if (vaccineType != VaccineType.OTHER) return

  FormField(label = stringResource(R.string.vaccination_field_custom_name)) {
    OutlinedTextField(
      value = customName,
      onValueChange = onValueChange,
      placeholder = { Text(stringResource(R.string.vaccination_field_custom_name_placeholder)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      isError = error != null,
      supportingText = error?.let { message -> { Text(message) } },
      shape = RoundedCornerShape(12.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
    )
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
