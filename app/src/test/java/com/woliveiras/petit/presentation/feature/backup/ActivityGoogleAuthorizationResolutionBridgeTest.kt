package com.woliveiras.petit.presentation.feature.backup

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.backup.google.GoogleAuthorizationResolutionResult
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ActivityGoogleAuthorizationResolutionBridgeTest {
  @Test
  fun successfulActivityResultResumesTheSinglePendingResolution() = runTest {
    val bridge = ActivityGoogleAuthorizationResolutionBridge()
    val pendingIntent = pendingIntent()
    val request = async { bridge.requests.first() }
    val resolution = async { bridge.resolve(pendingIntent) }

    assertThat(request.await()).isEqualTo(pendingIntent)
    val data = Intent("authorization-result")
    bridge.complete(Activity.RESULT_OK, data)

    assertThat(resolution.await()).isEqualTo(GoogleAuthorizationResolutionResult.Granted(data))
  }

  @Test
  fun cancelledActivityResultResumesAsCancelled() = runTest {
    val bridge = ActivityGoogleAuthorizationResolutionBridge()
    val request = async { bridge.requests.first() }
    val resolution = async { bridge.resolve(pendingIntent()) }

    request.await()
    bridge.complete(Activity.RESULT_CANCELED, null)

    assertThat(resolution.await()).isEqualTo(GoogleAuthorizationResolutionResult.Cancelled)
  }

  @Test
  fun resultDeliveredWhileTheLauncherConsumesTheRequestIsNotLost() = runTest {
    val bridge = ActivityGoogleAuthorizationResolutionBridge()
    val data = Intent("immediate-authorization-result")
    val launcher = async {
      bridge.requests.first()
      bridge.complete(Activity.RESULT_OK, data)
    }
    val resolution = async { bridge.resolve(pendingIntent()) }

    launcher.await()

    assertThat(resolution.await()).isEqualTo(GoogleAuthorizationResolutionResult.Granted(data))
  }

  @Test
  fun lateResultAfterCallerCancellationCannotCompleteTheNextResolution() = runTest {
    val bridge = ActivityGoogleAuthorizationResolutionBridge()
    val firstRequest = async { bridge.requests.first() }
    val cancelledResolution = async { bridge.resolve(pendingIntent()) }
    firstRequest.await()
    cancelledResolution.cancelAndJoin()

    bridge.complete(Activity.RESULT_OK, Intent("stale-result"))

    val secondRequest = async { bridge.requests.first() }
    val secondResolution = async { bridge.resolve(pendingIntent()) }
    secondRequest.await()
    bridge.complete(Activity.RESULT_CANCELED, null)
    assertThat(secondResolution.await()).isEqualTo(GoogleAuthorizationResolutionResult.Cancelled)
  }

  @Test
  fun hostCancellationResumesPendingResolutionAsCancelled() = runTest {
    val bridge = ActivityGoogleAuthorizationResolutionBridge()
    val request = async { bridge.requests.first() }
    val resolution = async { bridge.resolve(pendingIntent()) }
    request.await()

    bridge.cancelPending()

    assertThat(resolution.await()).isEqualTo(GoogleAuthorizationResolutionResult.Cancelled)
  }

  @Test
  fun pendingRequestSurvivesLifecycleCollectorRestartUntilLauncherAcceptsIt() = runTest {
    val bridge = ActivityGoogleAuthorizationResolutionBridge()
    val pendingIntent = pendingIntent()
    val firstLifecycleCollector = async { bridge.requests.first() }
    val resolution = async { bridge.resolve(pendingIntent) }
    assertThat(firstLifecycleCollector.await()).isEqualTo(pendingIntent)

    val requestAfterLifecycleRestart = withTimeoutOrNull(1) { bridge.requests.first() }

    assertThat(requestAfterLifecycleRestart).isEqualTo(pendingIntent)
    bridge.markLaunched(pendingIntent)
    bridge.cancelPending()
    assertThat(resolution.await()).isEqualTo(GoogleAuthorizationResolutionResult.Cancelled)
  }

  @Test
  fun acceptedLaunchIsNotReplayedAfterLifecycleRestart() = runTest {
    val bridge = ActivityGoogleAuthorizationResolutionBridge()
    val pendingIntent = pendingIntent()
    val firstLifecycleCollector = async { bridge.requests.first() }
    val resolution = async { bridge.resolve(pendingIntent) }
    assertThat(firstLifecycleCollector.await()).isEqualTo(pendingIntent)
    bridge.markLaunched(pendingIntent)

    val duplicate = withTimeoutOrNull(1) { bridge.requests.first() }

    assertThat(duplicate).isNull()
    bridge.cancelPending()
    assertThat(resolution.await()).isEqualTo(GoogleAuthorizationResolutionResult.Cancelled)
  }

  @Test
  fun resumedHostWithoutActivityResultCancelsLaunchedResolution() = runTest {
    val bridge = ActivityGoogleAuthorizationResolutionBridge()
    val pendingIntent = pendingIntent()
    val request = async { bridge.requests.first() }
    val resolution = async { bridge.resolve(pendingIntent) }
    request.await()
    bridge.markLaunched(pendingIntent)

    bridge.onHostResumed()

    assertThat(resolution.await()).isEqualTo(GoogleAuthorizationResolutionResult.Cancelled)
  }

  private fun pendingIntent(): PendingIntent {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return PendingIntent.getActivity(
      context,
      1,
      Intent(context, Activity::class.java),
      PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
