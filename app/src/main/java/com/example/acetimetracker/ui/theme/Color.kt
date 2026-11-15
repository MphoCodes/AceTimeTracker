package com.example.acetimetracker.ui.theme

import androidx.compose.ui.graphics.Color

// Ace brand: crimson, warm ivory and charcoal.
val AceRed = Color(0xFFB52A3B) // Deep Ruby Crimson (Tailwind Red 600)
val AceRedDark = Color(0xFF842030)
val AceRedLight = Color(0xFFF8E0E2)
val AceRedSubtle = Color(0xFFFCF0EE)

// Shared neutral tokens (names retained for existing components).
val Slate900 = Color(0xFF252421)
val Slate800 = Color(0xFF34322F)
val Slate700 = Color(0xFF494641)
val Slate600 = Color(0xFF625D56)
val Slate500 = Color(0xFF797269)
val Slate400 = Color(0xFFA49C92)
val Slate300 = Color(0xFFD4CCC1)
val Slate200 = Color(0xFFE8E1D7)
val Slate100 = Color(0xFFF2EDE5)
val Slate50 = Color(0xFFFAF7F2)

// Sophisticated Accents
val AccentEmerald = Color(0xFF10B981)
val AccentEmeraldLight = Color(0xFFD1FAE5)
val AccentAmber = Color(0xFFF59E0B)
val AccentAmberLight = Color(0xFFFEF3C7)
val AccentBlue = Color(0xFF2563EB)
val AccentBlueLight = Color(0xFFDBEAFE)
val AccentIndigo = Color(0xFF6366F1)
val AccentIndigoLight = Color(0xFFE0E7FF)

// Backwards-compatible Brand Tokens
val AceOrange = Color(0xFFF97316)
val AceDarkOrange = Color(0xFFC2410C)
val AceGreen = AccentEmerald
val AceDarkGreen = Color(0xFF047857)
val AceLightGreen = AccentEmeraldLight
val AceBlue = AccentBlue
val AceDarkBlue = Color(0xFF1D4ED8)
val AceLightBlue = AccentBlueLight
val AceYellow = AccentAmber
val AceLightYellow = AccentAmberLight

val DrawerGradientStart = Slate900
val DrawerGradientCenter = AceRed
val DrawerGradientEnd = AccentAmber

// Material 3 Color Roles (Light)
val PrimaryLight = AceRed
val OnPrimaryLight = Color.White
val PrimaryContainerLight = AceRedSubtle
val OnPrimaryContainerLight = AceRedDark

val SecondaryLight = Slate800
val OnSecondaryLight = Color.White
val SecondaryContainerLight = Slate100
val OnSecondaryContainerLight = Slate900

val TertiaryLight = AccentEmerald
val OnTertiaryLight = Color.White
val TertiaryContainerLight = AccentEmeraldLight
val OnTertiaryContainerLight = Color(0xFF065F46)

val BackgroundLight = Slate50
val OnBackgroundLight = Slate900
val SurfaceLight = Color.White
val OnSurfaceLight = Slate900
val SurfaceVariantLight = Slate100
val OnSurfaceVariantLight = Slate600
val OutlineLight = Slate300
val OutlineVariantLight = Slate200

// Material 3 Color Roles (Dark)
val PrimaryDark = Color(0xFFF87171)
val OnPrimaryDark = Color(0xFF450A0A)
val PrimaryContainerDark = Color(0xFF7F1D1D)
val OnPrimaryContainerDark = Color(0xFFF8E0E2)

val SecondaryDark = Slate200
val OnSecondaryDark = Slate900
val SecondaryContainerDark = Slate800
val OnSecondaryContainerDark = Slate100

val TertiaryDark = Color(0xFF6EE7B7)
val OnTertiaryDark = Color(0xFF064E3B)
val TertiaryContainerDark = Color(0xFF065F46)
val OnTertiaryContainerDark = Color(0xFFD1FAE5)

val BackgroundDark = Color(0xFF09090B)
val OnBackgroundDark = Color(0xFFF4F4F5)
val SurfaceDark = Color(0xFF18181B)
val OnSurfaceDark = Color(0xFFF4F4F5)
val SurfaceVariantDark = Color(0xFF27272A)
val OnSurfaceVariantDark = Color(0xFFA1A1AA)
val OutlineDark = Color(0xFF52525B)
val OutlineVariantDark = Color(0xFF3F3F46)

// Status & Progress Feedback Colors
val SuccessGreen = AccentEmerald
val SuccessGreenContainer = AccentEmeraldLight
val WarningYellow = AccentAmber
val WarningYellowContainer = AccentAmberLight
val InfoBlue = AccentBlue
val InfoBlueContainer = AccentBlueLight
