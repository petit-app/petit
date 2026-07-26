package com.woliveiras.petit.presentation.feature.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.BreedCatalogItem
import com.woliveiras.petit.domain.model.BreedIdentity
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.presentation.components.PetitTopAppBar

@Composable
fun BreedSelectionRoute(
  species: PetType,
  initialSelection: BreedSelectionValue,
  onNavigateBack: () -> Unit,
  onConfirmed: (BreedSelectionValue) -> Unit,
  viewModel: BreedSelectionViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  LaunchedEffect(species, initialSelection) { viewModel.initialize(species, initialSelection) }

  BreedSelectionScreen(
    uiState = uiState,
    onNavigateBack = onNavigateBack,
    onQueryChange = viewModel::updateQuery,
    onCatalogSelected = viewModel::selectCatalogBreed,
    onMixedSelected = viewModel::selectMixedBreed,
    onUnknownSelected = viewModel::selectUnknownBreed,
    onManualSelected = viewModel::selectManualEntry,
    onManualBreedChange = viewModel::updateManualBreed,
    onNoBreedSelected = viewModel::selectNoBreed,
    onInitialSelectionSelected = viewModel::selectInitialSelection,
    onConfirm = { viewModel.confirmSelection()?.let(onConfirmed) },
  )
}

@Composable
fun BreedSelectionScreen(
  uiState: BreedSelectionUiState,
  onNavigateBack: () -> Unit,
  onQueryChange: (String) -> Unit,
  onCatalogSelected: (BreedCatalogItem) -> Unit,
  onMixedSelected: () -> Unit,
  onUnknownSelected: () -> Unit,
  onManualSelected: () -> Unit,
  onManualBreedChange: (String) -> Unit,
  onNoBreedSelected: () -> Unit,
  onInitialSelectionSelected: () -> Unit,
  onConfirm: () -> Unit,
) {
  Scaffold(
    modifier = Modifier.systemBarsPadding(),
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.breed_catalog_title)) },
        onNavigateBack = onNavigateBack,
      )
    },
    bottomBar = {
      Button(
        onClick = onConfirm,
        enabled = uiState.canConfirm,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        Text(stringResource(R.string.action_confirm))
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OutlinedTextField(
        value = uiState.query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.breed_catalog_search_label)) },
        singleLine = true,
      )

      if (uiState.selectionMode == BreedSelectionMode.MANUAL) {
        OutlinedTextField(
          value = uiState.manualBreed,
          onValueChange = onManualBreedChange,
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.breed_catalog_manual)) },
          supportingText =
            if (uiState.manualError != null) {
              { Text(uiState.manualError) }
            } else {
              null
            },
          isError = uiState.manualError != null,
          singleLine = true,
        )
      }

      val isLoading = uiState.isCatalogLoading || uiState.isSearchLoading
      val resultStatus =
        if (isLoading) {
          stringResource(R.string.breed_catalog_loading)
        } else if (uiState.results.isEmpty()) {
          stringResource(R.string.breed_catalog_empty)
        } else {
          stringResource(R.string.breed_catalog_result_count, uiState.results.size)
        }
      Text(
        text = resultStatus,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).selectableGroup()) {
        item(key = "mixed") {
          BreedChoiceRow(
            label = stringResource(R.string.breed_mixed),
            selected = uiState.selectionMode == BreedSelectionMode.MIXED,
            onClick = onMixedSelected,
          )
        }
        item(key = "unknown") {
          BreedChoiceRow(
            label = stringResource(R.string.breed_unknown),
            selected = uiState.selectionMode == BreedSelectionMode.UNKNOWN,
            onClick = onUnknownSelected,
          )
        }
        item(key = "manual") {
          BreedChoiceRow(
            label = stringResource(R.string.breed_catalog_manual),
            selected = uiState.selectionMode == BreedSelectionMode.MANUAL,
            onClick = onManualSelected,
          )
        }
        item(key = "empty") {
          BreedChoiceRow(
            label = stringResource(R.string.pet_form_breed_none_selected),
            selected = uiState.selectionMode == BreedSelectionMode.EMPTY,
            onClick = onNoBreedSelected,
          )
        }
        val initialCatalogSelection =
          uiState.initialSelection.takeIf { initial ->
            initial.breedId != null &&
              uiState.results.none { it.id == initial.breedId } &&
              initial.breedId != BreedIdentity.MIXED_BREED_ID &&
              initial.breedId != BreedIdentity.UNKNOWN_BREED_ID
          }
        if (initialCatalogSelection != null) {
          item(key = "initial-${initialCatalogSelection.breedId}") {
            BreedChoiceRow(
              label =
                initialCatalogSelection.displayName
                  ?: initialCatalogSelection.breed.takeIf { it.isNotBlank() }
                  ?: initialCatalogSelection.breedId.orEmpty(),
              selected = uiState.draftSelection.breedId == initialCatalogSelection.breedId,
              onClick = onInitialSelectionSelected,
            )
          }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
        if (isLoading) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalArrangement = Arrangement.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        } else {
          items(uiState.results, key = { it.id }) { breed ->
            BreedChoiceRow(
              label = breed.displayName,
              selected = uiState.draftSelection.breedId == breed.id,
              onClick = { onCatalogSelected(breed) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BreedChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    Text(
      text = label,
      modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp),
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}
