package com.woliveiras.petit.presentation.feature.backup

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.woliveiras.petit.data.backup.google.GoogleAuthorizationResolutionBridge
import com.woliveiras.petit.data.backup.google.GoogleAuthorizationResolutionResult
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes foreground Google consent through the current Activity result registry. */
@Singleton
class ActivityGoogleAuthorizationResolutionBridge @Inject constructor() :
  GoogleAuthorizationResolutionBridge {
  private val pendingLaunch = MutableStateFlow<PendingIntent?>(null)
  private val pendingResult =
    AtomicReference<CompletableDeferred<GoogleAuthorizationResolutionResult>?>()
  private val resolutionMutex = Mutex()
  private val launched = AtomicReference<PendingIntent?>(null)

  /** Replays an unacknowledged launch after Activity lifecycle recreation. */
  val requests: Flow<PendingIntent> = pendingLaunch.filterNotNull()

  override suspend fun resolve(pendingIntent: PendingIntent): GoogleAuthorizationResolutionResult =
    resolutionMutex.withLock {
      val result = CompletableDeferred<GoogleAuthorizationResolutionResult>()
      check(pendingResult.compareAndSet(null, result)) { "Authorization is already in progress" }
      try {
        pendingLaunch.value = pendingIntent
        result.await()
      } finally {
        pendingLaunch.compareAndSet(pendingIntent, null)
        pendingResult.compareAndSet(result, null)
        result.cancel()
      }
    }

  /** Called immediately after the Activity Result launcher accepts this request. */
  fun markLaunched(pendingIntent: PendingIntent) {
    launched.set(pendingIntent)
    pendingLaunch.compareAndSet(pendingIntent, null)
  }

  /** Cancels a launched request when the host returns without an Activity Result callback. */
  fun onHostResumed() {
    if (launched.getAndSet(null) != null) cancelPending()
  }

  fun complete(resultCode: Int, data: Intent?) {
    launched.set(null)
    pendingLaunch.value = null
    val result =
      if (resultCode == Activity.RESULT_OK && data != null) {
        GoogleAuthorizationResolutionResult.Granted(data)
      } else {
        GoogleAuthorizationResolutionResult.Cancelled
      }
    pendingResult.getAndSet(null)?.complete(result)
  }

  fun cancelPending() {
    launched.set(null)
    pendingLaunch.value = null
    pendingResult.getAndSet(null)?.complete(GoogleAuthorizationResolutionResult.Cancelled)
  }
}
