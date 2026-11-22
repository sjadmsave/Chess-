package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = LeafGreenPrimaryDark,
    onPrimary = OnLeafGreenDark,
    primaryContainer = LeafGreenContainerDark,
    onPrimaryContainer = OnLeafGreenContainerDark,
    secondary = CrownGoldSecondaryDark,
    onSecondary = OnCrownGoldDark,
    secondaryContainer = CrownGoldContainerDark,
    onSecondaryContainer = OnCrownGoldContainerDark,
    tertiary = BrilliantCyanTertiaryDark,
    onTertiary = OnBrilliantCyanDark,
    tertiaryContainer = BrilliantCyanContainerDark,
    onTertiaryContainer = OnBrilliantCyanContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = MoveBlunderColor,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LeafGreenPrimaryLight,
    onPrimary = OnLeafGreenLight,
    primaryContainer = LeafGreenContainerLight,
    onPrimaryContainer = OnLeafGreenContainerLight,
    secondary = CrownGoldSecondaryLight,
    onSecondary = OnCrownGoldLight,
    secondaryContainer = CrownGoldContainerLight,
    onSecondaryContainer = OnCrownGoldContainerLight,
    tertiary = BrilliantCyanTertiaryLight,
    onTertiary = OnBrilliantCyanLight,
    tertiaryContainer = BrilliantCyanContainerLight,
    onTertiaryContainer = OnBrilliantCyanContainerLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = MoveBlunderColor,
    onError = Color.White
)

val EliteChessShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

enum class AppThemeMode(val labelAr: String) {
    SYSTEM("تلقائي"),
    DARK("الوضع الداكن"),
    LIGHT("الوضع الفاتح")
}

@Composable
fun EliteChessTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EliteChessTypography,
        shapes = EliteChessShapes,
        content = content
    )
}
