package com.woliveiras.petit.presentation.feature.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

  private val dispatcher: TestDispatcher = StandardTestDispatcher()
  private lateinit var repository: ControllableUserPreferencesRepository
  private lateinit var viewModel: OnboardingViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repository = ControllableUserPreferencesRepository()
    viewModel = OnboardingViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun successfulCompletionPersistsBeforeNavigating() =
    runTest(dispatcher) {
      viewModel.events.test {
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertThat(repository.writeCalls).isEqualTo(1)
        assertThat(awaitItem()).isEqualTo(OnboardingEvent.NavigateToHome)
        assertThat(viewModel.uiState.value.isCompleting).isFalse()
        assertThat(viewModel.uiState.value.hasCompletionError).isFalse()
      }
    }

  @Test
  fun repeatedCompletionWhileWriteIsRunningStartsOnlyOneWrite() =
    runTest(dispatcher) {
      repository.writeGate = CompletableDeferred()

      viewModel.completeOnboarding()
      viewModel.completeOnboarding()
      runCurrent()

      assertThat(repository.writeCalls).isEqualTo(1)
      assertThat(viewModel.uiState.value.isCompleting).isTrue()

      repository.writeGate?.complete(Unit)
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.isCompleting).isFalse()
    }

  @Test
  fun failedWriteExposesRecoverableErrorAndDoesNotNavigate() =
    runTest(dispatcher) {
      repository.failWrites = true
      viewModel.events.test {
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertThat(repository.writeCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.isCompleting).isFalse()
        assertThat(viewModel.uiState.value.hasCompletionError).isTrue()
        expectNoEvents()
      }
    }

  @Test
  fun retryClearsErrorAndNavigatesOnlyAfterSuccessfulPersistence() =
    runTest(dispatcher) {
      repository.failWrites = true
      viewModel.completeOnboarding()
      advanceUntilIdle()
      repository.failWrites = false

      viewModel.events.test {
        viewModel.completeOnboarding()
        assertThat(viewModel.uiState.value.hasCompletionError).isFalse()
        advanceUntilIdle()

        assertThat(repository.writeCalls).isEqualTo(2)
        assertThat(awaitItem()).isEqualTo(OnboardingEvent.NavigateToHome)
        assertThat(viewModel.uiState.value.hasCompletionError).isFalse()
      }
    }

  @Test
  fun cancellationIsPropagatedWithoutBecomingRecoverableError() =
    runTest(dispatcher) {
      repository.cancelWrites = true
      viewModel.events.test {
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertThat(repository.writeCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.isCompleting).isFalse()
        assertThat(viewModel.uiState.value.hasCompletionError).isFalse()
        expectNoEvents()
      }
    }

  private class ControllableUserPreferencesRepository : UserPreferencesRepository {
    override val userPreferences: Flow<UserPreferences> =
      MutableStateFlow(
        UserPreferences(
          theme = AppTheme.SYSTEM,
          language = AppLanguage.SYSTEM,
          hasCompletedOnboarding = false,
        )
      )
    var writeCalls = 0
    var failWrites = false
    var cancelWrites = false
    var writeGate: CompletableDeferred<Unit>? = null

    override suspend fun updateTheme(theme: AppTheme) = Unit

    override suspend fun updateLanguage(language: AppLanguage) = Unit

    override suspend fun setOnboardingCompleted() {
      writeCalls++
      writeGate?.await()
      if (cancelWrites) throw CancellationException("Preference write cancelled")
      if (failWrites) error("Preference write failed")
    }
  }
}
