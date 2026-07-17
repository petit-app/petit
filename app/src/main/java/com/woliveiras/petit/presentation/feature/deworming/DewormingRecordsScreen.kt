package com.woliveiras.petit.presentation.feature.deworming

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Medication
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.DewormingCategory
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.dewormingCategorySummaries
import com.woliveiras.petit.domain.model.sortedDewormingHistory
import com.woliveiras.petit.presentation.components.EmptyState
import com.woliveiras.petit.presentation.components.HealthStatusBadge
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.localizedName
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Screen displaying deworming records for a pet. Shows a timeline of all deworming treatments
 * grouped by month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DewormingRecordsScreen(
  petId: String,
  onNavigateBack: () -> Unit,
  onNavigateToAddEntry: () -> Unit = {},
  onNavigateToEditEntry: (entryId: String) -> Unit = {},
  viewModel: DewormingViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = {
          Column {
            Text(stringResource(R.string.deworming_title))
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
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.deworming_add))
      }
    },
  ) { padding ->
    if (uiState.allDewormings.isEmpty() && !uiState.isLoading) {
      EmptyState(
        icon = Icons.Default.Medication,
        title = stringResource(R.string.empty_deworming_title),
        description = stringResource(R.string.empty_deworming_description, uiState.petName),
        actionLabel = stringResource(R.string.empty_deworming_action),
        onAction = onNavigateToAddEntry,
        modifier = Modifier.padding(padding),
      )
    } else {
      DewormingTimeline(
        dewormings = uiState.allDewormings,
        today = uiState.today,
        onEditEntry = { onNavigateToEditEntry(it.id) },
        modifier = Modifier.padding(padding),
      )
    }
  }
}

@Composable
internal fun DewormingTimeline(
  dewormings: List<DewormingEntry>,
  today: LocalDate,
  onEditEntry: (DewormingEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
  val categorySummaries = remember(dewormings) { dewormings.dewormingCategorySummaries() }

  // Group dewormings by YearMonth, sorted descending (most recent first)
  val groupedByMonth =
    remember(dewormings) {
      dewormings
        .sortedDewormingHistory()
        .groupBy { YearMonth.from(it.applicationDate) }
        .toSortedMap(compareByDescending { it })
    }

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(categorySummaries, key = { "category_${it.category}" }) { summary ->
      DewormingCategorySummaryCard(
        category = summary.category,
        latest = summary.latest,
        status = summary.latest.status(today),
      )
    }

    groupedByMonth.entries.forEachIndexed { index, (yearMonth, entries) ->
      // Month header
      item(key = "header_$yearMonth") {
        if (index > 0) {
          Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
          text = yearMonth.format(monthFormatter).uppercase(),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 4.dp),
        )
      }

      // Deworming cards for this month
      items(entries, key = { it.id }) { deworming ->
        DewormingTimelineCard(
          deworming = deworming,
          status = deworming.status(today),
          onEdit = { onEditEntry(deworming) },
        )
      }
    }
  }
}

@Composable
private fun DewormingCategorySummaryCard(
  category: DewormingCategory,
  latest: DewormingEntry,
  status: HealthStatus,
) {
  val categoryLabel =
    stringResource(
      when (category) {
        DewormingCategory.INTERNAL -> R.string.deworming_internal
        DewormingCategory.EXTERNAL -> R.string.deworming_external
      }
    )
  val summaryDescription =
    listOfNotNull(
        categoryLabel,
        latest.medication?.takeIf { it.isNotBlank() },
        status.localizedName(),
      )
      .joinToString(", ")
  Card(
    modifier = Modifier.fillMaxWidth().semantics { contentDescription = summaryDescription },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(categoryLabel, style = MaterialTheme.typography.titleMedium)
        Text(
          latest.medication.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      HealthStatusBadge(status = status)
    }
  }
}

@Composable
private fun DewormingTimelineCard(
  deworming: DewormingEntry,
  status: HealthStatus,
  onEdit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  val cardDescription =
    listOfNotNull(
        deworming.medication,
        deworming.type.localizedName(),
        deworming.applicationDate.format(dateFormatter),
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
    Column(modifier = Modifier.padding(16.dp)) {
      // Header row: Medication name + Status badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = deworming.medication ?: "",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )

        HealthStatusBadge(status = status)
      }

      // Application date
      Text(
        text = deworming.applicationDate.format(dateFormatter),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Detail rows
      DetailRow(
        label = stringResource(R.string.deworming_detail_type),
        value = deworming.type.localizedName(),
      )

      deworming.nextDueDate?.let { nextDate ->
        DetailRow(
          label = stringResource(R.string.deworming_detail_next_dose),
          value = nextDate.format(dateFormatter),
        )
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
