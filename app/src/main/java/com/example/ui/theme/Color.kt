package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors
val Primary = Color(0xFF5B7CF6)
val Secondary = Color(0xFF7D96F8)
val Tertiary = Color(0xFF4E6EEA)

val Background = Color(0xFFF7F8FC)
val Surface = Color(0xFFFFFFFF)

val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFFFFFFFF)
val OnTertiary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF1A1D21)
val OnSurface = Color(0xFF1A1D21)
val OnSurfaceVariant = Color(0xFF6B7280)

val PrimaryContainer = Color(0xFFEEF2FF)
val SecondaryContainer = Color(0xFFF4F6FF)
val TertiaryContainer = Color(0xFFE0E7FF)

val OnPrimaryContainer = Color(0xFF1E40AF)
val OnSecondaryContainer = Color(0xFF1E3A8A)
val OnTertiaryContainer = Color(0xFF1E1B4B)

val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)

val SuccessContainer = Color(0xFFD1FAE5)
val WarningContainer = Color(0xFFFEF3C7)
val ErrorContainer = Color(0xFFFEE2E2)

val OnSuccess = Color(0xFFFFFFFF)
val OnWarning = Color(0xFFFFFFFF)
val OnError = Color(0xFFFFFFFF)

val OnSuccessContainer = Color(0xFF065F46)
val OnWarningContainer = Color(0xFF92400E)
val OnErrorContainer = Color(0xFF991B1B)

val Outline = Color(0xFFE5E7EB)
val OutlineVariant = Color(0xFFF3F4F6)

// Dark theme colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkOnBackground = Color(0xFFE6E6E6)
val DarkOnSurface = Color(0xFFE6E6E6)
val DarkOnSurfaceVariant = Color(0xFFA0A0A0)
val DarkOutline = Color(0xFF3C3C3C)
val DarkOutlineVariant = Color(0xFF2C2C2C)

val DarkPrimaryContainer = Color(0xFF1E3A5F)
val DarkSecondaryContainer = Color(0xFF1E3A5F)
val DarkTertiaryContainer = Color(0xFF2D1B69)
val DarkOnPrimaryContainer = Color(0xFFB3C7FF)
val DarkOnSecondaryContainer = Color(0xFFB3C7FF)
val DarkOnTertiaryContainer = Color(0xFFC4B5FF)

val DarkSuccess = Color(0xFF34D399)
val DarkWarning = Color(0xFFFBBF24)
val DarkError = Color(0xFFF87171)

val DarkSuccessContainer = Color(0xFF064E3B)
val DarkWarningContainer = Color(0xFF78350F)
val DarkErrorContainer = Color(0xFF7F1D1D)

val DarkOnSuccess = Color(0xFF1A1D21)
val DarkOnWarning = Color(0xFF1A1D21)
val DarkOnError = Color(0xFF1A1D21)

val DarkOnSuccessContainer = Color(0xFFA7F3D0)
val DarkOnWarningContainer = Color(0xFFFDE68A)
val DarkOnErrorContainer = Color(0xFFFECACA)

// Theme-aware semantic color helpers
@Composable
fun themeSuccess(): Color = if (isSystemInDarkTheme()) DarkSuccess else Success

@Composable
fun themeWarning(): Color = if (isSystemInDarkTheme()) DarkWarning else Warning

@Composable
fun themeSuccessContainer(): Color = if (isSystemInDarkTheme()) DarkSuccessContainer else SuccessContainer

@Composable
fun themeOnSuccessContainer(): Color = if (isSystemInDarkTheme()) DarkOnSuccessContainer else OnSuccessContainer

@Composable
fun themeWarningContainer(): Color = if (isSystemInDarkTheme()) DarkWarningContainer else WarningContainer

@Composable
fun themeOnWarningContainer(): Color = if (isSystemInDarkTheme()) DarkOnWarningContainer else OnWarningContainer
