package com.contactsapptwomktech.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand palette — deep indigo + warm coral accent
val Indigo10 = Color(0xFF0D0F2E)
val Indigo20 = Color(0xFF171A45)
val Indigo30 = Color(0xFF252A6B)
val Indigo40 = Color(0xFF3540A0)
val Indigo50 = Color(0xFF4A58CE)
val Indigo60 = Color(0xFF6B7BE8)
val Indigo80 = Color(0xFFB0BBFF)
val Indigo90 = Color(0xFFDEE2FF)
val Indigo95 = Color(0xFFF0F1FF)
val Indigo99 = Color(0xFFFFFBFF)

val Coral10 = Color(0xFF3A0A00)
val Coral20 = Color(0xFF6B1800)
val Coral40 = Color(0xFFD83B01)
val Coral60 = Color(0xFFFF7B54)
val Coral80 = Color(0xFFFFB598)
val Coral90 = Color(0xFFFFDBCF)
val Coral95 = Color(0xFFFFEDE7)

val Slate10 = Color(0xFF0C0E14)
val Slate20 = Color(0xFF1A1D27)
val Slate30 = Color(0xFF2B2F40)
val Slate40 = Color(0xFF3E4358)
val Slate60 = Color(0xFF717694)
val Slate80 = Color(0xFFB5B9D4)
val Slate90 = Color(0xFFD8DBF0)
val Slate95 = Color(0xFFEBECF8)
val Slate99 = Color(0xFFFAFAFF)

val MissedCallRed = Color(0xFFE53935)
val IncomingGreen = Color(0xFF2E7D32)
val OutgoingBlue = Color(0xFF1565C0)

private val LightColors = lightColorScheme(
    primary = Indigo50,
    onPrimary = Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    secondary = Coral40,
    onSecondary = Color.White,
    secondaryContainer = Coral90,
    onSecondaryContainer = Coral10,
    tertiary = Slate40,
    onTertiary = Color.White,
    tertiaryContainer = Slate90,
    onTertiaryContainer = Slate10,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Indigo99,
    onBackground = Indigo10,
    surface = Color.White,
    onSurface = Slate10,
    surfaceVariant = Slate95,
    onSurfaceVariant = Slate40,
    outline = Slate60,
    outlineVariant = Slate90,
    inverseSurface = Slate20,
    inverseOnSurface = Slate95,
    inversePrimary = Indigo80,
    surfaceTint = Indigo50,
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo90,
    secondary = Coral80,
    onSecondary = Coral20,
    secondaryContainer = Color(0xFF8C2900),
    onSecondaryContainer = Coral90,
    tertiary = Slate80,
    onTertiary = Slate20,
    tertiaryContainer = Slate30,
    onTertiaryContainer = Slate90,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    background = Indigo10,
    onBackground = Indigo90,
    surface = Slate20,
    onSurface = Slate90,
    surfaceVariant = Slate30,
    onSurfaceVariant = Slate80,
    outline = Slate60,
    outlineVariant = Slate40,
    inverseSurface = Slate90,
    inverseOnSurface = Slate20,
    inversePrimary = Indigo50,
    surfaceTint = Indigo80,
    scrim = Color.Black,
)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

enum class AppAccentColor(
    val label: String,
    val seed: Color
) {
    BLUE("Blue", Color(0xFF1A73E8)),
    PURPLE("Purple", Color(0xFF7C4DFF)),
    GREEN("Green", Color(0xFF00897B)),
    PARROTGREEN("Parrot", Color(0xFF2EEF0A)),
    ORANGE("Orange", Color(0xFFE65100)),
    PINK("Pink", Color(0xFFE91E63)),
    RED("Red", Color(0xFFD32F2F))
}

data class ThemeSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentColor: AppAccentColor = AppAccentColor.PARROTGREEN,
    val useDynamicColor: Boolean = false,    // Material You (Android 12+)
    val useAmoledBlack: Boolean = false,      // Pure black dark background
    val languageCode: String = "en"
)

private fun buildColorScheme(seed: Color, dark: Boolean): ColorScheme {
    val primary        = seed
    val primaryVariant = seed.copy(alpha = 0.8f)
    val container      = seed.copy(alpha = if (dark) 0.25f else 0.15f)
    val onContainer    = if (dark) Color.White else seed.copy(alpha = 0.9f)
    val surface        = if (dark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
    val background     = if (dark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)

    return if (dark) {
        darkColorScheme(
            primary             = primary,
            onPrimary           = Color.Black,
            primaryContainer    = container,
            onPrimaryContainer  = onContainer,
            surface             = surface,
            background          = background
        )
    } else {
        lightColorScheme(
            primary             = primary,
            onPrimary           = Color.White,
            primaryContainer    = container,
            onPrimaryContainer  = onContainer,
            surface             = surface,
            background          = background
        )
    }
}


//@Composable
//fun ContactsAppTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    dynamicColor: Boolean = false,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//        darkTheme -> DarkColors
//        else -> LightColors
//    }
//
//    val view = LocalView.current
//    if (!view.isInEditMode) {
//        SideEffect {
//            val window = (view.context as Activity).window
//            WindowCompat.setDecorFitsSystemWindows(window, false)
//            val insetsController = WindowCompat.getInsetsController(window, view)
//            insetsController.isAppearanceLightStatusBars = !darkTheme
//            insetsController.isAppearanceLightNavigationBars = !darkTheme
//        }
//    }
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = AppTypography,
//        shapes = AppShapes,
//        content = content
//    )
//}
@Composable
fun ContactsAppTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK   -> true
        AppThemeMode.LIGHT  -> false
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        settings.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else ->
            buildColorScheme(settings.accentColor.seed, isDark)
    }.let { base ->
        // AMOLED: replace surface/background with pure black
        if (settings.useAmoledBlack && isDark)
            base.copy(surface = Color.Black, background = Color.Black)
        else base
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography   = AppTypography,
        content      = content
    )
}
