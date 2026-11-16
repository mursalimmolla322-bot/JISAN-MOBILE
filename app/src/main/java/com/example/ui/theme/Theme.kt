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

private val JisanLightColorScheme = lightColorScheme(
    primary = JisanBluePrimary,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = JisanBluePrimary,
    secondary = JisanBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = JisanAmberOffer,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceVariant,
    outline = LightOutline,
    error = JisanDangerRed,
    onError = Color.White
)

private val JisanDarkColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = Color(0xFF082F49),
    primaryContainer = DarkBlueContainer,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = JisanSkyAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = JisanAmberOffer,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color.Black
)

val JisanShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun JisanMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JisanDarkColorScheme else JisanLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = JisanTypography,
        shapes = JisanShapes,
        content = content
    )
}
