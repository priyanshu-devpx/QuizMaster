package com.quizmaster.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =========================================================
// QUIZMASTER — LIGHT COLOR SCHEME
// =========================================================

private val LightColorScheme = lightColorScheme(

    // Primary
    primary = QuizPrimary,
    onPrimary = White,

    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,

    // Secondary
    secondary = QuizSecondary,
    onSecondary = White,

    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,

    // Tertiary
    tertiary = QuizTertiary,
    onTertiary = White,

    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,

    // Background
    background = LightBackground,
    onBackground = LightTextPrimary,

    // Surface
    surface = LightSurface,
    onSurface = LightTextPrimary,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,

    // Borders
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,

    // Error
    error = QuizErrorLight,
    onError = White,

    errorContainer = QuizErrorContainerLight,
    onErrorContainer = LightOnErrorContainer
)

// =========================================================
// QUIZMASTER — DARK COLOR SCHEME
// =========================================================

private val DarkColorScheme = darkColorScheme(

    // Primary
    primary = QuizPrimaryDark,
    onPrimary = DarkOnPrimary,

    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    // Secondary
    secondary = QuizSecondaryDark,
    onSecondary = DarkOnSecondary,

    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,

    // Tertiary
    tertiary = QuizTertiaryDark,
    onTertiary = DarkOnTertiary,

    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,

    // Background
    background = DarkBackground,
    onBackground = DarkTextPrimary,

    // Surface
    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,

    // Borders
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    // Error
    error = QuizErrorDark,
    onError = DarkOnError,

    errorContainer = QuizErrorContainerDark,
    onErrorContainer = DarkOnErrorContainer
)

// =========================================================
// QUIZMASTER — MAIN THEME
// =========================================================

@Composable
fun QuizMasterTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                // Dark icons for Light Mode (!darkTheme), Light/White icons for Dark Mode
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}