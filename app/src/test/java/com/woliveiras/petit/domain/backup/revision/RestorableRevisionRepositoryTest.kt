package com.woliveiras.petit.domain.backup.revision

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RestorableRevisionRepositoryTest {
  @Test
  fun everyRestorableMutationAdvancesWhileBookkeepingCannotAdvance() = runTest {
    val store = MemoryRevisionStateStore()
    val repository = TransactionalRestorableRevisionRepository(store)

    BackupMutationKind.entries
      .filter { it.isRestorable }
      .forEach { repository.recordCommittedMutation(it) }
    val afterRestorable = store.current.current
    BackupMutationKind.entries
      .filterNot { it.isRestorable }
      .forEach { repository.recordCommittedMutation(it) }

    assertThat(afterRestorable.value)
      .isEqualTo(BackupMutationKind.entries.count { it.isRestorable }.toLong())
    assertThat(store.current.current).isEqualTo(afterRestorable)
  }

  @Test
  fun completionWatermarkIsMonotonicAndCannotCoverAnUncommittedRevision() = runTest {
    val store = MemoryRevisionStateStore()
    val repository = TransactionalRestorableRevisionRepository(store)
    repeat(3) { repository.recordCommittedMutation(BackupMutationKind.PET) }

    repository.markCompleted(RestorableRevision(2))
    repository.markCompleted(RestorableRevision(1))

    assertThat(store.current.completed).isEqualTo(RestorableRevision(2))
    assertThat(runCatching { repository.markCompleted(RestorableRevision(4)) }.isFailure).isTrue()
  }

  private class MemoryRevisionStateStore : RestorableRevisionStore {
    private val mutableState = MutableStateFlow(RestorableRevisionState())
    val current: RestorableRevisionState
      get() = mutableState.value

    override val state: Flow<RestorableRevisionState> = mutableState

    override suspend fun read(): RestorableRevisionState = mutableState.value

    override suspend fun advance(): RestorableRevisionState {
      mutableState.value =
        mutableState.value.copy(current = RestorableRevision(mutableState.value.current.value + 1))
      return mutableState.value
    }

    override suspend fun markCompleted(revision: RestorableRevision): RestorableRevisionState {
      require(revision <= mutableState.value.current) { "Cannot complete an uncommitted revision" }
      mutableState.value =
        mutableState.value.copy(completed = maxOf(mutableState.value.completed, revision))
      return mutableState.value
    }
  }
}
