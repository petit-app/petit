package com.woliveiras.petit.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.woliveiras.petit.R
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetitBottomNavBarInsetsTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setUp() {
    composeRule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
    composeRule.setContent {
      PetitTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
          PetitBottomNavBar(
            currentRoute = Screen.Home.route,
            onHomeClick = {},
            onPetsClick = {},
            onAddClick = {},
            onTasksClick = {},
            onProfileClick = {},
          )
        }
      }
    }
  }

  @Test
  fun actionsStayAboveTheBottomSystemInset() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val actions =
      listOf(
        context.getString(R.string.nav_home) to
          composeRule
            .onNodeWithText(context.getString(R.string.nav_home))
            .fetchSemanticsNode()
            .boundsInRoot,
        context.getString(R.string.nav_pets) to
          composeRule
            .onNodeWithText(context.getString(R.string.nav_pets))
            .fetchSemanticsNode()
            .boundsInRoot,
        context.getString(R.string.nav_add) to
          composeRule
            .onNodeWithContentDescription(context.getString(R.string.nav_add))
            .fetchSemanticsNode()
            .boundsInRoot,
        context.getString(R.string.nav_tasks) to
          composeRule
            .onNodeWithText(context.getString(R.string.nav_tasks))
            .fetchSemanticsNode()
            .boundsInRoot,
        context.getString(R.string.nav_profile) to
          composeRule
            .onNodeWithText(context.getString(R.string.nav_profile))
            .fetchSemanticsNode()
            .boundsInRoot,
      )

    actions.forEach { (label, bounds) -> assertOutsideSystemBars(label, bounds) }
  }

  private fun assertOutsideSystemBars(label: String, bounds: Rect) {
    val rootBounds = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    val systemBars =
      checkNotNull(ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView))
        .getInsets(WindowInsetsCompat.Type.systemBars())

    assertWithMessage("the test device must expose a bottom system inset")
      .that(systemBars.bottom)
      .isGreaterThan(0)
    assertWithMessage("$label must be above the bottom system inset")
      .that(bounds.bottom)
      .isAtMost(rootBounds.bottom - systemBars.bottom)
  }
}
