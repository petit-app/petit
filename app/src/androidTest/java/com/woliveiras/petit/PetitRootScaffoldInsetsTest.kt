package com.woliveiras.petit

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetitRootScaffoldInsetsTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setUp() {
    composeRule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
    composeRule.setContent {
      PetitTheme {
        PetitRootScaffold(showBottomBar = false, bottomBar = {}) { modifier ->
          Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(TOP_ACTION)
            Text(BOTTOM_ACTION)
          }
        }
      }
    }
  }

  @Test
  fun contentActionsStayOutsideSystemBarsWhenBottomBarIsHidden() {
    assertOutsideSystemBars(
      TOP_ACTION,
      composeRule.onNodeWithText(TOP_ACTION).fetchSemanticsNode().boundsInRoot,
    )
    assertOutsideSystemBars(
      BOTTOM_ACTION,
      composeRule.onNodeWithText(BOTTOM_ACTION).fetchSemanticsNode().boundsInRoot,
    )
  }

  private fun assertOutsideSystemBars(label: String, bounds: Rect) {
    val rootBounds = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    val systemBars =
      checkNotNull(ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView))
        .getInsets(WindowInsetsCompat.Type.systemBars())

    assertWithMessage("the test device must expose top and bottom system insets")
      .that(systemBars.top > 0 && systemBars.bottom > 0)
      .isTrue()
    assertWithMessage("$label must be below the top system inset")
      .that(bounds.top)
      .isAtLeast(rootBounds.top + systemBars.top)
    assertWithMessage("$label must be above the bottom system inset")
      .that(bounds.bottom)
      .isAtMost(rootBounds.bottom - systemBars.bottom)
    assertWithMessage("$label must be after the left system inset")
      .that(bounds.left)
      .isAtLeast(rootBounds.left + systemBars.left)
    assertWithMessage("$label must be before the right system inset")
      .that(bounds.right)
      .isAtMost(rootBounds.right - systemBars.right)
  }

  private companion object {
    const val TOP_ACTION = "Top action"
    const val BOTTOM_ACTION = "Bottom action"
  }
}
