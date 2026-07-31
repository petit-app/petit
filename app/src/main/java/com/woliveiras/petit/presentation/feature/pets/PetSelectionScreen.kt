package com.woliveiras.petit.presentation.feature.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.navigation.Screen

/** Full-screen pet selection for actions like weight tracking, vaccination, etc. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetSelectionScreen(
  action: String,
  onNavigateBack: () -> Unit,
  onPetSelected: (petId: String) -> Unit,
  onNavigateToAddPet: () -> Unit,
  viewModel: PetSelectionViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val pets = uiState.pets

  val title =
    when (action) {
      Screen.PetSelection.ACTION_WEIGHT -> stringResource(R.string.weight_select_pet_title)
      Screen.PetSelection.ACTION_VACCINATION ->
        stringResource(R.string.vaccination_select_pet_title)
      Screen.PetSelection.ACTION_DEWORMING_INTERNAL,
      Screen.PetSelection.ACTION_DEWORMING_EXTERNAL ->
        stringResource(R.string.deworming_select_pet_title)
      else -> stringResource(R.string.weight_select_pet_title)
    }

  Scaffold(topBar = { PetitTopAppBar(title = { Text(title) }, onNavigateBack = onNavigateBack) }) {
    padding ->
    if (pets.isEmpty()) {
      EmptyState(modifier = Modifier.padding(padding), onAddPet = onNavigateToAddPet)
    } else {
      PetList(pets = pets, onPetSelected = onPetSelected, modifier = Modifier.padding(padding))
    }
  }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onAddPet: () -> Unit) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.Pets,
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = stringResource(R.string.pet_list_empty_message),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(24.dp))
      Button(onClick = onAddPet) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = stringResource(R.string.cd_icon_add),
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.pet_list_add_button))
      }
    }
  }
}

@Composable
private fun PetList(
  pets: List<Pet>,
  onPetSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(pets, key = { it.id }) { pet ->
      PetSelectionItem(pet = pet, onClick = { onPetSelected(pet.id) })
    }
  }
}

@Composable
private fun PetSelectionItem(pet: Pet, onClick: () -> Unit) {
  val context = LocalContext.current
  Card(
    onClick = onClick,
    modifier =
      Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = pet.name },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier.size(48.dp).clip(CircleShape),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (pet.photoUri != null) {
            AsyncImage(
              model = ImageRequest.Builder(context).data(pet.photoUri).crossfade(true).build(),
              contentDescription = pet.name,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
              contentScale = ContentScale.Crop,
            )
          } else {
            Text(
              text = pet.name.first().uppercase(),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
      Spacer(modifier = Modifier.width(16.dp))
      Text(
        text = pet.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
