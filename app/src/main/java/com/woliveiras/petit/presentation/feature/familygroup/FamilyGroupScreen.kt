package com.woliveiras.petit.presentation.feature.familygroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.presentation.components.PetitTopAppBar

@Composable
fun FamilyGroupScreen(
  onNavigateBack: () -> Unit,
  onNavigateToPairing: () -> Unit,
  onNavigateToSend: () -> Unit,
  onNavigateToReceive: () -> Unit,
  onNavigateToSyncHistory: () -> Unit,
  viewModel: FamilyGroupViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showLeaveDialog by remember { mutableStateOf(false) }
  var memberToRemove by remember { mutableStateOf<String?>(null) }

  if (memberToRemove != null) {
    AlertDialog(
      onDismissRequest = { memberToRemove = null },
      title = { Text(stringResource(R.string.family_group_remove_device)) },
      text = { Text(stringResource(R.string.family_group_remove_confirmation)) },
      confirmButton = {
        TextButton(
          onClick = {
            memberToRemove?.let { viewModel.removeMember(it) }
            memberToRemove = null
          }
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { memberToRemove = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  if (showLeaveDialog) {
    AlertDialog(
      onDismissRequest = { showLeaveDialog = false },
      title = { Text(stringResource(R.string.family_group_leave)) },
      text = { Text(stringResource(R.string.family_group_leave_confirmation)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.leaveGroup()
            showLeaveDialog = false
            onNavigateBack()
          }
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showLeaveDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.family_group_title)) },
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
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Members section
      Text(
        text = stringResource(R.string.family_group_members_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column {
          uiState.familyGroupInfo?.members?.forEachIndexed { index, member ->
            ListItem(
              colors =
                ListItemDefaults.colors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
              leadingContent = {
                Icon(
                  imageVector = Icons.Default.DevicesOther,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              },
              headlineContent = { Text(member.deviceName) },
              supportingContent = {
                if (member.isLocalDevice) {
                  Text(stringResource(R.string.family_group_this_device))
                }
              },
              trailingContent = {
                if (!member.isLocalDevice) {
                  IconButton(onClick = { memberToRemove = member.id }) {
                    Icon(
                      imageVector = Icons.Default.DeleteOutline,
                      contentDescription = stringResource(R.string.family_group_remove_device),
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              },
            )
            if (index < (uiState.familyGroupInfo?.members?.size ?: 0) - 1) {
              HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
              )
            }
          }
        }
      }

      // Actions section
      Text(
        text = stringResource(R.string.family_group_actions_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column {
          // Send data
          ListItem(
            modifier = Modifier.clickable(onClick = onNavigateToSend),
            colors =
              ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
              ),
            leadingContent = {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            },
            headlineContent = { Text(stringResource(R.string.family_group_send_data)) },
            trailingContent = {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )

          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )

          // Receive data
          ListItem(
            modifier = Modifier.clickable(onClick = onNavigateToReceive),
            colors =
              ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
              ),
            leadingContent = {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            },
            headlineContent = { Text(stringResource(R.string.family_group_receive_data)) },
            trailingContent = {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )

          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )

          // Pair new device
          ListItem(
            modifier = Modifier.clickable(onClick = onNavigateToPairing),
            colors =
              ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
              ),
            leadingContent = {
              Icon(
                imageVector = Icons.Default.DevicesOther,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            },
            headlineContent = { Text(stringResource(R.string.family_group_pair_device)) },
            trailingContent = {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )

          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )

          ListItem(
            modifier = Modifier.clickable(onClick = onNavigateToSyncHistory),
            colors =
              ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
              ),
            leadingContent = {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            },
            headlineContent = { Text(stringResource(R.string.family_group_sync_history)) },
            trailingContent = {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
          )
        }
      }

      // Leave group
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
      ) {
        ListItem(
          modifier = Modifier.clickable { showLeaveDialog = true },
          colors =
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          leadingContent = {
            Icon(
              imageVector = Icons.Default.ExitToApp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
            )
          },
          headlineContent = {
            Text(
              text = stringResource(R.string.family_group_leave),
              color = MaterialTheme.colorScheme.error,
            )
          },
        )
      }
    }
  }
}
