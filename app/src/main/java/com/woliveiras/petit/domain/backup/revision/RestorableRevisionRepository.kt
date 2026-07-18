package com.woliveiras.petit.domain.backup.revision

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@JvmInline
value class RestorableRevision(val value: Long) : Comparable<RestorableRevision> {
  init {
    require(value >= 0) { "Revision cannot be negative" }
  }

  override fun compareTo(other: RestorableRevision): Int = value.compareTo(other.value)
}

data class RestorableRevisionState(
  val current: RestorableRevision = RestorableRevision(0),
  val completed: RestorableRevision = RestorableRevision(0),
)

enum class BackupMutationKind(val isRestorable: Boolean) {
  PET(true),
  PET_ASSET(true),
  WEIGHT(true),
  VACCINATION(true),
  DEWORMING(true),
  TASK(true),
  REMINDER_STATE(true),
  RESTORABLE_PREFERENCE(true),
  BACKUP_ATTEMPT(false),
  UPLOAD_PROGRESS(false),
  PROVIDER_STATE(false),
  AUTHORIZATION_STATE(false),
  WORK_MANAGER_BOOKKEEPING(false),
  CACHE(false),
  TEMPORARY_STAGING(false),
}

/** Adapter implemented by the same transactional store that owns restorable mutations. */
interface RestorableRevisionStore {
  val state: Flow<RestorableRevisionState>

  suspend fun read(): RestorableRevisionState

  suspend fun advance(): RestorableRevisionState

  suspend fun markCompleted(revision: RestorableRevision): RestorableRevisionState
}

interface RestorableRevisionRepository {
  val state: Flow<RestorableRevisionState>

  suspend fun recordCommittedMutation(kind: BackupMutationKind): RestorableRevision

  suspend fun markCompleted(revision: RestorableRevision): RestorableRevisionState
}

@Singleton
class TransactionalRestorableRevisionRepository
@Inject
constructor(private val store: RestorableRevisionStore) : RestorableRevisionRepository {
  override val state: Flow<RestorableRevisionState> = store.state

  override suspend fun recordCommittedMutation(kind: BackupMutationKind): RestorableRevision {
    if (!kind.isRestorable) return store.read().current
    return store.advance().current
  }

  override suspend fun markCompleted(revision: RestorableRevision): RestorableRevisionState {
    return store.markCompleted(revision)
  }
}
