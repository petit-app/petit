package com.woliveiras.petit.data.backup.google

import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupProviderException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GoogleDriveAuthorizationGatewayTest {
  @Test
  fun authorizationRequestContainsOnlyDriveAppDataAndNoOfflineAccess() {
    val request = googleDriveAuthorizationRequest()

    assertThat(request.requestedScopes.map { it.scopeUri })
      .containsExactly(GOOGLE_DRIVE_APPDATA_SCOPE)
    assertThat(request.isOfflineAccessRequested).isFalse()
    assertThat(request.optOutIncludingGrantedScopes).isTrue()
  }

  @Test
  fun foregroundAuthorizationOwnerUsesLiveActivityAndReleasesDestroyedHost() {
    val application = RuntimeEnvironment.getApplication()
    val activityController = Robolectric.buildActivity(Activity::class.java).setup()
    val activity = activityController.get()
    val owner = ForegroundAuthorizationActivityOwner()

    owner.attach(activity)
    assertThat(owner.authorizationActivity()).isSameInstanceAs(activity)

    activityController.destroy()
    assertThat(owner.authorizationActivity()).isNull()
  }

  @Test
  fun neverConnectedBackgroundLookupDoesNotContactGoogle() = runTest {
    val client = FakeIdentityClient()
    val gateway = gateway(client, connected = false)

    val failure = runCatching { gateway.acquireToken() }.exceptionOrNull()

    assertThat(failure).isInstanceOf(BackupProviderException.AuthorizationRequired::class.java)
    assertThat(client.authorizeCalls).isEqualTo(0)
  }

  @Test
  fun previouslyConnectedBackgroundLookupSilentlyRefreshesShortLivedToken() = runTest {
    val client = FakeIdentityClient(GoogleIdentityAuthorizationResult.Granted("short-lived", null))
    val gateway = gateway(client, connected = true)

    assertThat(gateway.acquireToken()).isEqualTo("short-lived")
    assertThat(gateway.state.value).isEqualTo(BackupAuthorizationState.Authorized())
    assertThat(client.authorizeCalls).isEqualTo(1)
  }

  @Test
  fun processRecreationRefreshDetectsRevocationWithoutLaunchingForegroundConsent() = runTest {
    val client =
      FakeIdentityClient(GoogleIdentityAuthorizationResult.RequiresResolution(pendingIntent()))
    var bridgeCalls = 0
    val gateway =
      GoogleDriveAuthorizationGateway(
        client,
        GoogleAuthorizationResolutionBridge {
          bridgeCalls++
          GoogleAuthorizationResolutionResult.Cancelled
        },
        FakeMarker(true),
      )

    val refreshed = gateway.refresh()

    assertThat(refreshed).isEqualTo(BackupAuthorizationState.AuthorizationRequired)
    assertThat(client.authorizeCalls).isEqualTo(1)
    assertThat(bridgeCalls).isEqualTo(0)
  }

  @Test
  fun neverConnectedRefreshDoesNotContactGoogle() = runTest {
    val client = FakeIdentityClient()
    val gateway = gateway(client, connected = false)

    assertThat(gateway.refresh()).isEqualTo(BackupAuthorizationState.Disconnected)
    assertThat(client.authorizeCalls).isEqualTo(0)
  }

  @Test
  fun silentLookupNeverLaunchesConsentAndMarksReauthorizationRequired() = runTest {
    val client =
      FakeIdentityClient(GoogleIdentityAuthorizationResult.RequiresResolution(pendingIntent()))
    var bridgeCalls = 0
    val gateway =
      GoogleDriveAuthorizationGateway(
        client,
        GoogleAuthorizationResolutionBridge {
          bridgeCalls++
          GoogleAuthorizationResolutionResult.Cancelled
        },
        FakeMarker(true),
      )

    val failure = runCatching { gateway.acquireToken() }.exceptionOrNull()

    assertThat(failure).isInstanceOf(BackupProviderException.AuthorizationRequired::class.java)
    assertThat(bridgeCalls).isEqualTo(0)
    assertThat(gateway.state.value).isEqualTo(BackupAuthorizationState.AuthorizationRequired)
  }

  @Test
  fun foregroundCancellationRemainsActionableAndDoesNotPersistConnection() = runTest {
    val marker = FakeMarker(false)
    val gateway =
      GoogleDriveAuthorizationGateway(
        FakeIdentityClient(GoogleIdentityAuthorizationResult.RequiresResolution(pendingIntent())),
        GoogleAuthorizationResolutionBridge { GoogleAuthorizationResolutionResult.Cancelled },
        marker,
      )

    assertThat(gateway.authorize()).isEqualTo(BackupAuthorizationResult.Cancelled)
    assertThat(marker.connected).isFalse()
    assertThat(gateway.state.value).isEqualTo(BackupAuthorizationState.AuthorizationRequired)
  }

  @Test
  fun userInitiatedAuthorizationUsesForegroundClientPath() = runTest {
    val client =
      FakeIdentityClient(GoogleIdentityAuthorizationResult.Granted("foreground-token", null))
    val gateway = gateway(client)

    assertThat(gateway.authorize()).isEqualTo(BackupAuthorizationResult.Authorized)

    assertThat(client.foregroundAuthorizeCalls).isEqualTo(1)
    assertThat(client.authorizeCalls).isEqualTo(0)
  }

  @Test
  fun concurrentInteractiveCallsShareOneAuthorizationResult() = runTest {
    val gate = CompletableDeferred<Unit>()
    val client = FakeIdentityClient(GoogleIdentityAuthorizationResult.Granted("token", null))
    client.foregroundGate = gate
    val gateway = gateway(client)
    val first = async { gateway.authorize() }
    val second = async { gateway.authorize() }
    yield()
    gate.complete(Unit)

    assertThat(first.await()).isEqualTo(BackupAuthorizationResult.Authorized)
    assertThat(second.await()).isEqualTo(BackupAuthorizationResult.Authorized)
    assertThat(client.foregroundAuthorizeCalls).isEqualTo(1)
  }

  @Test
  fun missingResumedActivityMapsToStableUnavailableResult() = runTest {
    val client = FakeIdentityClient()
    client.foregroundFailure = ForegroundAuthorizationUnavailableException()
    val gateway = gateway(client)

    val result = gateway.authorize()

    assertThat(result).isInstanceOf(BackupAuthorizationResult.Unavailable::class.java)
    assertThat(gateway.state.value).isInstanceOf(BackupAuthorizationState.Unavailable::class.java)
  }

  @Test
  fun disconnectRevokesGrantButDoesNotRequestRemoteDeletion() = runTest {
    val client = FakeIdentityClient(GoogleIdentityAuthorizationResult.Granted("token", null))
    val marker = FakeMarker(true)
    val gateway = gateway(client, marker = marker)
    gateway.acquireToken()

    gateway.disconnect()

    assertThat(client.revokeCalls).isEqualTo(1)
    assertThat(marker.connected).isFalse()
    assertThat(gateway.state.value).isEqualTo(BackupAuthorizationState.Disconnected)
  }

  private fun gateway(
    client: FakeIdentityClient,
    connected: Boolean = false,
    marker: FakeMarker = FakeMarker(connected),
  ) =
    GoogleDriveAuthorizationGateway(
      client,
      GoogleAuthorizationResolutionBridge { GoogleAuthorizationResolutionResult.Cancelled },
      marker,
    )

  private fun pendingIntent(): PendingIntent =
    PendingIntent.getActivity(
      org.robolectric.RuntimeEnvironment.getApplication(),
      1,
      Intent("petit.test.authorization"),
      PendingIntent.FLAG_IMMUTABLE,
    )

  private class FakeMarker(override var connected: Boolean) : GoogleDriveConnectionMarker

  private class FakeIdentityClient(
    var next: GoogleIdentityAuthorizationResult =
      GoogleIdentityAuthorizationResult.Granted("token", Account("user", "com.google"))
  ) : GoogleIdentityAuthorizationClient {
    var authorizeCalls = 0
    var foregroundAuthorizeCalls = 0
    var revokeCalls = 0
    var foregroundGate: CompletableDeferred<Unit>? = null
    var foregroundFailure: Exception? = null

    override suspend fun authorize(): GoogleIdentityAuthorizationResult {
      authorizeCalls++
      return next
    }

    override suspend fun authorizeForeground(): GoogleIdentityAuthorizationResult {
      foregroundAuthorizeCalls++
      foregroundGate?.await()
      foregroundFailure?.let { throw it }
      return next
    }

    override fun resultFromIntent(data: Intent): GoogleIdentityAuthorizationResult.Granted =
      next as GoogleIdentityAuthorizationResult.Granted

    override suspend fun revoke(account: Account?) {
      revokeCalls++
    }

    override suspend fun clearToken(token: String) = Unit
  }
}
