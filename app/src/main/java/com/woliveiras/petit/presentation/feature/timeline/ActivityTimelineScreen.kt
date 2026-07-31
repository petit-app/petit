package com.woliveiras.petit.presentation.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.presentation.components.PetitDatePickerDialog
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.components.TimelineEventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTimelineScreen(
  onNavigateBack: () -> Unit,
  viewModel: ActivityTimelineViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showDatePicker by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.activity_timeline_title)) },
        onNavigateBack = onNavigateBack,
      )
    }
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      // Date filter chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        item {
          FilterChip(
            selected = uiState.filter == TimelineFilter.ALL,
            onClick = { viewModel.setFilter(TimelineFilter.ALL) },
            label = { Text(stringResource(R.string.activity_timeline_filter_all)) },
          )
        }
        item {
          FilterChip(
            selected = uiState.filter == TimelineFilter.DAYS_5,
            onClick = { viewModel.setFilter(TimelineFilter.DAYS_5) },
            label = { Text(stringResource(R.string.activity_timeline_filter_5_days)) },
          )
        }
        item {
          FilterChip(
            selected = uiState.filter == TimelineFilter.DAYS_10,
            onClick = { viewModel.setFilter(TimelineFilter.DAYS_10) },
            label = { Text(stringResource(R.string.activity_timeline_filter_10_days)) },
          )
        }
        item {
          FilterChip(
            selected = uiState.filter == TimelineFilter.DAYS_15,
            onClick = { viewModel.setFilter(TimelineFilter.DAYS_15) },
            label = { Text(stringResource(R.string.activity_timeline_filter_15_days)) },
          )
        }
        item {
          FilterChip(
            selected = uiState.filter == TimelineFilter.CUSTOM,
            onClick = { showDatePicker = true },
            label = {
              val label =
                if (uiState.filter == TimelineFilter.CUSTOM && uiState.customDate != null) {
                  uiState.customDate.toString()
                } else {
                  stringResource(R.string.activity_timeline_filter_custom)
                }
              Text(label)
            },
          )
        }
      }

      // Pet filter dropdown
      if (uiState.pets.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        val selectedCatName =
          uiState.pets.find { it.id == uiState.selectedCatId }?.name
            ?: stringResource(R.string.activity_timeline_filter_all_pets)

        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = it },
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
          OutlinedTextField(
            value = selectedCatName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.activity_timeline_filter_pet)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.activity_timeline_filter_all_pets)) },
              onClick = {
                viewModel.setSelectedPet(null)
                expanded = false
              },
            )
            uiState.pets.forEach { pet ->
              DropdownMenuItem(
                text = { Text(pet.name) },
                onClick = {
                  viewModel.setSelectedPet(pet.id)
                  expanded = false
                },
              )
            }
          }
        }
      }

      // Event type filter chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      ) {
        item {
          FilterChip(
            selected = EventTypeFilter.WEIGHT in uiState.selectedEventTypes,
            onClick = { viewModel.toggleEventType(EventTypeFilter.WEIGHT) },
            label = { Text(stringResource(R.string.activity_timeline_filter_weight)) },
          )
        }
        item {
          FilterChip(
            selected = EventTypeFilter.VACCINATION in uiState.selectedEventTypes,
            onClick = { viewModel.toggleEventType(EventTypeFilter.VACCINATION) },
            label = { Text(stringResource(R.string.activity_timeline_filter_vaccination)) },
          )
        }
        item {
          FilterChip(
            selected = EventTypeFilter.DEWORMING in uiState.selectedEventTypes,
            onClick = { viewModel.toggleEventType(EventTypeFilter.DEWORMING) },
            label = { Text(stringResource(R.string.activity_timeline_filter_deworming)) },
          )
        }
      }

      // Events list
      if (!uiState.isLoading && uiState.events.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
          )
          Spacer(Modifier.height(24.dp))
          Text(
            stringResource(R.string.activity_timeline_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            stringResource(R.string.activity_timeline_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          itemsIndexed(uiState.events, key = { _, event -> event.id }) { index, event ->
            TimelineEventCard(event = event)
            if (index < uiState.events.lastIndex) {
              Spacer(modifier = Modifier.height(10.dp))
            }
          }
        }
      }
    }

    // Date picker dialog
    if (showDatePicker) {
      PetitDatePickerDialog(
        selectedDate = uiState.customDate,
        onDateSelected = viewModel::setCustomDate,
        onDismissRequest = { showDatePicker = false },
      )
    }
  }
}
