package com.music.flux.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.flux.R

// Flux Signature Cyber Accents: Vibrant Cyan & Electric Violet
val FluxCyan = Color(0xFF00E5FF)
val FluxViolet = Color(0xFF8B5CF6)
val FluxIndigo = Color(0xFF6366F1)

/** Backwards-compatibility alias for previous red accent references */
val AccentRed = FluxCyan

private val DarkColors = darkColorScheme(
    primary = FluxCyan,
    onPrimary = Color(0xFF00222B),
    secondary = FluxViolet,
    onSecondary = Color.White,
    tertiary = FluxIndigo,
    background = Color(0xFF0A0C13),
    onBackground = Color(0xFFF1F3F9),
    surface = Color(0xFF121520),
    onSurface = Color(0xFFF1F3F9),
    surfaceVariant = Color(0xFF1B2030),
    onSurfaceVariant = Color(0xFFA2A9C0),
    outline = Color(0xFF262C42),
    outlineVariant = Color(0xFF1E2335),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color.White,
    secondary = Color(0xFF673AB7),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
)

/**
 * SF Pro Display, the face Apple Music itself is set in. Only the weights the
 * type scale actually asks for are bundled; Compose synthesises nothing, so a
 * missing weight would silently fall back to the nearest one shipped.
 */
val SFProDisplay = FontFamily.SansSerif

// Compact, screen-adaptive typography.
// All sizes are in sp — they scale automatically with the user's
// Android accessibility "Font size" setting.
private val FluxTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.W800, fontSize = 28.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleLarge  = TextStyle(fontWeight = FontWeight.W700, fontSize = 18.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 15.sp, letterSpacing = (-0.1).sp),
    bodyLarge   = TextStyle(fontWeight = FontWeight.W400, fontSize = 14.sp),
    bodyMedium  = TextStyle(fontWeight = FontWeight.W400, fontSize = 13.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 11.sp),
    labelSmall  = TextStyle(fontWeight = FontWeight.W600, fontSize = 10.sp),
).withFamily(SFProDisplay)

/** Applies [family] to every style in the scale, so nothing is left on Roboto. */
private fun Typography.withFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

@Composable
fun FluxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FluxTypography,
        content = content,
    )
}

/**
 * Draws the status and navigation bar glyphs dark or light.
 *
 * `enableEdgeToEdge()` decides this from the *system* dark-mode setting, which
 * is the wrong input the moment the in-app theme disagrees with it: Light theme
 * on a phone in dark mode left white icons on a white bar, invisible. The bars
 * have to follow the theme the app is actually painting — with one exception,
 * the player, which is dark artwork regardless and so always wants light
 * glyphs. Hence a parameter rather than reading the theme here.
 */
@Composable
fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}
