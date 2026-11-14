package com.music.dhvani.ui.theme

import android.app.Activity
import android.graphics.Typeface
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.R
import com.music.dhvani.data.settings.AppFontStyle
import com.music.dhvani.data.settings.AppSettings

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

val SFProDisplay = FontFamily(
    Font(R.font.sf_pro_display_regular,  FontWeight.W400),
    Font(R.font.sf_pro_display_medium,   FontWeight.W500),
    Font(R.font.sf_pro_display_semibold, FontWeight.W600),
    Font(R.font.sf_pro_display_bold,     FontWeight.W700),
    Font(R.font.sf_pro_display_heavy,    FontWeight.W800),
)

private val DhvaniPlatformStyle = PlatformTextStyle(includeFontPadding = false)
private val DhvaniLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

val CondensedFontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
val CasualFontFamily = FontFamily(Typeface.create("casual", Typeface.NORMAL))

fun createDhvaniTypography(fontFamily: FontFamily, isBold: Boolean = false): Typography {
    fun style(
        fontWeight: FontWeight,
        fontSize: androidx.compose.ui.unit.TextUnit,
        lineHeight: androidx.compose.ui.unit.TextUnit,
        letterSpacing: androidx.compose.ui.unit.TextUnit,
    ) = TextStyle(
        fontFamily = fontFamily,
        fontWeight = if (isBold) {
            when (fontWeight) {
                FontWeight.W400 -> FontWeight.W600
                FontWeight.W500 -> FontWeight.W700
                FontWeight.W600 -> FontWeight.W700
                FontWeight.W700 -> FontWeight.W800
                FontWeight.W800 -> FontWeight.W900
                else -> FontWeight.Bold
            }
        } else fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        platformStyle = DhvaniPlatformStyle,
        lineHeightStyle = DhvaniLineHeightStyle,
    )

    return Typography(
        displayLarge  = style(FontWeight.W800, 30.sp, 36.sp, (-0.6).sp),
        displayMedium = style(FontWeight.W800, 26.sp, 32.sp, (-0.5).sp),
        displaySmall  = style(FontWeight.W700, 22.sp, 28.sp, (-0.3).sp),
        headlineLarge = style(FontWeight.W700, 26.sp, 32.sp, (-0.4).sp),
        headlineMedium = style(FontWeight.W700, 20.sp, 26.sp, (-0.2).sp),
        headlineSmall = style(FontWeight.W600, 17.sp, 22.sp, (-0.1).sp),
        titleLarge    = style(FontWeight.W700, 18.sp, 24.sp, (-0.1).sp),
        titleMedium   = style(FontWeight.W600, 15.sp, 20.sp, 0.sp),
        titleSmall    = style(FontWeight.W600, 13.sp, 18.sp, 0.sp),
        bodyLarge     = style(FontWeight.W400, 16.sp, 24.sp, 0.sp),
        bodyMedium    = style(FontWeight.W400, 14.sp, 20.sp, 0.1.sp),
        bodySmall     = style(FontWeight.W400, 12.sp, 16.sp, 0.2.sp),
        labelLarge    = style(FontWeight.W600, 14.sp, 18.sp, 0.1.sp),
        labelMedium   = style(FontWeight.W600, 12.sp, 16.sp, 0.2.sp),
        labelSmall    = style(FontWeight.W700, 10.sp, 12.sp, 0.5.sp),
    )
}

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.W400),
    Font(R.font.poppins_bold, FontWeight.W700),
)
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.W400),
    Font(R.font.inter_bold, FontWeight.W700),
)
val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.W400),
    Font(R.font.montserrat_bold, FontWeight.W700),
)
val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.W400),
    Font(R.font.outfit_bold, FontWeight.W700),
)
val OpenSansFontFamily = FontFamily(
    Font(R.font.opensans_regular, FontWeight.W400),
    Font(R.font.opensans_bold, FontWeight.W700),
)
val LatoFontFamily = FontFamily(
    Font(R.font.lato_regular, FontWeight.W400),
    Font(R.font.lato_bold, FontWeight.W700),
)
val OswaldFontFamily = FontFamily(
    Font(R.font.oswald_regular, FontWeight.W400),
    Font(R.font.oswald_bold, FontWeight.W700),
)
val RalewayFontFamily = FontFamily(
    Font(R.font.raleway_regular, FontWeight.W400),
    Font(R.font.raleway_bold, FontWeight.W700),
)
val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.W400),
    Font(R.font.nunito_bold, FontWeight.W700),
)
val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_regular, FontWeight.W400),
    Font(R.font.playfair_bold, FontWeight.W700),
)
val TelmaFontFamily = FontFamily(
    Font(R.font.telma_regular, FontWeight.W400),
    Font(R.font.telma_medium, FontWeight.W500),
    Font(R.font.telma_bold, FontWeight.W700),
)

val SFProTypography = createDhvaniTypography(SFProDisplay)
val JakartaTypography = createDhvaniTypography(PlusJakartaSans)
val SystemTypography = createDhvaniTypography(FontFamily.Default)
val CondensedTypography = createDhvaniTypography(CondensedFontFamily)
val SerifTypography = createDhvaniTypography(FontFamily.Serif)
val MonospaceTypography = createDhvaniTypography(FontFamily.Monospace)
val CasualTypography = createDhvaniTypography(CasualFontFamily)
val CursiveTypography = createDhvaniTypography(FontFamily.Cursive)

val DhvaniTypography = SFProTypography


// ─────────────────────────────────────────────────────────────────────────────
// Theme Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DhvaniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val uiStyle = AppSettings.uiDesignStyle.collectAsStateWithLifecycle().value
    val accentInt = AppSettings.accentColor.collectAsStateWithLifecycle().value
    val dynamicTheme = AppSettings.dynamicTheme.collectAsStateWithLifecycle().value
    val fontStyle = AppSettings.fontStyle.collectAsStateWithLifecycle().value
    val boldText = AppSettings.boldText.collectAsStateWithLifecycle().value
    val customFontPath = AppSettings.customFontPath.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentTypography = androidx.compose.runtime.remember(fontStyle, boldText, customFontPath) {
        val baseFont = when (fontStyle) {
            AppFontStyle.SF_PRO -> SFProDisplay
            AppFontStyle.JAKARTA -> PlusJakartaSans
            AppFontStyle.TELMA -> TelmaFontFamily
            AppFontStyle.POPPINS -> PoppinsFontFamily
            AppFontStyle.INTER -> InterFontFamily
            AppFontStyle.MONTSERRAT -> MontserratFontFamily
            AppFontStyle.OUTFIT -> OutfitFontFamily
            AppFontStyle.OPEN_SANS -> OpenSansFontFamily
            AppFontStyle.LATO -> LatoFontFamily
            AppFontStyle.OSWALD -> OswaldFontFamily
            AppFontStyle.RALEWAY -> RalewayFontFamily
            AppFontStyle.NUNITO -> NunitoFontFamily
            AppFontStyle.PLAYFAIR -> PlayfairFontFamily
            AppFontStyle.SYSTEM -> FontFamily.Default
            AppFontStyle.CONDENSED -> CondensedFontFamily
            AppFontStyle.SERIF -> FontFamily.Serif
            AppFontStyle.MONOSPACE -> FontFamily.Monospace
            AppFontStyle.CASUAL -> CasualFontFamily
            AppFontStyle.CURSIVE -> FontFamily.Cursive
            AppFontStyle.CUSTOM -> {
                runCatching {
                    val file = java.io.File(customFontPath)
                    if (file.exists() && file.length() > 0) {
                        FontFamily(Typeface.createFromFile(file))
                    } else null
                }.getOrNull() ?: SFProDisplay
            }
        }
        createDhvaniTypography(baseFont, isBold = boldText)
    }

    val colorScheme = androidx.compose.runtime.remember(darkTheme, accentInt, dynamicTheme) {
        val base = when {
            accentInt == null && dynamicTheme && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
                if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context)
                else androidx.compose.material3.dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColors
            else -> LightColors
        }
        if (accentInt != null) {
            val accent = Color(accentInt)
            val isBright = (0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue) > 0.65f
            val onPrimary = if (isBright) Color(0xFF121418) else Color.White
            val containerAlpha = if (darkTheme) 0.28f else 0.18f
            val primaryContainer = accent.copy(alpha = containerAlpha)
            base.copy(
                primary = accent,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = accent,
                inversePrimary = accent.copy(alpha = 0.85f),
                surfaceTint = accent,
            )
        } else {
            base
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalUiDesignStyle provides uiStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = currentTypography,
            content     = content,
        )
    }
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
