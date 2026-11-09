package com.zonify.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Apple Music Dark & Vibrant Palette
val AppleMusicRed = Color(0xFFFA2D48)
val AppleMusicPink = Color(0xFFFF2D55)
val AppleMusicDarkBackground = Color(0xFF121212)
val AppleMusicSurfaceDark = Color(0xFF1C1C1E)
val AppleMusicCardDark = Color(0xFF2C2C2E)
val AppleMusicTextPrimary = Color(0xFFFFFFFF)
val AppleMusicTextSecondary = Color(0xFF8E8E93)
val AppleMusicDivider = Color(0xFF38383A)
val AudiophileGold = Color(0xFFFFD700)
val BitPerfectCyan = Color(0xFF00E5FF)

val DarkColorScheme = darkColorScheme(
    primary = AppleMusicPink,
    onPrimary = Color.White,
    secondary = AppleMusicRed,
    background = AppleMusicDarkBackground,
    surface = AppleMusicSurfaceDark,
    surfaceVariant = AppleMusicCardDark,
    onBackground = AppleMusicTextPrimary,
    onSurface = AppleMusicTextPrimary,
    onSurfaceVariant = AppleMusicTextSecondary
)
