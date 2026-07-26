package com.woliveiras.petit.presentation.feature.vaccination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.domain.model.groupVaccinationsByType
import com.woliveiras.petit.presentation.components.EmptyState
import com.woliveiras.petit.presentation.components.HealthStatusBadge
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.localizedName
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import com.woliveiras.petit.ui.theme.*
import java.time.LocalDate

/**
 * Screen displaying vaccination records for a pet. Shows latest vaccination for each type with
 * status indicators.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationRecordsScreen(
  petId: String,
  onNavigateBack: () -> Unit,
  onNavigateToAddEntry: () -> Unit = {},
  onNavigateToEditEntry: (entryId: String) -> Unit = {},
  viewModel: VaccinationViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = {
          Column {
            Text(stringResource(R.string.vaccination_title))
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
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = onNavigateToAddEntry) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.vaccination_add))
      }
    },
  ) { padding ->
    if (uiState.allVaccinations.isEmpty() && !uiState.isLoading) {
      EmptyState(
        icon = Icons.Default.Vaccines,
        title = stringResource(R.string.empty_vaccination_title),
        description = stringResource(R.string.empty_vaccination_description, uiState.petName),
        actionLabel = stringResource(R.string.empty_vaccination_action),
        onAction = onNavigateToAddEntry,
        modifier = Modifier.padding(padding),
      )
    } else {
      VaccinationTimeline(
        vaccinations = uiState.allVaccinations,
        today = uiState.today,
        onEditEntry = { onNavigateToEditEntry(it.id) },
        modifier = Modifier.padding(padding),
      )
    }
  }
}

@Composable
internal fun VaccinationTimeline(
  vaccinations: List<VaccinationEntry>,
  today: LocalDate,
  onEditEntry: (VaccinationEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  val groups = remember(vaccinations) { vaccinations.groupVaccinationsByType() }

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    groups.forEachIndexed { index, group ->
      item(key = "header_${group.vaccineType}") {
        if (index > 0) {
          Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = group.latest.effectiveVaccineDisplayName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = stringResource(R.string.vaccination_history_dose_count, group.history.size),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          HealthStatusBadge(status = group.latest.status(today))
        }
      }

      items(group.history, key = { it.id }) { vaccination ->
        VaccinationTimelineCard(
          vaccination = vaccination,
          status = vaccination.status(today),
          onEdit = { onEditEntry(vaccination) },
        )
      }
    }
  }
}

@Composable
private fun VaccinationTimelineCard(
  vaccination: VaccinationEntry,
  status: HealthStatus,
  onEdit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val displayFormatter = rememberAppDisplayFormatter()
  val cardDescription =
    listOfNotNull(
        vaccination.effectiveVaccineDisplayName,
        displayFormatter.shortDate(vaccination.applicationDate),
        vaccination.nextDueDate?.let(displayFormatter::shortDate),
        status.localizedName(),
      )
      .joinToString(", ")

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) { contentDescription = cardDescription }
        .clickable(onClick = onEdit),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row {
      // Colored side strip per vaccine type
      Box(
        modifier =
          Modifier.width(4.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .background(vaccineTypeColor(vaccination.vaccineType))
      )

      Column(modifier = Modifier.weight(1f).padding(16.dp)) {
        // Header row: Vaccine type + Status badge
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = vaccination.effectiveVaccineDisplayName,
            style = MaterialTheme.typography.titleLarge,
          )

          HealthStatusBadge(status = status)
        }

        // Application date
        Text(
          text = displayFormatter.shortDate(vaccination.applicationDate),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Detail rows
        vaccination.nextDueDate?.let { nextDate ->
          DetailRow(
            label = stringResource(R.string.vaccination_detail_next_dose),
            value = displayFormatter.shortDate(nextDate),
          )
        }

        vaccination.veterinarian
          ?.takeIf { it.isNotBlank() }
          ?.let { vet ->
            DetailRow(label = stringResource(R.string.vaccination_detail_veterinarian), value = vet)
          }

        vaccination.clinic
          ?.takeIf { it.isNotBlank() }
          ?.let { clinic ->
            DetailRow(label = stringResource(R.string.vaccination_detail_clinic), value = clinic)
          }

        vaccination.batchNumber
          ?.takeIf { it.isNotBlank() }
          ?.let { batch ->
            DetailRow(label = stringResource(R.string.vaccination_detail_batch), value = batch)
          }

        vaccination.note
          ?.takeIf { it.isNotBlank() }
          ?.let { note ->
            DetailRow(label = stringResource(R.string.vaccination_field_note), value = note)
          }
      }
    }
  }
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

private fun vaccineTypeColor(type: VaccineType): Color {
  return when (type) {
    VaccineType.V3 -> VaccineV3Color
    VaccineType.V4 -> VaccineV4Color
    VaccineType.V5 -> VaccineV5Color
    VaccineType.RABIES -> VaccineRabiesColor
    VaccineType.FELV -> VaccineFelvColor
    VaccineType.FIV -> VaccineFivColor
    VaccineType.OTHER -> VaccineOtherColor
    else -> VaccineOtherColor
  }
}
