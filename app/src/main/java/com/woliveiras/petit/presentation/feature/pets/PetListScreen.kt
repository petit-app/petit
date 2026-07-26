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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.formatAge
import com.woliveiras.petit.presentation.util.localizedBreed
import com.woliveiras.petit.presentation.util.localizedColor
import com.woliveiras.petit.presentation.util.localizedName
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetListScreen(
  onNavigateBack: () -> Unit,
  onNavigateToPetDetail: (String) -> Unit,
  onNavigateToAddPet: () -> Unit,
  viewModel: PetListViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.pet_list_title)) },
        onNavigateBack = onNavigateBack,
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = onNavigateToAddPet) {
        Icon(Icons.Default.Pets, contentDescription = stringResource(R.string.pet_list_add))
      }
    },
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      when {
        uiState.isLoading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        uiState.isEmpty -> {
          EmptyPetListContent(onNavigateToAddPet = onNavigateToAddPet)
        }
        else -> {
          PetListContent(pets = uiState.pets, onPetClick = onNavigateToPetDetail)
        }
      }
    }
  }
}

@Composable
private fun EmptyPetListContent(onNavigateToAddPet: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Default.Pets,
      contentDescription = null,
      modifier = Modifier.size(72.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.pet_list_empty_title),
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.pet_list_empty_message),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PetListContent(pets: List<PetListItem>, onPetClick: (String) -> Unit) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(pets) { petItem ->
      PetListCard(petItem = petItem, onClick = { onPetClick(petItem.pet.id) })
    }
  }
}

@Composable
private fun PetListCard(petItem: PetListItem, onClick: () -> Unit) {
  val pet = petItem.pet
  val context = LocalContext.current
  val displayFormatter = rememberAppDisplayFormatter()

  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Photo and Info Row
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Pet photo
        Surface(
          modifier = Modifier.size(80.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          if (pet.photoUri != null) {
            AsyncImage(
              model = ImageRequest.Builder(context).data(pet.photoUri).crossfade(true).build(),
              contentDescription = pet.name,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
              contentScale = ContentScale.Crop,
            )
          } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Text(
                text = pet.name.first().uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name and Breed
        Column {
          Text(
            text = pet.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          // Breed info
          val breedDisplay = localizedBreed(pet.breedId, pet.breed)
          val colorDisplay = pet.color?.let { localizedColor(it) }
          val breedInfo = listOfNotNull(breedDisplay, colorDisplay).joinToString(" ")
          if (breedInfo.isNotEmpty()) {
            Text(
              text = breedInfo,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Info Chips Row
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Sex chip
        val sexSymbol =
          when (pet.sex) {
            Sex.MALE -> "♂"
            Sex.FEMALE -> "♀"
            Sex.UNKNOWN -> ""
          }
        val sexText = pet.sex.localizedName()
        ListInfoChip(
          label = stringResource(R.string.pet_detail_sex_label),
          value = if (sexSymbol.isNotEmpty()) "$sexSymbol $sexText" else sexText,
          modifier = Modifier.weight(1f),
        )

        // Weight chip
        ListInfoChip(
          label = stringResource(R.string.pet_list_weight_label),
          value = petItem.latestWeight?.let { displayFormatter.weight(it.weightGrams) } ?: "-",
          modifier = Modifier.weight(1f),
        )

        // Age chip
        val ageText = pet.getAgeInMonths()?.let { months -> formatAge(months) } ?: "-"
        ListInfoChip(
          label = stringResource(R.string.pet_list_age_label),
          value = ageText,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun ListInfoChip(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
  }
}
