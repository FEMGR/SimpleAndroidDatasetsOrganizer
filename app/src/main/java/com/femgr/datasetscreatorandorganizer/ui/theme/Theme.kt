package com.femgr.datasetscreatorandorganizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ============================================================================
// Light Color Scheme
// ============================================================================

private val LightColorScheme = lightColorScheme(

    // ------------------------------------------------------------------------
    // Primary
    // ------------------------------------------------------------------------

    primary = ForestPrimary,
    onPrimary = ForestWhite,

    primaryContainer = ForestPrimaryLight,
    onPrimaryContainer = ForestText,

    // ------------------------------------------------------------------------
    // Secondary
    // ------------------------------------------------------------------------

    secondary = ForestSecondary,
    onSecondary = ForestWhite,

    secondaryContainer = ForestSecondaryLight,
    onSecondaryContainer = ForestText,

    // ------------------------------------------------------------------------
    // Tertiary / Accent
    // ------------------------------------------------------------------------

    tertiary = ForestAccent,
    onTertiary = ForestText,

    tertiaryContainer = ForestAccentLight,
    onTertiaryContainer = ForestText,

    // ------------------------------------------------------------------------
    // Background
    // ------------------------------------------------------------------------

    background = ForestBackground,
    onBackground = ForestText,

    // ------------------------------------------------------------------------
    // Surface
    // ------------------------------------------------------------------------

    surface = ForestSurface,
    onSurface = ForestText,

    surfaceVariant = ForestCard,
    onSurfaceVariant = ForestTextMuted,

    // ------------------------------------------------------------------------
    // Borders / Outlines
    // ------------------------------------------------------------------------

    outline = ForestBorder,
    outlineVariant = ForestBorderStrong,

    // ------------------------------------------------------------------------
    // Error
    // ------------------------------------------------------------------------

    error = StatusError,
    onError = ForestWhite,

    errorContainer = StatusErrorBackground,
    onErrorContainer = ForestText,
)


// ============================================================================
// Dark Color Scheme
// ============================================================================

private val DarkColorScheme = darkColorScheme(

    // ------------------------------------------------------------------------
    // Primary
    // ------------------------------------------------------------------------

    primary = DarkForestPrimary,
    onPrimary = DarkForestBackground,

    primaryContainer = DarkForestPrimaryContainer,
    onPrimaryContainer = DarkForestText,

    // ------------------------------------------------------------------------
    // Secondary
    // ------------------------------------------------------------------------

    secondary = DarkForestSecondary,
    onSecondary = DarkForestBackground,

    secondaryContainer = DarkForestSecondaryContainer,
    onSecondaryContainer = DarkForestText,

    // ------------------------------------------------------------------------
    // Tertiary / Accent
    // ------------------------------------------------------------------------

    tertiary = DarkForestAccent,
    onTertiary = DarkForestBackground,

    tertiaryContainer = DarkForestCard,
    onTertiaryContainer = DarkForestText,

    // ------------------------------------------------------------------------
    // Background
    // ------------------------------------------------------------------------

    background = DarkForestBackground,
    onBackground = DarkForestText,

    // ------------------------------------------------------------------------
    // Surface
    // ------------------------------------------------------------------------

    surface = DarkForestSurface,
    onSurface = DarkForestText,

    surfaceVariant = DarkForestCard,
    onSurfaceVariant = DarkForestTextMuted,

    // ------------------------------------------------------------------------
    // Borders / Outlines
    // ------------------------------------------------------------------------

    outline = DarkForestBorder,
    outlineVariant = DarkForestBorderStrong,

    // ------------------------------------------------------------------------
    // Error
    // ------------------------------------------------------------------------

    error = DarkStatusError,
    onError = DarkForestBackground,

    errorContainer = DarkStatusErrorBackground,
    onErrorContainer = DarkForestText,
)


// ============================================================================
// Application Theme
// ============================================================================

@Composable
fun ForestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}