package com.woliveiras.petit.presentation.feature.backup

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupCreationResult
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import java.io.File
import java.time.Instant
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManualBackupViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun foregroundAuthorizationStartsBackupWithoutAnyAccountOrPlanInput() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization()
      val action = FakeCreateBackupAction()
      val viewModel = ManualBackupViewModel(context, authorization, action) { "backup-1" }

      viewModel.authorizeAndBackUp()
      advanceUntilIdle()

      assertThat(authorization.authorizeCalls).isEqualTo(1)
      assertThat(action.calls).containsExactly("backup-1")
      assertThat(viewModel.uiState.value.operation)
        .isInstanceOf(ManualBackupOperation.Complete::class.java)
    }

  @Test
  fun retryableFailureReusesBackupIdAndProgressIsExposed() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
      val action = FakeCreateBackupAction()
      action.results += BackupCreationResult.RetryableFailure("offline")
      action.results += BackupCreationResult.Success(metadata("backup-1"))
      val viewModel = ManualBackupViewModel(context, authorization, action) { "backup-1" }

      viewModel.backUpNow()
      advanceUntilIdle()
      viewModel.backUpNow()
      advanceUntilIdle()

      assertThat(action.calls).containsExactly("backup-1", "backup-1").inOrder()
      assertThat(action.progressCallbacks).isEqualTo(2)
      assertThat(viewModel.uiState.value.operation)
        .isInstanceOf(ManualBackupOperation.Complete::class.java)
    }

  @Test
  fun cancelledAuthorizationRemainsActionable() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization()
      authorization.nextResult = BackupAuthorizationResult.Cancelled
      val action = FakeCreateBackupAction()
      val viewModel = ManualBackupViewModel(context, authorization, action)

      viewModel.authorizeAndBackUp()
      advanceUntilIdle()

      assertThat(action.calls).isEmpty()
      assertThat(viewModel.uiState.value.operation)
        .isEqualTo(ManualBackupOperation.AuthorizationRequired)
    }

  @Test
  fun revokedAuthorizationAfterCompletionReturnsToAnActionableState() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
      val viewModel =
        ManualBackupViewModel(context, authorization, FakeCreateBackupAction()) { "backup-1" }
      viewModel.backUpNow()
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.operation)
        .isInstanceOf(ManualBackupOperation.Complete::class.java)

      authorization.setState(BackupAuthorizationState.AuthorizationRequired)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.operation)
        .isEqualTo(ManualBackupOperation.AuthorizationRequired)
    }

  @Test
  fun unexpectedActionFailureUsesPortugueseAppCopyWithoutExposingDiagnosticsOrLiteralFallback() =
    runTest(dispatcher) {
      val portugueseContext = context.forLocale(Locale.forLanguageTag("pt-BR"))
      val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
      val action = FakeCreateBackupAction()
      action.failure = IllegalStateException("private path")
      val viewModel = ManualBackupViewModel(portugueseContext, authorization, action) { "backup-1" }

      viewModel.backUpNow()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.operation)
        .isEqualTo(
          ManualBackupOperation.PermanentFailure(
            portugueseContext.getString(R.string.backup_permanent_error)
          )
        )
      assertThat(viewModel.uiState.value.operation.toString()).doesNotContain("private path")
      assertThat(
          File(
              "src/main/java/com/woliveiras/petit/presentation/feature/backup/ManualBackupViewModel.kt"
            )
            .readText()
        )
        .doesNotContain("\"Backup could not be completed\"")
    }

  @Test
  fun cancellationReturnsToIdleInsteadOfBecomingLocalizedFailure() =
    runTest(dispatcher) {
      val authorization = FakeAuthorization(BackupAuthorizationState.Authorized())
      val action = FakeCreateBackupAction()
      action.failure = CancellationException("cancelled")
      val viewModel = ManualBackupViewModel(context, authorization, action) { "backup-1" }

      viewModel.backUpNow()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.operation).isEqualTo(ManualBackupOperation.Idle)
    }

  private fun Context.forLocale(locale: Locale): Context {
    val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(configuration)
  }

  private class FakeAuthorization(
    initial: BackupAuthorizationState = BackupAuthorizationState.AuthorizationRequired
  ) : BackupAuthorizationGateway {
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<BackupAuthorizationState> = mutableState
    var authorizeCalls = 0
    var nextResult: BackupAuthorizationResult = BackupAuthorizationResult.Authorized

    override suspend fun authorize(): BackupAuthorizationResult {
      authorizeCalls += 1
      if (nextResult == BackupAuthorizationResult.Authorized) {
        mutableState.value = BackupAuthorizationState.Authorized()
      }
      return nextResult
    }

    override suspend fun disconnect() {
      mutableState.value = BackupAuthorizationState.Disconnected
    }

    fun setState(state: BackupAuthorizationState) {
      mutableState.value = state
    }
  }

  private class FakeCreateBackupAction : CreateBackupAction {
    val calls = mutableListOf<String>()
    val results = ArrayDeque<BackupCreationResult>()
    var progressCallbacks = 0
    var failure: Exception? = null

    override suspend fun execute(
      backupId: String,
      trigger: BackupTrigger,
      onProgress: (BackupProgress) -> Unit,
    ): BackupCreationResult {
      calls += backupId
      failure?.let { throw it }
      onProgress(BackupProgress(1, 1))
      progressCallbacks += 1
      return results.removeFirstOrNull() ?: BackupCreationResult.Success(metadata(backupId))
    }
  }

  companion object {
    private fun metadata(backupId: String) =
      BackupMetadata(
        remoteId = "remote-$backupId",
        backupId = backupId,
        createdAt = Instant.parse("2026-07-18T10:00:00Z"),
        trigger = BackupTrigger.MANUAL,
        appVersion = "1.0.0",
        archiveFormatVersion = 1,
        schemaVersion = 1,
        contentCounts = BackupContentCounts(pets = 1),
        archiveSizeBytes = 1,
        archiveSha256 = "sha256",
      )
  }
}
