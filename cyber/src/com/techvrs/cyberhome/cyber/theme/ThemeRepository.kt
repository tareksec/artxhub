// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the active CyberHome theme.
 * Emits updates to all observing surfaces (Home, Icons, All Apps, Lock Screen, Control Center).
 */
object ThemeRepository {
    private const val PREFS_NAME = "cyber_theme_prefs"
    private const val KEY_ACTIVE_THEME = "active_theme_id"

    private val _themeState = MutableStateFlow(ThemeSpecs.MIKA)
    val themeState: StateFlow<CyberThemeSpec> = _themeState.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        val themeId = getPrefs(context).getString(KEY_ACTIVE_THEME, "mika") ?: "mika"
        _themeState.value = resolveSpec(themeId)
    }

    fun getActiveTheme(context: Context): CyberThemeSpec {
        val themeId = getPrefs(context).getString(KEY_ACTIVE_THEME, null)
        if (themeId != null && themeId != _themeState.value.id) {
            _themeState.value = resolveSpec(themeId)
        }
        return _themeState.value
    }

    fun setActiveTheme(context: Context, themeId: String) {
        getPrefs(context).edit().putString(KEY_ACTIVE_THEME, themeId).apply()
        _themeState.value = resolveSpec(themeId)
    }

    private fun resolveSpec(themeId: String): CyberThemeSpec {
        return when (themeId.lowercase()) {
            "mika" -> ThemeSpecs.MIKA
            "cyber" -> ThemeSpecs.CYBER
            else -> ThemeSpecs.MIKA
        }
    }
}
