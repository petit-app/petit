package com.woliveiras.petit.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.presentation.util.localizedName

@Composable
fun HealthStatusBadge(status: HealthStatus, modifier: Modifier = Modifier) {
  val (icon, color) =
    when (status) {
      HealthStatus.OK -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.tertiary
      HealthStatus.SCHEDULED -> Icons.Default.Schedule to MaterialTheme.colorScheme.secondary
      HealthStatus.OVERDUE -> Icons.Default.Warning to MaterialTheme.colorScheme.error
    }
  val label = status.localizedName()

  Row(
    modifier =
      modifier
        .semantics { contentDescription = label }
        .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
    Text(label, style = MaterialTheme.typography.labelSmall, color = color)
  }
}
