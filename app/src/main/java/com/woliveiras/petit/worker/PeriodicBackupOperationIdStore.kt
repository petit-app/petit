package com.woliveiras.petit.worker

import android.annotation.SuppressLint
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps only the non-sensitive operation ID needed to make a periodic WorkManager retry idempotent.
 * WorkManager resets [androidx.work.ListenableWorker.getRunAttemptCount] between periodic
 * executions, while the periodic work request ID itself remains stable.
 */
@Singleton
class PeriodicBackupOperationIdStore
internal constructor(context: Context, private val newOperationId: () -> String) {
  @Inject
  constructor(
    @ApplicationContext context: Context
  ) : this(context, { UUID.randomUUID().toString() })

  private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  fun operationIdFor(workRequestId: String, runAttemptCount: Int): String =
    synchronized(lock) {
      val persistedWorkRequestId = preferences.getString(KEY_PERIODIC_WORK_ID, null)
      val persistedOperationId = preferences.getString(KEY_OPERATION_ID, null)
      if (
        runAttemptCount > 0 &&
          persistedWorkRequestId == workRequestId &&
          persistedOperationId != null
      ) {
        persistedOperationId
      } else {
        newOperationId().also { persist(workRequestId, it) }
      }
    }

  @SuppressLint("ApplySharedPref")
  private fun persist(workRequestId: String, operationId: String) {
    // Commit before upload so a process-recreated retry can reuse the same idempotency key.
    check(
      preferences
        .edit()
        .putString(KEY_PERIODIC_WORK_ID, workRequestId)
        .putString(KEY_OPERATION_ID, operationId)
        .commit()
    ) {
      "Unable to persist the periodic backup operation ID"
    }
  }

  companion object {
    internal const val PREFERENCES_NAME = "periodic_backup_operation"
    private const val KEY_PERIODIC_WORK_ID = "periodic_work_id"
    private const val KEY_OPERATION_ID = "operation_id"
    private val lock = Any()
  }
}
