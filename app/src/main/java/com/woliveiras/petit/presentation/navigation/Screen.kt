package com.woliveiras.petit.presentation.navigation

import android.net.Uri

/** Navigation destinations for the app. */
sealed class Screen(val route: String) {

  /** Onboarding screen shown on first launch. */
  data object Onboarding : Screen("onboarding")

  /** Home dashboard screen. */
  data object Home : Screen("home")

  /** List of all pets. */
  data object PetList : Screen("pets")

  /** Pet detail/profile screen. */
  data object PetDetail : Screen("pets/{petId}") {
    fun createRoute(petId: String) = "pets/$petId"
  }

  /** Pet delete confirmation screen. */
  data object PetDeleteConfirmation : Screen("pets/{petId}/delete") {
    fun createRoute(petId: String) = "pets/$petId/delete"
  }

  /** Pet form for creating or editing. */
  data object PetForm : Screen("pets/form?petId={petId}") {
    fun createRoute(petId: String? = null) =
      if (petId != null) "pets/form?petId=$petId" else "pets/form"
  }

  /** Pet selection screen for actions (weight, vaccination, etc). */
  data object PetSelection : Screen("select-pet/{action}") {
    const val ACTION_WEIGHT = "weight"
    const val ACTION_ADD_WEIGHT = "add-weight"
    const val ACTION_VACCINATION = "vaccination"
    const val ACTION_DEWORMING = "deworming"

    fun createRoute(action: String) = "select-pet/$action"
  }

  /** Weight tracking screen. */
  data object WeightTracking : Screen("pets/{petId}/weight") {
    fun createRoute(petId: String) = "pets/$petId/weight"
  }

  /** Weight form for adding/editing a weight entry. */
  data object WeightForm : Screen("pets/{petId}/weight/form?entryId={entryId}") {
    fun createRoute(petId: String, entryId: String? = null) =
      if (entryId != null) "pets/$petId/weight/form?entryId=$entryId" else "pets/$petId/weight/form"
  }

  /** Vaccination records screen. */
  data object VaccinationRecords : Screen("pets/{petId}/vaccinations") {
    fun createRoute(petId: String) = "pets/$petId/vaccinations"
  }

  /** Vaccination form for adding/editing a vaccination entry. */
  data object VaccinationForm :
    Screen("pets/{petId}/vaccinations/form?entryId={entryId}&vaccineType={vaccineType}") {
    fun createRoute(petId: String, entryId: String? = null, vaccineType: String? = null): String {
      val base = "pets/$petId/vaccinations/form"
      val params = mutableListOf<String>()
      if (entryId != null) params.add("entryId=$entryId")
      if (vaccineType != null) params.add("vaccineType=$vaccineType")
      return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
    }
  }

  /** Deworming records screen. */
  data object DewormingRecords : Screen("pets/{petId}/deworming") {
    fun createRoute(petId: String) = "pets/$petId/deworming"
  }

  /** Deworming form for adding/editing a deworming entry. */
  data object DewormingForm : Screen("pets/{petId}/deworming/form?entryId={entryId}") {
    fun createRoute(petId: String, entryId: String? = null) =
      if (entryId != null) "pets/$petId/deworming/form?entryId=$entryId"
      else "pets/$petId/deworming/form"
  }

  /** Settings screen. */
  data object Settings : Screen("settings")

  /** Restore one exact provider backup ID. */
  data object RestoreBackup : Screen("settings/backups/{remoteId}/restore") {
    fun createRoute(remoteId: String) = "settings/backups/${Uri.encode(remoteId)}/restore"
  }

  /** Tasks list screen. */
  data object Tasks : Screen("tasks")

  /** Task form for adding/editing a standalone task. */
  data object TaskForm : Screen("tasks/form?taskId={taskId}") {
    fun createRoute(taskId: String? = null) =
      if (taskId != null) "tasks/form?taskId=$taskId" else "tasks/form"
  }

  /** Task settings screen. */
  data object TaskSettings : Screen("tasks/settings")

  /** Completed tasks screen. */
  data object CompletedTasks : Screen("tasks/completed")

  /** Quick add screen for selecting action type. */
  data object QuickAdd : Screen("quick-add")

  /** Delete all data confirmation screen. */
  data object DeleteAllData : Screen("settings/delete-all-data")

  /** Activity timeline history screen. */
  data object ActivityTimeline : Screen("activity-timeline")

  /** Family group management screen. */
  data object FamilyGroup : Screen("familygroup")

  /** Local synchronization audit history. */
  data object FamilyGroupSyncHistory : Screen("familygroup/sync-history")

  /** Pairing screen for connecting devices. */
  data object FamilyGroupPairing : Screen("familygroup/pairing")

  /** Transfer screen for sending/receiving data. */
  data object FamilyGroupTransfer : Screen("familygroup/transfer?mode={mode}") {
    const val MODE_SEND = "send"
    const val MODE_RECEIVE = "receive"

    fun createRoute(mode: String) = "familygroup/transfer?mode=$mode"
  }
}
