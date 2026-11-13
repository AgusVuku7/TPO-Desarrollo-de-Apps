package com.chargeroute.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = DarkBackground,
    primaryContainer = BrandPrimaryDarkContainer,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = ChargingStationBlue,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    // Colores que usan por defecto el slider, los menus desplegables y el menu lateral.
    // Si no se definen, Material 3 pone sus colores de base (lilas).
    secondaryContainer = DarkBorder,
    onSecondaryContainer = DarkTextPrimary,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkSurfaceElevated,
    surfaceContainerHighest = DarkSurfaceElevated,
    outline = DarkBorder,
    error = BatteryRed
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = LightSurface,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandPrimary,
    secondary = RouteBlue,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    // Mismo motivo que en el tema oscuro
    secondaryContainer = LightBorder,
    onSecondaryContainer = LightTextPrimary,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceElevated,
    surfaceContainerHighest = LightSurfaceElevated,
    outline = LightBorder,
    error = BatteryRed
)

// Son las tres opciones del menu. Si no hay nada guardado se usa LIGHT.
enum class ThemeMode {
    LIGHT,
    DARK,
    AUTO_SENSOR // Uses TYPE_LIGHT ambient sensor (RNF04, RNF05)
}

@Composable
fun ChargeRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
