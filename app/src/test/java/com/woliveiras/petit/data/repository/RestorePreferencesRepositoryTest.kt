package com.woliveiras.petit.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.revision.BackupMutationKind
import com.woliveiras.petit.domain.backup.revision.RestorableRevision
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionRepository
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionState
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

  @Test
  fun successfulRestorablePreferenceWritesAdvanceTheirMutationKinds() = runTest {
    val revisions = RecordingRevisions()
    val userStore =
      PreferenceDataStoreFactory.create(scope = backgroundScope) {
        File(temporaryFolder.root, "user-revision.preferences_pb")
      }
    val reminderStore =
      PreferenceDataStoreFactory.create(scope = backgroundScope) {
        File(temporaryFolder.root, "reminder-revision.preferences_pb")
      }
    val users = UserPreferencesRepositoryImpl(userStore, revisions)
    val reminders = ReminderPreferencesRepositoryImpl(reminderStore, revisions)

    users.updateTheme(AppTheme.DARK)
    reminders.updateNotificationTime(8, 30)

    assertThat(revisions.kinds)
      .containsExactly(BackupMutationKind.RESTORABLE_PREFERENCE, BackupMutationKind.REMINDER_STATE)
      .inOrder()
  }

  private class RecordingRevisions : RestorableRevisionRepository {
    private val mutable = MutableStateFlow(RestorableRevisionState())
    val kinds = mutableListOf<BackupMutationKind>()
    override val state: Flow<RestorableRevisionState> = mutable

    override suspend fun recordCommittedMutation(kind: BackupMutationKind): RestorableRevision {
      kinds += kind
      val next = RestorableRevision(mutable.value.current.value + 1)
      mutable.value = mutable.value.copy(current = next)
      return next
    }

    override suspend fun markCompleted(revision: RestorableRevision) = mutable.value
  }
}
