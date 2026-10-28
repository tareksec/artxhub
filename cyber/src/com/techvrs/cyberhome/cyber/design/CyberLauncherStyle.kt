// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.theme.color.ColorOption
import com.android.launcher3.R
import com.patrykmichalik.opto.core.firstBlocking
import com.patrykmichalik.opto.core.setBlocking

/**
 * Matches the rest of the launcher to the Cozy Board (bug-fix/03 §5) through Lawnchair's own
 * preferences, so no upstream code changes: a soft pill behind the dock in our surface color.
 * The dock search bar keeps Lawnchair's themed, rounded default. Icon shape (squircle) is a config default (cyber_config.xml); drawer and folders
 * follow the launcher theme plus our pink accent.
 *
 * Lawnchair's dock color is one fixed color for both modes, so [syncDock] swaps it to the light or
 * dark surface when the mode changes, as long as the user has not picked an own color.
 */
object CyberLauncherStyle {
    private const val DOCK_ALPHA = 82

    /** Part of "Apply look". Settings the user changes later in Lawnchair settings stay theirs. */
    @WorkerThread
    fun apply(context: Context) {
        val prefs = PreferenceManager.getInstance(context)
        if (!prefs.hotseatBG.get()) prefs.hotseatBG.set(true)
        if (prefs.hotseatBGAlpha.get() != DOCK_ALPHA) prefs.hotseatBGAlpha.set(DOCK_ALPHA)
        syncDock(context, force = true)
    }

    /** Sets the dock color to the surface of the current mode (only if it is still ours). */
    @WorkerThread
    fun syncDock(context: Context, force: Boolean = false) {
        val prefs2 = PreferenceManager2.getInstance(context)
        val current = prefs2.hotseatBackgroundColor.firstBlocking()
        val ours = setOf(surface(context, dark = false), surface(context, dark = true))
        if (!force && (current !is ColorOption.CustomColor || current.color !in ours)) return
        val wanted = ColorOption.CustomColor(surface(context, CyberThemeMode.isDark(context)))
        if (current != wanted) prefs2.hotseatBackgroundColor.setBlocking(wanted)
    }

    private fun surface(context: Context, dark: Boolean): Int {
        val config = Configuration(context.resources.configuration)
        val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        config.uiMode = config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or night
        return ContextCompat.getColor(context.createConfigurationContext(config), R.color.cyber_surface)
    }
}
