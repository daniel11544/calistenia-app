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

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = OnGreenPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = OnGreenPrimaryContainerDark,
    secondary = OrangeSecondaryDark,
    onSecondary = OnOrangeSecondaryDark,
    secondaryContainer = OrangeSecondaryContainerDark,
    onSecondaryContainer = OnOrangeSecondaryContainerDark,
    tertiary = CyanTertiaryDark,
    onTertiary = OnCyanTertiaryDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = OnGreenPrimaryLight,
    primaryContainer = GreenPrimaryContainerLight,
    onPrimaryContainer = OnGreenPrimaryContainerLight,
    secondary = OrangeSecondaryLight,
    onSecondary = OnOrangeSecondaryLight,
    secondaryContainer = OrangeSecondaryContainerLight,
    onSecondaryContainer = OnOrangeSecondaryContainerLight,
    tertiary = CyanTertiaryLight,
    onTertiary = OnCyanTertiaryLight,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun FitCalisthenicsTheme(
    darkTheme: Boolean = true, // Default to sleek modern dark theme for calisthenics
    dynamicColor: Boolean = false, // Keep branded vibrant green+orange identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
