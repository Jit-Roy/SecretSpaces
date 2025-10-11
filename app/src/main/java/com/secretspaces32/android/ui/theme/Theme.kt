package com.secretspaces32.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkElevated,
    onPrimaryContainer = TealLight,

    secondary = SoftBlue,
    onSecondary = Color.White,
    secondaryContainer = DarkCard,
    onSecondaryContainer = AquaGreen,

    tertiary = CoralPink,
    onTertiary = Color.White,
    tertiaryContainer = DarkCard,
    onTertiaryContainer = Color(0xFFFFB3D4),

    background = DarkBackground,
    onBackground = Platinum,

    surface = DarkSurface,
    onSurface = Platinum,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Silver,

    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF5F2120),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = CharcoalGray,
    outlineVariant = Color(0xFF49454F),
    scrim = Color.Black,

    inverseSurface = Platinum,
    inverseOnSurface = DarkBackground,
    inversePrimary = TealPrimary,

    surfaceTint = TealPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = LightTeal,
    onPrimaryContainer = TealDark,

    secondary = SoftBlue,
    onSecondary = Color.White,
    secondaryContainer = LightBlue,
    onSecondaryContainer = Color(0xFF001D36),

    tertiary = CoralPink,
    onTertiary = Color.White,
    tertiaryContainer = LightPink,
    onTertiaryContainer = Color(0xFF3E001D),

    background = Color(0xFFFFFBFE),
    onBackground = DeepGray,

    surface = Color.White,
    onSurface = DeepGray,
    surfaceVariant = Color(0xFFF3F0F4),
    onSurfaceVariant = CharcoalGray,

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color.Black,

    inverseSurface = DeepGray,
    inverseOnSurface = Platinum,
    inversePrimary = TealLight,

    surfaceTint = TealPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}