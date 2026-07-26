package com.woliveiras.petit.presentation.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.presentation.components.EmptyState
import com.woliveiras.petit.presentation.components.HealthStatusBadge
import com.woliveiras.petit.presentation.components.PetCard
import com.woliveiras.petit.presentation.components.PetCardData
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.components.TimelineSection
import com.woliveiras.petit.presentation.feature.tasks.getTaskKindIcon
import com.woliveiras.petit.presentation.util.localizedName
import com.woliveiras.petit.presentation.util.rememberAppDisplayFormatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  onNavigateToPetList: () -> Unit,
  onNavigateToPetDetail: (String) -> Unit,
  onNavigateToAddPet: () -> Unit,
  onNavigateToVaccinationForm: (petId: String, entryId: String) -> Unit,
  onNavigateToDewormingForm: (petId: String, entryId: String) -> Unit,
  onNavigateToWeightForm: (petId: String, entryId: String?) -> Unit,
  onNavigateToTaskForm: (taskId: String?) -> Unit,
  onNavigateToTaskList: () -> Unit,
  onNavigateToActivityTimeline: () -> Unit,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(topBar = { PetitTopAppBar(title = { Text(stringResource(R.string.home_title)) }) }) {
    padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      when {
        uiState.isLoading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        uiState.isEmpty -> {
          EmptyHomeContent(onNavigateToAddPet = onNavigateToAddPet)
        }
        else -> {
          PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
          ) {
            HomeContent(
              uiState = uiState,
              onPetClick = onNavigateToPetDetail,
              onTaskClick = { task ->
                when {
                  task.referenceEntityId != null && task.petId != null -> {
                    when (task.kind) {
                      TaskKind.VACCINATION ->
                        onNavigateToVaccinationForm(task.petId, task.referenceEntityId)
                      TaskKind.DEWORMING ->
                        onNavigateToDewormingForm(task.petId, task.referenceEntityId)
                      TaskKind.WEIGHT -> onNavigateToWeightForm(task.petId, task.referenceEntityId)
                      else -> onNavigateToTaskForm(task.id)
                    }
                  }
                  else -> onNavigateToTaskForm(task.id)
                }
              },
              onCompleteTask = { viewModel.completeTask(it) },
              onSeeAllPets = onNavigateToPetList,
              onSeeAllTasks = onNavigateToTaskList,
              onTimelineClick = onNavigateToActivityTimeline,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyHomeContent(onNavigateToAddPet: () -> Unit) {
  EmptyState(
    icon = Icons.Default.Pets,
    title = stringResource(R.string.home_welcome_title),
    description = stringResource(R.string.home_welcome_message),
    actionLabel = stringResource(R.string.home_register_pet),
    onAction = onNavigateToAddPet,
  )
}

@Composable
internal fun HomeContent(
  uiState: HomeUiState,
  onPetClick: (String) -> Unit,
  onTaskClick: (Task) -> Unit,
  onCompleteTask: (String) -> Unit,
  onSeeAllPets: () -> Unit,
  onSeeAllTasks: () -> Unit,
  onTimelineClick: () -> Unit,
) {
  val displayFormatter = rememberAppDisplayFormatter()
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    if (uiState.isAllGood) {
      item { AllGoodBanner() }
    }

    if (uiState.alerts.isNotEmpty()) {
      item {
        Text(
          text = stringResource(R.string.home_section_alerts),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.error,
        )
      }
      items(uiState.alerts, key = { it.petWithSummary.pet.id }) { alert ->
        HomeAlertCard(alert = alert, onClick = { onPetClick(alert.petWithSummary.pet.id) })
      }
    }

    // Cats Section (always first)
    item {
      Text(
        text = stringResource(R.string.home_section_pets),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }

    // Use horizontal list when more than 3 cats
    if (uiState.pets.size > 3) {
      item {
        HorizontalPetList(
          pets = uiState.pets,
          onPetClick = onPetClick,
          showSeeAll = uiState.pets.size > 3,
          onSeeAllClick = onSeeAllPets,
        )
      }
    } else {
      items(uiState.pets) { petWithSummary ->
        PetCard(
          data =
            PetCardData(
              pet = petWithSummary.pet,
              weight = petWithSummary.latestWeight?.let { displayFormatter.weight(it.weightGrams) },
              nextVaccineType = petWithSummary.nextVaccineType,
              nextVaccinationDate = petWithSummary.nextVaccinationDate,
              nextDewormingType = petWithSummary.nextDewormingType,
              nextDewormingDate = petWithSummary.nextDewormingDate,
              overallStatus =
                petWithSummary.overallStatus.takeIf { petWithSummary.isHealthSummaryAvailable },
            ),
          onClick = { onPetClick(petWithSummary.pet.id) },
          compact = true,
        )
      }
    }

    // Next Tasks section (flat list, max 5 tasks)
    if (uiState.nextTasks.isNotEmpty()) {
      item {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(R.string.home_section_next_tasks),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = onSeeAllTasks) {
            Text(
              text = stringResource(R.string.action_see_all).uppercase(),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      items(uiState.nextTasks, key = { it.id }) { task ->
        HomeTaskCard(
          task = task,
          onClick = { onTaskClick(task) },
          onComplete = { onCompleteTask(task.id) },
        )
      }
    }

    // Recent Activity Timeline Section
    if (uiState.recentActivity.isNotEmpty()) {
      item {
        TimelineSection(
          title = stringResource(R.string.timeline_section_recent),
          events = uiState.recentActivity,
          onEventClick = { onTimelineClick() },
        )
      }
      item {
        TextButton(onClick = onTimelineClick, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.activity_timeline_see_all))
        }
      }
    } else if (uiState.pets.isNotEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
          Text(
            text = stringResource(R.string.empty_home_recent_activity),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun AllGoodBanner() {
  val title = stringResource(R.string.home_all_good)
  val description = stringResource(R.string.home_all_good_description)
  Card(
    modifier =
      Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = "$title. $description"
      },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
      )
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
    }
  }
}

@Composable
private fun HomeAlertCard(alert: HomeAlert, onClick: () -> Unit) {
  val displayFormatter = rememberAppDisplayFormatter()
  val status = alert.petWithSummary.overallStatus.localizedName()
  val date = alert.relevantDate?.let(displayFormatter::shortDate)
  val dateText = date?.let { stringResource(R.string.home_alert_relevant_date, it) }
  val description =
    listOfNotNull(alert.petWithSummary.pet.name, status, dateText).joinToString(", ")

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .semantics(mergeDescendants = true) { contentDescription = description }
        .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = alert.petWithSummary.pet.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        if (dateText != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = dateText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
      HealthStatusBadge(status = alert.petWithSummary.overallStatus)
    }
  }
}

@Composable
private fun HomeTaskCard(task: Task, onClick: () -> Unit, onComplete: () -> Unit) {
  val displayFormatter = rememberAppDisplayFormatter()
  val isOverdue = task.isPastDue
  val taskDate = task.scheduledFor.toLocalDate()
  val today = LocalDate.now()
  val dateText =
    when {
      taskDate.isBefore(today) ->
        stringResource(R.string.task_date_overdue, displayFormatter.shortDate(taskDate))
      taskDate == today -> stringResource(R.string.task_date_today)
      taskDate == today.plusDays(1) -> stringResource(R.string.task_date_tomorrow)
      else -> displayFormatter.shortDate(taskDate)
    }
  val cardDescription = "${task.title}, $dateText"

  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)) {
      // Informational content merged for TalkBack
      Column(
        modifier =
          Modifier.semantics(mergeDescendants = true) { contentDescription = cardDescription }
      ) {
        // Icon circle at top
        Box(
          modifier =
            Modifier.size(40.dp)
              .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = getTaskKindIcon(task.kind),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title (dark purple to match design)
        Text(
          text = task.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        // Date info

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = dateText,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (isOverdue) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Complete action button (TextButton for 48dp touch target + button semantics)
      TextButton(onClick = onComplete, contentPadding = PaddingValues(horizontal = 0.dp)) {
        Icon(
          Icons.Default.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = stringResource(R.string.action_complete_task).uppercase(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun HorizontalPetList(
  pets: List<PetWithSummary>,
  onPetClick: (String) -> Unit,
  showSeeAll: Boolean = false,
  onSeeAllClick: () -> Unit = {},
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(vertical = 8.dp),
  ) {
    items(pets, key = { it.pet.id }) { petWithSummary ->
      HorizontalPetItem(
        petWithSummary = petWithSummary,
        onClick = { onPetClick(petWithSummary.pet.id) },
      )
    }

    // "See all" button when more than 4 cats
    if (showSeeAll) {
      item { SeeAllButton(onClick = onSeeAllClick) }
    }
  }
}

@Composable
private fun SeeAllButton(onClick: () -> Unit) {
  Column(
    modifier = Modifier.width(72.dp).clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier.size(64.dp).clip(CircleShape),
      color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.NavigateNext,
          contentDescription = stringResource(R.string.action_see_all),
          modifier = Modifier.size(32.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = stringResource(R.string.action_see_all),
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun HorizontalPetItem(petWithSummary: PetWithSummary, onClick: () -> Unit) {
  val context = LocalContext.current
  val healthStatus =
    if (petWithSummary.isHealthSummaryAvailable) {
      petWithSummary.overallStatus.localizedName()
    } else {
      stringResource(R.string.health_status_unavailable)
    }

  Column(
    modifier =
      Modifier.width(96.dp)
        .semantics(mergeDescendants = true) {
          contentDescription = "${petWithSummary.pet.name}, $healthStatus"
        }
        .clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Avatar circle with border
    Surface(
      modifier =
        Modifier.size(64.dp)
          .border(width = 3.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape)
          .padding(3.dp)
          .clip(CircleShape),
      color = MaterialTheme.colorScheme.primaryContainer,
    ) {
      Box(contentAlignment = Alignment.Center) {
        if (petWithSummary.pet.photoUri != null) {
          AsyncImage(
            model =
              ImageRequest.Builder(context)
                .data(petWithSummary.pet.photoUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop,
          )
        } else {
          Text(
            text = petWithSummary.pet.name.first().uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Pet name
    Text(
      text = petWithSummary.pet.name,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(4.dp))
    if (petWithSummary.isHealthSummaryAvailable) {
      HealthStatusBadge(status = petWithSummary.overallStatus)
    }
  }
}
