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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.localizedName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Common option
private const val OTHER_OPTION = "OTHER"

private val monthlyIntervalOptions = listOf(1, 2, 3, 4, 5, 6)
private val intervalUnitOptions = listOf("DAILY", "WEEKLY", "MONTHLY")

// Common internal deworming medications (for intestinal parasites)
private val internalDewormingMedications =
  listOf(
    "MILBEMAX",
    "DRONTAL",
    "PANACUR",
    "PROFENDER",
    "ADVOCATE",
    "BROADLINE",
    "STRONGHOLD_PLUS",
    "CAZITEL",
    "DRONCIT",
    OTHER_OPTION,
  )

// Common external deworming medications (for fleas, ticks, mites)
private val externalDewormingMedications =
  listOf(
    "FRONTLINE",
    "ADVANTAGE",
    "REVOLUTION",
    "SERESTO",
    "BRAVECTO",
    "NEXGARD",
    "ADVOCATE",
    "BROADLINE",
    "STRONGHOLD",
    "FIPRONIL",
    OTHER_OPTION,
  )

// Combined deworming medications (for both internal and external)
private val bothDewormingMedications =
  listOf(
    "ADVOCATE",
    "BROADLINE",
    "STRONGHOLD_PLUS",
    "MILBEMAX",
    "DRONTAL",
    "PROFENDER",
    "FRONTLINE",
    "REVOLUTION",
    "BRAVECTO",
    OTHER_OPTION,
  )

/** Screen for adding or editing a deworming entry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DewormingFormScreen(
  petId: String,
  entryId: String? = null,
  onNavigateBack: () -> Unit,
  viewModel: DewormingViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val form = uiState.form
  var showApplicationDatePicker by remember { mutableStateOf(false) }
  var showNextDueDatePicker by remember { mutableStateOf(false) }
  var dewormingTypeExpanded by remember { mutableStateOf(false) }
  var medicationDropdownExpanded by remember { mutableStateOf(false) }
  var isMedicationOther by remember { mutableStateOf(false) }

  var showMonthlyIntervalPicker by remember { mutableStateOf(false) }
  var selectedMonthlyInterval by remember {
    mutableStateOf(form.dewormingType.defaultIntervalMonths)
  }
  var isIntervalCustom by remember { mutableStateOf(false) }
  var customIntervalValue by remember { mutableStateOf("") }
  var customIntervalUnit by remember { mutableStateOf("MONTHLY") }
  var customIntervalUnitExpanded by remember { mutableStateOf(false) }
  // Track if the user has explicitly selected a deworming type
  var hasSelectedType by remember { mutableStateOf(false) }
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

  // Restore interval picker state when editing an existing entry
  LaunchedEffect(form.editingEntryId) {
    form.editingEntryId ?: return@LaunchedEffect
    val nextDate = form.nextDueDate ?: return@LaunchedEffect
    val appDate = form.applicationDate

    var handled = false
    for (months in 1..6) {
      if (appDate.plusMonths(months.toLong()) == nextDate) {
        selectedMonthlyInterval = months
        isIntervalCustom = false
        handled = true
        break
      }
    }

    if (!handled) {
      isIntervalCustom = true
      val totalMonths = ChronoUnit.MONTHS.between(appDate, nextDate)
      if (totalMonths > 0 && appDate.plusMonths(totalMonths) == nextDate) {
        customIntervalUnit = "MONTHLY"
        customIntervalValue = totalMonths.toString()
      } else {
        val totalWeeks = ChronoUnit.WEEKS.between(appDate, nextDate)
        if (totalWeeks > 0 && appDate.plusWeeks(totalWeeks) == nextDate) {
          customIntervalUnit = "WEEKLY"
          customIntervalValue = totalWeeks.toString()
        } else {
          val totalDays = ChronoUnit.DAYS.between(appDate, nextDate)
          customIntervalUnit = "DAILY"
          customIntervalValue = totalDays.toString()
        }
      }
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
              if (!date.isAfter(LocalDate.now())) {
                viewModel.updateApplicationDate(date)
              }
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
    val initialDate = form.nextDueDate ?: form.applicationDate.plusMonths(3)
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis =
          initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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
                  // Reset medication when type changes
                  isMedicationOther = false
                  viewModel.updateMedication("")
                  dewormingTypeExpanded = false
                },
              )
            }
          }
        }
      }

      // Medication
      FormField(label = stringResource(R.string.deworming_field_medication).replace(" *", "")) {
        // Get the right medication list based on deworming type
        val medicationList =
          when (form.dewormingType) {
            DewormingType.INTERNAL -> internalDewormingMedications
            DewormingType.EXTERNAL -> externalDewormingMedications
            DewormingType.BOTH -> bothDewormingMedications
          }
        val localizedMedications = medicationList.associateWith { getMedicationName(it) }
        val otherLabel = stringResource(R.string.option_other)
        val selectMedicationLabel = stringResource(R.string.deworming_field_medication_select)
        val selectTypeFirstLabel =
          stringResource(R.string.deworming_field_medication_select_type_first)

        // Determine if current medication is a known option or custom
        val isKnownMedication =
          form.medication.isNotEmpty() &&
            localizedMedications.values.any { it.equals(form.medication, ignoreCase = true) }
        val showCustomMedicationInput =
          hasSelectedType &&
            (isMedicationOther || (form.medication.isNotEmpty() && !isKnownMedication))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ExposedDropdownMenuBox(
            expanded = medicationDropdownExpanded && hasSelectedType,
            onExpandedChange = { if (hasSelectedType) medicationDropdownExpanded = it },
          ) {
            val displayText =
              when {
                !hasSelectedType -> selectTypeFirstLabel
                form.medication.isEmpty() -> selectMedicationLabel
                showCustomMedicationInput -> otherLabel
                else -> form.medication
              }
            OutlinedTextField(
              value = displayText,
              onValueChange = {},
              readOnly = true,
              enabled = hasSelectedType,
              placeholder = {
                Text(if (hasSelectedType) selectMedicationLabel else selectTypeFirstLabel)
              },
              trailingIcon = {
                if (hasSelectedType) {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = medicationDropdownExpanded)
                }
              },
              modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
              isError = form.medicationError != null && hasSelectedType,
              shape = RoundedCornerShape(12.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                  disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                ),
            )
            ExposedDropdownMenu(
              expanded = medicationDropdownExpanded && hasSelectedType,
              onDismissRequest = { medicationDropdownExpanded = false },
            ) {
              localizedMedications.forEach { (key, displayName) ->
                DropdownMenuItem(
                  text = { Text(displayName) },
                  onClick = {
                    if (key == OTHER_OPTION) {
                      isMedicationOther = true
                      viewModel.updateMedication("")
                    } else {
                      isMedicationOther = false
                      viewModel.updateMedication(displayName)
                    }
                    medicationDropdownExpanded = false
                  },
                )
              }
            }
          }

          // Custom medication input when "Other" is selected
          if (showCustomMedicationInput) {
            OutlinedTextField(
              value = form.medication,
              onValueChange = viewModel::updateMedication,
              placeholder = { Text(stringResource(R.string.deworming_field_medication_custom)) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              isError = form.medicationError != null,
              shape = RoundedCornerShape(12.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
            )
          }

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
            text = form.applicationDate.format(dateFormatter),
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

      FormField(label = stringResource(R.string.deworming_field_monthly_interval)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ExposedDropdownMenuBox(
            expanded = showMonthlyIntervalPicker,
            onExpandedChange = { showMonthlyIntervalPicker = it },
          ) {
            val intervalLabel =
              when {
                isIntervalCustom -> stringResource(R.string.interval_custom)
                else ->
                  pluralStringResource(
                    R.plurals.interval_months,
                    selectedMonthlyInterval,
                    selectedMonthlyInterval,
                  )
              }
            OutlinedTextField(
              value = intervalLabel,
              onValueChange = {},
              readOnly = true,
              trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMonthlyIntervalPicker)
              },
              modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
              shape = RoundedCornerShape(12.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
            )
            ExposedDropdownMenu(
              expanded = showMonthlyIntervalPicker,
              onDismissRequest = { showMonthlyIntervalPicker = false },
            ) {
              monthlyIntervalOptions.forEach { months ->
                DropdownMenuItem(
                  text = { Text(pluralStringResource(R.plurals.interval_months, months, months)) },
                  onClick = {
                    selectedMonthlyInterval = months
                    isIntervalCustom = false
                    showMonthlyIntervalPicker = false
                    viewModel.updateNextDueDate(form.applicationDate.plusMonths(months.toLong()))
                  },
                )
              }
              DropdownMenuItem(
                text = { Text(stringResource(R.string.interval_custom)) },
                onClick = {
                  isIntervalCustom = true
                  customIntervalValue = ""
                  showMonthlyIntervalPicker = false
                },
              )
            }
          }

          if (isIntervalCustom) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = customIntervalValue,
                onValueChange = { newValue ->
                  val digits = newValue.filter { it.isDigit() }
                  customIntervalValue = digits
                  val num = digits.toLongOrNull() ?: return@OutlinedTextField
                  val newDate =
                    when (customIntervalUnit) {
                      "DAILY" -> form.applicationDate.plusDays(num)
                      "WEEKLY" -> form.applicationDate.plusWeeks(num)
                      else -> form.applicationDate.plusMonths(num)
                    }
                  viewModel.updateNextDueDate(newDate)
                },
                placeholder = { Text(stringResource(R.string.interval_custom_value_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                  ),
              )
              ExposedDropdownMenuBox(
                expanded = customIntervalUnitExpanded,
                onExpandedChange = { customIntervalUnitExpanded = it },
                modifier = Modifier.weight(1f),
              ) {
                OutlinedTextField(
                  value = getIntervalUnitLabel(customIntervalUnit),
                  onValueChange = {},
                  readOnly = true,
                  trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = customIntervalUnitExpanded)
                  },
                  modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                  shape = RoundedCornerShape(12.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                )
                ExposedDropdownMenu(
                  expanded = customIntervalUnitExpanded,
                  onDismissRequest = { customIntervalUnitExpanded = false },
                ) {
                  intervalUnitOptions.forEach { unit ->
                    DropdownMenuItem(
                      text = { Text(getIntervalUnitLabel(unit)) },
                      onClick = {
                        customIntervalUnit = unit
                        customIntervalUnitExpanded = false
                        val num = customIntervalValue.toLongOrNull() ?: return@DropdownMenuItem
                        val newDate =
                          when (unit) {
                            "DAILY" -> form.applicationDate.plusDays(num)
                            "WEEKLY" -> form.applicationDate.plusWeeks(num)
                            else -> form.applicationDate.plusMonths(num)
                          }
                        viewModel.updateNextDueDate(newDate)
                      },
                    )
                  }
                }
              }
            }
          }
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
                form.nextDueDate?.format(dateFormatter)
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
private fun getIntervalUnitLabel(unit: String): String =
  when (unit) {
    "DAILY" -> stringResource(R.string.interval_unit_daily)
    "WEEKLY" -> stringResource(R.string.interval_unit_weekly)
    else -> stringResource(R.string.interval_unit_monthly)
  }

@Composable
private fun getMedicationName(key: String): String {
  return when (key) {
    // Internal medications
    "MILBEMAX" -> stringResource(R.string.medication_milbemax)
    "DRONTAL" -> stringResource(R.string.medication_drontal)
    "PANACUR" -> stringResource(R.string.medication_panacur)
    "PROFENDER" -> stringResource(R.string.medication_profender)
    "CAZITEL" -> stringResource(R.string.medication_cazitel)
    "DRONCIT" -> stringResource(R.string.medication_droncit)
    // External medications
    "FRONTLINE" -> stringResource(R.string.medication_frontline)
    "ADVANTAGE" -> stringResource(R.string.medication_advantage)
    "REVOLUTION" -> stringResource(R.string.medication_revolution)
    "SERESTO" -> stringResource(R.string.medication_seresto)
    "BRAVECTO" -> stringResource(R.string.medication_bravecto)
    "NEXGARD" -> stringResource(R.string.medication_nexgard)
    "STRONGHOLD" -> stringResource(R.string.medication_stronghold)
    "FIPRONIL" -> stringResource(R.string.medication_fipronil)
    // Shared medications (both internal and external)
    "ADVOCATE" -> stringResource(R.string.medication_advocate)
    "BROADLINE" -> stringResource(R.string.medication_broadline)
    "STRONGHOLD_PLUS" -> stringResource(R.string.medication_stronghold_plus)
    // Other option
    OTHER_OPTION -> stringResource(R.string.option_other)
    else -> key
  }
}
