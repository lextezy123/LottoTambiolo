package com.lextezy.tambiolo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The app's own palette, matching the mockups. Read with `Tambiolo.colors`. */
@Immutable
data class TambioloColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val selectedBg: Color,
    val selectedInk: Color,
    val navPill: Color,
    val navPillInk: Color,
    val navBar: Color,
    val red: Color,
    val match: Color,
    val drumCenter: Color,
    val drumEdgeFill: Color,
    val drumRim: Color,
    val isDark: Boolean,
)

val LightColors = TambioloColors(
    bg = Color(0xFFF3F5FA), card = Color(0xFFFFFFFF), ink = Color(0xFF15203B), muted = Color(0xFF55607D),
    line = Color(0xFFD5DBE8), chip = Color(0xFFEEF1F7),
    selectedBg = Color(0xFF15203B), selectedInk = Color(0xFFFFFFFF),
    navPill = Color(0xFFFFE29A), navPillInk = Color(0xFF15203B), navBar = Color(0xFFFFFFFF),
    red = Color(0xFFE0353F), match = Color(0xFF1E8A55),
    drumCenter = Color(0xFFFFFFFF), drumEdgeFill = Color(0xFFD3DBEA), drumRim = Color(0xFF9AA6C0),
    isDark = false,
)

val DarkColors = TambioloColors(
    bg = Color(0xFF0E1424), card = Color(0xFF182036), ink = Color(0xFFEEF1F8), muted = Color(0xFFA3ACC4),
    line = Color(0xFF2C3653), chip = Color(0xFF232C46),
    selectedBg = Color(0xFFFFC23D), selectedInk = Color(0xFF0E1424),
    navPill = Color(0xFFFFC23D), navPillInk = Color(0xFF0E1424), navBar = Color(0xFF141B2F),
    red = Color(0xFFF2545B), match = Color(0xFF3CCF86),
    drumCenter = Color(0xFF2A3456), drumEdgeFill = Color(0xFF141B33), drumRim = Color(0xFF4A5678),
    isDark = true,
)

/** Ball colors by number range: 1-9, 10-18, 19-27, 28-36, 37-45, 46-54, 55-58. */
private val ballMain = listOf(
    Color(0xFFFFC23D), Color(0xFFFF8F5A), Color(0xFFFF6F9C), Color(0xFFB68CFF),
    Color(0xFF4FB3E8), Color(0xFF4CD39A), Color(0xFFB9DE4F),
)
private val ballShade = listOf(
    Color(0xFFC98A00), Color(0xFFD45A23), Color(0xFFCC3C6C), Color(0xFF7B55CC),
    Color(0xFF1F7FB5), Color(0xFF1E9A63), Color(0xFF7FA51F),
)
val BallInk = Color(0xFF15203B)

fun ballColor(n: Int): Color = ballMain[((n - 1) / 9) % ballMain.size]
fun ballShadeColor(n: Int): Color = ballShade[((n - 1) / 9) % ballShade.size]

private val LocalTambioloColors = staticCompositionLocalOf { LightColors }

object Tambiolo {
    val colors: TambioloColors
        @Composable get() = LocalTambioloColors.current
}

@Composable
fun TambioloTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.selectedBg, onPrimary = c.selectedInk, background = c.bg, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, surfaceContainerLow = c.card, surfaceContainer = c.card,
            surfaceContainerHigh = c.card, onSurfaceVariant = c.muted, outline = c.line, error = c.red,
        )
    } else {
        lightColorScheme(
            primary = c.selectedBg, onPrimary = c.selectedInk, background = c.bg, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, surfaceContainerLow = c.card, surfaceContainer = c.card,
            surfaceContainerHigh = c.card, onSurfaceVariant = c.muted, outline = c.line, error = c.red,
        )
    }
    CompositionLocalProvider(LocalTambioloColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
