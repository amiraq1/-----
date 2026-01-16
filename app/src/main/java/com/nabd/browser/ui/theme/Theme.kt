package com.nabd.browser.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ========================================
// تعريف الألوان
// ========================================

// Primary Colors
val NabdPrimary = Color(0xFF6366F1)
val NabdPrimaryDark = Color(0xFF4F46E5)
val NabdPrimaryLight = Color(0xFF818CF8)

// Secondary Colors
val NabdSecondary = Color(0xFFEC4899)
val NabdSecondaryDark = Color(0xFFDB2777)

// Tertiary/Accent Colors
val NabdTertiary = Color(0xFF14B8A6)
val NabdTertiaryDark = Color(0xFF0D9488)

// Background & Surface - Light Theme
val BackgroundLight = Color(0xFFFAFAFA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)

// Background & Surface - Dark Theme
val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceVariantDark = Color(0xFF334155)

// Text Colors - Light
val OnBackgroundLight = Color(0xFF1F2937)
val OnSurfaceLight = Color(0xFF1F2937)
val OnSurfaceVariantLight = Color(0xFF6B7280)

// Text Colors - Dark
val OnBackgroundDark = Color(0xFFF1F5F9)
val OnSurfaceDark = Color(0xFFF1F5F9)
val OnSurfaceVariantDark = Color(0xFF94A3B8)

// Status Colors
val NabdSuccess = Color(0xFF10B981)
val NabdWarning = Color(0xFFF59E0B)
val NabdError = Color(0xFFEF4444)

// ========================================
// تعريف أنظمة الألوان
// ========================================

private val LightColorScheme = lightColorScheme(
    primary = NabdPrimary,
    onPrimary = Color.White,
    primaryContainer = NabdPrimaryLight,
    onPrimaryContainer = NabdPrimaryDark,
    
    secondary = NabdSecondary,
    onSecondary = Color.White,
    secondaryContainer = NabdSecondary.copy(alpha = 0.2f),
    onSecondaryContainer = NabdSecondaryDark,
    
    tertiary = NabdTertiary,
    onTertiary = Color.White,
    tertiaryContainer = NabdTertiary.copy(alpha = 0.2f),
    onTertiaryContainer = NabdTertiaryDark,
    
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    
    error = NabdError,
    onError = Color.White,
    errorContainer = NabdError.copy(alpha = 0.2f),
    onErrorContainer = NabdError,
    
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFF3F4F6)
)

private val DarkColorScheme = darkColorScheme(
    primary = NabdPrimaryLight,
    onPrimary = NabdPrimaryDark,
    primaryContainer = NabdPrimary,
    onPrimaryContainer = Color.White,
    
    secondary = NabdSecondary,
    onSecondary = Color.White,
    secondaryContainer = NabdSecondaryDark,
    onSecondaryContainer = Color.White,
    
    tertiary = NabdTertiary,
    onTertiary = Color.White,
    tertiaryContainer = NabdTertiaryDark,
    onTertiaryContainer = Color.White,
    
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    
    error = NabdError,
    onError = Color.White,
    errorContainer = NabdError.copy(alpha = 0.3f),
    onErrorContainer = Color.White,
    
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155)
)

// ========================================
// الثيم الرئيسي
// ========================================

@Composable
fun NabdBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NabdTypography,
        content = content
    )
}
