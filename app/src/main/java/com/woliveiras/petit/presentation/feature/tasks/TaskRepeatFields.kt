package com.woliveiras.petit.presentation.feature.tasks

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.RecurrenceUnit
import com.woliveiras.petit.presentation.components.PetitDatePickerDialog
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import com.woliveiras.petit.presentation.util.summary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle

/** Repeat controls of the task form: how often the task comes back and when it stops. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskRepeatFields(
  state: TaskRepeatFormState,
  error: String?,
  startDate: LocalDate,
  onPresetChange: (RepeatPreset) -> Unit,
  onStateChange: (TaskRepeatFormState) -> Unit,
  modifier: Modifier = Modifier,
) {
  var presetExpanded by remember { mutableStateOf(false) }
  var unitExpanded by remember { mutableStateOf(false) }
  var endModeExpanded by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }
  var windowTimeTarget by remember { mutableStateOf<WindowTimeTarget?>(null) }
  val displayFormatter = rememberAppDisplayFormatter()

  if (showEndDatePicker) {
    PetitDatePickerDialog(
      selectedDate = state.endDate ?: startDate.plusMonths(1),
      onDateSelected = { onStateChange(state.copy(endDate = it)) },
      onDismissRequest = { showEndDatePicker = false },
      minDate = startDate,
    )
  }

  windowTimeTarget?.let { target ->
    val initial = if (target == WindowTimeTarget.START) state.windowStart else state.windowEnd
    RepeatTimePickerDialog(
      initialTime = initial,
      onDismissRequest = { windowTimeTarget = null },
      onTimeSelected = { time ->
        onStateChange(
          when (target) {
            WindowTimeTarget.START -> state.copy(windowStart = time)
            WindowTimeTarget.END -> state.copy(windowEnd = time)
          }
        )
        windowTimeTarget = null
      },
    )
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    ExposedDropdownMenuBox(expanded = presetExpanded, onExpandedChange = { presetExpanded = it }) {
      OutlinedTextField(
        value = state.preset.localizedLabel(),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.task_field_repeat)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
        isError = error != null,
        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
      )
      ExposedDropdownMenu(
        expanded = presetExpanded,
        onDismissRequest = { presetExpanded = false },
      ) {
        RepeatPreset.entries.forEach { preset ->
          DropdownMenuItem(
            text = { Text(preset.localizedLabel()) },
            onClick = {
              onPresetChange(preset)
              presetExpanded = false
            },
          )
        }
      }
    }

    if (state.preset == RepeatPreset.CUSTOM) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = state.interval,
          onValueChange = { value ->
            onStateChange(state.copy(interval = value.filter { it.isDigit() }.take(3)))
          },
          label = { Text(stringResource(R.string.repeat_field_interval)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.width(120.dp),
        )
        ExposedDropdownMenuBox(
          expanded = unitExpanded,
          onExpandedChange = { unitExpanded = it },
          modifier = Modifier.weight(1f),
        ) {
          OutlinedTextField(
            value = state.unit.localizedLabel(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.repeat_field_unit)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
          )
          ExposedDropdownMenu(
            expanded = unitExpanded,
            onDismissRequest = { unitExpanded = false },
          ) {
            RecurrenceUnit.entries.forEach { unit ->
              DropdownMenuItem(
                text = { Text(unit.localizedLabel()) },
                onClick = {
                  onStateChange(state.copy(unit = unit))
                  unitExpanded = false
                },
              )
            }
          }
        }
      }
    }

    if (state.showsWeekdays) {
      Text(
        text = stringResource(R.string.repeat_field_weekdays),
        style = MaterialTheme.typography.labelLarge,
      )
      val locale = LocalConfiguration.current.locales[0]
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { day ->
          val selected = day in state.weekdays
          FilterChip(
            selected = selected,
            onClick = {
              val weekdays = if (selected) state.weekdays - day else state.weekdays + day
              onStateChange(state.copy(weekdays = weekdays))
            },
            label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
            modifier =
              Modifier.semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) },
          )
        }
      }
    }

    if (state.showsDailyWindow) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(R.string.repeat_field_window),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = state.useDailyWindow,
          onCheckedChange = { onStateChange(state.copy(useDailyWindow = it)) },
        )
      }
      if (state.useDailyWindow) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ReadOnlyTimeField(
            label = stringResource(R.string.repeat_field_window_start),
            value = displayFormatter.time(state.windowStart),
            onClick = { windowTimeTarget = WindowTimeTarget.START },
            modifier = Modifier.weight(1f),
          )
          ReadOnlyTimeField(
            label = stringResource(R.string.repeat_field_window_end),
            value = displayFormatter.time(state.windowEnd),
            onClick = { windowTimeTarget = WindowTimeTarget.END },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    if (state.repeats) {
      ExposedDropdownMenuBox(
        expanded = endModeExpanded,
        onExpandedChange = { endModeExpanded = it },
      ) {
        OutlinedTextField(
          value = state.endMode.localizedLabel(),
          onValueChange = {},
          readOnly = true,
          label = { Text(stringResource(R.string.repeat_field_end)) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endModeExpanded) },
          modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
          expanded = endModeExpanded,
          onDismissRequest = { endModeExpanded = false },
        ) {
          RepeatEndMode.entries.forEach { mode ->
            DropdownMenuItem(
              text = { Text(mode.localizedLabel()) },
              onClick = {
                onStateChange(state.copy(endMode = mode))
                endModeExpanded = false
              },
            )
          }
        }
      }

      when (state.endMode) {
        RepeatEndMode.NEVER -> Unit
        RepeatEndMode.ON_DATE -> {
          val label = stringResource(R.string.repeat_field_end_date)
          val value =
            state.endDate?.let { displayFormatter.shortDate(it) }
              ?: stringResource(R.string.task_field_subject_unset)
          Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = value,
              onValueChange = {},
              readOnly = true,
              label = { Text(label) },
              modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            )
            Box(
              modifier =
                Modifier.matchParentSize()
                  .semantics {
                    contentDescription = "$label: $value"
                    role = Role.Button
                  }
                  .clickable { showEndDatePicker = true }
            )
          }
        }
        RepeatEndMode.AFTER_OCCURRENCES ->
          OutlinedTextField(
            value = state.endCount,
            onValueChange = { input ->
              onStateChange(state.copy(endCount = input.filter { it.isDigit() }.take(4)))
            },
            label = { Text(stringResource(R.string.repeat_field_end_count)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
          )
      }

      state.recurrence?.let { recurrence ->
        Text(
          text = recurrence.summary(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    error?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    }
  }
}

private enum class WindowTimeTarget {
  START,
  END,
}

@Composable
private fun ReadOnlyTimeField(
  label: String,
  value: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
    )
    Box(
      modifier =
        Modifier.matchParentSize()
          .semantics {
            contentDescription = "$label: $value"
            role = Role.Button
          }
          .clickable { onClick() }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatTimePickerDialog(
  initialTime: LocalTime,
  onDismissRequest: () -> Unit,
  onTimeSelected: (LocalTime) -> Unit,
) {
  val timePickerState =
    rememberTimePickerState(
      initialHour = initialTime.hour,
      initialMinute = initialTime.minute,
      is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
  AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
      TextButton(
        onClick = { onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute)) }
      ) {
        Text(stringResource(R.string.action_ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.action_cancel)) }
    },
    text = { TimePicker(state = timePickerState) },
  )
}

/** Explains that automatic health tasks follow the health record instead of a repeat rule. */
@Composable
fun AutomaticTaskRepeatNote(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(R.string.task_auto_repeat_explanation),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
  )
}

@Composable
private fun RepeatPreset.localizedLabel(): String =
  stringResource(
    when (this) {
      RepeatPreset.NONE -> R.string.repeat_preset_none
      RepeatPreset.DAILY -> R.string.repeat_preset_daily
      RepeatPreset.WEEKLY -> R.string.repeat_preset_weekly
      RepeatPreset.MONTHLY -> R.string.repeat_preset_monthly
      RepeatPreset.QUARTERLY -> R.string.repeat_preset_quarterly
      RepeatPreset.SEMIANNUAL -> R.string.repeat_preset_semiannual
      RepeatPreset.YEARLY -> R.string.repeat_preset_yearly
      RepeatPreset.CUSTOM -> R.string.repeat_preset_custom
    }
  )

@Composable
private fun RecurrenceUnit.localizedLabel(): String =
  stringResource(
    when (this) {
      RecurrenceUnit.HOURS -> R.string.repeat_unit_hours
      RecurrenceUnit.DAYS -> R.string.repeat_unit_days
      RecurrenceUnit.WEEKS -> R.string.repeat_unit_weeks
      RecurrenceUnit.MONTHS -> R.string.repeat_unit_months
      RecurrenceUnit.YEARS -> R.string.repeat_unit_years
    }
  )

@Composable
private fun RepeatEndMode.localizedLabel(): String =
  stringResource(
    when (this) {
      RepeatEndMode.NEVER -> R.string.repeat_end_never
      RepeatEndMode.ON_DATE -> R.string.repeat_end_on_date
      RepeatEndMode.AFTER_OCCURRENCES -> R.string.repeat_end_after_occurrences
    }
  )
