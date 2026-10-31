package com.mulkora.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

// --- Açık Tema ---
private val MulkoraLightColorScheme = lightColorScheme(
    primary            = LightPrimary,
    onPrimary          = LightOnPrimary,
    primaryContainer   = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary            = LightSecondary,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background      = LightBackground,
    onBackground    = LightOnBackground,
    surface         = LightSurface,
    onSurface       = LightOnSurface,
    surfaceVariant  = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline         = LightOutline,
    error   = LightError,
    onError = LightOnError
)

// --- Koyu Tema ---
private val MulkoraDarkColorScheme = darkColorScheme(
    primary            = DarkPrimary,
    onPrimary          = DarkOnPrimary,
    primaryContainer   = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background      = DarkBackground,
    onBackground    = DarkOnBackground,
    surface         = DarkSurface,
    onSurface       = DarkOnSurface,
    surfaceVariant  = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline         = DarkOutline,
    error   = DarkError,
    onError = DarkOnError
)

/**
 * Mülkora tema tercihi.
 * DataStore'dan okunur; CLAUDE.md Bölüm 20.
 */
enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Uygulamanın ana tema composable'ı.
 *
 * @param darkTheme - Sistem veya kullanıcı tercihine göre belirlenir.
 * @param dynamicColor - Android 12+'da Material You dinamik renk desteği.
 *                       Mülkora markası için false (marka kimliği korunur).
 */
@Composable
fun MulkoraTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    dynamicColor: Boolean = false, // Marka renkleri korunmak için kapalı
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (themePreference) {
        ThemePreference.LIGHT  -> false
        ThemePreference.DARK   -> true
        ThemePreference.SYSTEM -> systemInDarkTheme
    }

    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        useDarkTheme -> MulkoraDarkColorScheme
        else         -> MulkoraLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MulkoraTypography,
        shapes      = MulkoraShapes,
        content     = content
    )
}