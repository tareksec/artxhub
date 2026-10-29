// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.theme

import android.graphics.Color
import android.os.Build
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Ensures status bar and navigation bar have pixel-perfect contrast, transparency,
 * and theme-driven light/dark icon styling across all CyberHome windows.
 */
object SystemBarStyler {

    fun apply(window: Window, spec: SystemBarSpec, isDark: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val lightIcons = if (isDark) spec.lightIconsOnDark else spec.lightIconsOnLight

        // In WindowInsetsControllerCompat:
        // isAppearanceLightStatusBars = true means DARK icons on light background.
        // isAppearanceLightStatusBars = false means LIGHT/WHITE icons on dark background.
        controller.isAppearanceLightStatusBars = !lightIcons
        controller.isAppearanceLightNavigationBars = !lightIcons
    }
}
