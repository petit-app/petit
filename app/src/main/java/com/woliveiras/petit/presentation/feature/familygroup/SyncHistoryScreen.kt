package com.woliveiras.petit.presentation.feature.familygroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.SyncLog
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import java.text.DateFormat
import java.util.Date

@Composable
fun SyncHistoryScreen(
  onNavigateBack: () -> Unit,
  viewModel: SyncHistoryViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.family_group_sync_history)) },
        onNavigateBack = onNavigateBack,
      )
    }
  ) { padding ->
    when {
      state.isLoading ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
          val loadingDescription = stringResource(R.string.family_group_sync_history_loading)
          CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = loadingDescription }
          )
        }
      state.hasError ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
          Text(
            stringResource(R.string.family_group_sync_history_error),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            color = MaterialTheme.colorScheme.error,
          )
        }
      state.logs.isEmpty() ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
          Text(stringResource(R.string.family_group_sync_history_empty))
        }
      else ->
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(state.logs, key = SyncLog::id) { log -> SyncHistoryItem(log) }
        }
    }
  }
}

@Composable
private fun SyncHistoryItem(log: SyncLog) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = log.peerName.ifBlank { stringResource(R.string.family_group_unknown_peer) },
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        stringResource(
          R.string.family_group_sync_history_time,
          DateFormat.getDateTimeInstance().format(Date(log.syncTimestamp)),
        )
      )
      val localizedType =
        when (log.syncType) {
          "MERGE" -> stringResource(R.string.family_group_sync_type_merge)
          "REPLACE" -> stringResource(R.string.family_group_sync_type_replace)
          "SEND" -> stringResource(R.string.family_group_sync_type_send)
          "LAN" -> stringResource(R.string.family_group_sync_type_lan)
          else -> log.syncType
        }
      Text(stringResource(R.string.family_group_sync_history_type, localizedType))
      Text(
        stringResource(
          R.string.family_group_sync_history_counts,
          log.entitiesSent,
          log.entitiesReceived,
          log.conflictsResolved,
        )
      )
    }
  }
}
