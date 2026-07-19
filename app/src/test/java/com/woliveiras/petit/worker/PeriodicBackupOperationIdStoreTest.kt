package com.woliveiras.petit.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PeriodicBackupOperationIdStoreTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun clearState() {
    context
      .getSharedPreferences(PeriodicBackupOperationIdStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit()
      .clear()
      .commit()
  }

  @Test
  fun retryReusesTheCurrentOperationAndANewPeriodRotatesIt() {
    val ids = ArrayDeque(listOf("operation-1", "operation-2"))
    val store = PeriodicBackupOperationIdStore(context) { ids.removeFirst() }

    val firstPeriod = store.operationIdFor("stable-periodic-work", runAttemptCount = 0)
    val retry = store.operationIdFor("stable-periodic-work", runAttemptCount = 1)
    val nextPeriod = store.operationIdFor("stable-periodic-work", runAttemptCount = 0)

    assertThat(firstPeriod).isEqualTo("operation-1")
    assertThat(retry).isEqualTo(firstPeriod)
    assertThat(nextPeriod).isEqualTo("operation-2")
  }

  @Test
  fun retrySurvivesStoreRecreationWithoutPersistingBackupContent() {
    val first = PeriodicBackupOperationIdStore(context) { "operation-1" }
    first.operationIdFor("stable-periodic-work", runAttemptCount = 0)

    val recreated = PeriodicBackupOperationIdStore(context) { "unexpected-operation" }

    assertThat(recreated.operationIdFor("stable-periodic-work", runAttemptCount = 2))
      .isEqualTo("operation-1")
    assertThat(
        context
          .getSharedPreferences(
            PeriodicBackupOperationIdStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
          )
          .all
          .keys
      )
      .containsExactly("periodic_work_id", "operation_id")
  }
}
