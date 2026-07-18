package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.woliveiras.petit.domain.backup.revision.BackupMutationKind
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderPreferencesDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "reminder_preferences")

@Singleton
class ReminderPreferencesRepositoryImpl
internal constructor(
  private val dataStore: DataStore<Preferences>,
  private val revisionRepository: RestorableRevisionRepository? = null,
) : ReminderPreferencesRepository {
  @Inject
  constructor(
    @ApplicationContext context: Context,
    revisions: RestorableRevisionRepository,
  ) : this(context.reminderPreferencesDataStore, revisions)

  constructor(context: Context) : this(context.reminderPreferencesDataStore, null)

  private object PreferencesKeys {
    val VACCINATION_ENABLED = booleanPreferencesKey("vaccination_reminders_enabled")
    val VACCINATION_DAYS_BEFORE = intPreferencesKey("vaccination_days_before")
    val DEWORMING_ENABLED = booleanPreferencesKey("deworming_reminders_enabled")
    val DEWORMING_DAYS_BEFORE = intPreferencesKey("deworming_days_before")
    val WEIGHT_ENABLED = booleanPreferencesKey("weight_reminders_enabled")
    val WEIGHT_INTERVAL_DAYS = intPreferencesKey("weight_interval_days")
    val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
    val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
  }

  override val preferences: Flow<ReminderPreferences> =
    dataStore.data.map { prefs ->
      ReminderPreferences(
        vaccinationRemindersEnabled = prefs[PreferencesKeys.VACCINATION_ENABLED] ?: true,
        vaccinationDaysBefore = prefs[PreferencesKeys.VACCINATION_DAYS_BEFORE] ?: 7,
        dewormingRemindersEnabled = prefs[PreferencesKeys.DEWORMING_ENABLED] ?: true,
        dewormingDaysBefore = prefs[PreferencesKeys.DEWORMING_DAYS_BEFORE] ?: 7,
        weightRemindersEnabled = prefs[PreferencesKeys.WEIGHT_ENABLED] ?: false,
        weightReminderIntervalDays = prefs[PreferencesKeys.WEIGHT_INTERVAL_DAYS] ?: 30,
        defaultNotificationHour = prefs[PreferencesKeys.NOTIFICATION_HOUR] ?: 9,
        defaultNotificationMinute = prefs[PreferencesKeys.NOTIFICATION_MINUTE] ?: 0,
      )
    }

  override suspend fun getPreferences(): ReminderPreferences {
    return preferences.first()
  }

  override suspend fun updateVaccinationSettings(enabled: Boolean, daysBefore: Int) {
    dataStore.edit { prefs ->
      prefs[PreferencesKeys.VACCINATION_ENABLED] = enabled
      prefs[PreferencesKeys.VACCINATION_DAYS_BEFORE] = daysBefore.coerceIn(1, 30)
    }
    recordRestorableChange()
  }

  override suspend fun updateDewormingSettings(enabled: Boolean, daysBefore: Int) {
    dataStore.edit { prefs ->
      prefs[PreferencesKeys.DEWORMING_ENABLED] = enabled
      prefs[PreferencesKeys.DEWORMING_DAYS_BEFORE] = daysBefore.coerceIn(1, 30)
    }
    recordRestorableChange()
  }

  override suspend fun updateWeightSettings(enabled: Boolean, intervalDays: Int) {
    dataStore.edit { prefs ->
      prefs[PreferencesKeys.WEIGHT_ENABLED] = enabled
      prefs[PreferencesKeys.WEIGHT_INTERVAL_DAYS] = intervalDays.coerceIn(7, 90)
    }
    recordRestorableChange()
  }

  override suspend fun updateNotificationTime(hour: Int, minute: Int) {
    dataStore.edit { prefs ->
      prefs[PreferencesKeys.NOTIFICATION_HOUR] = hour.coerceIn(0, 23)
      prefs[PreferencesKeys.NOTIFICATION_MINUTE] = minute.coerceIn(0, 59)
    }
    recordRestorableChange()
  }

  override suspend fun reset() {
    dataStore.edit { it.clear() }
    recordRestorableChange()
  }

  override suspend fun replaceRestorablePreferences(preferences: ReminderPreferences) {
    require(preferences.vaccinationDaysBefore in 0..365)
    require(preferences.dewormingDaysBefore in 0..365)
    require(preferences.weightReminderIntervalDays in 1..365)
    require(preferences.defaultNotificationHour in 0..23)
    require(preferences.defaultNotificationMinute in 0..59)
    dataStore.edit { stored ->
      stored[PreferencesKeys.VACCINATION_ENABLED] = preferences.vaccinationRemindersEnabled
      stored[PreferencesKeys.VACCINATION_DAYS_BEFORE] = preferences.vaccinationDaysBefore
      stored[PreferencesKeys.DEWORMING_ENABLED] = preferences.dewormingRemindersEnabled
      stored[PreferencesKeys.DEWORMING_DAYS_BEFORE] = preferences.dewormingDaysBefore
      stored[PreferencesKeys.WEIGHT_ENABLED] = preferences.weightRemindersEnabled
      stored[PreferencesKeys.WEIGHT_INTERVAL_DAYS] = preferences.weightReminderIntervalDays
      stored[PreferencesKeys.NOTIFICATION_HOUR] = preferences.defaultNotificationHour
      stored[PreferencesKeys.NOTIFICATION_MINUTE] = preferences.defaultNotificationMinute
    }
    recordRestorableChange()
  }

  private suspend fun recordRestorableChange() {
    revisionRepository?.recordCommittedMutation(BackupMutationKind.REMINDER_STATE)
  }
}
