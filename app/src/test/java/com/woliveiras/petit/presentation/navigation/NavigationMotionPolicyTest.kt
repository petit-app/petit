package com.woliveiras.petit.presentation.navigation

import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavigationMotionPolicyTest {

  @Test
  fun directNavigationToMainAreaUsesCrossfade() {
    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.Home.route,
          targetRoute = Screen.PetList.route,
          isPop = false,
        )
      )
      .isEqualTo(NavigationMotion.MainAreaCrossfade)

    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.PetDetail.route,
          targetRoute = Screen.Settings.route,
          isPop = false,
        )
      )
      .isEqualTo(NavigationMotion.MainAreaCrossfade)
  }

  @Test
  fun onboardingCompletionUsesMainAreaCrossfade() {
    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.Onboarding.route,
          targetRoute = Screen.Home.route,
          isPop = false,
        )
      )
      .isEqualTo(NavigationMotion.MainAreaCrossfade)
  }

  @Test
  fun hierarchicalDestinationUsesForwardSlide() {
    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.PetList.route,
          targetRoute = Screen.PetDetail.route,
          isPop = false,
        )
      )
      .isEqualTo(NavigationMotion.HierarchicalForward)

    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.PetDetail.route,
          targetRoute = "future/unknown/{id}",
          isPop = false,
        )
      )
      .isEqualTo(NavigationMotion.HierarchicalForward)
  }

  @Test
  fun popFromHierarchyUsesBackwardSlideEvenWhenTargetIsMainArea() {
    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.PetDetail.route,
          targetRoute = Screen.PetList.route,
          isPop = true,
        )
      )
      .isEqualTo(NavigationMotion.HierarchicalBackward)
  }

  @Test
  fun popBetweenMainAreasUsesCrossfade() {
    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.Tasks.route,
          targetRoute = Screen.Home.route,
          isPop = true,
        )
      )
      .isEqualTo(NavigationMotion.MainAreaCrossfade)
  }

  @Test
  fun reselectingCurrentMainAreaUsesNoMotion() {
    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.Settings.route,
          targetRoute = Screen.Settings.route,
          isPop = false,
        )
      )
      .isEqualTo(NavigationMotion.None)
  }

  @Test
  fun matchingHierarchicalPatternsAndMissingRoutesStillUseHierarchy() {
    assertThat(
        selectNavigationMotion(
          initialRoute = Screen.PetDetail.route,
          targetRoute = Screen.PetDetail.route,
          isPop = false,
        )
      )
      .isEqualTo(NavigationMotion.HierarchicalForward)
    assertThat(selectNavigationMotion(initialRoute = null, targetRoute = null, isPop = false))
      .isEqualTo(NavigationMotion.HierarchicalForward)
  }

  @Test
  fun horizontalEdgesFollowLayoutDirection() {
    assertThat(
        horizontalOffset(
          fullWidth = 400,
          edge = HorizontalEdge.Trailing,
          layoutDirection = LayoutDirection.Ltr,
        )
      )
      .isEqualTo(400)
    assertThat(
        horizontalOffset(
          fullWidth = 400,
          edge = HorizontalEdge.Leading,
          layoutDirection = LayoutDirection.Ltr,
        )
      )
      .isEqualTo(-400)
    assertThat(
        horizontalOffset(
          fullWidth = 400,
          edge = HorizontalEdge.Trailing,
          layoutDirection = LayoutDirection.Rtl,
        )
      )
      .isEqualTo(-400)
    assertThat(
        horizontalOffset(
          fullWidth = 400,
          edge = HorizontalEdge.Leading,
          layoutDirection = LayoutDirection.Rtl,
        )
      )
      .isEqualTo(400)
  }

  @Test
  fun motionSpecsUseApprovedDurationsAndEdges() {
    assertThat(navigationTransitionSpec(NavigationMotion.MainAreaCrossfade))
      .isEqualTo(
        NavigationTransitionSpec(
          durationMillis = 150,
          enterFrom = null,
          exitTo = null,
          usesOpacity = true,
        )
      )
    assertThat(navigationTransitionSpec(NavigationMotion.HierarchicalForward))
      .isEqualTo(
        NavigationTransitionSpec(
          durationMillis = 300,
          enterFrom = HorizontalEdge.Trailing,
          exitTo = HorizontalEdge.Leading,
          usesOpacity = false,
        )
      )
    assertThat(navigationTransitionSpec(NavigationMotion.HierarchicalBackward))
      .isEqualTo(
        NavigationTransitionSpec(
          durationMillis = 300,
          enterFrom = HorizontalEdge.Leading,
          exitTo = HorizontalEdge.Trailing,
          usesOpacity = false,
        )
      )
    assertThat(navigationTransitionSpec(NavigationMotion.None))
      .isEqualTo(
        NavigationTransitionSpec(
          durationMillis = 0,
          enterFrom = null,
          exitTo = null,
          usesOpacity = false,
        )
      )
  }
}
