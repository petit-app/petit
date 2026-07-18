package com.woliveiras.petit.presentation.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.SyncLog
import com.woliveiras.petit.domain.usecase.DeleteAllDataAction
import com.woliveiras.petit.util.LanguageApplyResult
import com.woliveiras.petit.util.LocaleApplicator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var preferences: FakeUserPreferencesRepository
  private lateinit var localeApplicator: FakeLocaleApplicator
  private lateinit var viewModel: SettingsViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    preferences = FakeUserPreferencesRepository()
    localeApplicator = FakeLocaleApplicator()
    viewModel =
      SettingsViewModel(
        context,
        preferences,
        FakeFamilyGroupRepository(),
        object : DeleteAllDataAction {
          override suspend fun execute(): Result<Unit> = Result.success(Unit)
        },
        localeApplicator,
      )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun preferenceFlowAndSheetsExposeCurrentSelections() =
    runTest(dispatcher) {
      preferences.state.value =
        UserPreferences(theme = AppTheme.DARK, language = AppLanguage.PORTUGUESE_BR)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.currentTheme).isEqualTo(AppTheme.DARK)
      assertThat(viewModel.uiState.value.currentLanguage).isEqualTo(AppLanguage.PORTUGUESE_BR)
      assertThat(viewModel.uiState.value.availableThemes)
        .containsExactlyElementsIn(AppTheme.entries)
      assertThat(viewModel.uiState.value.availableLanguages)
        .containsExactlyElementsIn(AppLanguage.entries)

      viewModel.showThemeDialog()
      assertThat(viewModel.uiState.value.showThemeDialog).isTrue()
      viewModel.hideThemeDialog()
      viewModel.showLanguageDialog()
      assertThat(viewModel.uiState.value.showThemeDialog).isFalse()
      assertThat(viewModel.uiState.value.showLanguageDialog).isTrue()
    }

  @Test
  fun successfulThemeSelectionPersistsAndClosesSheet() =
    runTest(dispatcher) {
      viewModel.showThemeDialog()

      viewModel.updateTheme(AppTheme.LIGHT)
      advanceUntilIdle()

      assertThat(preferences.state.value.theme).isEqualTo(AppTheme.LIGHT)
      assertThat(viewModel.uiState.value.showThemeDialog).isFalse()
      assertThat(viewModel.uiState.value.preferenceError).isNull()
    }

  @Test
  fun preAndroid13LanguageSelectionPersistsClosesAndRequestsRestart() =
    runTest(dispatcher) {
      localeApplicator.nextResult = LanguageApplyResult.RESTART_REQUIRED
      viewModel.showLanguageDialog()

      viewModel.updateLanguage(AppLanguage.PORTUGUESE_BR)
      advanceUntilIdle()

      assertThat(preferences.state.value.language).isEqualTo(AppLanguage.PORTUGUESE_BR)
      assertThat(localeApplicator.appliedLanguages).containsExactly(AppLanguage.PORTUGUESE_BR)
      assertThat(viewModel.uiState.value.showLanguageDialog).isFalse()
      assertThat(viewModel.uiState.value.languageRestartRequired).isTrue()
    }

  @Test
  fun android13LanguageSelectionDoesNotRequestRestart() =
    runTest(dispatcher) {
      localeApplicator.nextResult = LanguageApplyResult.APPLIED

      viewModel.updateLanguage(AppLanguage.ENGLISH)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.languageRestartRequired).isFalse()
      assertThat(localeApplicator.appliedLanguages).containsExactly(AppLanguage.ENGLISH)
    }

  @Test
  fun failedLanguagePersistenceKeepsSheetOpenAndDoesNotApplyLocale() =
    runTest(dispatcher) {
      preferences.failLanguageWrite = true
      viewModel.showLanguageDialog()

      viewModel.updateLanguage(AppLanguage.ENGLISH)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.showLanguageDialog).isTrue()
      assertThat(viewModel.uiState.value.preferenceError).isNotNull()
      assertThat(localeApplicator.appliedLanguages).isEmpty()
    }

  @Test
  fun failedThemePersistenceKeepsSheetOpenAndExposesError() =
    runTest(dispatcher) {
      preferences.failThemeWrite = true
      viewModel.showThemeDialog()

      viewModel.updateTheme(AppTheme.DARK)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.showThemeDialog).isTrue()
      assertThat(viewModel.uiState.value.preferenceError).isNotNull()
      assertThat(viewModel.uiState.value.isSavingPreference).isFalse()
    }

  @Test
  fun repeatedLanguageSelectionWhileSavingWritesAndAppliesOnlyOnce() =
    runTest(dispatcher) {
      preferences.languageWriteGate = CompletableDeferred()

      viewModel.updateLanguage(AppLanguage.ENGLISH)
      viewModel.updateLanguage(AppLanguage.PORTUGUESE_BR)
      runCurrent()

      assertThat(preferences.languageWriteCalls).isEqualTo(1)
      assertThat(viewModel.uiState.value.isSavingPreference).isTrue()

      preferences.languageWriteGate?.complete(Unit)
      advanceUntilIdle()

      assertThat(localeApplicator.appliedLanguages).containsExactly(AppLanguage.ENGLISH)
    }

  @Test
  fun cancelledLanguageWriteIsNotConvertedIntoPreferenceError() =
    runTest(dispatcher) {
      preferences.cancelLanguageWrite = true
      viewModel.showLanguageDialog()

      viewModel.updateLanguage(AppLanguage.ENGLISH)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.preferenceError).isNull()
      assertThat(viewModel.uiState.value.showLanguageDialog).isTrue()
      assertThat(localeApplicator.appliedLanguages).isEmpty()
    }

  @Test
  fun localeApplicationFailureKeepsLanguageSheetOpenAndExposesError() =
    runTest(dispatcher) {
      localeApplicator.failApplication = true
      viewModel.showLanguageDialog()

      viewModel.updateLanguage(AppLanguage.ENGLISH)
      advanceUntilIdle()

      assertThat(preferences.state.value.language).isEqualTo(AppLanguage.ENGLISH)
      assertThat(viewModel.uiState.value.showLanguageDialog).isTrue()
      assertThat(viewModel.uiState.value.preferenceError).isNotNull()
    }

  private class FakeUserPreferencesRepository : UserPreferencesRepository {
    val state = MutableStateFlow(UserPreferences())
    var failThemeWrite = false
    var failLanguageWrite = false
    var cancelLanguageWrite = false
    var languageWriteCalls = 0
    var languageWriteGate: CompletableDeferred<Unit>? = null
    override val userPreferences: Flow<UserPreferences> = state

    override suspend fun updateTheme(theme: AppTheme) {
      if (failThemeWrite) error("DataStore unavailable")
      state.value = state.value.copy(theme = theme)
    }

    override suspend fun updateLanguage(language: AppLanguage) {
      languageWriteCalls += 1
      languageWriteGate?.await()
      if (cancelLanguageWrite) throw CancellationException("DataStore write cancelled")
      if (failLanguageWrite) error("DataStore unavailable")
      state.value = state.value.copy(language = language)
    }

    override suspend fun setOnboardingCompleted() = Unit
  }

  private class FakeLocaleApplicator : LocaleApplicator {
    var nextResult = LanguageApplyResult.APPLIED
    var failApplication = false
    val appliedLanguages = mutableListOf<AppLanguage>()

    override fun applyLanguage(context: Context, language: AppLanguage): LanguageApplyResult {
      appliedLanguages += language
      if (failApplication) error("LocaleManager unavailable")
      return nextResult
    }

    override fun applyLanguageAtStartup(context: Context, language: AppLanguage) = Unit
  }

  private class FakeFamilyGroupRepository : FamilyGroupRepository {
    override val familyGroupInfo: Flow<FamilyGroupInfo?> = MutableStateFlow(null)
    override val localDevice: Flow<FamilyGroupMember?> = MutableStateFlow(null)
    override val isSyncEnabled: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun getFamilyGroupKey(): String? = null

    override suspend fun createFamilyGroup(deviceName: String): String = ""

    override suspend fun joinFamilyGroup(familyGroupKey: String, deviceName: String) = Unit

    override suspend fun persistAuthorizedPairing(
      familyGroupKey: String,
      localMember: FamilyGroupMember,
      remoteMember: FamilyGroupMember,
    ) = Unit

    override suspend fun addRemoteMember(member: FamilyGroupMember) = Unit

    override suspend fun leaveFamilyGroup() = Unit

    override suspend fun removeMember(memberId: String) = Unit

    override suspend fun updateLastSyncAt(memberId: String) = Unit

    override suspend fun setSyncEnabled(enabled: Boolean) = Unit

    override suspend fun recordSyncLog(syncLog: SyncLog) = Unit

    override fun getSyncLogs(): Flow<List<SyncLog>> = MutableStateFlow(emptyList())

    override suspend fun getLatestSyncLog(): SyncLog? = null

    override suspend fun resetLocalPreferences() = Unit
  }
}
