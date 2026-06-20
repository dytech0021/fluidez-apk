package com.exemplo.fluidez.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E0FF),
    onPrimaryContainer = BrandPurpleDark,
    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9F7EF),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = BrandCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0DB),
    onTertiaryContainer = Color(0xFF7A1C12),
    background = SurfaceLight,
    onBackground = Color(0xFF1B1A20),
    surface = SurfaceLight,
    onSurface = Color(0xFF1B1A20),
    surfaceVariant = Color(0xFFEDE7F6),
    onSurfaceVariant = Color(0xFF494456),
)

private val DarkColors = darkColorScheme(
    primary = BrandPurpleLight,
    onPrimary = Color(0xFF2A1772),
    primaryContainer = Color(0xFF3B2A8C),
    onPrimaryContainer = Color(0xFFE7E0FF),
    secondary = BrandTealLight,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005044),
    onSecondaryContainer = Color(0xFFC9F7EF),
    tertiary = BrandCoralLight,
    onTertiary = Color(0xFF5C160C),
    tertiaryContainer = Color(0xFF8A2417),
    onTertiaryContainer = Color(0xFFFFE0DB),
    background = SurfaceDark,
    onBackground = Color(0xFFE9E2F2),
    surface = SurfaceDark,
    onSurface = Color(0xFFE9E2F2),
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = Color(0xFFC9C1D6),
)

@Composable
fun FluidezTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Cores da marca por padrão. Quem quiser "Material You" pode ligar o dynamicColor.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
