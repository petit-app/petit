package com.woliveiras.petit.presentation.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.usecase.backup.BackupConnectionController
import com.woliveiras.petit.domain.usecase.backup.BackupConnectionCoordinator
import com.woliveiras.petit.domain.usecase.backup.BackupDeletionResult
import com.woliveiras.petit.domain.usecase.backup.ManageBackupsUseCase
import com.woliveiras.petit.domain.usecase.backup.SavedBackupCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedBackupsUiState(
  val authorization: BackupAuthorizationState = BackupAuthorizationState.Disconnected,
  val content: SavedBackupsContent = SavedBackupsContent.Loading,
  val selectedRemoteIds: Set<String> = emptySet(),
  val details: BackupMetadata? = null,
  val deletionConfirmation: BackupDeletionConfirmation? = null,
)

sealed interface SavedBackupsContent {
  data object Loading : SavedBackupsContent

  data object AuthorizationRequired : SavedBackupsContent

  data class Unavailable(val reason: String?) : SavedBackupsContent

  data object Empty : SavedBackupsContent

  data class Ready(val collection: SavedBackupCollection) : SavedBackupsContent

  data class PartialDeletion(
    val collection: SavedBackupCollection,
    val result: BackupDeletionResult,
  ) : SavedBackupsContent

  data class Error(val category: SavedBackupsErrorCategory) : SavedBackupsContent
}

enum class SavedBackupsErrorCategory {
  AUTHORIZATION_REQUIRED,
  QUOTA_EXCEEDED,
  RETRYABLE,
  PERMANENT,
}

sealed interface BackupDeletionConfirmation {
  val remoteIds: Set<String>

  data class Selected(override val remoteIds: Set<String>) : BackupDeletionConfirmation

  data class All(override val remoteIds: Set<String>) : BackupDeletionConfirmation
}

@HiltViewModel
class SavedBackupsViewModel
internal constructor(
  private val authorizationGateway: BackupAuthorizationGateway,
  private val manageBackups: ManageBackupsUseCase,
  private val connectionController: BackupConnectionController,
) : ViewModel() {
  @Inject
  constructor(
    authorizationGateway: BackupAuthorizationGateway,
    manageBackups: ManageBackupsUseCase,
    connectionCoordinator: BackupConnectionCoordinator,
  ) : this(authorizationGateway, manageBackups, connectionCoordinator as BackupConnectionController)

  internal constructor(
    authorizationGateway: BackupAuthorizationGateway,
    manageBackups: ManageBackupsUseCase,
  ) : this(
    authorizationGateway,
    manageBackups,
    DirectBackupConnectionController(authorizationGateway),
  )

  private val mutableUiState =
    MutableStateFlow(SavedBackupsUiState(authorization = authorizationGateway.state.value))
  val uiState: StateFlow<SavedBackupsUiState> = mutableUiState.asStateFlow()

  private var loadJob: Job? = null

  init {
    viewModelScope.launch {
      authorizationGateway.state.collect { authorization ->
        mutableUiState.update { it.copy(authorization = authorization) }
        when (authorization) {
          is BackupAuthorizationState.Authorized -> refresh()
          BackupAuthorizationState.Authorizing -> Unit
          BackupAuthorizationState.Disconnected,
          BackupAuthorizationState.AuthorizationRequired ->
            mutableUiState.update {
              it.copy(
                content = SavedBackupsContent.AuthorizationRequired,
                selectedRemoteIds = emptySet(),
                details = null,
                deletionConfirmation = null,
              )
            }
          is BackupAuthorizationState.Unavailable ->
            mutableUiState.update {
              it.copy(
                content = SavedBackupsContent.Unavailable(authorization.reason),
                selectedRemoteIds = emptySet(),
                details = null,
                deletionConfirmation = null,
              )
            }
        }
      }
    }
  }

  fun refresh() {
    if (authorizationGateway.state.value !is BackupAuthorizationState.Authorized) {
      mutableUiState.update { it.copy(content = SavedBackupsContent.AuthorizationRequired) }
      return
    }
    loadJob?.cancel()
    loadJob =
      viewModelScope.launch {
        mutableUiState.update { it.copy(content = SavedBackupsContent.Loading) }
        loadCollection()
      }
  }

  fun authorizeAndRefresh() {
    viewModelScope.launch {
      when (val result = connectionController.authorize()) {
        BackupAuthorizationResult.Authorized -> refresh()
        BackupAuthorizationResult.Cancelled ->
          mutableUiState.update { it.copy(content = SavedBackupsContent.AuthorizationRequired) }
        is BackupAuthorizationResult.Unavailable ->
          mutableUiState.update {
            it.copy(content = SavedBackupsContent.Unavailable(result.reason))
          }
      }
    }
  }

  fun disconnect() {
    viewModelScope.launch { connectionController.disconnect() }
  }

  fun toggleSelection(remoteId: String) {
    val available = currentCollection()?.backups?.any { it.remoteId == remoteId } == true
    if (!available) return
    mutableUiState.update { state ->
      val selected = state.selectedRemoteIds.toMutableSet()
      if (!selected.add(remoteId)) selected.remove(remoteId)
      state.copy(selectedRemoteIds = selected)
    }
  }

  fun showDetails(remoteId: String) {
    viewModelScope.launch {
      runCatching { manageBackups.details(remoteId) }
        .onSuccess { details -> mutableUiState.update { it.copy(details = details) } }
        .onFailure(::showErrorUnlessCancelled)
    }
  }

  fun dismissDetails() {
    mutableUiState.update { it.copy(details = null) }
  }

  fun requestDeleteSelected() {
    val ids = mutableUiState.value.selectedRemoteIds
    if (ids.isNotEmpty()) {
      mutableUiState.update {
        it.copy(deletionConfirmation = BackupDeletionConfirmation.Selected(ids))
      }
    }
  }

  fun requestDeleteOne(remoteId: String) {
    if (currentCollection()?.backups?.any { it.remoteId == remoteId } == true) {
      mutableUiState.update {
        it.copy(
          details = null,
          deletionConfirmation = BackupDeletionConfirmation.Selected(setOf(remoteId)),
        )
      }
    }
  }

  fun requestDeleteAll() {
    val ids = currentCollection()?.backups?.mapTo(linkedSetOf(), BackupMetadata::remoteId).orEmpty()
    if (ids.isNotEmpty()) {
      mutableUiState.update { it.copy(deletionConfirmation = BackupDeletionConfirmation.All(ids)) }
    }
  }

  fun dismissDeletion() {
    mutableUiState.update { it.copy(deletionConfirmation = null) }
  }

  fun confirmDeletion() {
    val confirmation = mutableUiState.value.deletionConfirmation ?: return
    mutableUiState.update { it.copy(deletionConfirmation = null) }
    deleteIds(confirmation.remoteIds)
  }

  fun retryFailedDeletion() {
    val partial = mutableUiState.value.content as? SavedBackupsContent.PartialDeletion ?: return
    viewModelScope.launch {
      mutableUiState.update { it.copy(content = SavedBackupsContent.Loading) }
      try {
        val retry = manageBackups.retryDeletion(partial.result)
        showDeletionResult(retry)
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Exception) {
        showError(error)
      }
    }
  }

  private fun deleteIds(remoteIds: Set<String>) {
    viewModelScope.launch {
      mutableUiState.update { it.copy(content = SavedBackupsContent.Loading) }
      try {
        showDeletionResult(manageBackups.deleteSelected(remoteIds))
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Exception) {
        showError(error)
      }
    }
  }

  private suspend fun showDeletionResult(result: BackupDeletionResult) {
    val collection = manageBackups.listAll()
    mutableUiState.update { state ->
      state.copy(
        content =
          if (result.failures.isEmpty()) {
            if (collection.backups.isEmpty()) SavedBackupsContent.Empty
            else SavedBackupsContent.Ready(collection)
          } else {
            SavedBackupsContent.PartialDeletion(collection, result)
          },
        selectedRemoteIds = result.failures.mapTo(linkedSetOf()) { it.remoteId },
        details =
          state.details?.takeIf { detail ->
            collection.backups.any { it.remoteId == detail.remoteId }
          },
      )
    }
  }

  private suspend fun loadCollection() {
    try {
      val collection = manageBackups.listAll()
      mutableUiState.update { state ->
        state.copy(
          content =
            if (collection.backups.isEmpty()) SavedBackupsContent.Empty
            else SavedBackupsContent.Ready(collection),
          selectedRemoteIds =
            state.selectedRemoteIds.intersect(collection.backups.map { it.remoteId }.toSet()),
        )
      }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Exception) {
      showError(error)
    }
  }

  private fun currentCollection(): SavedBackupCollection? =
    when (val content = mutableUiState.value.content) {
      is SavedBackupsContent.Ready -> content.collection
      is SavedBackupsContent.PartialDeletion -> content.collection
      else -> null
    }

  private fun showErrorUnlessCancelled(error: Throwable) {
    if (error is CancellationException) throw error
    showError(error)
  }

  private fun showError(error: Throwable) {
    if (error is BackupProviderException.AuthorizationRequired) {
      mutableUiState.update {
        it.copy(
          content = SavedBackupsContent.AuthorizationRequired,
          selectedRemoteIds = emptySet(),
          details = null,
          deletionConfirmation = null,
        )
      }
      return
    }
    val category =
      when (error) {
        is BackupProviderException.QuotaExceeded -> SavedBackupsErrorCategory.QUOTA_EXCEEDED
        is BackupProviderException.Retryable -> SavedBackupsErrorCategory.RETRYABLE
        else -> SavedBackupsErrorCategory.PERMANENT
      }
    mutableUiState.update { it.copy(content = SavedBackupsContent.Error(category)) }
  }
}

private class DirectBackupConnectionController(
  private val authorizationGateway: BackupAuthorizationGateway
) : BackupConnectionController {
  override suspend fun refresh(): BackupAuthorizationState = authorizationGateway.refresh()

  override suspend fun authorize(): BackupAuthorizationResult = authorizationGateway.authorize()

  override suspend fun disconnect() = authorizationGateway.disconnect()
}
