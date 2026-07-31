package com.woliveiras.petit.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woliveiras.petit.R
import com.woliveiras.petit.presentation.util.PickerDate
import java.time.LocalDate

/**
 * Single date picker dialog for every form. Conversions go through [PickerDate] so the confirmed
 * day matches the selected day in any device time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetitDatePickerDialog(
  selectedDate: LocalDate?,
  onDateSelected: (LocalDate) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  minDate: LocalDate? = null,
  maxDate: LocalDate? = null,
) {
  val selectableDates = remember(minDate, maxDate) { BoundedSelectableDates(minDate, maxDate) }
  val datePickerState =
    rememberDatePickerState(
      initialSelectedDateMillis = selectedDate?.let(PickerDate::toEpochMillis),
      selectableDates = selectableDates,
    )

  DatePickerDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    confirmButton = {
      TextButton(
        onClick = {
          datePickerState.selectedDateMillis?.let { millis ->
            onDateSelected(PickerDate.toLocalDate(millis))
          }
          onDismissRequest()
        }
      ) {
        Text(stringResource(R.string.action_ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.action_cancel)) }
    },
  ) {
    DatePicker(state = datePickerState, showModeToggle = false)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
private class BoundedSelectableDates(
  private val minDate: LocalDate?,
  private val maxDate: LocalDate?,
) : SelectableDates {

  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
    val date = PickerDate.toLocalDate(utcTimeMillis)
    return (minDate == null || !date.isBefore(minDate)) &&
      (maxDate == null || !date.isAfter(maxDate))
  }

  override fun isSelectableYear(year: Int): Boolean =
    (minDate == null || year >= minDate.year) && (maxDate == null || year <= maxDate.year)
}
