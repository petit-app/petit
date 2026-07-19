package com.woliveiras.petit

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LauncherIconTest {
  @Test
  fun launcherIconCanBeRasterizedByExternalAuthorizationUi() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val launcherIcon = ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, null)

    assertNotNull("Launcher icon must exist", launcherIcon)
    assertTrue(
      "Launcher icon width must be positive",
      requireNotNull(launcherIcon).intrinsicWidth > 0,
    )
    assertTrue("Launcher icon height must be positive", launcherIcon.intrinsicHeight > 0)
  }
}
