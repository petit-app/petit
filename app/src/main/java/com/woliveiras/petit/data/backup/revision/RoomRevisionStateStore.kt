package com.woliveiras.petit.data.backup.revision

import com.woliveiras.petit.data.local.dao.RestorableRevisionDao
import com.woliveiras.petit.data.local.entity.RestorableRevisionEntity
import com.woliveiras.petit.domain.backup.revision.RestorableRevision
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionState
import com.woliveiras.petit.domain.backup.revision.RestorableRevisionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomRevisionStateStore @Inject constructor(private val dao: RestorableRevisionDao) :
  RestorableRevisionStore {
  override val state: Flow<RestorableRevisionState> = dao.observe().map { it.model() }

  override suspend fun read(): RestorableRevisionState = dao.read().model()

  override suspend fun advance(): RestorableRevisionState = dao.advanceAndRead().model()

  override suspend fun markCompleted(revision: RestorableRevision): RestorableRevisionState =
    dao.markCompletedAndRead(revision.value).model()

  private fun RestorableRevisionEntity.model() =
    RestorableRevisionState(
      current = RestorableRevision(currentRevision),
      completed = RestorableRevision(completedRevision),
    )
}
