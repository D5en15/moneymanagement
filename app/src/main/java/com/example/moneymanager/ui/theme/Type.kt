package com.example.moneymanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Script Specs:
// Title: 34sp, 800
// Section: 20sp, 700
// List Title: 18sp, 700
// List Subtitle: 14sp, 400-500
// Amount: 20sp, 800

val Typography = Typography(
    // Large Title "Accounts"
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    // Section Title "My Accounts"
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    // List Card Title "Bank"
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    // List Card Subtitle "Bank"
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Amount in List
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    // Net Worth Big Amount (Custom)
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 40.sp, // ~40-44sp
        lineHeight = 48.sp
    )
)
