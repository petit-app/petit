package com.woliveiras.petit.presentation.feature.tasks

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.localizedName
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import java.time.LocalDate

/** Tasks list screen showing pending tasks with time-based filters. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
  modifier: Modifier = Modifier,
  onNavigateToTaskForm: (taskId: String?) -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  onNavigateToCompletedTasks: () -> Unit = {},
  onNavigateToVaccinationForm: (petId: String, entryId: String) -> Unit = { _, _ -> },
  onNavigateToWeightForm: (petId: String, entryId: String?) -> Unit = { _, _ -> },
  onNavigateToDewormingForm: (petId: String, entryId: String) -> Unit = { _, _ -> },
  viewModel: TaskListViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is TaskListEvent.NavigateToRecord -> {
          when (event.kind) {
            TaskKind.VACCINATION ->
              if (event.petId != null && event.entryId != null) {
                onNavigateToVaccinationForm(event.petId, event.entryId)
              }
            TaskKind.DEWORMING ->
              if (event.petId != null && event.entryId != null) {
                onNavigateToDewormingForm(event.petId, event.entryId)
              }
            TaskKind.WEIGHT ->
              if (event.petId != null) {
                onNavigateToWeightForm(event.petId, event.entryId)
              }
            else -> onNavigateToTaskForm(null)
          }
        }
        is TaskListEvent.NavigateToTaskForm -> onNavigateToTaskForm(event.taskId)
        is TaskListEvent.Error -> {}
      }
    }
  }

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.tasks_title)) },
        actions = {
          IconButton(onClick = onNavigateToCompletedTasks) {
            Icon(
              Icons.Default.Inventory2,
              contentDescription = stringResource(R.string.completed_tasks_title),
            )
          }
          IconButton(onClick = onNavigateToSettings) {
            Icon(
              Icons.Default.Settings,
              contentDescription = stringResource(R.string.task_settings_title),
            )
          }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { onNavigateToTaskForm(null) }) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_task))
      }
    },
    modifier = modifier,
  ) { padding ->
    Column(modifier = Modifier.padding(padding)) {
      // Filter chips row
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp),
      ) {
        items(TaskFilter.entries) { filter ->
          FilterChip(
            selected = uiState.activeFilter == filter,
            onClick = { viewModel.setFilter(filter) },
            label = { Text(filter.localizedName()) },
          )
        }
      }

      if (uiState.tasks.isEmpty() && !uiState.isLoading) {
        EmptyTasksContent(filter = uiState.activeFilter)
      } else {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(uiState.tasks, key = { it.id }) { task ->
            TaskCard(
              task = task,
              onClick = { viewModel.onTaskClicked(task) },
              onComplete = { viewModel.completeTask(task.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyTasksContent(filter: TaskFilter, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Default.Checklist,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.tasks_empty_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.tasks_empty_for_filter, filter.localizedName()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun TaskCard(
  task: Task,
  onClick: () -> Unit,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isOverdue = task.isPastDue
  val containerColor =
    if (isOverdue) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
  val dateText = formatTaskDate(task)
  val cardDescription = "${task.title}, $dateText"

  Card(
    colors = CardDefaults.cardColors(containerColor = containerColor),
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(16.dp).fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier =
          Modifier.weight(1f).semantics(mergeDescendants = true) {
            contentDescription = cardDescription
          },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = getTaskKindIcon(task.kind),
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint =
            if (isOverdue) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = task.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = formatTaskDate(task),
            style = MaterialTheme.typography.bodySmall,
            color =
              if (isOverdue) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      IconButton(onClick = onComplete) {
        Icon(
          Icons.Default.Check,
          contentDescription = stringResource(R.string.action_complete_task),
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

fun getTaskKindIcon(kind: TaskKind): ImageVector {
  return when (kind) {
    TaskKind.VACCINATION -> Icons.Default.Vaccines
    TaskKind.DEWORMING -> Icons.Default.Healing
    TaskKind.WEIGHT -> Icons.Default.MonitorWeight
    TaskKind.MEDICATION -> Icons.Default.Medication
    TaskKind.CUSTOM -> Icons.Default.Checklist
  }
}

@Composable
private fun formatTaskDate(task: Task): String {
  val displayFormatter = rememberAppDisplayFormatter()
  val today = LocalDate.now()
  val taskDate = task.scheduledFor.toLocalDate()
  return when {
    taskDate.isBefore(today) ->
      stringResource(R.string.task_date_overdue, displayFormatter.shortDate(taskDate))
    taskDate == today -> stringResource(R.string.task_date_today)
    taskDate == today.plusDays(1) -> stringResource(R.string.task_date_tomorrow)
    else -> displayFormatter.shortDate(taskDate)
  }
}
