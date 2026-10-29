// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.theme

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import org.json.JSONObject

/**
 * Controller for optional "Advanced ADB Mode" system palette customization (Android 12+ Material You).
 * Safely backs up original system palette and applies custom theme colors.
 */
object SystemPaletteController {
    private const val TAG = "SystemPalette"
    private const val PREFS_NAME = "cyber_system_palette_prefs"
    private const val KEY_PREV_PALETTE = "previous_theme_customization"
    private const val SETTING_THEME_CUSTOMIZATION = "theme_customization_overlay_packages"

    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val check = context.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
        return check == PackageManager.PERMISSION_GRANTED
    }

    fun apply(context: Context, spec: SystemPaletteSpec): Boolean = runCatching {
        if (!isAvailable(context)) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted. Cannot apply system palette.")
            return false
        }

        val resolver = context.contentResolver
        val current = Settings.Secure.getString(resolver, SETTING_THEME_CUSTOMIZATION)

        // Save original if not already saved
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_PREV_PALETTE) && current != null) {
            prefs.edit().putString(KEY_PREV_PALETTE, current).apply()
        }

        val payload = JSONObject().apply {
            put("android.theme.customization.color_source", "preset")
            put("android.theme.customization.system_palette", spec.seedHex)
            put("android.theme.customization.accent_color", spec.seedHex)
            put("android.theme.customization.theme_style", spec.themeStyle)
            put("_applied_timestamp", System.currentTimeMillis())
        }

        val success = Settings.Secure.putString(resolver, SETTING_THEME_CUSTOMIZATION, payload.toString())
        Log.i(TAG, "Applied system palette ${spec.seedHex}: $success")
        success
    }.onFailure { Log.e(TAG, "Failed to apply system palette", it) }.getOrDefault(false)

    fun restore(context: Context): Boolean = runCatching {
        if (!isAvailable(context)) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prev = prefs.getString(KEY_PREV_PALETTE, null) ?: return false

        val resolver = context.contentResolver
        val success = Settings.Secure.putString(resolver, SETTING_THEME_CUSTOMIZATION, prev)
        prefs.edit().remove(KEY_PREV_PALETTE).apply()
        Log.i(TAG, "Restored previous system palette: $success")
        success
    }.onFailure { Log.e(TAG, "Failed to restore system palette", it) }.getOrDefault(false)
}
