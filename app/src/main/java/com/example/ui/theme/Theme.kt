package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SindoorPrimaryDark,
    onPrimary = OnSindoorPrimaryDark,
    primaryContainer = SindoorContainerDark,
    onPrimaryContainer = OnSindoorContainerDark,
    secondary = MarigoldSecondaryDark,
    onSecondary = OnMarigoldSecondaryDark,
    secondaryContainer = MarigoldContainerDark,
    onSecondaryContainer = OnMarigoldContainerDark,
    tertiary = EmeraldTertiaryDark,
    onTertiary = OnEmeraldTertiaryDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = OnEmeraldContainerDark,
    background = MahoganyBackgroundDark,
    onBackground = OnMahoganyBackgroundDark,
    surface = MahoganySurfaceDark,
    onSurface = OnMahoganySurfaceDark,
    surfaceVariant = MahoganySurfaceVariantDark,
    onSurfaceVariant = OnMahoganySurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = SindoorPrimaryLight,
    onPrimary = OnSindoorPrimaryLight,
    primaryContainer = SindoorContainerLight,
    onPrimaryContainer = OnSindoorContainerLight,
    secondary = MarigoldSecondaryLight,
    onSecondary = OnMarigoldSecondaryLight,
    secondaryContainer = MarigoldContainerLight,
    onSecondaryContainer = OnMarigoldContainerLight,
    tertiary = EmeraldTertiaryLight,
    onTertiary = OnEmeraldTertiaryLight,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = OnEmeraldContainerLight,
    background = ParchmentBackgroundLight,
    onBackground = OnParchmentBackgroundLight,
    surface = ParchmentSurfaceLight,
    onSurface = OnParchmentSurfaceLight,
    surfaceVariant = ParchmentSurfaceVariantLight,
    onSurfaceVariant = OnParchmentSurfaceVariantLight,
    outline = OutlineLight
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
