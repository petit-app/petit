package com.woliveiras.petit.presentation.feature.weight

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorWeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.components.WeightChart
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import com.woliveiras.petit.ui.theme.LocalPetitColors

/**
 * Screen displaying weight history and chart for a pet. The form for adding/editing is now a
 * separate screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightFormScreen(
  petId: String,
  onNavigateBack: () -> Unit,
  onNavigateToAddEntry: () -> Unit,
  onNavigateToEditEntry: (entryId: String) -> Unit,
  viewModel: WeightFormViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = {
          Column {
            Text(stringResource(R.string.weight_screen_title))
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
        Icon(Icons.Default.MonitorWeight, contentDescription = stringResource(R.string.weight_add))
      }
    },
  ) { padding ->
    WeightHistoryContent(
      entries = uiState.weightEntries,
      onNavigateToEditEntry = onNavigateToEditEntry,
      modifier = Modifier.padding(padding),
    )
  }
}

@Composable
internal fun WeightHistoryContent(
  entries: List<WeightEntry>,
  onNavigateToEditEntry: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val displayFormatter = rememberAppDisplayFormatter()
  val latestWeight = entries.maxByOrNull { it.date }
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      CurrentWeightCard(weight = latestWeight?.let { displayFormatter.weight(it.weightGrams) })
    }

    if (entries.size >= 2) {
      item { EvolutionChartCard(entries = entries) }
    }

    item {
      Text(
        text = stringResource(R.string.weight_history_title),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
      )
    }

    if (entries.isEmpty()) {
      item { EmptyHistoryCard() }
    } else {
      val sortedEntries = entries.sortedByDescending { it.date }
      itemsIndexed(sortedEntries, key = { _, entry -> entry.id }) { index, entry ->
        val previousEntry = if (index < sortedEntries.size - 1) sortedEntries[index + 1] else null
        val weightDiff = previousEntry?.let { entry.weightKg - it.weightKg }

        WeightHistoryItem(
          entry = entry,
          weightDifference = weightDiff,
          onClick = { onNavigateToEditEntry(entry.id) },
        )
      }
    }
  }
}

@Composable
private fun CurrentWeightCard(weight: String?) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.weight_current_label),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (weight != null) {
        Text(
          text = weight,
          fontSize = 48.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
      } else {
        Text(
          text = "-",
          style = MaterialTheme.typography.displayMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun EvolutionChartCard(entries: List<WeightEntry>) {
  val displayFormatter = rememberAppDisplayFormatter()
  val sortedEntries = entries.sortedBy { it.date }
  val firstEntry = sortedEntries.firstOrNull()
  val lastEntry = sortedEntries.lastOrNull()
  val chartDescription =
    if (firstEntry != null && lastEntry != null) {
      stringResource(
        R.string.weight_chart_description,
        displayFormatter.decimal(firstEntry.weightKg, fractionDigits = 1),
        displayFormatter.dayMonth(firstEntry.date),
        displayFormatter.decimal(lastEntry.weightKg, fractionDigits = 1),
        displayFormatter.dayMonth(lastEntry.date),
      )
    } else {
      ""
    }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.weight_evolution_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp),
      )
      Box(modifier = Modifier.semantics { contentDescription = chartDescription }) {
        WeightChart(entries = entries)
      }
    }
  }
}

@Composable
private fun EmptyHistoryCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Text(
      text = stringResource(R.string.weight_no_history),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    )
  }
}

@Composable
private fun WeightHistoryItem(entry: WeightEntry, weightDifference: Double?, onClick: () -> Unit) {
  val petitColors = LocalPetitColors.current
  val displayFormatter = rememberAppDisplayFormatter()
  val weightText = displayFormatter.weight(entry.weightGrams)
  val dateText = displayFormatter.shortDate(entry.date)
  val diffText =
    if (weightDifference != null && weightDifference != 0.0) {
      displayFormatter.signedKilograms(weightDifference)
    } else null
  val itemDescription = listOfNotNull(weightText, dateText, diffText).joinToString(", ")

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .semantics(mergeDescendants = true) { contentDescription = itemDescription }
        .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Weight and Date
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = weightText,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = dateText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Weight Difference
      if (weightDifference != null && weightDifference != 0.0) {
        val isPositive = weightDifference > 0
        val diffText = displayFormatter.signedKilograms(weightDifference)
        val diffColor =
          if (isPositive) {
            petitColors.weightGain
          } else {
            petitColors.weightLoss
          }

        Text(
          text = diffText,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
          color = diffColor,
        )
      }
    }
  }
}
