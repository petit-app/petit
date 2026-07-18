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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import java.text.DateFormat
import java.util.Date

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
  var memberToRename by remember { mutableStateOf<FamilyGroupMember?>(null) }
  var renamedDeviceName by remember { mutableStateOf("") }

  LaunchedEffect(uiState.hasLeftGroup) { if (uiState.hasLeftGroup) onNavigateBack() }

  if (memberToRename != null) {
    AlertDialog(
      onDismissRequest = { memberToRename = null },
      title = { Text(stringResource(R.string.family_group_rename_device)) },
      text = {
        OutlinedTextField(
          value = renamedDeviceName,
          onValueChange = { renamedDeviceName = it.take(80) },
          label = { Text(stringResource(R.string.family_group_device_name)) },
          singleLine = true,
        )
      },
      confirmButton = {
        TextButton(
          enabled = renamedDeviceName.isNotBlank(),
          onClick = {
            viewModel.renameLocalDevice(renamedDeviceName)
            memberToRename = null
          },
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { memberToRename = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

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
      if (uiState.operationFailed) {
        Text(
          text = stringResource(R.string.family_group_action_error),
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
          color = MaterialTheme.colorScheme.error,
        )
      }
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
          val members = uiState.familyGroupInfo?.members.orEmpty()
          if (members.isEmpty()) {
            Text(
              text = stringResource(R.string.family_group_no_devices),
              modifier = Modifier.padding(16.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          members.forEachIndexed { index, member ->
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
                Column {
                  if (member.isLocalDevice) {
                    Text(stringResource(R.string.family_group_this_device))
                  }
                  Text(
                    if (member.lastSyncAt == null) {
                      stringResource(R.string.family_group_never_synced)
                    } else {
                      stringResource(
                        R.string.family_group_member_last_sync,
                        DateFormat.getDateTimeInstance().format(Date(member.lastSyncAt)),
                      )
                    }
                  )
                }
              },
              trailingContent = {
                if (member.isLocalDevice) {
                  IconButton(
                    onClick = {
                      renamedDeviceName = member.deviceName
                      memberToRename = member
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = stringResource(R.string.family_group_rename_device),
                    )
                  }
                } else {
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
            if (index < members.size - 1) {
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
