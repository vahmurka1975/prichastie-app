package ru.example.prichastie.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Цвета макета: --bg, --card, --ink, --mut, --acc, --gold, --line, --ok, --hl. */
@Immutable
data class Palette(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val mut: Color,
    val acc: Color,
    val gold: Color,
    val line: Color,
    val ok: Color,
    val hl: Color,
    val onAcc: Color
)

val LightPalette = Palette(
    bg = Color(0xFFF6F1E7), card = Color(0xFFFFFDF8), ink = Color(0xFF2B2118),
    mut = Color(0xFF7A6A58), acc = Color(0xFF8A2B2B), gold = Color(0xFFB8892E),
    line = Color(0xFFE3D8C3), ok = Color(0xFF3F7A4B), hl = Color(0xFFFBF0D3),
    onAcc = Color.White
)

val NightPalette = Palette(
    bg = Color(0xFF17130F), card = Color(0xFF221C16), ink = Color(0xFFEADFCC),
    mut = Color(0xFFA39580), acc = Color(0xFFD9885F), gold = Color(0xFFD8B058),
    line = Color(0xFF383026), ok = Color(0xFF78B886), hl = Color(0xFF33291A),
    onAcc = Color(0xFF17130F)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val palette: Palette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

/** Накладывает палитру макета поверх PrichastieTheme (сам Theme.kt не меняется). */
@Composable
fun PrichastieColors(dark: Boolean, content: @Composable () -> Unit) {
    val pal = if (dark) NightPalette else LightPalette
    val base = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    val scheme = base.copy(
        primary = pal.acc,
        onPrimary = pal.onAcc,
        primaryContainer = pal.hl,
        onPrimaryContainer = pal.ink,
        secondary = pal.gold,
        background = pal.bg,
        onBackground = pal.ink,
        surface = pal.card,
        onSurface = pal.ink,
        surfaceVariant = pal.line,
        onSurfaceVariant = pal.mut,
        outline = pal.line,
        outlineVariant = pal.line,
        surfaceContainerLowest = pal.card,
        surfaceContainerLow = pal.card,
        surfaceContainer = pal.card,
        surfaceContainerHigh = pal.card,
        surfaceContainerHighest = pal.card
    )
    CompositionLocalProvider(LocalPalette provides pal) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}