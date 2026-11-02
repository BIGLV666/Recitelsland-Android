package com.biglv.recitelsland.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 海岛青绿 + 暖沙。品牌色不依赖动态取色，保证上架后视觉一致。
val Teal40 = Color(0xFF2F6F6B)
val Teal80 = Color(0xFF8FD1C9)
val Teal90 = Color(0xFFB7E4DE)
val Teal10 = Color(0xFF00201E)
val Teal20 = Color(0xFF0B3733)
val Teal30 = Color(0xFF15514C)
val Sand40 = Color(0xFF8A6A2F)
val Sand80 = Color(0xFFE2C48B)
val Sand90 = Color(0xFFF7E3BC)
val Sand10 = Color(0xFF2A1C00)

val LightBackground = Color(0xFFF7F9F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE4EBE9)
val LightOnSurfaceVariant = Color(0xFF414947)
val LightOutline = Color(0xFF71807D)

val DarkBackground = Color(0xFF0F1514)
val DarkSurface = Color(0xFF171F1E)
val DarkSurfaceVariant = Color(0xFF2A3634)
val DarkOnSurfaceVariant = Color(0xFFC0CAC7)
val DarkOutline = Color(0xFF8A9A96)

data class SemanticColors(
    val correct: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val incorrect: Color,
    val incorrectContainer: Color,
    val onIncorrectContainer: Color,
)

val LightSemanticColors = SemanticColors(
    correct = Color(0xFF2E7D4F),
    correctContainer = Color(0xFFDDF3E4),
    onCorrectContainer = Color(0xFF0B3D22),
    incorrect = Color(0xFFB3261E),
    incorrectContainer = Color(0xFFF9DEDC),
    onIncorrectContainer = Color(0xFF410E0B),
)

val DarkSemanticColors = SemanticColors(
    correct = Color(0xFF8ED9A6),
    correctContainer = Color(0xFF17402A),
    onCorrectContainer = Color(0xFFBFEBCE),
    incorrect = Color(0xFFF2B8B5),
    incorrectContainer = Color(0xFF4A1614),
    onIncorrectContainer = Color(0xFFF9DEDC),
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
