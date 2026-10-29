// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.theme

import android.graphics.Color

/**
 * Unified Theme Specification representing the entire visual identity of CyberHome:
 * Colors (light & dark), Typography, Shapes, Spring Motion, Glass Surfaces,
 * Status & Navigation Bars, Icon Transformers, Wallpapers, Lockscreen,
 * Control Center, and System Palette (Material You ADB Mode).
 */
data class CyberThemeSpec(
    val id: String,
    val name: String,
    val colors: ThemeColors,
    val colorsDark: ThemeColors,
    val type: ThemeType = ThemeType(),
    val shape: ThemeShape = ThemeShape(),
    val motion: ThemeMotion = ThemeMotion(),
    val surfaces: ThemeSurfaces = ThemeSurfaces(),
    val systemBars: SystemBarSpec = SystemBarSpec(),
    val iconStyle: IconStyleSpec = IconStyleSpec(),
    val wallpaper: WallpaperSpec = WallpaperSpec(),
    val lock: LockSpec? = null,
    val controlCenter: ControlCenterSpec = ControlCenterSpec(),
    val systemPalette: SystemPaletteSpec? = null,
)

data class ThemeColors(
    val bg: Int,
    val surface: Int,
    val surfaceGlass: Int,
    val border: Int,
    val ink: Int,
    val inkBody: Int,
    val inkDim: Int,
    val accent: Int,
    val accentSoft: Int,
    val cardBackground: Int,
    val success: Int = 0xFF4CAF50.toInt(),
    val warning: Int = 0xFFFF9800.toInt(),
    val danger: Int = 0xFFE57373.toInt(),
)

data class ThemeType(
    val displayFontFamily: String = "nunito",
    val bodyFontFamily: String = "nunito",
    val clockScale: Float = 1.0f,
    val bodyScale: Float = 1.0f,
)

data class ThemeShape(
    val cornerRadiusSmallDp: Float = 12f,
    val cornerRadiusMediumDp: Float = 20f,
    val cornerRadiusLargeDp: Float = 28f,
    val iconShape: String = "squircle", // "squircle", "circle", "rounded_rect"
)

data class ThemeMotion(
    val snappyStiffness: Float = 800f,
    val snappyDamping: Float = 0.85f,
    val smoothStiffness: Float = 380f,
    val smoothDamping: Float = 0.9f,
    val bouncyStiffness: Float = 500f,
    val bouncyDamping: Float = 0.6f,
)

data class ThemeSurfaces(
    val glassAlpha: Float = 0.82f,
    val blurRadiusDp: Float = 20f,
    val borderWidthDp: Float = 1f,
    val shadowAlpha: Float = 0.08f,
)

data class SystemBarSpec(
    val lightIconsOnLight: Boolean = false, // false = dark icons for light theme
    val lightIconsOnDark: Boolean = true,   // true = light icons for dark theme
    val topScrimColor: Int = 0x80FBF6EF.toInt(),
    val bottomScrimColor: Int = 0x00000000,
)

data class IconStyleSpec(
    val transformerId: String = "default", // "mika_3d_clay", "cyber_hud", etc.
    val squircleRadiusFraction: Float = 0.28f,
    val innerScale: Float = 0.58f,
    val useOfficial3DIfAvailable: Boolean = true,
)

data class WallpaperSpec(
    val homeWallpaperResName: String = "",
    val lockWallpaperResName: String = "",
    val darkHomeWallpaperResName: String = "",
)

data class LockSpec(
    val layout: String = "mika", // "mika", "cyber", "default"
    val defaultNoteQuote: String = "Small steps, big dreams ♡",
    val defaultDisplayName: String = "Fonseca 🦋",
    val showIdleMusicCard: Boolean = true,
    val supportEasterEggPaw: Boolean = true,
)

data class ControlCenterSpec(
    val style: String = "mika_glass", // "mika_glass", "cyber_hud"
    val cardBackground: Int = 0xCCEFE5DC.toInt(),
    val tileAccent: Int = 0xFFF4A3B4.toInt(),
)

data class SystemPaletteSpec(
    val seedHex: String = "F4A3B4",
    val themeStyle: String = "TONAL_SPOT", // "TONAL_SPOT", "VIBRANT", "EXPRESSIVE"
)

/**
 * Built-in Theme Definitions
 */
object ThemeSpecs {
    val MIKA = CyberThemeSpec(
        id = "mika",
        name = "Mika Cozy Cat",
        colors = ThemeColors(
            bg = 0xFFFBF6EF.toInt(),          // Warm cream
            surface = 0xFFF4EDE2.toInt(),
            surfaceGlass = 0xCCEFE5DC.toInt(),
            border = 0x66E8D2C1.toInt(),
            ink = 0xFF4A3728.toInt(),         // Mocha brown
            inkBody = 0xFF6B5848.toInt(),
            inkDim = 0xFF9C8778.toInt(),
            accent = 0xFFF4A3B4.toInt(),      // Pastel pink
            accentSoft = 0xFFFFD7DE.toInt(),
            cardBackground = 0xFFF8EFE4.toInt(),
        ),
        colorsDark = ThemeColors(
            bg = 0xFF2A211D.toInt(),          // Cocoa Night Café
            surface = 0xFF352B26.toInt(),
            surfaceGlass = 0xCC3A2E28.toInt(),
            border = 0x6655453E.toInt(),
            ink = 0xFFF6E7DA.toInt(),         // Cream white ink
            inkBody = 0xFFD8C3B3.toInt(),
            inkDim = 0xFFA59284.toInt(),
            accent = 0xFFF4A3B4.toInt(),
            accentSoft = 0xFF7A4A53.toInt(),
            cardBackground = 0xFF352B26.toInt(),
        ),
        systemBars = SystemBarSpec(
            lightIconsOnLight = false,
            lightIconsOnDark = true,
            topScrimColor = 0x40FBF6EF.toInt(),
        ),
        iconStyle = IconStyleSpec(
            transformerId = "mika_3d_clay",
            squircleRadiusFraction = 0.28f,
            innerScale = 0.58f,
            useOfficial3DIfAvailable = true,
        ),
        lock = LockSpec(
            layout = "mika",
            defaultNoteQuote = "Small steps, big dreams ♡",
            defaultDisplayName = "Fonseca 🦋",
            showIdleMusicCard = true,
            supportEasterEggPaw = true,
        ),
        systemPalette = SystemPaletteSpec(
            seedHex = "F4A3B4",
            themeStyle = "TONAL_SPOT",
        ),
    )

    val CYBER = CyberThemeSpec(
        id = "cyber",
        name = "CyberHome",
        colors = ThemeColors(
            bg = 0xFF0B0C0F.toInt(),
            surface = 0xFF15171C.toInt(),
            surfaceGlass = 0xCC1A1D24.toInt(),
            border = 0x3336D1DC.toInt(),
            ink = 0xFFE0E6ED.toInt(),
            inkBody = 0xFFA0AAB5.toInt(),
            inkDim = 0xFF656F7D.toInt(),
            accent = 0xFF36D1DC.toInt(),      // Cyan
            accentSoft = 0xFF5B86E5.toInt(),
            cardBackground = 0xFF15171C.toInt(),
        ),
        colorsDark = ThemeColors(
            bg = 0xFF0B0C0F.toInt(),
            surface = 0xFF15171C.toInt(),
            surfaceGlass = 0xCC1A1D24.toInt(),
            border = 0x3336D1DC.toInt(),
            ink = 0xFFE0E6ED.toInt(),
            inkBody = 0xFFA0AAB5.toInt(),
            inkDim = 0xFF656F7D.toInt(),
            accent = 0xFF36D1DC.toInt(),
            accentSoft = 0xFF5B86E5.toInt(),
            cardBackground = 0xFF15171C.toInt(),
        ),
        systemBars = SystemBarSpec(
            lightIconsOnLight = true,
            lightIconsOnDark = true,
        ),
        systemPalette = SystemPaletteSpec(
            seedHex = "36D1DC",
            themeStyle = "VIBRANT",
        ),
    )
}
