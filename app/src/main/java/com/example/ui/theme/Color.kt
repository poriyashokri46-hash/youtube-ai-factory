package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Colors: Deep Navy, Neon Turquoise / Cyan, Electric Violet / Purple
val DeepNavy = Color(0xFF0A0F1D)
val SurfaceNavy = Color(0xFF11192C)
val SurfaceNavyElevated = Color(0xFF192238)
val SurfaceNavyCard = Color(0xFF151D33)
val BorderNavy = Color(0xFF263352)

val NeonCyan = Color(0xFF00E5FF)
val CyanGlow = Color(0x3300E5FF)
val ElectricViolet = Color(0xFF9D4EDD)
val VioletGlow = Color(0x339D4EDD)
val CoralRed = Color(0xFFFF3366)

val TextWhite = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val TextDark = Color(0xFF64748B)

val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Gradient Brushes
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(NeonCyan, ElectricViolet)
)

val CardGlowGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1C2744), Color(0xFF11192C))
)

val AccentGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF00E5FF), Color(0xFF7928CA), Color(0xFFFF0080))
)
