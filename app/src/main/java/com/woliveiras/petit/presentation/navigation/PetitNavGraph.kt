package com.woliveiras.petit.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woliveiras.petit.presentation.feature.deworming.DewormingFormScreen
import com.woliveiras.petit.presentation.feature.deworming.DewormingRecordsScreen
import com.woliveiras.petit.presentation.feature.familygroup.FamilyGroupScreen
import com.woliveiras.petit.presentation.feature.familygroup.PairingScreen
import com.woliveiras.petit.presentation.feature.familygroup.TransferScreen
import com.woliveiras.petit.presentation.feature.home.HomeScreen
import com.woliveiras.petit.presentation.feature.onboarding.OnboardingScreen
import com.woliveiras.petit.presentation.feature.pets.PetDeleteConfirmationScreen
import com.woliveiras.petit.presentation.feature.pets.PetDetailScreen
import com.woliveiras.petit.presentation.feature.pets.PetFormScreen
import com.woliveiras.petit.presentation.feature.pets.PetListScreen
import com.woliveiras.petit.presentation.feature.pets.PetSelectionScreen
import com.woliveiras.petit.presentation.feature.quickadd.QuickAddScreen
import com.woliveiras.petit.presentation.feature.settings.SettingsScreen
import com.woliveiras.petit.presentation.feature.tasks.CompletedTasksScreen
import com.woliveiras.petit.presentation.feature.tasks.TaskFormScreen
import com.woliveiras.petit.presentation.feature.tasks.TaskListScreen
import com.woliveiras.petit.presentation.feature.tasks.TaskSettingsScreen
import com.woliveiras.petit.presentation.feature.timeline.ActivityTimelineScreen
import com.woliveiras.petit.presentation.feature.vaccination.VaccinationFormScreen
import com.woliveiras.petit.presentation.feature.vaccination.VaccinationRecordsScreen
import com.woliveiras.petit.presentation.feature.weight.WeightEntryScreen
import com.woliveiras.petit.presentation.feature.weight.WeightFormScreen

/** Main navigation graph for the app. */
@Composable
fun PetitNavGraph(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  startDestination: String = Screen.Home.route,
) {
  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    // Onboarding
    composable(Screen.Onboarding.route) {
      OnboardingScreen(
        onFinished = {
          navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Onboarding.route) { inclusive = true }
          }
        }
      )
    }

    // Home Dashboard
    composable(Screen.Home.route) {
      HomeScreen(
        onNavigateToPetList = { navController.navigate(Screen.PetList.route) },
        onNavigateToPetDetail = { petId ->
          navController.navigate(Screen.PetDetail.createRoute(petId))
        },
        onNavigateToAddPet = { navController.navigate(Screen.PetForm.createRoute()) },
        onNavigateToVaccinationForm = { petId, entryId ->
          navController.navigate(Screen.VaccinationForm.createRoute(petId, entryId))
        },
        onNavigateToDewormingForm = { petId, entryId ->
          navController.navigate(Screen.DewormingForm.createRoute(petId, entryId))
        },
        onNavigateToWeightForm = { petId, entryId ->
          navController.navigate(Screen.WeightForm.createRoute(petId, entryId))
        },
        onNavigateToTaskForm = { taskId ->
          navController.navigate(Screen.TaskForm.createRoute(taskId))
        },
        onNavigateToTaskList = { navController.navigate(Screen.Tasks.route) },
        onNavigateToActivityTimeline = { navController.navigate(Screen.ActivityTimeline.route) },
      )
    }

    // Pet Selection (for choosing a pet before an action)
    composable(
      route = Screen.PetSelection.route,
      arguments = listOf(navArgument("action") { type = NavType.StringType }),
    ) { backStackEntry ->
      val action = backStackEntry.arguments?.getString("action") ?: return@composable
      PetSelectionScreen(
        action = action,
        onNavigateBack = { navController.popBackStack() },
        onPetSelected = { petId ->
          navController.popBackStack()
          when (action) {
            Screen.PetSelection.ACTION_WEIGHT ->
              navController.navigate(Screen.WeightTracking.createRoute(petId))
            Screen.PetSelection.ACTION_ADD_WEIGHT ->
              navController.navigate(Screen.WeightForm.createRoute(petId))
            Screen.PetSelection.ACTION_VACCINATION ->
              navController.navigate(Screen.VaccinationRecords.createRoute(petId))
            Screen.PetSelection.ACTION_DEWORMING ->
              navController.navigate(Screen.DewormingRecords.createRoute(petId))
          }
        },
        onNavigateToAddPet = {
          navController.popBackStack()
          navController.navigate(Screen.PetForm.createRoute())
        },
      )
    }

    // Pet List
    composable(Screen.PetList.route) {
      PetListScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToPetDetail = { petId ->
          navController.navigate(Screen.PetDetail.createRoute(petId))
        },
        onNavigateToAddPet = { navController.navigate(Screen.PetForm.createRoute()) },
      )
    }

    // Pet Detail
    composable(
      route = Screen.PetDetail.route,
      arguments = listOf(navArgument("petId") { type = NavType.StringType }),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
      PetDetailScreen(
        petId = petId,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToEdit = { navController.navigate(Screen.PetForm.createRoute(petId)) },
        onNavigateToWeight = { navController.navigate(Screen.WeightTracking.createRoute(petId)) },
        onNavigateToVaccinations = {
          navController.navigate(Screen.VaccinationRecords.createRoute(petId))
        },
        onNavigateToDeworming = {
          navController.navigate(Screen.DewormingRecords.createRoute(petId))
        },
        onNavigateToDelete = {
          navController.navigate(Screen.PetDeleteConfirmation.createRoute(petId))
        },
      )
    }

    // Pet Delete Confirmation
    composable(
      route = Screen.PetDeleteConfirmation.route,
      arguments = listOf(navArgument("petId") { type = NavType.StringType }),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
      PetDeleteConfirmationScreen(
        petId = petId,
        onNavigateBack = { navController.popBackStack() },
        onPetDeleted = { navController.popBackStack(Screen.Home.route, inclusive = false) },
      )
    }

    // Pet Form (Create/Edit)
    composable(
      route = Screen.PetForm.route,
      arguments =
        listOf(
          navArgument("petId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
          }
        ),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId")
      PetFormScreen(
        petId = petId,
        onNavigateBack = { navController.popBackStack() },
        onPetSaved = { savedPetId ->
          navController.popBackStack()
          if (petId == null) {
            navController.navigate(Screen.PetDetail.createRoute(savedPetId))
          }
        },
      )
    }

    // Weight Tracking (from pet detail)
    composable(
      route = Screen.WeightTracking.route,
      arguments = listOf(navArgument("petId") { type = NavType.StringType }),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
      WeightFormScreen(
        petId = petId,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToAddEntry = { navController.navigate(Screen.WeightForm.createRoute(petId)) },
        onNavigateToEditEntry = { entryId ->
          navController.navigate(Screen.WeightForm.createRoute(petId, entryId))
        },
      )
    }

    // Weight Entry Form (Add/Edit)
    composable(
      route = Screen.WeightForm.route,
      arguments =
        listOf(
          navArgument("petId") { type = NavType.StringType },
          navArgument("entryId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
          },
        ),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
      val entryId = backStackEntry.arguments?.getString("entryId")
      WeightEntryScreen(
        petId = petId,
        entryId = entryId,
        onNavigateBack = { navController.popBackStack() },
      )
    }

    // Vaccination Records
    composable(
      route = Screen.VaccinationRecords.route,
      arguments = listOf(navArgument("petId") { type = NavType.StringType }),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
      VaccinationRecordsScreen(
        petId = petId,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToAddEntry = {
          navController.navigate(Screen.VaccinationForm.createRoute(petId))
        },
        onNavigateToEditEntry = { entryId ->
          navController.navigate(Screen.VaccinationForm.createRoute(petId, entryId))
        },
      )
    }

    // Vaccination Form (Add/Edit)
    composable(
      route = Screen.VaccinationForm.route,
      arguments =
        listOf(
          navArgument("petId") { type = NavType.StringType },
          navArgument("entryId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
          },
          navArgument("vaccineType") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
          },
        ),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
      val entryId = backStackEntry.arguments?.getString("entryId")
      val vaccineType = backStackEntry.arguments?.getString("vaccineType")
      VaccinationFormScreen(
        petId = petId,
        entryId = entryId,
        preselectedVaccineType = vaccineType,
        onNavigateBack = { navController.popBackStack() },
      )
    }

    // Deworming Records
    composable(
      route = Screen.DewormingRecords.route,
      arguments = listOf(navArgument("petId") { type = NavType.StringType }),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
      DewormingRecordsScreen(
        petId = petId,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToAddEntry = { navController.navigate(Screen.DewormingForm.createRoute(petId)) },
        onNavigateToEditEntry = { entryId ->
          navController.navigate(Screen.DewormingForm.createRoute(petId, entryId))
        },
      )
    }

    // Deworming Form
    composable(
      route = Screen.DewormingForm.route,
      arguments =
        listOf(
          navArgument("petId") { type = NavType.StringType },
          navArgument("entryId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
          },
        ),
    ) { backStackEntry ->
      val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
      val entryId = backStackEntry.arguments?.getString("entryId")
      DewormingFormScreen(
        petId = petId,
        entryId = entryId,
        onNavigateBack = { navController.popBackStack() },
      )
    }

    // Settings
    composable(Screen.Settings.route) {
      SettingsScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToDeleteAllData = { navController.navigate(Screen.DeleteAllData.route) },
        onNavigateToFamilyGroup = { navController.navigate(Screen.FamilyGroup.route) },
        onNavigateToPairing = { navController.navigate(Screen.FamilyGroupPairing.route) },
      )
    }

    // Delete All Data
    composable(Screen.DeleteAllData.route) {
      com.woliveiras.petit.presentation.feature.settings.DeleteAllDataScreen(
        onNavigateBack = { navController.popBackStack() },
        onDataDeleted = { navController.popBackStack(Screen.Home.route, inclusive = false) },
      )
    }

    // Family Group
    composable(Screen.FamilyGroup.route) {
      FamilyGroupScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToPairing = { navController.navigate(Screen.FamilyGroupPairing.route) },
        onNavigateToSend = {
          navController.navigate(
            Screen.FamilyGroupTransfer.createRoute(Screen.FamilyGroupTransfer.MODE_SEND)
          )
        },
        onNavigateToReceive = {
          navController.navigate(
            Screen.FamilyGroupTransfer.createRoute(Screen.FamilyGroupTransfer.MODE_RECEIVE)
          )
        },
        onNavigateToSyncHistory = { navController.navigate(Screen.FamilyGroupSyncHistory.route) },
      )
    }

    composable(Screen.FamilyGroupSyncHistory.route) {
      com.woliveiras.petit.presentation.feature.familygroup.SyncHistoryScreen(
        onNavigateBack = { navController.popBackStack() }
      )
    }

    // Pairing
    composable(Screen.FamilyGroupPairing.route) {
      PairingScreen(
        onNavigateBack = { navController.popBackStack() },
        onPairingComplete = { isSender ->
          val mode =
            if (isSender) Screen.FamilyGroupTransfer.MODE_SEND
            else Screen.FamilyGroupTransfer.MODE_RECEIVE
          navController.navigate(Screen.FamilyGroupTransfer.createRoute(mode)) {
            popUpTo(Screen.FamilyGroupPairing.route) { inclusive = true }
          }
        },
      )
    }

    // Transfer
    composable(
      route = Screen.FamilyGroupTransfer.route,
      arguments =
        listOf(
          navArgument("mode") {
            type = NavType.StringType
            defaultValue = Screen.FamilyGroupTransfer.MODE_SEND
          }
        ),
    ) {
      TransferScreen(onNavigateBack = { navController.popBackStack() })
    }

    // Tasks
    composable(Screen.Tasks.route) {
      TaskListScreen(
        onNavigateToTaskForm = { taskId ->
          navController.navigate(Screen.TaskForm.createRoute(taskId))
        },
        onNavigateToSettings = { navController.navigate(Screen.TaskSettings.route) },
        onNavigateToCompletedTasks = { navController.navigate(Screen.CompletedTasks.route) },
        onNavigateToVaccinationForm = { petId, entryId ->
          navController.navigate(Screen.VaccinationForm.createRoute(petId, entryId))
        },
        onNavigateToWeightForm = { petId, entryId ->
          navController.navigate(Screen.WeightForm.createRoute(petId, entryId))
        },
        onNavigateToDewormingForm = { petId, entryId ->
          navController.navigate(Screen.DewormingForm.createRoute(petId, entryId))
        },
      )
    }

    // Task Form
    composable(
      route = Screen.TaskForm.route,
      arguments =
        listOf(
          navArgument("taskId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
          }
        ),
    ) {
      TaskFormScreen(onNavigateBack = { navController.popBackStack() })
    }

    // Task Settings
    composable(Screen.TaskSettings.route) {
      TaskSettingsScreen(onNavigateBack = { navController.popBackStack() })
    }

    // Completed Tasks
    composable(Screen.CompletedTasks.route) {
      CompletedTasksScreen(onNavigateBack = { navController.popBackStack() })
    }

    // Quick Add (action selection)
    composable(Screen.QuickAdd.route) {
      QuickAddScreen(
        onNavigateBack = { navController.popBackStack() },
        onSelectWeight = {
          navController.popBackStack()
          navController.navigate(
            Screen.PetSelection.createRoute(Screen.PetSelection.ACTION_ADD_WEIGHT)
          )
        },
        onSelectVaccination = {
          navController.popBackStack()
          navController.navigate(
            Screen.PetSelection.createRoute(Screen.PetSelection.ACTION_VACCINATION)
          )
        },
        onSelectDeworming = {
          navController.popBackStack()
          navController.navigate(
            Screen.PetSelection.createRoute(Screen.PetSelection.ACTION_DEWORMING)
          )
        },
        onSelectReminder = {
          navController.popBackStack()
          navController.navigate(Screen.TaskForm.createRoute())
        },
        onSelectNewPet = {
          navController.popBackStack()
          navController.navigate(Screen.PetForm.createRoute())
        },
      )
    }

    // Activity Timeline
    composable(Screen.ActivityTimeline.route) {
      ActivityTimelineScreen(onNavigateBack = { navController.popBackStack() })
    }
  }
}
