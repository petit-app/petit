package com.woliveiras.petit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.woliveiras.petit.data.backup.google.PlayServicesGoogleIdentityAuthorizationClient
import com.woliveiras.petit.data.lan.LanSyncCoordinator
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.presentation.feature.backup.ActivityGoogleAuthorizationResolutionBridge
import com.woliveiras.petit.presentation.navigation.PetitBottomNavBar
import com.woliveiras.petit.presentation.navigation.PetitNavGraph
import com.woliveiras.petit.presentation.navigation.Screen
import com.woliveiras.petit.ui.theme.PetitTheme
import com.woliveiras.petit.util.LocaleApplicator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
  @Inject lateinit var localeApplicator: LocaleApplicator
  @Inject lateinit var lanSyncCoordinator: LanSyncCoordinator
  @Inject
  lateinit var googleAuthorizationResolutionBridge: ActivityGoogleAuthorizationResolutionBridge
  @Inject
  lateinit var googleIdentityAuthorizationClient: PlayServicesGoogleIdentityAuthorizationClient

  private val googleAuthorizationLauncher =
    registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
      googleAuthorizationResolutionBridge.complete(result.resultCode, result.data)
    }

  override fun onStart() {
    super.onStart()
    lifecycleScope.launch { lanSyncCoordinator.startForeground() }
  }

  override fun onStop() {
    lifecycleScope.launch { lanSyncCoordinator.stopForeground() }
    super.onStop()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        googleAuthorizationResolutionBridge.requests.collect { pendingIntent ->
          try {
            googleAuthorizationLauncher.launch(
              IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
            googleAuthorizationResolutionBridge.markLaunched(pendingIntent)
          } catch (_: Exception) {
            googleAuthorizationResolutionBridge.cancelPending()
          }
        }
      }
    }
    lifecycleScope.launch {
      val initialPreferences = userPreferencesRepository.userPreferences.first()
      localeApplicator.applyLanguageAtStartup(this@MainActivity, initialPreferences.language)
      showContent()
    }
  }

  override fun onResume() {
    super.onResume()
    googleIdentityAuthorizationClient.attachForegroundActivity(this)
    googleAuthorizationResolutionBridge.onHostResumed()
  }

  override fun onPause() {
    googleIdentityAuthorizationClient.detachForegroundActivity(this)
    super.onPause()
  }

  override fun onDestroy() {
    if (isFinishing) googleAuthorizationResolutionBridge.cancelPending()
    super.onDestroy()
  }

  private fun showContent() {
    setContent {
      val userPreferences by
        userPreferencesRepository.userPreferences.collectAsStateWithLifecycle(initialValue = null)

      val prefs = userPreferences

      val isDarkTheme = resolveDarkTheme(prefs?.theme ?: AppTheme.SYSTEM, isSystemInDarkTheme())

      PetitTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          if (prefs == null) return@Surface

          val navController = rememberNavController()
          val startDestination =
            if (prefs.hasCompletedOnboarding) {
              Screen.Home.route
            } else {
              Screen.Onboarding.route
            }
          PetitAppContent(navController = navController, startDestination = startDestination)
        }
      }
    }
  }
}

internal fun resolveDarkTheme(theme: AppTheme, systemIsDark: Boolean): Boolean =
  when (theme) {
    AppTheme.SYSTEM -> systemIsDark
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
  }

@Composable
private fun PetitAppContent(navController: NavHostController, startDestination: String) {
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val showBottomBar = currentRoute != Screen.Onboarding.route

  Scaffold(
    bottomBar = {
      if (showBottomBar) {
        PetitBottomNavBar(
          currentRoute = currentRoute,
          onHomeClick = {
            navController.navigate(Screen.Home.route) {
              popUpTo(Screen.Home.route) { inclusive = true }
            }
          },
          onPetsClick = {
            navController.navigate(Screen.PetList.route) {
              popUpTo(Screen.Home.route) { inclusive = false }
            }
          },
          onAddClick = {
            if (currentRoute?.startsWith("select-pet/") == true) {
              navController.navigate(Screen.PetForm.createRoute()) {
                popUpTo(Screen.Home.route) { inclusive = false }
              }
            } else {
              navController.navigate(Screen.QuickAdd.route)
            }
          },
          onTasksClick = {
            navController.navigate(Screen.Tasks.route) {
              popUpTo(Screen.Home.route) { inclusive = false }
            }
          },
          onProfileClick = {
            navController.navigate(Screen.Settings.route) {
              popUpTo(Screen.Home.route) { inclusive = false }
            }
          },
        )
      }
    }
  ) { innerPadding ->
    PetitNavGraph(
      navController = navController,
      modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
      startDestination = startDestination,
    )
  }
}
