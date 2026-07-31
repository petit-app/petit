package com.woliveiras.petit.presentation.feature.tasks

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.presentation.components.PetitDatePickerDialog
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.localizedName
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import java.time.LocalDateTime
import java.time.LocalTime

/** Screen for creating or editing a standalone task. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: TaskFormViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var showDeleteConfirmation by remember { mutableStateOf(false) }
  var petDropdownExpanded by remember { mutableStateOf(false) }
  var kindDropdownExpanded by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is TaskFormEvent.TaskSaved -> onNavigateBack()
        is TaskFormEvent.TaskDeleted -> onNavigateBack()
        is TaskFormEvent.Error -> snackbarHostState.showSnackbar(event.message)
      }
    }
  }

  // Date Picker Dialog
  if (showDatePicker) {
    PetitDatePickerDialog(
      selectedDate = uiState.scheduledDate.toLocalDate(),
      onDateSelected = { date ->
        viewModel.updateScheduledDate(LocalDateTime.of(date, uiState.scheduledDate.toLocalTime()))
      },
      onDismissRequest = { showDatePicker = false },
    )
  }

  // Time Picker Dialog
  if (showTimePicker) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = uiState.scheduledDate.hour,
        initialMinute = uiState.scheduledDate.minute,
        is24Hour = true,
      )

    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
            val newDateTime = LocalDateTime.of(uiState.scheduledDate.toLocalDate(), newTime)
            viewModel.updateScheduledDate(newDateTime)
            showTimePicker = false
          }
        ) {
          Text(stringResource(R.string.action_ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
      text = { TimePicker(state = timePickerState) },
    )
  }

  // Delete Confirmation Dialog
  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      title = { Text(stringResource(R.string.task_delete_title)) },
      text = { Text(stringResource(R.string.task_delete_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirmation = false
            viewModel.deleteTask()
          }
        ) {
          Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PetitTopAppBar(
        title = {
          Text(
            if (uiState.isEditMode) stringResource(R.string.task_edit_title)
            else stringResource(R.string.task_add_title)
          )
        },
        onNavigateBack = onNavigateBack,
        actions = {
          if (uiState.isEditMode) {
            IconButton(onClick = { showDeleteConfirmation = true }) {
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
    modifier = modifier,
  ) { padding ->
    val displayFormatter = rememberAppDisplayFormatter()

    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Title
      OutlinedTextField(
        value = uiState.title,
        onValueChange = viewModel::updateTitle,
        label = { Text(stringResource(R.string.task_field_title)) },
        placeholder = { Text(stringResource(R.string.task_field_title_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = uiState.titleError != null,
        supportingText = uiState.titleError?.let { { Text(it) } },
      )

      // Description
      OutlinedTextField(
        value = uiState.description,
        onValueChange = viewModel::updateDescription,
        label = { Text(stringResource(R.string.task_field_description)) },
        placeholder = { Text(stringResource(R.string.task_field_description_placeholder)) },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        maxLines = 4,
      )

      // Pet Dropdown
      ExposedDropdownMenuBox(
        expanded = petDropdownExpanded,
        onExpandedChange = { petDropdownExpanded = it },
      ) {
        OutlinedTextField(
          value = uiState.selectedPetName ?: stringResource(R.string.task_field_pet_general),
          onValueChange = {},
          readOnly = true,
          label = { Text(stringResource(R.string.task_field_pet)) },
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = petDropdownExpanded)
          },
          modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
          expanded = petDropdownExpanded,
          onDismissRequest = { petDropdownExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.task_field_pet_general)) },
            onClick = {
              viewModel.updateSelectedPet(null, null)
              petDropdownExpanded = false
            },
          )
          uiState.availablePets.forEach { pet ->
            DropdownMenuItem(
              text = { Text(pet.name) },
              onClick = {
                viewModel.updateSelectedPet(pet.id, pet.name)
                petDropdownExpanded = false
              },
            )
          }
        }
      }

      // Kind Dropdown
      ExposedDropdownMenuBox(
        expanded = kindDropdownExpanded,
        onExpandedChange = { kindDropdownExpanded = it },
      ) {
        OutlinedTextField(
          value = uiState.kind.localizedName(),
          onValueChange = {},
          readOnly = true,
          label = { Text(stringResource(R.string.task_field_kind)) },
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindDropdownExpanded)
          },
          modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
          expanded = kindDropdownExpanded,
          onDismissRequest = { kindDropdownExpanded = false },
        ) {
          TaskKind.entries.forEach { kind ->
            DropdownMenuItem(
              text = { Text(kind.localizedName()) },
              onClick = {
                viewModel.updateKind(kind)
                kindDropdownExpanded = false
              },
            )
          }
        }
      }

      // Date and Time
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = displayFormatter.shortDate(uiState.scheduledDate.toLocalDate()),
          onValueChange = {},
          label = { Text(stringResource(R.string.task_field_date)) },
          modifier = Modifier.weight(1f).clickable { showDatePicker = true },
          readOnly = true,
          enabled = false,
          isError = uiState.dateError != null,
          trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
              Icon(
                Icons.Default.CalendarMonth,
                contentDescription = stringResource(R.string.action_select_date),
              )
            }
          },
        )
        OutlinedTextField(
          value = displayFormatter.time(uiState.scheduledDate.toLocalTime()),
          onValueChange = {},
          label = { Text(stringResource(R.string.task_field_time)) },
          modifier = Modifier.width(120.dp).clickable { showTimePicker = true },
          readOnly = true,
          enabled = false,
          trailingIcon = {
            IconButton(onClick = { showTimePicker = true }) {
              Icon(
                Icons.Default.Schedule,
                contentDescription = stringResource(R.string.task_field_time),
              )
            }
          },
        )
      }
      if (uiState.dateError != null) {
        Text(
          text = uiState.dateError!!,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Save Button
      Button(
        onClick = { viewModel.saveTask() },
        modifier = Modifier.fillMaxWidth(),
        enabled = !uiState.isSaving,
      ) {
        if (uiState.isSaving) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(stringResource(R.string.action_save))
      }
    }
  }
}
