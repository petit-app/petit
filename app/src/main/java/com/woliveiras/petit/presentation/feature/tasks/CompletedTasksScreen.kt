package com.woliveiras.petit.presentation.feature.tasks

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import com.woliveiras.petit.presentation.util.subjectLabel

/** Screen showing completed tasks that can be reactivated or deleted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTasksScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: CompletedTasksViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.completed_tasks_title)) },
        onNavigateBack = onNavigateBack,
      )
    },
    modifier = modifier,
  ) { padding ->
    if (uiState.tasks.isEmpty() && !uiState.isLoading) {
      EmptyCompletedContent(modifier = Modifier.padding(padding))
    } else {
      LazyColumn(
        contentPadding =
          PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 8.dp,
          ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(uiState.tasks, key = { it.id }) { task ->
          CompletedTaskCard(
            task = task,
            onReactivate = { viewModel.reactivateTask(task.id) },
            onDelete = { viewModel.deleteTask(task.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyCompletedContent(modifier: Modifier = Modifier) {
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
        text = stringResource(R.string.completed_tasks_empty),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun CompletedTaskCard(
  task: Task,
  onReactivate: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val displayFormatter = rememberAppDisplayFormatter()
  val formattedDate = displayFormatter.shortDate(task.scheduledFor.toLocalDate())
  val subjectText = task.subjectLabel()?.takeIf { it != task.title }
  val cardDescription =
    listOfNotNull(task.title, subjectText, formattedDate).joinToString(separator = ", ")

  Card(
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = modifier.fillMaxWidth(),
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
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
          if (subjectText != null) {
            Text(
              text = subjectText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(onClick = onReactivate) {
        Icon(
          Icons.Default.Replay,
          contentDescription = stringResource(R.string.action_reactivate_task),
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      IconButton(onClick = onDelete) {
        Icon(
          Icons.Default.Delete,
          contentDescription = stringResource(R.string.action_delete),
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}
