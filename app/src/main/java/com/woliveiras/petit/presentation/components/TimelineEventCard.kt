package com.woliveiras.petit.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.TimelineEvent
import com.woliveiras.petit.domain.model.TimelineEventType
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import com.woliveiras.petit.ui.theme.LocalPetitColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A compact card displaying a timeline event. Shows colored icon, cat name, event title, subtitle,
 * and date.
 */
@Composable
fun TimelineEventCard(
  modifier: Modifier = Modifier,
  event: TimelineEvent,
  onClick: (() -> Unit)? = null,
) {
  val (icon, backgroundColor) = getEventIconAndColor(event.eventType)
  val dateText = formatEventDate(event)
  val title = buildEventTitle(event)
  val subtitle = if (!event.subtitle.isNullOrBlank()) formatSubtitle(event) else null
  val cardDescription = listOfNotNull(title, subtitle, dateText).joinToString(", ")

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) { contentDescription = cardDescription }
        .then(
          if (onClick != null) {
            Modifier.clickable(onClick = onClick)
          } else {
            Modifier
          }
        ),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
      // Colored icon
      Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(backgroundColor),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Event details
      Column(modifier = Modifier.weight(1f)) {
        // Title with pet name
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        // Subtitle and date
        Row {
          if (subtitle != null) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Text(
              text = " · ",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = dateText,
            style = MaterialTheme.typography.bodySmall,
            color =
              if (event.isOverdue) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            fontWeight = if (event.isOverdue) FontWeight.Medium else FontWeight.Normal,
          )
        }
      }
    }
  }
}

@Composable
private fun buildEventTitle(event: TimelineEvent): String {
  val actionText =
    when (event.eventType) {
      TimelineEventType.WEIGHT -> stringResource(R.string.timeline_weight_recorded)
      TimelineEventType.VACCINATION -> stringResource(R.string.timeline_vaccination_done)
      TimelineEventType.DEWORMING -> stringResource(R.string.timeline_deworming_done)
      TimelineEventType.VACCINATION_DUE -> stringResource(R.string.timeline_vaccination_due)
      TimelineEventType.DEWORMING_DUE -> stringResource(R.string.timeline_deworming_due)
      TimelineEventType.REMINDER -> event.title
    }

  return if (event.petName.isNotBlank()) {
    "${event.petName} - $actionText"
  } else {
    actionText
  }
}

@Composable
private fun formatSubtitle(event: TimelineEvent): String {
  val displayFormatter = rememberAppDisplayFormatter()
  return when (event.eventType) {
    TimelineEventType.WEIGHT -> {
      // Subtitle contains weight in grams, convert to kg
      val grams = event.subtitle?.toIntOrNull() ?: 0
      displayFormatter.weight(grams)
    }
    else -> event.subtitle ?: ""
  }
}

@Composable
private fun formatEventDate(event: TimelineEvent): String {
  val displayFormatter = rememberAppDisplayFormatter()
  val today = LocalDate.now()
  val days = ChronoUnit.DAYS.between(today, event.eventDate)

  return when {
    days == 0L -> stringResource(R.string.timeline_today)
    days == 1L -> stringResource(R.string.timeline_tomorrow)
    days == -1L -> stringResource(R.string.timeline_yesterday)
    days > 1 && days <= 7 -> stringResource(R.string.timeline_in_days, days.toInt())
    days < -1 && days >= -7 -> stringResource(R.string.timeline_days_ago, (-days).toInt())
    else -> {
      displayFormatter.dayMonth(event.eventDate)
    }
  }
}

@Composable
private fun getEventIconAndColor(eventType: TimelineEventType): Pair<ImageVector, Color> {
  val petitColors = LocalPetitColors.current
  return when (eventType) {
    TimelineEventType.WEIGHT -> Icons.Default.MonitorWeight to petitColors.timelineWeightBg
    TimelineEventType.VACCINATION,
    TimelineEventType.VACCINATION_DUE -> Icons.Default.Vaccines to petitColors.timelineVaccineBg
    TimelineEventType.DEWORMING,
    TimelineEventType.DEWORMING_DUE -> Icons.Default.Healing to petitColors.timelineDewormingBg
    TimelineEventType.REMINDER -> Icons.Default.Notifications to petitColors.timelineReminderBg
  }
}
