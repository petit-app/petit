package com.woliveiras.petit.presentation.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

@Composable
fun OnboardingScreen(
  onFinished: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OnboardingViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        OnboardingEvent.NavigateToHome -> onFinished()
      }
    }
  }

  OnboardingContent(
    uiState = uiState,
    onComplete = viewModel::completeOnboarding,
    modifier = modifier,
  )
}

@Composable
internal fun OnboardingContent(
  uiState: OnboardingUiState,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
  val scope = rememberCoroutineScope()

  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Skip button row
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      if (pagerState.currentPage < PAGE_COUNT - 1) {
        TextButton(onClick = onComplete, enabled = !uiState.isCompleting) {
          Text(stringResource(R.string.onboarding_skip))
        }
      }
    }

    // Pager content
    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
      when (page) {
        0 -> WelcomePage()
        1 -> FeaturesPage()
        2 -> CtaPage()
      }
    }

    // Page indicator dots
    PageIndicator(
      pageCount = PAGE_COUNT,
      currentPage = pagerState.currentPage,
      modifier = Modifier.padding(vertical = 16.dp),
    )

    if (uiState.isCompleting) {
      CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
      Spacer(modifier = Modifier.height(8.dp))
    }

    if (uiState.hasCompletionError) {
      Text(
        text = stringResource(R.string.onboarding_completion_error),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
    }

    // Bottom action button
    if (pagerState.currentPage < PAGE_COUNT - 1) {
      Button(
        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
      ) {
        Text(stringResource(R.string.onboarding_next))
      }
    } else {
      Button(
        onClick = onComplete,
        enabled = !uiState.isCompleting,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
      ) {
        Text(stringResource(R.string.onboarding_get_started))
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun WelcomePage() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Default.Pets,
      contentDescription = null,
      modifier = Modifier.size(96.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(32.dp))
    Text(
      text = stringResource(R.string.onboarding_welcome_title),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.onboarding_welcome_description),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun FeaturesPage() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = stringResource(R.string.onboarding_features_title),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    FeatureItem(
      icon = Icons.Default.MonitorWeight,
      label = stringResource(R.string.onboarding_feature_weight),
    )
    Spacer(modifier = Modifier.height(16.dp))
    FeatureItem(
      icon = Icons.Default.Vaccines,
      label = stringResource(R.string.onboarding_feature_vaccination),
    )
    Spacer(modifier = Modifier.height(16.dp))
    FeatureItem(
      icon = Icons.Default.Healing,
      label = stringResource(R.string.onboarding_feature_deworming),
    )
    Spacer(modifier = Modifier.height(16.dp))
    FeatureItem(
      icon = Icons.Default.Notifications,
      label = stringResource(R.string.onboarding_feature_reminders),
    )
  }
}

@Composable
private fun FeatureItem(icon: ImageVector, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    modifier =
      Modifier.fillMaxWidth().padding(horizontal = 32.dp).semantics(mergeDescendants = true) {},
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(40.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(text = label, style = MaterialTheme.typography.titleMedium)
  }
}

@Composable
private fun CtaPage() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Default.Pets,
      contentDescription = null,
      modifier = Modifier.size(96.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(32.dp))
    Text(
      text = stringResource(R.string.onboarding_cta_title),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.onboarding_cta_description),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
  val pageDescription =
    stringResource(R.string.onboarding_page_indicator, currentPage + 1, pageCount)
  Row(
    modifier = modifier.semantics { stateDescription = pageDescription },
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(pageCount) { index ->
      val color =
        animateColorAsState(
          targetValue =
            if (index == currentPage) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.outlineVariant
            },
          label = "pageIndicatorColor",
        )
      Box(
        modifier =
          Modifier.size(if (index == currentPage) 10.dp else 8.dp)
            .clip(CircleShape)
            .background(color.value)
      )
    }
  }
}
