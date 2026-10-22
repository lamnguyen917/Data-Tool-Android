package com.elp.datatool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Colors used by the wireframes that have no Material 3 role. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
)

private val LightExtendedColors = ExtendedColors(
    success = LightSuccess,
    successContainer = LightSuccessSoft,
)

private val DarkExtendedColors = ExtendedColors(
    success = DarkSuccess,
    successContainer = DarkSuccessSoft,
)

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = LightStrong,
    onPrimary = LightOnStrong,
    primaryContainer = LightSurface2,
    onPrimaryContainer = LightText,
    secondary = LightStrong,
    onSecondary = LightOnStrong,
    secondaryContainer = LightSurface2,
    onSecondaryContainer = LightText,
    tertiary = LightStrong,
    onTertiary = LightOnStrong,
    background = LightSurface,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightMuted,
    surfaceTint = Color.Transparent,
    surfaceBright = LightSurface,
    surfaceDim = LightSurface2,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface,
    surfaceContainerHighest = LightSurface2,
    outline = LightLine,
    outlineVariant = LightLine,
    error = LightDanger,
    onError = LightOnStrong,
    errorContainer = LightDangerSoft,
    onErrorContainer = LightDanger,
    inverseSurface = LightStrong,
    inverseOnSurface = LightOnStrong,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkStrong,
    onPrimary = DarkOnStrong,
    primaryContainer = DarkSurface2,
    onPrimaryContainer = DarkText,
    secondary = DarkStrong,
    onSecondary = DarkOnStrong,
    secondaryContainer = DarkSurface2,
    onSecondaryContainer = DarkText,
    tertiary = DarkStrong,
    onTertiary = DarkOnStrong,
    background = DarkSurface,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkMuted,
    surfaceTint = Color.Transparent,
    surfaceBright = DarkSurface2,
    surfaceDim = DarkSurface,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface2,
    surfaceContainerHighest = DarkSurface2,
    outline = DarkLine,
    outlineVariant = DarkLine,
    error = DarkDanger,
    onError = DarkOnStrong,
    errorContainer = DarkDangerSoft,
    onErrorContainer = DarkDanger,
    inverseSurface = DarkStrong,
    inverseOnSurface = DarkOnStrong,
)

private val DataToolShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DataToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = DataToolShapes,
            content = content
        )
    }
}

object DataToolTheme {
    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
