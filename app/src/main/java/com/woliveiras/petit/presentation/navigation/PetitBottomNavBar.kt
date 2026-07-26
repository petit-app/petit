package com.woliveiras.petit.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.R

private val NavBarPillShape = RoundedCornerShape(16.dp)

/** Bottom navigation item types. */
enum class BottomNavItem {
  HOME,
  PETS,
  ADD,
  TASKS,
  PROFILE,
}

/** Custom bottom navigation bar with Home, Pets, Add (FAB), Tasks, and Profile. */
@Composable
fun PetitBottomNavBar(
  currentRoute: String?,
  onHomeClick: () -> Unit,
  onPetsClick: () -> Unit,
  onAddClick: () -> Unit,
  onTasksClick: () -> Unit,
  onProfileClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth().navigationBarsPadding().height(84.dp).padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NavBarItem(
        selected = currentRoute == Screen.Home.route,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        label = stringResource(R.string.nav_home),
        onClick = onHomeClick,
        modifier = Modifier.weight(1f),
      )

      NavBarItem(
        selected = currentRoute == Screen.PetList.route,
        selectedIcon = Icons.Filled.Pets,
        unselectedIcon = Icons.Outlined.Pets,
        label = stringResource(R.string.nav_pets),
        onClick = onPetsClick,
        modifier = Modifier.weight(1f),
      )

      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        FloatingActionButton(
          onClick = onAddClick,
          shape = CircleShape,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(52.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.nav_add),
            modifier = Modifier.size(26.dp),
          )
        }
      }

      NavBarItem(
        selected = currentRoute == Screen.Tasks.route,
        selectedIcon = Icons.Filled.Checklist,
        unselectedIcon = Icons.Outlined.Checklist,
        label = stringResource(R.string.nav_tasks),
        onClick = onTasksClick,
        modifier = Modifier.weight(1f),
      )

      NavBarItem(
        selected = currentRoute == Screen.Settings.route,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        label = stringResource(R.string.nav_profile),
        onClick = onProfileClick,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun NavBarItem(
  selected: Boolean,
  selectedIcon: ImageVector,
  unselectedIcon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .clickable(role = Role.Tab, onClick = onClick)
        .semantics { this.selected = selected }
        .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier =
        Modifier.clip(NavBarPillShape)
          .then(
            if (selected) {
              Modifier.background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                NavBarPillShape,
              )
            } else {
              Modifier
            }
          )
          .padding(horizontal = 16.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = if (selected) selectedIcon else unselectedIcon,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )

    Spacer(modifier = Modifier.height(3.dp))

    Box(
      modifier =
        Modifier.size(width = 14.dp, height = 4.dp)
          .clip(RoundedCornerShape(percent = 100))
          .background(
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            RoundedCornerShape(percent = 100),
          )
    )
  }
}
