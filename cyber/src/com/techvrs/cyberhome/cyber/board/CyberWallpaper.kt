// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.board

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import com.techvrs.cyberhome.cyber.design.CyberThemeMode
import java.io.File
import java.io.InputStream
import kotlin.math.max

/**
 * Home screen wallpaper: "auto" (a bundled light / dark pair that follows the theme), one bundled
 * wallpaper, or the user's own uploaded image. Only the home screen is changed (FLAG_SYSTEM).
 */
object CyberWallpaper {
    const val AUTO = "auto"
    const val CUSTOM = "custom"

    val AUTO_LIGHT = BuiltInWallpaper.SNOW
    val AUTO_DARK = BuiltInWallpaper.LOUNGE

    private const val PREFS = "cyber_look"
    private const val KEY_CHOICE = "wallpaper_choice"
    private const val KEY_ID = "wallpaper_id"
    private const val KEY_DARK = "wallpaper_dark"

    fun choice(context: Context): String = prefs(context).getString(KEY_CHOICE, AUTO) ?: AUTO

    fun customFile(context: Context) = File(context.filesDir, "wallpaper.jpg")

    /** Saves the choice and applies it. Blocking; call off the main thread. */
    @WorkerThread
    fun choose(context: Context, choice: String): Boolean {
        prefs(context).edit { putString(KEY_CHOICE, choice) }
        return apply(context)
    }

    /** Stores [uri] (downscaled to the screen) as the custom wallpaper and applies it. Blocking. */
    @WorkerThread
    fun upload(context: Context, uri: Uri): Boolean = upload(context) { context.contentResolver.openInputStream(uri) }

    /** Same as [upload] for any image source ([open] is called twice: bounds, then pixels). */
    @WorkerThread
    fun upload(context: Context, open: () -> InputStream?): Boolean = runCatching {
        val dm = context.resources.displayMetrics
        val maxSide = max(dm.widthPixels, dm.heightPixels)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bitmap = open()!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })!!
        }
        customFile(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        choose(context, CUSTOM)
    }.getOrDefault(false)

    /** Applies the current choice for the current light/dark mode. Blocking. */
    @WorkerThread
    fun apply(context: Context): Boolean = runCatching {
        val dark = CyberThemeMode.isDark(context)
        val manager = WallpaperManager.getInstance(context)
        when (val choice = choice(context)) {
            CUSTOM -> customFile(context).inputStream().use { manager.setStream(it, null, true, WallpaperManager.FLAG_SYSTEM) }
            else -> {
                val wallpaper = if (choice == AUTO) {
                    if (dark) AUTO_DARK else AUTO_LIGHT
                } else {
                    runCatching { BuiltInWallpaper.valueOf(choice) }.getOrDefault(AUTO_LIGHT)
                }
                manager.setResource(wallpaper.res, WallpaperManager.FLAG_SYSTEM)
            }
        }
        prefs(context).edit {
            putInt(KEY_ID, manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM))
            putBoolean(KEY_DARK, dark)
        }
        true
    }.getOrDefault(false)

    /**
     * After a light/dark switch: swaps the "auto" wallpaper to the matching one, but only while
     * the user still has the wallpaper we set (never replaces one they picked elsewhere). Blocking.
     */
    @WorkerThread
    fun sync(context: Context) {
        val prefs = prefs(context)
        if (choice(context) != AUTO) return
        val savedId = prefs.getInt(KEY_ID, -1)
        if (savedId < 0) return
        val current = runCatching { WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        if (current != savedId) return
        if (prefs.getBoolean(KEY_DARK, false) != CyberThemeMode.isDark(context)) apply(context)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
