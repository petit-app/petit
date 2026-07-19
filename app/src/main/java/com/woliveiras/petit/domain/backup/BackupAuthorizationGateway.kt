package com.woliveiras.petit.domain.backup

import kotlinx.coroutines.flow.StateFlow

/** Provider-neutral authorization state. Interactive consent stays in the foreground adapter. */
interface BackupAuthorizationGateway {
  val state: StateFlow<BackupAuthorizationState>

  /**
   * Refreshes authorization without launching user interaction. Background callers use this before
   * preparing sensitive backup content. Provider-neutral fakes may return their current state.
   */
  suspend fun refresh(): BackupAuthorizationState = state.value

  /** Starts provider consent. Foreground callers only; workers must inspect [state] instead. */
  suspend fun authorize(): BackupAuthorizationResult

  /** Revokes local access without deleting any remote backup. */
  suspend fun disconnect()
}
