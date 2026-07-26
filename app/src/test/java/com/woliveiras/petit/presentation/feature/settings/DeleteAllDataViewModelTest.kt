package com.woliveiras.petit.presentation.feature.settings

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.usecase.DeleteAllDataAction
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DeleteAllDataViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var action: FakeDeleteAllDataAction
  private lateinit var viewModel: DeleteAllDataViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    action = FakeDeleteAllDataAction()
    viewModel = DeleteAllDataViewModel(context, action)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun mismatchedConfirmationDoesNotStartDeletion() =
    runTest(dispatcher) {
      viewModel.updateConfirmText("delete")

      viewModel.deleteAllData("DELETE")
      advanceUntilIdle()

      assertThat(action.calls).isEqualTo(0)
      assertThat(viewModel.uiState.value.isDeleting).isFalse()
    }

  @Test
  fun successfulDeletionShowsSuccessOnlyAfterActionCompletes() =
    runTest(dispatcher) {
      viewModel.updateConfirmText("DELETE")
      action.nextResult = Result.success(Unit)

      viewModel.deleteAllData("DELETE")
      advanceUntilIdle()

      assertThat(action.calls).isEqualTo(1)
      assertThat(viewModel.uiState.value.isDeleted).isTrue()
      assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

  @Test
  fun failureStaysOnConfirmationAndRetryCanSucceed() =
    runTest(dispatcher) {
      viewModel.updateConfirmText("DELETE")
      action.results += Result.failure(IllegalStateException("Could not clear data"))
      action.results += Result.success(Unit)

      viewModel.deleteAllData("DELETE")
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.isDeleted).isFalse()
      assertThat(viewModel.uiState.value.isDeleting).isFalse()
      assertThat(viewModel.uiState.value.errorMessage)
        .isEqualTo(context.getString(R.string.error_unknown))

      viewModel.deleteAllData("DELETE")
      advanceUntilIdle()

      assertThat(action.calls).isEqualTo(2)
      assertThat(viewModel.uiState.value.isDeleted).isTrue()
      assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

  @Test
  fun duplicateSubmissionsAreIgnoredWhileDeletionIsRunning() =
    runTest(dispatcher) {
      viewModel.updateConfirmText("DELETE")
      action.suspendNextCall = true

      viewModel.deleteAllData("DELETE")
      advanceUntilIdle()
      viewModel.deleteAllData("DELETE")
      advanceUntilIdle()

      assertThat(action.calls).isEqualTo(1)
      assertThat(viewModel.uiState.value.isDeleting).isTrue()

      action.completeWith(Result.success(Unit))
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.isDeleted).isTrue()
    }

  @Test
  fun failuresUseLocalizedGenericErrorInsteadOfExceptionMessage() =
    runTest(dispatcher) {
      for (locale in listOf(Locale.ENGLISH, Locale.forLanguageTag("pt-BR"))) {
        val localizedContext = localizedContext(locale)
        val localizedAction =
          FakeDeleteAllDataAction().apply {
            nextResult = Result.failure(IllegalStateException("Provider detail: permission denied"))
          }
        val localizedViewModel = DeleteAllDataViewModel(localizedContext, localizedAction)

        localizedViewModel.updateConfirmText("DELETE")
        localizedViewModel.deleteAllData("DELETE")
        advanceUntilIdle()

        assertThat(localizedViewModel.uiState.value.errorMessage)
          .isEqualTo(localizedContext.getString(R.string.error_unknown))
      }
    }

  private fun localizedContext(locale: Locale): Context {
    val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
    return context.createConfigurationContext(configuration)
  }

  private class FakeDeleteAllDataAction : DeleteAllDataAction {
    var calls = 0
    var nextResult: Result<Unit> = Result.success(Unit)
    var suspendNextCall = false
    val results = ArrayDeque<Result<Unit>>()
    private var pending: kotlinx.coroutines.CompletableDeferred<Result<Unit>>? = null

    override suspend fun execute(): Result<Unit> {
      calls += 1
      if (suspendNextCall) {
        suspendNextCall = false
        return kotlinx.coroutines.CompletableDeferred<Result<Unit>>().also { pending = it }.await()
      }
      return results.removeFirstOrNull() ?: nextResult
    }

    fun completeWith(result: Result<Unit>) {
      pending?.complete(result)
    }
  }
}
