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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ImmersiveLavender,
    secondary = ImmersiveTextMuted,
    tertiary = ImmersiveLavender,
    background = ImmersiveObsidian,
    surface = ImmersiveSlateDim,
    onPrimary = ImmersiveDeepViolet,
    onSecondary = ImmersiveLavender,
    onTertiary = ImmersiveLavender,
    onBackground = ImmersiveTextMain,
    onSurface = ImmersiveTextMain,
    surfaceVariant = ImmersiveContainers,
    onSurfaceVariant = ImmersiveTextMain
)

private val LightColorScheme = lightColorScheme(
    primary = ImmersiveLavender,
    secondary = ImmersiveTextMuted,
    tertiary = ImmersiveLavender,
    background = ImmersiveObsidian,
    surface = ImmersiveSlateDim,
    onPrimary = ImmersiveDeepViolet,
    onSecondary = ImmersiveLavender,
    onTertiary = ImmersiveLavender,
    onBackground = ImmersiveTextMain,
    onSurface = ImmersiveTextMain,
    surfaceVariant = ImmersiveContainers,
    onSurfaceVariant = ImmersiveTextMain
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color support but default to our customized immersive branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Force dark theme by default to preserve CT authentic visual identity!
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
