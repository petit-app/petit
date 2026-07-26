package com.woliveiras.petit.presentation.navigation

import androidx.compose.ui.unit.LayoutDirection

internal enum class NavigationMotion {
  None,
  MainAreaCrossfade,
  HierarchicalForward,
  HierarchicalBackward,
}

internal enum class HorizontalEdge {
  Leading,
  Trailing,
}

internal data class NavigationTransitionSpec(
  val durationMillis: Int,
  val enterFrom: HorizontalEdge?,
  val exitTo: HorizontalEdge?,
  val usesOpacity: Boolean,
)

private val mainAreaRoutes =
  setOf(
    Screen.Home.route,
    Screen.PetList.route,
    Screen.QuickAdd.route,
    Screen.Tasks.route,
    Screen.Settings.route,
  )

internal fun selectNavigationMotion(
  initialRoute: String?,
  targetRoute: String?,
  isPop: Boolean,
): NavigationMotion {
  if (initialRoute == targetRoute && targetRoute in mainAreaRoutes) {
    return NavigationMotion.None
  }

  val initialIsMainArea = initialRoute in mainAreaRoutes
  val targetIsMainArea = targetRoute in mainAreaRoutes

  return when {
    isPop && initialIsMainArea && targetIsMainArea -> NavigationMotion.MainAreaCrossfade
    isPop -> NavigationMotion.HierarchicalBackward
    targetIsMainArea -> NavigationMotion.MainAreaCrossfade
    else -> NavigationMotion.HierarchicalForward
  }
}

internal fun horizontalOffset(
  fullWidth: Int,
  edge: HorizontalEdge,
  layoutDirection: LayoutDirection,
): Int {
  val trailingOffset =
    when (layoutDirection) {
      LayoutDirection.Ltr -> fullWidth
      LayoutDirection.Rtl -> -fullWidth
    }
  return if (edge == HorizontalEdge.Trailing) trailingOffset else -trailingOffset
}

internal fun navigationTransitionSpec(motion: NavigationMotion): NavigationTransitionSpec =
  when (motion) {
    NavigationMotion.None ->
      NavigationTransitionSpec(
        durationMillis = 0,
        enterFrom = null,
        exitTo = null,
        usesOpacity = false,
      )
    NavigationMotion.MainAreaCrossfade ->
      NavigationTransitionSpec(
        durationMillis = 150,
        enterFrom = null,
        exitTo = null,
        usesOpacity = true,
      )
    NavigationMotion.HierarchicalForward ->
      NavigationTransitionSpec(
        durationMillis = 300,
        enterFrom = HorizontalEdge.Trailing,
        exitTo = HorizontalEdge.Leading,
        usesOpacity = false,
      )
    NavigationMotion.HierarchicalBackward ->
      NavigationTransitionSpec(
        durationMillis = 300,
        enterFrom = HorizontalEdge.Leading,
        exitTo = HorizontalEdge.Trailing,
        usesOpacity = false,
      )
  }
