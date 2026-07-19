package com.woliveiras.petit.data.backup.google

import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupProviderException
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

const val GOOGLE_DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

/**
 * Foreground-only consent launcher. The Activity-owned implementation must serialize requests,
 * resume exactly one caller for each result, and report host destruction as cancellation.
 */
fun interface GoogleAuthorizationResolutionBridge {
  suspend fun resolve(pendingIntent: PendingIntent): GoogleAuthorizationResolutionResult
}

sealed interface GoogleAuthorizationResolutionResult {
  data class Granted(val data: Intent) : GoogleAuthorizationResolutionResult

  data object Cancelled : GoogleAuthorizationResolutionResult
}

interface GoogleDriveConnectionMarker {
  var connected: Boolean
}

class SharedPreferencesGoogleDriveConnectionMarker(context: Context) : GoogleDriveConnectionMarker {
  private val preferences =
    context.getSharedPreferences("google_drive_backup_connection", Context.MODE_PRIVATE)

  override var connected: Boolean
    get() = preferences.getBoolean(CONNECTED_KEY, false)
    set(value) {
      preferences.edit().putBoolean(CONNECTED_KEY, value).apply()
    }

  private companion object {
    const val CONNECTED_KEY = "connected"
  }
}

interface GoogleDriveAccessTokenProvider {
  suspend fun acquireToken(): String

  suspend fun invalidateToken(token: String)
}

sealed interface GoogleIdentityAuthorizationResult {
  data class Granted(val accessToken: String, val account: Account?) :
    GoogleIdentityAuthorizationResult

  data class RequiresResolution(val pendingIntent: PendingIntent) :
    GoogleIdentityAuthorizationResult
}

interface GoogleIdentityAuthorizationClient {
  suspend fun authorize(): GoogleIdentityAuthorizationResult

  /** Uses the current resumed Activity client for a user-initiated authorization request. */
  suspend fun authorizeForeground(): GoogleIdentityAuthorizationResult = authorize()

  fun resultFromIntent(data: Intent): GoogleIdentityAuthorizationResult.Granted

  suspend fun revoke(account: Account?)

  suspend fun clearToken(token: String)
}

class PlayServicesGoogleIdentityAuthorizationClient(
  context: Context,
  private val applicationClient: AuthorizationClient =
    Identity.getAuthorizationClient(context.applicationContext),
) : GoogleIdentityAuthorizationClient {
  private val scope = Scope(GOOGLE_DRIVE_APPDATA_SCOPE)
  private val foregroundOwner = ForegroundAuthorizationActivityOwner()

  override suspend fun authorize(): GoogleIdentityAuthorizationResult =
    applicationClient.authorize(googleDriveAuthorizationRequest()).await().toInternal()

  override suspend fun authorizeForeground(): GoogleIdentityAuthorizationResult {
    return foregroundClient().authorize(googleDriveAuthorizationRequest()).await().toInternal()
  }

  fun attachForegroundActivity(activity: Activity) {
    foregroundOwner.attach(activity)
  }

  fun detachForegroundActivity(activity: Activity) {
    foregroundOwner.detach(activity)
  }

  override fun resultFromIntent(data: Intent): GoogleIdentityAuthorizationResult.Granted =
    applicationClient.getAuthorizationResultFromIntent(data).toGranted()

  override suspend fun revoke(account: Account?) {
    val builder = RevokeAccessRequest.builder().setScopes(listOf(scope))
    if (account != null) builder.setAccount(account)
    applicationClient.revokeAccess(builder.build()).await()
  }

  override suspend fun clearToken(token: String) {
    applicationClient.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
  }

  private fun foregroundClient(): AuthorizationClient =
    foregroundOwner.authorizationActivity()?.let(Identity::getAuthorizationClient)
      ?: throw ForegroundAuthorizationUnavailableException()

  private fun AuthorizationResult.toInternal(): GoogleIdentityAuthorizationResult =
    if (hasResolution()) {
      GoogleIdentityAuthorizationResult.RequiresResolution(requireNotNull(pendingIntent))
    } else {
      toGranted()
    }

  private fun AuthorizationResult.toGranted(): GoogleIdentityAuthorizationResult.Granted {
    require(grantedScopes.contains(GOOGLE_DRIVE_APPDATA_SCOPE)) {
      "Google Drive app-data scope was not granted"
    }
    return GoogleIdentityAuthorizationResult.Granted(
      accessToken = requireNotNull(accessToken) { "Authorization returned no access token" },
      account = toGoogleSignInAccount()?.account,
    )
  }
}

internal class ForegroundAuthorizationActivityOwner {
  private val foregroundActivity = AtomicReference<WeakReference<Activity>?>(null)

  fun attach(activity: Activity) {
    foregroundActivity.set(WeakReference(activity))
  }

  fun detach(activity: Activity) {
    val reference = foregroundActivity.get()
    if (reference?.get() === activity) foregroundActivity.compareAndSet(reference, null)
  }

  fun authorizationActivity(): Activity? =
    foregroundActivity.get()?.get()?.takeUnless { it.isFinishing || it.isDestroyed }
}

internal class ForegroundAuthorizationUnavailableException :
  IllegalStateException("Google authorization requires a resumed Activity")

internal fun googleDriveAuthorizationRequest(): AuthorizationRequest =
  AuthorizationRequest.builder()
    .setRequestedScopes(listOf(Scope(GOOGLE_DRIVE_APPDATA_SCOPE)))
    .setOptOutIncludingGrantedScopes(true)
    .build()

class GoogleDriveAuthorizationGateway(
  private val client: GoogleIdentityAuthorizationClient,
  private val resolutionBridge: GoogleAuthorizationResolutionBridge,
  private val marker: GoogleDriveConnectionMarker,
) : BackupAuthorizationGateway, GoogleDriveAccessTokenProvider {
  private val token = AtomicReference<String?>()
  private val account = AtomicReference<Account?>()
  private val mutableState =
    MutableStateFlow<BackupAuthorizationState>(
      if (marker.connected) BackupAuthorizationState.Authorized()
      else BackupAuthorizationState.Disconnected
    )
  private val interactiveAuthorization =
    AtomicReference<CompletableDeferred<BackupAuthorizationResult>?>(null)

  override val state: StateFlow<BackupAuthorizationState> = mutableState

  override suspend fun refresh(): BackupAuthorizationState {
    if (!marker.connected) {
      mutableState.value = BackupAuthorizationState.Disconnected
      return mutableState.value
    }
    mutableState.value =
      try {
        when (val result = client.authorize()) {
          is GoogleIdentityAuthorizationResult.Granted -> {
            accept(result)
            BackupAuthorizationState.Authorized()
          }
          is GoogleIdentityAuthorizationResult.RequiresResolution ->
            BackupAuthorizationState.AuthorizationRequired
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (error: Exception) {
        BackupAuthorizationState.Unavailable(error.safeReason())
      }
    return mutableState.value
  }

  override suspend fun authorize(): BackupAuthorizationResult {
    val (shared, isLeader) = claimInteractiveAuthorization()
    if (!isLeader) return shared.await()
    try {
      return authorizeOnce().also(shared::complete)
    } catch (error: Throwable) {
      shared.completeExceptionally(error)
      throw error
    } finally {
      interactiveAuthorization.compareAndSet(shared, null)
    }
  }

  private fun claimInteractiveAuthorization():
    Pair<CompletableDeferred<BackupAuthorizationResult>, Boolean> {
    while (true) {
      interactiveAuthorization.get()?.let {
        return it to false
      }
      val candidate = CompletableDeferred<BackupAuthorizationResult>()
      if (interactiveAuthorization.compareAndSet(null, candidate)) return candidate to true
    }
  }

  private suspend fun authorizeOnce(): BackupAuthorizationResult {
    mutableState.value = BackupAuthorizationState.Authorizing
    return try {
      val result =
        when (val initial = client.authorizeForeground()) {
          is GoogleIdentityAuthorizationResult.Granted -> initial
          is GoogleIdentityAuthorizationResult.RequiresResolution ->
            when (val resolved = resolutionBridge.resolve(initial.pendingIntent)) {
              is GoogleAuthorizationResolutionResult.Granted ->
                client.resultFromIntent(resolved.data)
              GoogleAuthorizationResolutionResult.Cancelled -> {
                mutableState.value = BackupAuthorizationState.AuthorizationRequired
                return BackupAuthorizationResult.Cancelled
              }
            }
        }
      accept(result)
      BackupAuthorizationResult.Authorized
    } catch (cancelled: CancellationException) {
      mutableState.value = BackupAuthorizationState.AuthorizationRequired
      throw cancelled
    } catch (cancelled: ApiException) {
      if (cancelled.statusCode == CommonStatusCodes.CANCELED) {
        mutableState.value = BackupAuthorizationState.AuthorizationRequired
        BackupAuthorizationResult.Cancelled
      } else {
        mutableState.value = BackupAuthorizationState.Unavailable(cancelled.safeReason())
        BackupAuthorizationResult.Unavailable(cancelled.safeReason())
      }
    } catch (error: Exception) {
      mutableState.value = BackupAuthorizationState.Unavailable(error.safeReason())
      BackupAuthorizationResult.Unavailable(error.safeReason())
    }
  }

  override suspend fun acquireToken(): String {
    token.get()?.let {
      return it
    }
    if (!marker.connected) {
      mutableState.value = BackupAuthorizationState.AuthorizationRequired
      throw BackupProviderException.AuthorizationRequired()
    }
    return try {
      when (val result = client.authorize()) {
        is GoogleIdentityAuthorizationResult.Granted -> {
          accept(result)
          result.accessToken
        }
        is GoogleIdentityAuthorizationResult.RequiresResolution -> {
          mutableState.value = BackupAuthorizationState.AuthorizationRequired
          throw BackupProviderException.AuthorizationRequired()
        }
      }
    } catch (expected: BackupProviderException.AuthorizationRequired) {
      throw expected
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (error: Exception) {
      throw BackupProviderException.Retryable(
        "Google authorization is temporarily unavailable",
        error,
      )
    }
  }

  override suspend fun invalidateToken(token: String) {
    this.token.compareAndSet(token, null)
    try {
      client.clearToken(token)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      // The next silent request still bypasses the adapter's in-memory cache.
    }
  }

  override suspend fun disconnect() {
    val resolvedAccount =
      account.get()
        ?: if (marker.connected) {
          (runCatching { client.authorize() }.getOrNull()
              as? GoogleIdentityAuthorizationResult.Granted)
            ?.account
        } else {
          null
        }
    try {
      if (marker.connected) client.revoke(resolvedAccount)
    } finally {
      token.set(null)
      account.set(null)
      marker.connected = false
      mutableState.value = BackupAuthorizationState.Disconnected
    }
  }

  private fun accept(result: GoogleIdentityAuthorizationResult.Granted) {
    token.set(result.accessToken)
    account.set(result.account)
    marker.connected = true
    mutableState.value = BackupAuthorizationState.Authorized()
  }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
  addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
  addOnFailureListener { error ->
    if (continuation.isActive) continuation.resumeWithException(error)
  }
  addOnCanceledListener { continuation.cancel() }
}

private fun Exception.safeReason(): String =
  when (this) {
    is ApiException -> "Google authorization error (${statusCode})"
    else -> "Google authorization is unavailable"
  }
