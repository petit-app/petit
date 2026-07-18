package com.woliveiras.petit.data.repository

import kotlinx.coroutines.flow.Flow

/** Data class holding reminder preferences/settings. */
data class ReminderPreferences(
  /** Whether automatic reminders for vaccination are enabled. */
  val vaccinationRemindersEnabled: Boolean = true,
  /** Days before vaccination due date to send reminder. */
  val vaccinationDaysBefore: Int = 7,

  /** Whether automatic reminders for deworming are enabled. */
  val dewormingRemindersEnabled: Boolean = true,
  /** Days before deworming due date to send reminder. */
  val dewormingDaysBefore: Int = 7,

  /** Whether weight reminders are enabled. */
  val weightRemindersEnabled: Boolean = false,
  /** Interval in days between weight reminders. */
  val weightReminderIntervalDays: Int = 30,

  /** Default hour for reminder notifications (0-23). */
  val defaultNotificationHour: Int = 9,
  /** Default minute for reminder notifications (0-59). */
  val defaultNotificationMinute: Int = 0,
)

/** Repository for managing reminder preferences using DataStore. */
interface ReminderPreferencesRepository {
  /** Flow of reminder preferences. */
  val preferences: Flow<ReminderPreferences>

  /** Get current preferences synchronously. */
  suspend fun getPreferences(): ReminderPreferences

  /** Update vaccination reminder settings. */
  suspend fun updateVaccinationSettings(enabled: Boolean, daysBefore: Int)

  /** Update deworming reminder settings. */
  suspend fun updateDewormingSettings(enabled: Boolean, daysBefore: Int)

  /** Update weight reminder settings. */
  suspend fun updateWeightSettings(enabled: Boolean, intervalDays: Int)

  /** Update default notification time. */
  suspend fun updateNotificationTime(hour: Int, minute: Int)

  /** Reset all reminder settings to their product defaults. */
  suspend fun reset()

  /** Atomically replace every restorable reminder preference. */
  suspend fun replaceRestorablePreferences(preferences: ReminderPreferences) {
    throw UnsupportedOperationException("Exact reminder preference restore is not supported")
  }
}
