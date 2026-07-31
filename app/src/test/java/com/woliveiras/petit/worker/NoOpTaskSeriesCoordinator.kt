package com.woliveiras.petit.worker

import com.woliveiras.petit.domain.model.Task

/** Test double for screens that do not exercise repeating series. */
class NoOpTaskSeriesCoordinator : TaskSeriesCoordinator {
  val completed = mutableListOf<String>()
  val stopped = mutableListOf<String>()
  var failCompletion = false
  var failStop = false

  override suspend fun completeOccurrence(taskId: String) {
    if (failCompletion) error("completion failed")
    completed += taskId
  }

  override suspend fun stopSeries(taskId: String) {
    if (failStop) error("stop failed")
    stopped += taskId
  }

  override suspend fun onNotificationDelivered(task: Task): Task = task

  override fun scheduleFollowUp(task: Task) = Unit

  override suspend fun reconcilePendingSeries() = Unit
}
