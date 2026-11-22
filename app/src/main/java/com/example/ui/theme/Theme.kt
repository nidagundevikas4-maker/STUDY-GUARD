package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = SageForestPrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    error = TerracottaErrorLight,
    errorContainer = TerracottaErrorContainerLight,
    background = AlabasterBackground,
    onBackground = CharcoalOnBackground,
    surface = AlabasterSurface,
    onSurface = CharcoalOnBackground,
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceVariant,
    outline = SageOutline,
    outlineVariant = SageOutlineVariant,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = MintSagePrimaryDark,
    onPrimary = PineOnPrimaryDark,
    primaryContainer = DeepSageContainerDark,
    onPrimaryContainer = MintOnContainerDark,
    secondary = MistSlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = MistOnContainerDark,
    tertiary = HoneyTertiaryDark,
    onTertiary = HoneyOnTertiaryDark,
    tertiaryContainer = HoneyContainerDark,
    onTertiaryContainer = HoneyOnContainerDark,
    error = TerracottaErrorDark,
    errorContainer = TerracottaErrorContainerDark,
    background = PineBackgroundDark,
    onBackground = MistOnBackgroundDark,
    surface = PineSurfaceDark,
    onSurface = MistOnBackgroundDark,
    surfaceVariant = PineSurfaceVariantDark,
    onSurfaceVariant = MistOnSurfaceVariantDark,
    outline = PineOutlineDark,
    outlineVariant = PineOutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
  )

@Composable
fun FocusFlowTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Keep Calm Sage & Slate as the intentional default aesthetic; users can opt into dynamic color later in Settings.
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

  MaterialTheme(
    colorScheme = colorScheme,
    typography = FocusFlowTypography,
    shapes = FocusFlowShapes,
    content = content,
  )
}
