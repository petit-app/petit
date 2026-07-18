package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

/** Data class holding user preferences. */
data class UserPreferences(
  val theme: AppTheme = AppTheme.SYSTEM,
  val language: AppLanguage = AppLanguage.SYSTEM,
  val hasCompletedOnboarding: Boolean = false,
)

/** Repository for managing user preferences using DataStore. */
interface UserPreferencesRepository {
  /** Flow of user preferences. */
  val userPreferences: Flow<UserPreferences>

  /** Update the app theme. */
  suspend fun updateTheme(theme: AppTheme)

  /** Update the app language. */
  suspend fun updateLanguage(language: AppLanguage)

  /** Mark onboarding as completed. */
  suspend fun setOnboardingCompleted()

  /** Atomically replace every restorable preference, including a false onboarding value. */
  suspend fun replaceRestorablePreferences(preferences: UserPreferences) {
    throw UnsupportedOperationException("Exact preference restore is not supported")
  }
}
