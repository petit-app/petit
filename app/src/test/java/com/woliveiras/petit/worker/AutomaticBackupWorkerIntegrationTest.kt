package com.woliveiras.petit.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.backup.testing.ByteArrayBackupContent
import com.woliveiras.petit.data.backup.testing.DeterministicBackupStorageGateway
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupArchivePreparer
import com.woliveiras.petit.domain.usecase.backup.CreateManualBackupUseCase
import com.woliveiras.petit.domain.usecase.backup.PreparedBackupArchive
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AutomaticBackupWorkerIntegrationTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val now = Instant.parse("2026-07-18T08:00:00Z")
  private val clock = Clock.fixed(now, ZoneOffset.UTC)

  @Before
  fun clearOperationState() {
    context
      .getSharedPreferences(PeriodicBackupOperationIdStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit()
      .clear()
      .commit()
  }

  @Test
  fun workerReusesAnOperationForRetryAndRotatesItForTheNextPeriodicExecution() = runTest {
    val workId = UUID.fromString("5df6cd9d-bfc0-458d-b8b6-2949fffcdf63")
    val gateway = DeterministicBackupStorageGateway(uploadChunkSize = 2)
    val history = RecordingAttemptRepository()
    val runner = runner(gateway, history)
    val operationIds = ArrayDeque(listOf("period-1", "period-2"))
    val store = PeriodicBackupOperationIdStore(context) { operationIds.removeFirst() }

    val first = buildWorker(workId, runAttemptCount = 0, runner, store).doWork()
    val duplicateRetry = buildWorker(workId, runAttemptCount = 1, runner, store).doWork()
    val nextPeriod = buildWorker(workId, runAttemptCount = 0, runner, store).doWork()

    assertThat(first).isInstanceOf(ListenableWorker.Result.Success::class.java)
    assertThat(duplicateRetry).isInstanceOf(ListenableWorker.Result.Success::class.java)
    assertThat(nextPeriod).isInstanceOf(ListenableWorker.Result.Success::class.java)
    assertThat(gateway.completedBackups().map { it.backupId })
      .containsExactly("period-1", "period-2")
      .inOrder()
    assertThat(history.current.map { it.id }).containsExactly("period-1", "period-2")
    assertThat(history.current.map { it.status }.distinct())
      .containsExactly(BackupAttemptStatus.SUCCEEDED)
  }

  @Test
  fun authorizationRequiredFailsWithoutPreparingArchiveOrLaunchingUi() = runTest {
    val gateway =
      DeterministicBackupStorageGateway(
        initialAuthorization = BackupAuthorizationState.AuthorizationRequired
      )
    val history = RecordingAttemptRepository()
    var prepareCalls = 0
    val action =
      CreateManualBackupUseCase(
        gateway,
        gateway,
        object : BackupArchivePreparer {
          override suspend fun prepare(
            backupId: String,
            trigger: BackupTrigger,
          ): PreparedBackupArchive {
            prepareCalls += 1
            return prepared("unexpected")
          }
        },
      )
    val runner = AutomaticBackupRunner(action, history, clock)

    val result =
      buildWorker(
          UUID.randomUUID(),
          runAttemptCount = 0,
          runner,
          PeriodicBackupOperationIdStore(context) { "authorization-attempt" },
        )
        .doWork()

    assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    assertThat(prepareCalls).isEqualTo(0)
    assertThat(history.current.single().status)
      .isEqualTo(BackupAttemptStatus.AUTHORIZATION_REQUIRED)
    assertThat(gateway.state.value).isEqualTo(BackupAuthorizationState.AuthorizationRequired)
  }

  private fun runner(
    gateway: DeterministicBackupStorageGateway,
    history: BackupAttemptRepository,
  ): AutomaticBackupRunner =
    AutomaticBackupRunner(
      CreateManualBackupUseCase(
        gateway,
        gateway,
        object : BackupArchivePreparer {
          override suspend fun prepare(
            backupId: String,
            trigger: BackupTrigger,
          ): PreparedBackupArchive = prepared(backupId)
        },
      ),
      history,
      clock,
    )

  private fun prepared(backupId: String): PreparedBackupArchive {
    val bytes = byteArrayOf(1, 2, 3, 4)
    val backupMetadata =
      BackupMetadata(
        remoteId = "",
        backupId = backupId,
        createdAt = now,
        trigger = BackupTrigger.AUTOMATIC,
        appVersion = "1.0",
        archiveFormatVersion = 1,
        schemaVersion = 1,
        contentCounts = BackupContentCounts(pets = 1),
        archiveSizeBytes = bytes.size.toLong(),
        archiveSha256 = "checksum",
      )
    return object : PreparedBackupArchive {
      override val metadata = backupMetadata
      override val content = ByteArrayBackupContent(bytes)

      override fun close() = Unit
    }
  }

  private fun buildWorker(
    id: UUID,
    runAttemptCount: Int,
    runner: AutomaticBackupRunner,
    operationIds: PeriodicBackupOperationIdStore,
  ): AutomaticBackupWorker =
    TestListenableWorkerBuilder.from(context, AutomaticBackupWorker::class.java)
      .setId(id)
      .setRunAttemptCount(runAttemptCount)
      .setWorkerFactory(
        object : WorkerFactory() {
          override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
          ): ListenableWorker =
            AutomaticBackupWorker(appContext, workerParameters, runner, operationIds)
        }
      )
      .build()

  private class RecordingAttemptRepository : BackupAttemptRepository {
    private val state = MutableStateFlow<List<BackupAttempt>>(emptyList())
    val current: List<BackupAttempt>
      get() = state.value

    override val attempts: Flow<List<BackupAttempt>> = state

    override suspend fun getAttempt(id: String): BackupAttempt? =
      state.value.firstOrNull { it.id == id }

    override suspend fun upsert(attempt: BackupAttempt) {
      state.value = state.value.filterNot { it.id == attempt.id } + attempt
    }
  }
}
