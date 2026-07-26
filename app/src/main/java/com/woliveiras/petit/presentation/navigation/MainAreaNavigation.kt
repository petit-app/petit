package com.woliveiras.petit.presentation.navigation

import androidx.navigation.NavHostController

internal fun NavHostController.navigateToMainArea(route: String) {
  if (currentDestination?.route == route) return

  navigate(route) {
    popUpTo(Screen.Home.route) { inclusive = false }
    launchSingleTop = true
  }
}
