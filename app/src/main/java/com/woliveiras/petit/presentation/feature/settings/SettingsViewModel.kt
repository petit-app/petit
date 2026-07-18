package com.woliveiras.petit.presentation.feature.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.lan.LanSyncController
import com.woliveiras.petit.data.lan.LanSyncState
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.usecase.DeleteAllDataAction
import com.woliveiras.petit.util.LanguageApplyResult
import com.woliveiras.petit.util.LocaleApplicator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
  val currentTheme: AppTheme = AppTheme.SYSTEM,
  val currentLanguage: AppLanguage = AppLanguage.SYSTEM,
  val availableThemes: List<AppTheme> = AppTheme.entries,
  val availableLanguages: List<AppLanguage> = AppLanguage.entries,
  val showThemeDialog: Boolean = false,
  val showLanguageDialog: Boolean = false,
  val showDeleteAllDataDialog: Boolean = false,
  val isDeletingAllData: Boolean = false,
  val familyGroupInfo: FamilyGroupInfo? = null,
  val lastSyncText: String? = null,
  val isSavingPreference: Boolean = false,
  val languageRestartRequired: Boolean = false,
  val preferenceError: String? = null,
  val lanSyncEnabled: Boolean = false,
  val lanSyncState: LanSyncState = LanSyncState.Idle,
)

sealed class SettingsEvent {
  data object DeleteAllDataSuccess : SettingsEvent()

  data class DeleteAllDataError(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val familyGroupRepository: FamilyGroupRepository,
  private val deleteAllDataUseCase: DeleteAllDataAction,
  private val localeApplicator: LocaleApplicator,
  private val lanSyncController: LanSyncController,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<SettingsEvent>()
  val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

  init {
    observePreferences()
    observeFamilyGroup()
    observeLanSync()
  }

  private fun observeLanSync() {
    viewModelScope.launch {
      familyGroupRepository.isSyncEnabled.collect { enabled ->
        _uiState.update { it.copy(lanSyncEnabled = enabled) }
      }
    }
    viewModelScope.launch {
      lanSyncController.state.collect { state -> _uiState.update { it.copy(lanSyncState = state) } }
    }
  }

  fun setLanSyncEnabled(enabled: Boolean) {
    viewModelScope.launch {
      try {
        familyGroupRepository.setSyncEnabled(enabled)
        if (enabled) lanSyncController.startForeground() else lanSyncController.stopForeground()
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _uiState.update { it.copy(preferenceError = context.getString(R.string.error_saving)) }
      }
    }
  }

  fun attemptLanSync() {
    viewModelScope.launch { lanSyncController.attemptNow() }
  }

  private fun observePreferences() {
    viewModelScope.launch {
      userPreferencesRepository.userPreferences.collect { prefs ->
        _uiState.update { it.copy(currentTheme = prefs.theme, currentLanguage = prefs.language) }
      }
    }
  }

  private fun observeFamilyGroup() {
    viewModelScope.launch {
      familyGroupRepository.familyGroupInfo.collect { info ->
        val syncText =
          if (info != null) {
            val latestSync = familyGroupRepository.getLatestSyncLog()
            latestSync?.let {
              val relative =
                DateUtils.getRelativeTimeSpanString(
                    it.syncTimestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                  )
                  .toString()
              context.getString(R.string.family_group_last_sync, relative)
            }
          } else {
            null
          }
        _uiState.update { it.copy(familyGroupInfo = info, lastSyncText = syncText) }
      }
    }
  }

  fun showThemeDialog() {
    _uiState.update { it.copy(showThemeDialog = true) }
  }

  fun hideThemeDialog() {
    _uiState.update { it.copy(showThemeDialog = false) }
  }

  fun showLanguageDialog() {
    _uiState.update { it.copy(showLanguageDialog = true) }
  }

  fun hideLanguageDialog() {
    _uiState.update { it.copy(showLanguageDialog = false) }
  }

  fun updateTheme(theme: AppTheme) {
    if (_uiState.value.isSavingPreference) return
    _uiState.update { it.copy(isSavingPreference = true, preferenceError = null) }
    viewModelScope.launch {
      try {
        userPreferencesRepository.updateTheme(theme)
        _uiState.update { it.copy(showThemeDialog = false) }
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _uiState.update { it.copy(preferenceError = context.getString(R.string.error_saving)) }
      } finally {
        _uiState.update { it.copy(isSavingPreference = false) }
      }
    }
  }

  fun updateLanguage(language: AppLanguage) {
    if (_uiState.value.isSavingPreference) return
    _uiState.update { it.copy(isSavingPreference = true, preferenceError = null) }
    viewModelScope.launch {
      try {
        userPreferencesRepository.updateLanguage(language)
        val result = localeApplicator.applyLanguage(context, language)
        _uiState.update {
          it.copy(
            showLanguageDialog = false,
            languageRestartRequired = result == LanguageApplyResult.RESTART_REQUIRED,
          )
        }
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _uiState.update { it.copy(preferenceError = context.getString(R.string.error_saving)) }
      } finally {
        _uiState.update { it.copy(isSavingPreference = false) }
      }
    }
  }

  fun showDeleteAllDataDialog() {
    _uiState.update { it.copy(showDeleteAllDataDialog = true) }
  }

  fun hideDeleteAllDataDialog() {
    _uiState.update { it.copy(showDeleteAllDataDialog = false) }
  }

  fun deleteAllData() {
    viewModelScope.launch {
      _uiState.update { it.copy(isDeletingAllData = true) }
      deleteAllDataUseCase
        .execute()
        .onSuccess {
          _uiState.update { it.copy(isDeletingAllData = false, showDeleteAllDataDialog = false) }
          _events.emit(SettingsEvent.DeleteAllDataSuccess)
        }
        .onFailure { error ->
          _uiState.update { it.copy(isDeletingAllData = false, showDeleteAllDataDialog = false) }
          _events.emit(
            SettingsEvent.DeleteAllDataError(
              error.message ?: context.getString(R.string.error_unknown)
            )
          )
        }
    }
  }
}
