package com.woliveiras.petit

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.util.LanguageApplyResult
import com.woliveiras.petit.util.LocaleApplicator
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PetitApplicationLanguageInitializationTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun preferenceFailureAppliesSystemBeforeCreatingNotificationChannel() = runTest {
    val events = mutableListOf<String>()
    val repository = FakeUserPreferencesRepository(flow { error("DataStore unavailable") })
    val localeApplicator =
      object : LocaleApplicator {
        override fun applyLanguage(context: Context, language: AppLanguage) =
          LanguageApplyResult.APPLIED

        override fun applyLanguageAtStartup(context: Context, language: AppLanguage) {
          events += "locale:$language"
        }
      }

    initializeLanguageBeforeNotifications(context, repository, localeApplicator) {
      events += "channel"
    }

    assertThat(events).containsExactly("locale:SYSTEM", "channel").inOrder()
  }

  @Test
  fun preferenceCancellationStopsBeforeLocaleAndChannel() = runTest {
    val cancellation = CancellationException("cancelled")
    val events = mutableListOf<String>()
    val repository = FakeUserPreferencesRepository(flow { throw cancellation })
    val localeApplicator =
      object : LocaleApplicator {
        override fun applyLanguage(context: Context, language: AppLanguage) =
          LanguageApplyResult.APPLIED

        override fun applyLanguageAtStartup(context: Context, language: AppLanguage) {
          events += "locale"
        }
      }

    val thrown =
      runCatching {
          initializeLanguageBeforeNotifications(context, repository, localeApplicator) {
            events += "channel"
          }
        }
        .exceptionOrNull()

    assertThat(thrown).isSameInstanceAs(cancellation)
    assertThat(events).isEmpty()
  }

  private class FakeUserPreferencesRepository(override val userPreferences: Flow<UserPreferences>) :
    UserPreferencesRepository {
    override suspend fun updateTheme(theme: AppTheme) = Unit

    override suspend fun updateLanguage(language: AppLanguage) = Unit

    override suspend fun setOnboardingCompleted() = Unit
  }
}
