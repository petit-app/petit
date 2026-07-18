package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.woliveiras.petit.data.local.entity.RestorableRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestorableRevisionDao {
  @Query("SELECT * FROM restorable_revision WHERE id = 0")
  fun observe(): Flow<RestorableRevisionEntity>

  @Query("SELECT * FROM restorable_revision WHERE id = 0")
  suspend fun read(): RestorableRevisionEntity

  @Query("UPDATE restorable_revision SET currentRevision = currentRevision + 1 WHERE id = 0")
  suspend fun advance()

  @Query(
    """
    UPDATE restorable_revision
    SET completedRevision = MAX(completedRevision, :revision)
    WHERE id = 0 AND :revision <= currentRevision
    """
  )
  suspend fun markCompletedInternal(revision: Long): Int

  @Transaction
  suspend fun advanceAndRead(): RestorableRevisionEntity {
    advance()
    return read()
  }

  @Transaction
  suspend fun markCompletedAndRead(revision: Long): RestorableRevisionEntity {
    check(markCompletedInternal(revision) == 1) { "Cannot complete an uncommitted revision" }
    return read()
  }
}
