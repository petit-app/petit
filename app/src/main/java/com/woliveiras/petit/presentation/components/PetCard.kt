package com.woliveiras.petit.presentation.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.presentation.util.formatAge
import com.woliveiras.petit.presentation.util.localizedBreed
import com.woliveiras.petit.presentation.util.localizedName
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import java.time.LocalDate

/** Data class containing all the information needed to display a pet card. */
data class PetCardData(
  val pet: Pet,
  val weight: String? = null,
  val nextVaccineType: VaccineType? = null,
  val nextVaccinationDate: LocalDate? = null,
  val nextDewormingType: DewormingType? = null,
  val nextDewormingDate: LocalDate? = null,
  val overallStatus: HealthStatus? = null,
)

/**
 * Reusable pet card component with two display modes:
 * - Full (default): Photo centered, name, age/sex, horizontal divider, vertical details
 * - Compact: Photo on left, name, vertical divider, details on right (no age/sex)
 */
@Composable
fun PetCard(
  data: PetCardData,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  if (compact) {
    CompactPetCard(data = data, onClick = onClick, modifier = modifier)
  } else {
    FullPetCard(data = data, onClick = onClick, modifier = modifier)
  }
}

@Composable
private fun CompactPetCard(data: PetCardData, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val breedText = data.pet.breed?.takeIf { it.isNotBlank() }?.let { localizedBreed(it) }
  val weightText = data.weight
  val infoText = listOfNotNull(breedText, weightText).joinToString(", ")
  val healthStatus =
    data.overallStatus?.localizedName() ?: stringResource(R.string.health_status_unavailable)
  val description =
    listOfNotNull(data.pet.name, infoText.takeIf { it.isNotEmpty() }, healthStatus)
      .joinToString(", ")

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) { contentDescription = description }
        .clickable(onClick = onClick),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier.size(64.dp).clip(CircleShape),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (data.pet.photoUri != null) {
            AsyncImage(
              model = ImageRequest.Builder(context).data(data.pet.photoUri).crossfade(true).build(),
              contentDescription = null,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
              contentScale = ContentScale.Crop,
            )
          } else {
            Text(
              text = data.pet.name.first().uppercase(),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = data.pet.name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )

        val breedText2 = data.pet.breed?.takeIf { it.isNotBlank() }?.let { localizedBreed(it) }
        val weightText2 = data.weight
        val infoText2 = listOfNotNull(breedText2, weightText2).joinToString(" · ")

        if (infoText2.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = infoText2,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        data.overallStatus?.let { HealthStatusBadge(status = it) }
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun FullPetCard(data: PetCardData, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val displayFormatter = rememberAppDisplayFormatter()
  val ageInMonths = data.pet.getAgeInMonths()
  val ageText = ageInMonths?.let { formatAge(it) }
  val sexText = if (data.pet.sex != Sex.UNKNOWN) data.pet.sex.localizedName() else null
  val petTypeText = data.pet.petType.localizedName()
  val ageInfoText = listOfNotNull(ageText, sexText).joinToString(", ")
  val healthStatus =
    data.overallStatus?.localizedName() ?: stringResource(R.string.health_status_unavailable)
  val description =
    listOfNotNull(data.pet.name, ageInfoText.takeIf { it.isNotEmpty() }, healthStatus)
      .joinToString(", ")

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) { contentDescription = description }
        .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        modifier = Modifier.size(72.dp).clip(CircleShape),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (data.pet.photoUri != null) {
            AsyncImage(
              model = ImageRequest.Builder(context).data(data.pet.photoUri).crossfade(true).build(),
              contentDescription = null,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
              contentScale = ContentScale.Crop,
            )
          } else {
            Text(
              text = data.pet.name.first().uppercase(),
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Pet type chip
      AssistChip(
        onClick = {},
        label = { Text(petTypeText, style = MaterialTheme.typography.labelSmall) },
        colors =
          AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
          ),
        border = null,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = data.pet.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(8.dp))
      data.overallStatus?.let { HealthStatusBadge(status = it) }

      val infoText = listOfNotNull(ageText, sexText).joinToString(" • ")
      if (infoText.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = infoText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant,
      )
      Spacer(modifier = Modifier.height(12.dp))

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow(
          icon = {
            Icon(
              Icons.Default.Scale,
              contentDescription = stringResource(R.string.cd_icon_weight),
              modifier = Modifier.size(18.dp),
            )
          },
          label = stringResource(R.string.pet_card_weight),
          value = data.weight ?: stringResource(R.string.pet_card_no_data),
        )

        DetailRow(
          icon = {
            Icon(
              Icons.Default.MedicalServices,
              contentDescription = stringResource(R.string.cd_icon_vaccine),
              modifier = Modifier.size(18.dp),
            )
          },
          label = stringResource(R.string.pet_card_vaccine),
          value =
            if (data.nextVaccinationDate != null) {
              val vaccineName = data.nextVaccineType?.localizedName() ?: ""
              val dateStr = displayFormatter.shortDate(data.nextVaccinationDate)
              if (vaccineName.isNotEmpty()) "$vaccineName • $dateStr" else dateStr
            } else {
              stringResource(R.string.pet_card_no_data)
            },
        )

        DetailRow(
          icon = {
            Icon(
              Icons.Default.Pets,
              contentDescription = stringResource(R.string.cd_icon_deworming),
              modifier = Modifier.size(18.dp),
            )
          },
          label = stringResource(R.string.pet_card_deworming),
          value =
            if (data.nextDewormingDate != null) {
              val dewormingName = data.nextDewormingType?.localizedName() ?: ""
              val dateStr = displayFormatter.shortDate(data.nextDewormingDate)
              if (dewormingName.isNotEmpty()) "$dewormingName • $dateStr" else dateStr
            } else {
              stringResource(R.string.pet_card_no_data)
            },
        )
      }
    }
  }
}

@Composable
private fun DetailRow(icon: @Composable () -> Unit, label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    icon()
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.3f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(0.7f),
    )
  }
}
