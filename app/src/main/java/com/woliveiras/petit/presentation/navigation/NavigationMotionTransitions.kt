package com.woliveiras.petit.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavBackStackEntry

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.navigationEnterTransition(
  layoutDirection: LayoutDirection,
  isPop: Boolean,
): EnterTransition {
  val motion =
    selectNavigationMotion(
      initialRoute = initialState.destination.route,
      targetRoute = targetState.destination.route,
      isPop = isPop,
    )
  val spec = navigationTransitionSpec(motion)

  return when {
    spec.durationMillis == 0 -> EnterTransition.None
    spec.usesOpacity -> fadeIn(animationSpec = tween(durationMillis = spec.durationMillis))
    spec.enterFrom != null ->
      slideInHorizontally(
        animationSpec = tween(durationMillis = spec.durationMillis, easing = FastOutSlowInEasing)
      ) { fullWidth ->
        horizontalOffset(
          fullWidth = fullWidth,
          edge = spec.enterFrom,
          layoutDirection = layoutDirection,
        )
      }
    else -> EnterTransition.None
  }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.navigationExitTransition(
  layoutDirection: LayoutDirection,
  isPop: Boolean,
): ExitTransition {
  val motion =
    selectNavigationMotion(
      initialRoute = initialState.destination.route,
      targetRoute = targetState.destination.route,
      isPop = isPop,
    )
  val spec = navigationTransitionSpec(motion)

  return when {
    spec.durationMillis == 0 -> ExitTransition.None
    spec.usesOpacity -> fadeOut(animationSpec = tween(durationMillis = spec.durationMillis))
    spec.exitTo != null ->
      slideOutHorizontally(
        animationSpec = tween(durationMillis = spec.durationMillis, easing = FastOutSlowInEasing)
      ) { fullWidth ->
        horizontalOffset(
          fullWidth = fullWidth,
          edge = spec.exitTo,
          layoutDirection = layoutDirection,
        )
      }
    else -> ExitTransition.None
  }
}
