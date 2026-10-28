// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.ui.preferences.components.ThemeChoice

/**
 * Light ("Milk tea") / dark ("Night cafe") switch. The choice is Lawnchair's own
 * "pref_launcherTheme" (light / dark / system), so the drawer, folders and settings of the
 * launcher always match our widgets and screens.
 *
 * Colors live in values/ and values-night/cyber_colors.xml. Code that resolves them (widgets,
 * our activities) uses [themed], which forces the night qualifier to the chosen mode; widget
 * layouts therefore get every theme color from code, not from @color in XML (the host would
 * resolve those with its own, possibly different, configuration).
 */
object CyberThemeMode {

    fun choice(context: Context): String =
        runCatching { PreferenceManager.getInstance(context).launcherTheme.get() }.getOrNull() ?: ThemeChoice.SYSTEM

    /** Saves the choice; CyberRuntime sees the change and redraws widgets and wallpaper. */
    fun set(context: Context, choice: String) {
        PreferenceManager.getInstance(context).launcherTheme.set(choice)
    }

    fun isDark(context: Context): Boolean = when (choice(context)) {
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
        else -> systemIsDark(context)
    }

    fun systemIsDark(context: Context): Boolean =
        context.applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    /** [context] with its night qualifier set to the chosen mode (resources only). */
    fun themed(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        val night = if (isDark(context)) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        if (config.uiMode and Configuration.UI_MODE_NIGHT_MASK == night) return context
        config.uiMode = config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or night
        return context.createConfigurationContext(config)
    }
}

/**
 * Base for our own screens: resources follow [CyberThemeMode], system bar icons match the
 * background (dark icons on "Milk tea", light icons on "Night cafe").
 */
abstract class CyberActivity : ComponentActivity() {
    private var dark = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CyberThemeMode.themed(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        dark = CyberThemeMode.isDark(this)
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        enableEdgeToEdge(style, style)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        // The mode may have changed in another screen (or the system switched day/night).
        if (CyberThemeMode.isDark(this) != dark) recreate()
    }
}
