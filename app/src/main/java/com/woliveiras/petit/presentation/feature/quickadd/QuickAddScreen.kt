package com.woliveiras.petit.presentation.feature.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.R
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.ui.theme.LocalPetitColors

/** Quick add screen for selecting what type of record to add. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
  onNavigateBack: () -> Unit,
  onSelectWeight: () -> Unit,
  onSelectVaccination: () -> Unit,
  onSelectDeworming: () -> Unit,
  onSelectFleaAndTick: () -> Unit,
  onSelectReminder: () -> Unit,
  onSelectNewPet: () -> Unit,
) {
  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.quick_add_title)) },
        onNavigateBack = onNavigateBack,
      )
    }
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.quick_add_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(8.dp))

      val petitColors = LocalPetitColors.current

      // Weight option
      QuickAddOption(
        icon = Icons.Default.MonitorWeight,
        iconBackground = petitColors.weightSectionBg,
        title = stringResource(R.string.quick_add_weight),
        description = stringResource(R.string.quick_add_weight_desc),
        onClick = onSelectWeight,
      )

      // Vaccination option
      QuickAddOption(
        icon = Icons.Default.Vaccines,
        iconBackground = petitColors.vaccinationSectionBg,
        title = stringResource(R.string.quick_add_vaccination),
        description = stringResource(R.string.quick_add_vaccination_desc),
        onClick = onSelectVaccination,
      )

      // Internal dewormer option
      QuickAddOption(
        icon = Icons.Default.Healing,
        iconBackground = petitColors.dewormingSectionBg,
        title = stringResource(R.string.quick_add_deworming),
        description = stringResource(R.string.quick_add_deworming_desc),
        onClick = onSelectDeworming,
      )

      // Flea and tick option
      QuickAddOption(
        icon = Icons.Default.BugReport,
        iconBackground = petitColors.dewormingSectionBg,
        title = stringResource(R.string.quick_add_flea_tick),
        description = stringResource(R.string.quick_add_flea_tick_desc),
        onClick = onSelectFleaAndTick,
      )

      // Reminder option
      QuickAddOption(
        icon = Icons.Default.Notifications,
        iconBackground = petitColors.weightSectionBg,
        title = stringResource(R.string.quick_add_reminder),
        description = stringResource(R.string.quick_add_reminder_desc),
        onClick = onSelectReminder,
      )

      // New pet option
      QuickAddOption(
        icon = Icons.Default.Pets,
        iconBackground = petitColors.weightSectionBg,
        title = stringResource(R.string.quick_add_new_pet),
        description = stringResource(R.string.quick_add_new_pet_desc),
        onClick = onSelectNewPet,
      )
    }
  }
}

@Composable
private fun QuickAddOption(
  icon: ImageVector,
  iconBackground: Color,
  title: String,
  description: String,
  onClick: () -> Unit,
) {
  val optionDescription = "$title, $description"

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .semantics(mergeDescendants = true) { contentDescription = optionDescription }
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBackground),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
