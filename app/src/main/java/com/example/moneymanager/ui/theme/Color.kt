package com.example.moneymanager.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==============================================================================
// THEME TOKENS
// ==============================================================================

// 1) Brand / Accent
val BrandPurple = Color(0xFF8B5CF6)      // Violet 500 (Main Brand)
val BrandPurpleDark = Color(0xFF7C3AED)  // Violet 600 (Darker for Light Mode Text/Primary)
val BrandPurpleText = Color(0xFF6D28D9)  // Violet 700 (High contrast text)

val BrandBlue = Color(0xFF3B82F6)
val BrandBlueDark = Color(0xFF2563EB)

// 2) Light Mode
val LightBackground = Color(0xFFF5F6FB) // Soft lavender-gray
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFF0F2FA)   // Subtle tinted surface

// Text: Darkened for better contrast against LightBackground
val LightTextPrimary = Color(0xFF0F172A)   // Slate 900 (Deepest Navy/Black)
val LightTextSecondary = Color(0xFF475569) // Slate 600 (Darker than before for legibility)

val LightDivider = Color(0xFFE2E8F0)    // Slate 200
val LightShadow = Color(0x1A0F172A)     // 10% Slate 900

// 3) Dark Mode
val DarkBackground = Color(0xFF070A14)  // Deep navy
val DarkSurface = Color(0xFF0D1224)
val DarkSurface2 = Color(0xFF111A33)
val DarkCardGlass = Color(0xCC0F1730)   // ~80% alpha
val DarkTextPrimary = Color(0xFFF3F4FF)
val DarkTextSecondary = Color(0xFFA7B0D6)
val DarkDivider = Color(0xFF223055)
val DarkShadow = Color(0x66000000)      // ~40% alpha

// 4) Gradients
val DonutGradientColors = listOf(BrandPurple, BrandBlue)
val DonutHighlight = Color(0xFFA78BFA) // Lavender slice accent

val DarkTopGradientColors = listOf(Color(0xFF0B1022), Color(0xFF070A14))
val LightTopGradientColors = listOf(Color(0xFFEEF0FF), Color(0xFFF5F6FB))

// Common
val SuccessGreen = Color(0xFF22C55E)
val ErrorRed = Color(0xFFEF4444)

// Account Colors (15 distinct colors - Material 500/600 for contrast on both Light/Dark)
val AccountColors = listOf(
    Color(0xFFF44336), // Red 500
    Color(0xFFE91E63), // Pink 500
    Color(0xFF9C27B0), // Purple 500
    Color(0xFF673AB7), // Deep Purple 500
    Color(0xFF3F51B5), // Indigo 500
    Color(0xFF2196F3), // Blue 500
    Color(0xFF03A9F4), // Light Blue 500
    Color(0xFF00BCD4), // Cyan 500
    Color(0xFF009688), // Teal 500
    Color(0xFF4CAF50), // Green 500
    Color(0xFF8BC34A), // Light Green 500
    Color(0xFFCDDC39), // Lime 500
    Color(0xFFFFC107), // Amber 500
    Color(0xFFFF9800), // Orange 500
    Color(0xFF795548)  // Brown 500
)