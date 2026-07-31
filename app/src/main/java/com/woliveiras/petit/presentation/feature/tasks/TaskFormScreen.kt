package com.woliveiras.petit.presentation.feature.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskSubjectControl
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

      // Subject
      TaskSubjectFields(
        uiState = uiState,
        onSubjectCodeChange = viewModel::updateSubjectCode,
        onSubjectNameChange = viewModel::updateSubjectName,
      )

      // Date and Time
      val dateLabel = stringResource(R.string.task_field_date)
      val timeLabel = stringResource(R.string.task_field_time)
      val formattedDate = displayFormatter.shortDate(uiState.scheduledDate.toLocalDate())
      val formattedTime = displayFormatter.time(uiState.scheduledDate.toLocalTime())
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
          OutlinedTextField(
            value = formattedDate,
            onValueChange = {},
            label = { Text(dateLabel) },
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            readOnly = true,
            isError = uiState.dateError != null,
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
          )
          Box(
            modifier =
              Modifier.matchParentSize()
                .semantics {
                  contentDescription = "$dateLabel: $formattedDate"
                  role = Role.Button
                }
                .clickable { showDatePicker = true }
          )
        }
        Box(modifier = Modifier.width(120.dp)) {
          OutlinedTextField(
            value = formattedTime,
            onValueChange = {},
            label = { Text(timeLabel) },
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            readOnly = true,
            isError = uiState.dateError != null,
            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
          )
          Box(
            modifier =
              Modifier.matchParentSize()
                .semantics {
                  contentDescription = "$timeLabel: $formattedTime"
                  role = Role.Button
                }
                .clickable { showTimePicker = true }
          )
        }
      }
      uiState.dateError?.let { error ->
        Text(
          text = error,
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

/** Which item the task is about: a vaccine, an antiparasitic treatment, or a medicine. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskSubjectFields(
  uiState: TaskFormUiState,
  onSubjectCodeChange: (String?) -> Unit,
  onSubjectNameChange: (String) -> Unit,
) {
  var subjectDropdownExpanded by remember { mutableStateOf(false) }
  val unsetLabel = stringResource(R.string.task_field_subject_unset)

  LaunchedEffect(uiState.subjectControl) { subjectDropdownExpanded = false }

  when (uiState.subjectControl) {
    TaskSubjectControl.NONE -> Unit
    TaskSubjectControl.VACCINE -> {
      val selected = uiState.vaccineOptions.firstOrNull { it.name == uiState.subjectCode }
      ExposedDropdownMenuBox(
        expanded = subjectDropdownExpanded,
        onExpandedChange = { subjectDropdownExpanded = it },
      ) {
        OutlinedTextField(
          value = selected?.localizedName() ?: unsetLabel,
          onValueChange = {},
          readOnly = true,
          label = { Text(stringResource(R.string.task_field_subject_vaccine)) },
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded)
          },
          modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
          expanded = subjectDropdownExpanded,
          onDismissRequest = { subjectDropdownExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(unsetLabel) },
            onClick = {
              onSubjectCodeChange(null)
              subjectDropdownExpanded = false
            },
          )
          uiState.vaccineOptions.forEach { vaccine ->
            DropdownMenuItem(
              text = { Text(vaccine.localizedName()) },
              onClick = {
                onSubjectCodeChange(vaccine.name)
                subjectDropdownExpanded = false
              },
            )
          }
        }
      }
      if (uiState.requiresSubjectFreeText) {
        OutlinedTextField(
          value = uiState.subjectName,
          onValueChange = onSubjectNameChange,
          label = { Text(stringResource(R.string.vaccination_field_custom_name)) },
          placeholder = {
            Text(stringResource(R.string.vaccination_field_custom_name_placeholder))
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          isError = uiState.subjectError != null,
          supportingText = uiState.subjectError?.let { { Text(it) } },
        )
      }
    }
    TaskSubjectControl.ANTIPARASITIC -> {
      val selected = uiState.antiparasiticOptions.firstOrNull { it.name == uiState.subjectCode }
      ExposedDropdownMenuBox(
        expanded = subjectDropdownExpanded,
        onExpandedChange = { subjectDropdownExpanded = it },
      ) {
        OutlinedTextField(
          value = selected?.localizedName() ?: unsetLabel,
          onValueChange = {},
          readOnly = true,
          label = { Text(stringResource(R.string.task_field_subject_antiparasitic)) },
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded)
          },
          modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
          expanded = subjectDropdownExpanded,
          onDismissRequest = { subjectDropdownExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(unsetLabel) },
            onClick = {
              onSubjectCodeChange(null)
              subjectDropdownExpanded = false
            },
          )
          uiState.antiparasiticOptions.forEach { type ->
            DropdownMenuItem(
              text = { Text(type.localizedName()) },
              onClick = {
                onSubjectCodeChange(type.name)
                subjectDropdownExpanded = false
              },
            )
          }
        }
      }
      OutlinedTextField(
        value = uiState.subjectName,
        onValueChange = onSubjectNameChange,
        label = { Text(stringResource(R.string.task_field_subject_product)) },
        placeholder = { Text(stringResource(R.string.task_field_subject_product_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = uiState.subjectError != null,
        supportingText = uiState.subjectError?.let { { Text(it) } },
      )
    }
    TaskSubjectControl.MEDICATION -> {
      OutlinedTextField(
        value = uiState.subjectName,
        onValueChange = onSubjectNameChange,
        label = { Text(stringResource(R.string.task_field_subject_medication)) },
        placeholder = { Text(stringResource(R.string.task_field_subject_medication_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = uiState.subjectError != null,
        supportingText = uiState.subjectError?.let { { Text(it) } },
      )
      if (uiState.subjectSuggestions.isNotEmpty()) {
        Text(
          text = stringResource(R.string.task_field_subject_suggestions),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          uiState.subjectSuggestions.forEach { suggestion ->
            SuggestionChip(
              onClick = { onSubjectNameChange(suggestion) },
              label = { Text(suggestion) },
            )
          }
        }
      }
    }
  }
}
