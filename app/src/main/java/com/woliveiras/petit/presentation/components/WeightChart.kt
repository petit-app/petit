package com.woliveiras.petit.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import com.woliveiras.petit.ui.theme.PetitTheme

/**
 * A bar chart component that displays weight evolution over time.
 *
 * @param entries List of weight entries to display (should be sorted by date ascending).
 * @param modifier Modifier for the chart container.
 */
@Composable
fun WeightChart(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
  if (entries.size < 2) return

  val primaryColor = MaterialTheme.colorScheme.primary
  val displayFormatter = rememberAppDisplayFormatter()
  val chartDescription =
    pluralStringResource(
      R.plurals.weight_chart_accessibility_description,
      entries.size,
      entries.size,
    )

  val modelProducer = remember { CartesianChartModelProducer() }

  // Prepare data - entries should be sorted by date ASC for proper display
  val sortedEntries = remember(entries) { entries.sortedBy { it.date } }
  val weights = remember(sortedEntries) { sortedEntries.map { it.weightKg } }

  // Date formatter for axis labels
  val dateLabels =
    remember(sortedEntries, displayFormatter) {
      sortedEntries.map { displayFormatter.dayMonth(it.date) }
    }

  // Update chart data
  LaunchedEffect(sortedEntries) {
    modelProducer.runTransaction { columnSeries { series(weights) } }
  }

  val column =
    remember(primaryColor) {
      LineComponent(
        fill = Fill(primaryColor.toArgb()),
        thicknessDp = 16f,
        shape = CorneredShape.rounded(allPercent = 20),
      )
    }

  val onSurfaceColor = MaterialTheme.colorScheme.onSurface
  val outlineColor = MaterialTheme.colorScheme.outlineVariant

  val axisLabel = remember(onSurfaceColor) { TextComponent(color = onSurfaceColor.toArgb()) }
  val axisLine =
    remember(outlineColor) { LineComponent(fill = Fill(outlineColor.toArgb()), thicknessDp = 1f) }
  val guideline =
    remember(outlineColor) {
      LineComponent(fill = Fill(outlineColor.copy(alpha = 0.3f).toArgb()), thicknessDp = 0.5f)
    }

  CartesianChartHost(
    chart =
      rememberCartesianChart(
        rememberColumnCartesianLayer(
          columnProvider = ColumnCartesianLayer.ColumnProvider.series(column)
        ),
        startAxis =
          VerticalAxis.rememberStart(
            label = axisLabel,
            line = axisLine,
            guideline = guideline,
            valueFormatter = { _, value, _ -> displayFormatter.decimal(value, fractionDigits = 1) },
          ),
        bottomAxis =
          HorizontalAxis.rememberBottom(
            label = axisLabel,
            line = axisLine,
            guideline = null,
            valueFormatter = { _, value, _ -> dateLabels.getOrNull(value.toInt()) ?: "" },
          ),
      ),
    modelProducer = modelProducer,
    modifier =
      modifier.fillMaxWidth().height(200.dp).semantics { contentDescription = chartDescription },
  )
}

@Preview(showBackground = true)
@Composable
private fun WeightChartPreview() {
  PetitTheme {
    WeightChart(
      entries =
        listOf(
          WeightEntry(
            id = "1",
            petId = "pet1",
            date = java.time.LocalDate.of(2025, 1, 10),
            weightGrams = 4200,
            createdAt = 0L,
            updatedAt = 0L,
          ),
          WeightEntry(
            id = "2",
            petId = "pet1",
            date = java.time.LocalDate.of(2025, 2, 10),
            weightGrams = 4350,
            createdAt = 0L,
            updatedAt = 0L,
          ),
          WeightEntry(
            id = "3",
            petId = "pet1",
            date = java.time.LocalDate.of(2025, 3, 10),
            weightGrams = 4100,
            createdAt = 0L,
            updatedAt = 0L,
          ),
        )
    )
  }
}
