package com.music.dhvani.ui.theme

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
import com.music.dhvani.R

// ─────────────────────────────────────────────────────────────────────────────
// Stitch "Midnight Raag" Design System — Dhvani Indian Music App
// Source: projects/6466663551718142121
// Color Mode: DARK | Roundness: ROUND_FULL | Font: PLUS_JAKARTA_SANS
// ─────────────────────────────────────────────────────────────────────────────

// ── Primary — Deep Saffron Amber ─────────────────────────────────────────────
val DhvaniPrimary          = Color(0xFFFFB68D) // primary
val DhvaniOnPrimary        = Color(0xFF532200) // on-primary
val DhvaniPrimaryContainer = Color(0xFFFF8A3D) // primary-container
val DhvaniOnPrimaryContainer = Color(0xFF682D00)
val DhvaniInversePrimary   = Color(0xFF9A4600)

// ── Secondary — Peacock Emerald ──────────────────────────────────────────────
val DhvaniSecondary          = Color(0xFF59DDA9) // secondary
val DhvaniOnSecondary        = Color(0xFF003826)
val DhvaniSecondaryContainer = Color(0xFF00A878) // secondary-container
val DhvaniOnSecondaryContainer = Color(0xFF003322)

// ── Tertiary — Bollywood Crimson ─────────────────────────────────────────────
val DhvaniTertiary          = Color(0xFFFFB2B6) // tertiary
val DhvaniOnTertiary        = Color(0xFF67001A)
val DhvaniTertiaryContainer = Color(0xFFFF848E)
val DhvaniOnTertiaryContainer = Color(0xFF810023)

// ── Error ────────────────────────────────────────────────────────────────────
val DhvaniError            = Color(0xFFFFB4AB)
val DhvaniOnError          = Color(0xFF690005)
val DhvaniErrorContainer   = Color(0xFF93000A)
val DhvaniOnErrorContainer = Color(0xFFFFDAD6)

// ── Surfaces — Obsidian Layering ─────────────────────────────────────────────
val DhvaniBackground            = Color(0xFF111318) // background / surface
val DhvaniOnBackground          = Color(0xFFE2E2E8)
val DhvaniSurface               = Color(0xFF111318)
val DhvaniOnSurface             = Color(0xFFE2E2E8)
val DhvaniSurfaceDim            = Color(0xFF111318)
val DhvaniSurfaceBright         = Color(0xFF37393E)
val DhvaniSurfaceContainerLowest = Color(0xFF0C0E12)
val DhvaniSurfaceContainerLow   = Color(0xFF1A1C20)
val DhvaniSurfaceContainer      = Color(0xFF1E2024)
val DhvaniSurfaceContainerHigh  = Color(0xFF282A2E)
val DhvaniSurfaceContainerHighest = Color(0xFF333539)

val DhvaniOnSurfaceVariant = Color(0xFFDDC1B3)
val DhvaniSurfaceVariant   = Color(0xFF333539)
val DhvaniInverseSurface   = Color(0xFFE2E2E8)
val DhvaniInverseOnSurface = Color(0xFF2F3035)

// ── Outline ──────────────────────────────────────────────────────────────────
val DhvaniOutline        = Color(0xFFA58C7F)
val DhvaniOutlineVariant = Color(0xFF564338)

// ── Surface Tint ─────────────────────────────────────────────────────────────
val DhvaniSurfaceTint = Color(0xFFFFB68D)

// ── Backwards-compatibility aliases (existing code references these) ──────────
val DhvaniSaffron      = DhvaniPrimaryContainer  // #FF8A3D
val DhvaniSaffronDim   = DhvaniPrimary           // #FFB68D
val DhvaniEmerald      = DhvaniSecondaryContainer // #00A878
val DhvaniEmeraldDim   = DhvaniSecondary          // #59DDA9
val DhvaniCrimson      = DhvaniTertiaryContainer  // #FF848E
val DhvaniCanvas       = DhvaniSurfaceContainerLowest // #0C0E12
val DhvaniSurface1     = DhvaniSurfaceContainerLow
val DhvaniSurface2     = DhvaniSurfaceContainer
val DhvaniSurfaceHighlight = DhvaniSurfaceContainerHigh
val DhvaniTextHigh     = DhvaniOnSurface
val DhvaniTextMedium   = DhvaniOnSurfaceVariant
val AccentRed          = DhvaniSaffron

// ─────────────────────────────────────────────────────────────────────────────
// Color Schemes
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary                = DhvaniPrimary,
    onPrimary              = DhvaniOnPrimary,
    primaryContainer       = DhvaniPrimaryContainer,
    onPrimaryContainer     = DhvaniOnPrimaryContainer,
    inversePrimary         = DhvaniInversePrimary,
    secondary              = DhvaniSecondary,
    onSecondary            = DhvaniOnSecondary,
    secondaryContainer     = DhvaniSecondaryContainer,
    onSecondaryContainer   = DhvaniOnSecondaryContainer,
    tertiary               = DhvaniTertiary,
    onTertiary             = DhvaniOnTertiary,
    tertiaryContainer      = DhvaniTertiaryContainer,
    onTertiaryContainer    = DhvaniOnTertiaryContainer,
    error                  = DhvaniError,
    onError                = DhvaniOnError,
    errorContainer         = DhvaniErrorContainer,
    onErrorContainer       = DhvaniOnErrorContainer,
    background             = DhvaniBackground,
    onBackground           = DhvaniOnBackground,
    surface                = DhvaniSurface,
    onSurface              = DhvaniOnSurface,
    surfaceVariant         = DhvaniSurfaceVariant,
    onSurfaceVariant       = DhvaniOnSurfaceVariant,
    inverseSurface         = DhvaniInverseSurface,
    inverseOnSurface       = DhvaniInverseOnSurface,
    outline                = DhvaniOutline,
    outlineVariant         = DhvaniOutlineVariant,
    surfaceTint            = DhvaniSurfaceTint,
)

private val LightColors = lightColorScheme(
    primary                = Color(0xFF9A4600),
    onPrimary              = Color.White,
    primaryContainer       = Color(0xFFFFDBCA),
    onPrimaryContainer     = Color(0xFF341200),
    secondary              = Color(0xFF006C4E),
    onSecondary            = Color.White,
    secondaryContainer     = Color(0xFF89F8C7),
    onSecondaryContainer   = Color(0xFF002116),
    tertiary               = Color(0xFF9C404C),
    onTertiary             = Color.White,
    background             = Color(0xFFFFFBFF),
    onBackground           = Color(0xFF1F1B16),
    surface                = Color(0xFFFFFBFF),
    onSurface              = Color(0xFF1F1B16),
    surfaceVariant         = Color(0xFFF4DED3),
    onSurfaceVariant       = Color(0xFF52443D),
    outline                = Color(0xFF85746C),
    outlineVariant         = Color(0xFFD7C2B9),
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography — Plus Jakarta Sans (Stitch Dhvani Design System)
// ─────────────────────────────────────────────────────────────────────────────

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular,  FontWeight.W400),
    Font(R.font.plus_jakarta_sans_medium,   FontWeight.W500),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.W600),
    Font(R.font.plus_jakarta_sans_bold,     FontWeight.W700),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.W800),
)

// Legacy alias used in existing code
val SFProDisplay = PlusJakartaSans

private val DhvaniTypography = Typography(
    // Display scale — hero editorial headers, player takeovers
    displayLarge  = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W800,
        fontSize     = 30.sp,
        lineHeight   = 36.sp,
        letterSpacing = (-0.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W800,
        fontSize     = 26.sp,
        lineHeight   = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall  = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W700,
        fontSize     = 22.sp,
        lineHeight   = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    // Headline scale — shelf categorizations, section headers
    headlineLarge = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W700,
        fontSize     = 26.sp,
        lineHeight   = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W700,
        fontSize     = 20.sp,
        lineHeight   = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W600,
        fontSize     = 17.sp,
        lineHeight   = 22.sp,
        letterSpacing = (-0.1).sp,
    ),
    // Title scale
    titleLarge    = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W700,
        fontSize     = 18.sp,
        lineHeight   = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium   = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W600,
        fontSize     = 15.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall    = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W600,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp,
    ),
    // Body scale — descriptions, biographies, liner notes
    bodyLarge     = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W400,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium    = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W400,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall     = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W400,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    // Label scale — micro-badges, genre tokens, nav labels
    labelLarge    = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W600,
        fontSize     = 14.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium   = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W600,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall    = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.W700,
        fontSize     = 10.sp,
        lineHeight   = 12.sp,
        letterSpacing = 0.5.sp,
    ),
)


// ─────────────────────────────────────────────────────────────────────────────
// Theme Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DhvaniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = DhvaniTypography,
        content     = content,
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
            isAppearanceLightStatusBars    = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}
