package com.woliveiras.petit.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ManualBackupHistoryRunnerTest {
  @Test
  fun retryResumesTheDurableIndeterminateBackupId() = runTest {
    val repository = MemoryAttemptRepository()
    val action = SequencedCreateBackupAction()
    action.results += BackupCreationResult.RetryableFailure("response lost")
    action.results += BackupCreationResult.Success(metadata("first-id"))
    val runner =
      ManualBackupHistoryRunner(
        action,
        repository,
        Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneOffset.UTC),
      )

    val first = runner.run("first-id")
    val retry = runner.run("newly-generated-id")

    assertThat(first).isEqualTo(BackupAttemptStatus.RETRYING)
    assertThat(retry).isEqualTo(BackupAttemptStatus.SUCCEEDED)
    assertThat(action.backupIds).containsExactly("first-id", "first-id").inOrder()
    assertThat(repository.current.map { it.id }).containsExactly("first-id")
  }

  private class MemoryAttemptRepository : BackupAttemptRepository {
    private val mutableAttempts = MutableStateFlow<List<BackupAttempt>>(emptyList())
    override val attempts: Flow<List<BackupAttempt>> = mutableAttempts
    val current: List<BackupAttempt>
      get() = mutableAttempts.value

    override suspend fun getAttempt(id: String): BackupAttempt? =
      current.firstOrNull { it.id == id }

    override suspend fun upsert(attempt: BackupAttempt) {
      mutableAttempts.value = current.filterNot { it.id == attempt.id } + attempt
    }
  }

  private class SequencedCreateBackupAction : CreateBackupAction {
    val results = ArrayDeque<BackupCreationResult>()
    val backupIds = mutableListOf<String>()

    override suspend fun execute(
      backupId: String,
      trigger: BackupTrigger,
      onProgress: (BackupProgress) -> Unit,
    ): BackupCreationResult {
      backupIds += backupId
      return results.removeFirst()
    }
  }

  private fun metadata(backupId: String) =
    BackupMetadata(
      remoteId = "remote-$backupId",
      backupId = backupId,
      createdAt = Instant.parse("2026-07-18T08:00:00Z"),
      trigger = BackupTrigger.MANUAL,
      appVersion = "1.0.0",
      archiveFormatVersion = 1,
      schemaVersion = 1,
      contentCounts = BackupContentCounts(),
      archiveSizeBytes = 1,
      archiveSha256 = "checksum",
    )
}
