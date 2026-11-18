package ru.example.prichastie.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A2B2B),
    onPrimary = Color.White,
    secondary = Color(0xFFB8892E),
    onSecondary = Color(0xFF2B2118),
    tertiary = Color(0xFF3F7A4B),
    background = Color(0xFFF6F1E7),
    onBackground = Color(0xFF2B2118),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFE3D8C3),
    onSurfaceVariant = Color(0xFF7A6A58),
    outline = Color(0xFFE3D8C3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD9885F),
    onPrimary = Color(0xFF17130F),
    secondary = Color(0xFFD8B058),
    onSecondary = Color(0xFF17130F),
    tertiary = Color(0xFF78B886),
    background = Color(0xFF17130F),
    onBackground = Color(0xFFEADFCC),
    surface = Color(0xFF221C16),
    onSurface = Color(0xFFEADFCC),
    surfaceVariant = Color(0xFF383026),
    onSurfaceVariant = Color(0xFFA39580),
    outline = Color(0xFF383026)
)

private val Serif = FontFamily.Serif

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontFamily = Serif, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Serif, fontSize = 18.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = Serif, fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = Serif, fontSize = 13.sp, lineHeight = 18.sp)
)

@Composable
fun PrichastieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}