package com.woliveiras.petit.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationMotionTransitionTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun mainAreasCrossfadeWhileHierarchySlidesAndPops() {
    lateinit var navController: NavHostController
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      navController = rememberNavController()
      MotionTestHost(navController = navController, layoutDirection = LayoutDirection.Ltr)
    }
    composeRule.mainClock.advanceTimeByFrame()

    val containerWidth = composeRule.onNodeWithTag(HOME_TAG).fetchSemanticsNode().boundsInRoot.width

    composeRule.runOnIdle { navController.navigate(Screen.PetList.route) }
    composeRule.mainClock.advanceTimeBy(75)

    val petsDuringCrossfade =
      composeRule.onNodeWithTag(PETS_TAG).fetchSemanticsNode().boundsInRoot.left
    assertThat(petsDuringCrossfade).isEqualTo(0f)

    composeRule.mainClock.advanceTimeBy(100)
    composeRule.runOnIdle { navController.navigate("pets/pet-1") }
    composeRule.mainClock.advanceTimeBy(150)

    val detailDuringForward =
      composeRule.onNodeWithTag(DETAIL_TAG).fetchSemanticsNode().boundsInRoot.left
    assertThat(detailDuringForward).isGreaterThan(0f)
    assertThat(detailDuringForward).isLessThan(containerWidth)

    composeRule.mainClock.advanceTimeBy(200)
    composeRule.runOnIdle { navController.popBackStack() }
    composeRule.mainClock.advanceTimeBy(150)

    val detailDuringPop =
      composeRule.onNodeWithTag(DETAIL_TAG).fetchSemanticsNode().boundsInRoot.left
    assertThat(detailDuringPop).isGreaterThan(0f)
    assertThat(detailDuringPop).isLessThan(containerWidth)
  }

  @Test
  fun hierarchicalSlideMirrorsInRtl() {
    lateinit var navController: NavHostController
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      navController = rememberNavController()
      MotionTestHost(navController = navController, layoutDirection = LayoutDirection.Rtl)
    }
    composeRule.mainClock.advanceTimeByFrame()

    val containerWidth = composeRule.onNodeWithTag(HOME_TAG).fetchSemanticsNode().boundsInRoot.width

    composeRule.runOnIdle { navController.navigate("pets/pet-1") }
    composeRule.mainClock.advanceTimeBy(150)

    val detailDuringForward =
      composeRule.onNodeWithTag(DETAIL_TAG).fetchSemanticsNode().boundsInRoot.left
    assertThat(detailDuringForward).isLessThan(0f)
    assertThat(detailDuringForward).isGreaterThan(-containerWidth)
  }

  @Test
  fun nestedHierarchicalDestinationUsesForwardSlide() {
    lateinit var navController: NavHostController
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      navController = rememberNavController()
      MotionTestHost(navController = navController, layoutDirection = LayoutDirection.Ltr)
    }
    composeRule.mainClock.advanceTimeByFrame()

    composeRule.runOnIdle { navController.navigate("pets/pet-1") }
    composeRule.mainClock.advanceTimeBy(350)
    val containerWidth =
      composeRule.onNodeWithTag(DETAIL_TAG).fetchSemanticsNode().boundsInRoot.width

    composeRule.runOnIdle { navController.navigate(Screen.PetForm.createRoute("pet-1")) }
    composeRule.mainClock.advanceTimeBy(150)

    val formDuringForward =
      composeRule.onNodeWithTag(FORM_TAG).fetchSemanticsNode().boundsInRoot.left
    assertThat(formDuringForward).isGreaterThan(0f)
    assertThat(formDuringForward).isLessThan(containerWidth)
  }

  @Test
  fun onboardingCompletionCrossfadesAndRemovesOnboarding() {
    lateinit var navController: NavHostController
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      navController = rememberNavController()
      MotionTestHost(
        navController = navController,
        layoutDirection = LayoutDirection.Ltr,
        startDestination = Screen.Onboarding.route,
      )
    }
    composeRule.mainClock.advanceTimeByFrame()

    composeRule.runOnIdle {
      navController.navigate(Screen.Home.route) {
        popUpTo(Screen.Onboarding.route) { inclusive = true }
      }
    }
    composeRule.mainClock.advanceTimeBy(75)

    val homeDuringCrossfade =
      composeRule.onNodeWithTag(HOME_TAG).fetchSemanticsNode().boundsInRoot.left
    assertThat(homeDuringCrossfade).isEqualTo(0f)

    composeRule.mainClock.advanceTimeBy(100)
    composeRule.runOnIdle {
      assertThat(navController.currentDestination?.route).isEqualTo(Screen.Home.route)
      assertThat(navController.previousBackStackEntry).isNull()
    }
  }

  @Test
  fun reselectingMainAreaDoesNotAddAnotherBackStackEntry() {
    lateinit var navController: NavHostController
    composeRule.setContent {
      navController = rememberNavController()
      MotionTestHost(navController = navController, layoutDirection = LayoutDirection.Ltr)
    }

    composeRule.runOnIdle {
      navController.navigateToMainArea(Screen.PetList.route)
      navController.navigateToMainArea(Screen.PetList.route)
      navController.popBackStack()
    }

    composeRule.runOnIdle {
      assertThat(navController.currentDestination?.route).isEqualTo(Screen.Home.route)
    }
  }

  @Test
  fun hierarchicalRoundTripPreservesDestinationState() {
    lateinit var navController: NavHostController
    composeRule.setContent {
      navController = rememberNavController()
      MotionTestHost(navController = navController, layoutDirection = LayoutDirection.Ltr)
    }

    composeRule.onNodeWithTag(HOME_TAG).performClick()
    composeRule.runOnIdle {
      navController.navigate("pets/pet-1")
      navController.previousBackStackEntry?.savedStateHandle?.set(RESULT_KEY, "preserved")
      navController.popBackStack()
    }

    composeRule.onNodeWithTag(HOME_TAG).assertTextEquals("1")
    composeRule.runOnIdle {
      assertThat(navController.currentBackStackEntry?.savedStateHandle?.get<String>(RESULT_KEY))
        .isEqualTo("preserved")
    }
  }
}

@Composable
private fun MotionTestHost(
  navController: NavHostController,
  layoutDirection: LayoutDirection,
  startDestination: String = Screen.Home.route,
) {
  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = Modifier.size(width = 300.dp, height = 200.dp),
      enterTransition = {
        navigationEnterTransition(layoutDirection = layoutDirection, isPop = false)
      },
      exitTransition = {
        navigationExitTransition(layoutDirection = layoutDirection, isPop = false)
      },
      popEnterTransition = {
        navigationEnterTransition(layoutDirection = layoutDirection, isPop = true)
      },
      popExitTransition = {
        navigationExitTransition(layoutDirection = layoutDirection, isPop = true)
      },
    ) {
      composable(Screen.Home.route) {
        var retainedValue by rememberSaveable { mutableIntStateOf(0) }
        Box(Modifier.fillMaxSize().testTag(HOME_TAG).clickable { retainedValue += 1 }) {
          Text(retainedValue.toString())
        }
      }
      composable(Screen.PetList.route) { Box(Modifier.fillMaxSize().testTag(PETS_TAG)) }
      composable(Screen.PetDetail.route) { Box(Modifier.fillMaxSize().testTag(DETAIL_TAG)) }
      composable(Screen.PetForm.route) { Box(Modifier.fillMaxSize().testTag(FORM_TAG)) }
      composable(Screen.Onboarding.route) { Box(Modifier.fillMaxSize().testTag(ONBOARDING_TAG)) }
    }
  }
}

private const val HOME_TAG = "motion-home"
private const val PETS_TAG = "motion-pets"
private const val DETAIL_TAG = "motion-detail"
private const val FORM_TAG = "motion-form"
private const val ONBOARDING_TAG = "motion-onboarding"
private const val RESULT_KEY = "motion-result"
