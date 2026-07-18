package com.woliveiras.petit.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestorePreferencesRepositoryTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test
  fun userPreferencesExactReplacementCanRestoreOnboardingToFalse() = runTest {
    val dataStore =
      PreferenceDataStoreFactory.create(scope = backgroundScope) {
        File(temporaryFolder.root, "user.preferences_pb")
      }
    val repository = UserPreferencesRepositoryImpl(dataStore)
    repository.replaceRestorablePreferences(
      UserPreferences(AppTheme.LIGHT, AppLanguage.ENGLISH, true)
    )

    repository.replaceRestorablePreferences(
      UserPreferences(AppTheme.DARK, AppLanguage.PORTUGUESE_BR, false)
    )

    assertThat(repository.userPreferences.first())
      .isEqualTo(UserPreferences(AppTheme.DARK, AppLanguage.PORTUGUESE_BR, false))
  }

  @Test
  fun reminderPreferencesExactReplacementPersistsEveryValueWithoutSetterCoercion() = runTest {
    val dataStore =
      PreferenceDataStoreFactory.create(scope = backgroundScope) {
        File(temporaryFolder.root, "reminders.preferences_pb")
      }
    val repository = ReminderPreferencesRepositoryImpl(dataStore)
    val restored =
      ReminderPreferences(
        vaccinationRemindersEnabled = false,
        vaccinationDaysBefore = 0,
        dewormingRemindersEnabled = false,
        dewormingDaysBefore = 365,
        weightRemindersEnabled = true,
        weightReminderIntervalDays = 1,
        defaultNotificationHour = 0,
        defaultNotificationMinute = 59,
      )

    repository.replaceRestorablePreferences(restored)

    assertThat(repository.getPreferences()).isEqualTo(restored)
  }
}
