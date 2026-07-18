package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.woliveiras.petit.domain.backup.revision.BackupMutationKind
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by
  preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepositoryImpl
internal constructor(
  private val dataStore: DataStore<Preferences>,
  private val revisions: RestorableRevisionRepository? = null,
) : UserPreferencesRepository {
  @Inject
  constructor(
    @ApplicationContext context: Context,
    revisions: RestorableRevisionRepository,
  ) : this(context.dataStore, revisions)

  constructor(context: Context) : this(context.dataStore, null)

  private object PreferencesKeys {
    val THEME = stringPreferencesKey("theme")
    val LANGUAGE = stringPreferencesKey("language")
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
  }

  override val userPreferences: Flow<UserPreferences> =
    dataStore.data.map { preferences ->
      val theme =
        preferences[PreferencesKeys.THEME]?.let {
          try {
            AppTheme.valueOf(it)
          } catch (_: Exception) {
            AppTheme.SYSTEM
          }
        } ?: AppTheme.SYSTEM

      val language =
        preferences[PreferencesKeys.LANGUAGE]?.let { AppLanguage.fromCode(it) }
          ?: AppLanguage.SYSTEM

      UserPreferences(
        theme = theme,
        language = language,
        hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
      )
    }

  override suspend fun updateTheme(theme: AppTheme) {
    dataStore.edit { preferences -> preferences[PreferencesKeys.THEME] = theme.name }
    recordRestorableChange()
  }

  override suspend fun updateLanguage(language: AppLanguage) {
    dataStore.edit { preferences -> preferences[PreferencesKeys.LANGUAGE] = language.code }
    recordRestorableChange()
  }

  override suspend fun setOnboardingCompleted() {
    dataStore.edit { preferences -> preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true }
    recordRestorableChange()
  }

  override suspend fun replaceRestorablePreferences(preferences: UserPreferences) {
    dataStore.edit { stored ->
      stored[PreferencesKeys.THEME] = preferences.theme.name
      stored[PreferencesKeys.LANGUAGE] = preferences.language.code
      stored[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = preferences.hasCompletedOnboarding
    }
    recordRestorableChange()
  }

  private suspend fun recordRestorableChange() {
    revisions?.recordCommittedMutation(BackupMutationKind.RESTORABLE_PREFERENCE)
  }
}
