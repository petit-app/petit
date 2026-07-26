package com.woliveiras.petit.presentation.util

import android.content.Context
import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException

/**
 * Returns app-owned localized copy for a user-visible failure boundary.
 *
 * Exception messages remain diagnostic implementation details and are never suitable UI copy.
 */
fun Throwable.uiFailureText(context: Context, @StringRes fallbackResourceId: Int): String {
  rethrowIfCancellation()
  return context.getString(fallbackResourceId)
}

/** Keeps structured coroutine cancellation from being converted into failure UI or swallowed. */
fun Throwable.rethrowIfCancellation() {
  if (this is CancellationException) throw this
}
