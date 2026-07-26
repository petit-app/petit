package com.woliveiras.petit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class PetitExtendedColors(
  val weightSectionBg: Color,
  val weightSectionTint: Color,
  val vaccinationSectionBg: Color,
  val vaccinationSectionTint: Color,
  val dewormingSectionBg: Color,
  val dewormingSectionTint: Color,
  val timelineWeightBg: Color,
  val timelineVaccineBg: Color,
  val timelineDewormingBg: Color,
  val timelineReminderBg: Color,
  val weightGain: Color,
  val weightLoss: Color,
)

private val LightExtendedColors =
  PetitExtendedColors(
    weightSectionBg = WeightSectionBg,
    weightSectionTint = WeightSectionTint,
    vaccinationSectionBg = VaccinationSectionBg,
    vaccinationSectionTint = VaccinationSectionTint,
    dewormingSectionBg = DewormingSectionBg,
    dewormingSectionTint = DewormingSectionTint,
    timelineWeightBg = TimelineWeightBg,
    timelineVaccineBg = TimelineVaccineBg,
    timelineDewormingBg = TimelineDewormingBg,
    timelineReminderBg = TimelineReminderBg,
    weightGain = WeightGain,
    weightLoss = WeightLoss,
  )

private val DarkExtendedColors =
  PetitExtendedColors(
    weightSectionBg = WeightSectionBgDark,
    weightSectionTint = WeightSectionTintDark,
    vaccinationSectionBg = VaccinationSectionBgDark,
    vaccinationSectionTint = VaccinationSectionTintDark,
    dewormingSectionBg = DewormingSectionBgDark,
    dewormingSectionTint = DewormingSectionTintDark,
    timelineWeightBg = TimelineWeightBgDark,
    timelineVaccineBg = TimelineVaccineBgDark,
    timelineDewormingBg = TimelineDewormingBgDark,
    timelineReminderBg = TimelineReminderBgDark,
    weightGain = WeightGainDark,
    weightLoss = WeightLossDark,
  )

val LocalPetitColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
  )

val PetitShapes =
  Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
  )

@Composable
fun PetitTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      // System bars are drawn transparently by enableEdgeToEdge(); only the bar icon
      // appearance is managed here. Setting window.statusBarColor/navigationBarColor is
      // deprecated on Android 15 (API 35) and must not be used with edge-to-edge.
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !darkTheme
      insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  androidx.compose.runtime.CompositionLocalProvider(LocalPetitColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = PetitShapes,
      content = content,
    )
  }
}
