package com.woliveiras.petit.presentation.feature.pets

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.presentation.components.HealthStatusBadge
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.feature.settings.ExportImportEvent
import com.woliveiras.petit.presentation.feature.settings.ExportImportViewModel
import com.woliveiras.petit.presentation.feature.settings.createBackupShareIntent
import com.woliveiras.petit.presentation.util.localizedBreed
import com.woliveiras.petit.presentation.util.localizedColor
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import com.woliveiras.petit.ui.theme.LocalPetitColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
  petId: String,
  onNavigateBack: () -> Unit,
  onNavigateToEdit: () -> Unit,
  onNavigateToWeight: () -> Unit,
  onNavigateToVaccinations: () -> Unit,
  onNavigateToDeworming: () -> Unit,
  onNavigateToDelete: () -> Unit,
  viewModel: PetDetailViewModel = hiltViewModel(),
  exportImportViewModel: ExportImportViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showMenu by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
      uri: Uri? ->
      uri?.let(exportImportViewModel::writeExportToUri)
    }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is PetDetailEvent.PetDeleted -> {
          /* Handled by delete confirmation screen */
        }
        is PetDetailEvent.Error -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }
  LaunchedEffect(Unit) {
    exportImportViewModel.events.collect { event ->
      when (event) {
        is ExportImportEvent.ExportReady -> exportLauncher.launch(event.filename)
        is ExportImportEvent.ExportSuccess ->
          context.startActivity(Intent.createChooser(createBackupShareIntent(event.uri), null))
        is ExportImportEvent.Error -> snackbarHostState.showSnackbar(event.message)
        is ExportImportEvent.ImportSuccess -> Unit
      }
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PetitTopAppBar(
        title = { Text(uiState.pet?.name ?: stringResource(R.string.pet_detail_loading)) },
        onNavigateBack = onNavigateBack,
        actions = {
          IconButton(onClick = onNavigateToEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
          }
          Box {
            IconButton(onClick = { showMenu = true }) {
              Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.action_more_options),
              )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                text = {
                  Text(
                    stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                  )
                },
                onClick = {
                  showMenu = false
                  onNavigateToDelete()
                },
                leadingIcon = {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_icon_delete),
                    tint = MaterialTheme.colorScheme.error,
                  )
                },
              )
            }
          }
        },
      )
    },
  ) { padding ->
    val layoutDirection = LocalLayoutDirection.current
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            top = padding.calculateTopPadding(),
            start = padding.calculateLeftPadding(layoutDirection),
            end = padding.calculateRightPadding(layoutDirection),
          )
    ) {
      when {
        uiState.isLoading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        uiState.error != null -> {
          Text(
            text = uiState.error!!,
            modifier = Modifier.align(Alignment.Center).padding(16.dp),
            color = MaterialTheme.colorScheme.error,
          )
        }
        uiState.pet != null -> {
          PetDetailContent(
            uiState = uiState,
            onWeightClick = onNavigateToWeight,
            onVaccinationsClick = onNavigateToVaccinations,
            onDewormingClick = onNavigateToDeworming,
            onShareProfile = { exportImportViewModel.startExportForPet(petId) },
          )
        }
      }
    }
  }
}

@Composable
internal fun PetDetailContent(
  uiState: PetDetailUiState,
  onWeightClick: () -> Unit,
  onVaccinationsClick: () -> Unit,
  onDewormingClick: () -> Unit,
  onShareProfile: () -> Unit,
) {
  val pet = uiState.pet ?: return
  val context = LocalContext.current
  val displayFormatter = rememberAppDisplayFormatter()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Profile Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        // Photo and Info Row
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Pet photo
          Surface(
            modifier = Modifier.size(92.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
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
                  style = MaterialTheme.typography.headlineLarge,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(16.dp))

          // Name and Info
          Column {
            Text(
              text = pet.name,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            // Breed info
            val breedDisplay = localizedBreed(pet.breedId, pet.breed)
            val colorDisplay = pet.color?.let { localizedColor(it) }
            val breedInfo = listOfNotNull(breedDisplay, colorDisplay).joinToString(" ")
            if (breedInfo.isNotEmpty()) {
              Text(
                text = breedInfo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            // Birth date
            pet.birthDate?.let { birthDate ->
              Text(
                text = displayFormatter.shortDate(birthDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Chips Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Sex chip
          InfoChip(
            label = stringResource(R.string.pet_detail_sex_label),
            value =
              when (pet.sex) {
                Sex.MALE -> "♂"
                Sex.FEMALE -> "♀"
                Sex.UNKNOWN -> "?"
              },
            modifier = Modifier.weight(1f),
          )

          // Color chip
          InfoChip(
            label = stringResource(R.string.pet_detail_color_label),
            value = pet.color?.let { localizedColor(it) } ?: "-",
            modifier = Modifier.weight(1f),
          )

          // Microchip chip
          InfoChip(
            label = stringResource(R.string.pet_detail_chip_label),
            value = if (pet.microchipNumber != null) "✓" else "-",
            showCheck = pet.microchipNumber != null,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    Text(
      text = stringResource(R.string.pet_detail_section_management),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
    )

    ManagementActionsGrid(
      weightRecordCount = uiState.weightRecordCount,
      currentWeight = uiState.latestWeight?.let { displayFormatter.weight(it.weightGrams) },
      vaccinationRecordCount = uiState.vaccinationRecordCount,
      dewormingRecordCount = uiState.dewormingRecordCount,
      onWeightClick = onWeightClick,
      onVaccinationsClick = onVaccinationsClick,
      onDewormingClick = onDewormingClick,
      onShareProfile = onShareProfile,
    )
  }
}

@Composable
private fun ManagementActionsGrid(
  weightRecordCount: Int,
  currentWeight: String?,
  vaccinationRecordCount: Int,
  dewormingRecordCount: Int,
  onWeightClick: () -> Unit,
  onVaccinationsClick: () -> Unit,
  onDewormingClick: () -> Unit,
  onShareProfile: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      ManagementActionCard(
        icon = Icons.Default.MedicalServices,
        title = stringResource(R.string.pet_detail_section_vaccinations),
        subtitle = formatRecordCount(vaccinationRecordCount),
        backgroundColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        iconBackgroundColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        iconTint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.weight(1f),
        onClick = onVaccinationsClick,
      )

      ManagementActionCard(
        icon = Icons.Default.Scale,
        title = stringResource(R.string.pet_detail_section_weight),
        subtitle = stringResource(R.string.pet_detail_weight_ideal_range),
        value = currentWeight ?: "- kg",
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.primary,
        iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.weight(1f),
        onClick = onWeightClick,
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      ManagementActionCard(
        icon = Icons.Default.Medication,
        title = stringResource(R.string.pet_detail_section_deworming),
        subtitle = formatRecordCount(dewormingRecordCount),
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.weight(1f),
        onClick = onDewormingClick,
      )

      ManagementActionCard(
        icon = Icons.Default.Share,
        title = stringResource(R.string.pet_detail_section_share_profile),
        subtitle = stringResource(R.string.pet_detail_share_backup_description),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
        onClick = onShareProfile,
      )
    }
  }
}

@Composable
private fun ManagementActionCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  backgroundColor: Color,
  contentColor: Color,
  iconBackgroundColor: Color,
  iconTint: Color,
  modifier: Modifier = Modifier,
  value: String? = null,
  onClick: (() -> Unit)? = null,
) {
  val enabled = onClick != null
  val container = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.68f)
  val content = if (enabled) contentColor else contentColor.copy(alpha = 0.82f)
  val cardDescription = if (value != null) "$title, $value, $subtitle" else "$title, $subtitle"

  Card(
    onClick = { onClick?.invoke() },
    enabled = enabled,
    modifier =
      modifier.height(148.dp).semantics(mergeDescendants = true) {
        contentDescription = cardDescription
      },
    colors = CardDefaults.cardColors(containerColor = container),
  ) {
    ManagementActionCardContent(
      icon = icon,
      title = title,
      subtitle = subtitle,
      contentColor = content,
      iconBackgroundColor = iconBackgroundColor,
      iconTint = iconTint,
      value = value,
    )
  }
}

@Composable
private fun ManagementActionCardContent(
  icon: ImageVector,
  title: String,
  subtitle: String,
  contentColor: Color,
  iconBackgroundColor: Color,
  iconTint: Color,
  value: String?,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier = Modifier.size(44.dp).background(iconBackgroundColor, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
    }

    if (value != null) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = contentColor,
      )
    }

    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = contentColor,
      maxLines = 2,
    )

    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = contentColor.copy(alpha = 0.88f),
      maxLines = 2,
    )
  }
}

@Composable
private fun formatRecordCount(recordCount: Int): String {
  return if (recordCount == 1) {
    stringResource(R.string.pet_detail_record_count_singular, recordCount)
  } else {
    stringResource(R.string.pet_detail_records_count, recordCount)
  }
}

@Composable
private fun InfoChip(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  showCheck: Boolean = false,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
  ) {
    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      if (showCheck) {
        Icon(
          Icons.Default.Check,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
      } else {
        Text(
          text = value,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun WeightHealthCard(
  title: String,
  recordCount: Int,
  currentWeight: String?,
  onClick: () -> Unit,
) {
  val recordsText =
    if (recordCount == 1) {
      stringResource(R.string.pet_detail_record_count_singular, recordCount)
    } else {
      stringResource(R.string.pet_detail_records_count, recordCount)
    }
  val weightDescription =
    if (currentWeight != null) "$title, $currentWeight, $recordsText" else "$title, $recordsText"

  Card(
    onClick = onClick,
    modifier =
      Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = weightDescription
      },
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Icon circle
        Box(
          modifier =
            Modifier.size(48.dp).background(LocalPetitColors.current.weightSectionBg, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Default.Scale,
            contentDescription = null,
            tint = LocalPetitColors.current.weightSectionTint,
            modifier = Modifier.size(24.dp),
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and record count
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              if (recordCount == 1) {
                stringResource(R.string.pet_detail_record_count_singular, recordCount)
              } else {
                stringResource(R.string.pet_detail_records_count, recordCount)
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // Chevron
        Icon(
          Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      }

      // Weight value at bottom
      if (currentWeight != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = currentWeight,
          style = MaterialTheme.typography.displaySmall,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun StatusHealthCard(
  icon: ImageVector,
  iconBackgroundColor: Color,
  iconTint: Color,
  title: String,
  recordCount: Int,
  status: HealthStatus,
  onClick: () -> Unit,
) {
  val statusRecordsText =
    if (recordCount == 1) {
      stringResource(R.string.pet_detail_record_count_singular, recordCount)
    } else {
      stringResource(R.string.pet_detail_records_count, recordCount)
    }
  val statusDescription = "$title, $statusRecordsText"

  Card(
    onClick = onClick,
    modifier =
      Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = statusDescription
      },
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Icon circle
        Box(
          modifier = Modifier.size(48.dp).background(iconBackgroundColor, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and record count
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              if (recordCount == 1) {
                stringResource(R.string.pet_detail_record_count_singular, recordCount)
              } else {
                stringResource(R.string.pet_detail_records_count, recordCount)
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // Chevron
        Icon(
          Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      }

      // Status badge at bottom
      if (status != HealthStatus.OK) {
        Spacer(modifier = Modifier.height(12.dp))
        HealthStatusBadge(status = status)
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
    Text(
      text = "$label: ",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(text = value, style = MaterialTheme.typography.bodyMedium)
  }
}
