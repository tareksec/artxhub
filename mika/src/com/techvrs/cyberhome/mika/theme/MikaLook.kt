// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import app.lawnchair.hotseat.DisabledHotseat
import app.lawnchair.hotseat.LawnchairHotseat
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.LauncherAppState
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import com.patrykmichalik.opto.core.setBlocking
import com.techvrs.cyberhome.cyber.board.CyberLayout
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.cyber.theme.ThemeSnapshot

/**
 * Controller to apply and switch the Mika Theme look:
 * - Safely replaces the Screen 0 custom widget avoiding LoaderCursor overlap deletions.
 * - Backs up user favorites (Screen 0 + Hotseat) via ThemeSnapshot before any modification.
 * - Applies clean cozy wallpaper (without baked clocks) to system home and lockscreen.
 * - Reverts cleanly to CyberHome and restores all user favorites.
 */
object MikaLook {
    private const val TAG = "MikaLook"

    /** Applies the currently selected Mika wallpaper to the home screen and lockscreen */
    @WorkerThread
    fun applyWallpaper(context: Context, wallpaperRes: Int = MikaStore.getWallpaperRes(context)): Boolean = runCatching {
        val wm = WallpaperManager.getInstance(context)
        // 1. Set System Home Screen Wallpaper
        runCatching {
            val homeBmp = BitmapFactory.decodeResource(context.resources, wallpaperRes)
            if (homeBmp != null) {
                wm.setBitmap(homeBmp, null, true, WallpaperManager.FLAG_SYSTEM)
            } else {
                wm.setResource(wallpaperRes, WallpaperManager.FLAG_SYSTEM)
            }
        }.onFailure {
            wm.setResource(wallpaperRes, WallpaperManager.FLAG_SYSTEM)
        }

        // 2. Set System Lock Screen Wallpaper to CLEAN art (no baked clocks or UI)
        runCatching {
            val cleanLockRes = com.android.launcher3.R.drawable.mika_wallpaper_cat_bed
            val lockBmp = BitmapFactory.decodeResource(context.resources, cleanLockRes)
            if (lockBmp != null) {
                wm.setBitmap(lockBmp, null, true, WallpaperManager.FLAG_LOCK)
            } else {
                wm.setResource(cleanLockRes, WallpaperManager.FLAG_LOCK)
            }
        }.onFailure {
            wm.setResource(com.android.launcher3.R.drawable.mika_wallpaper_cat_bed, WallpaperManager.FLAG_LOCK)
        }

        Log.i(TAG, "Mika home and clean lockscreen wallpapers applied successfully.")
        true
    }.onFailure { Log.w(TAG, "Failed to apply Mika wallpaper", it) }.getOrDefault(false)

    /**
     * Places the Mika Cozy Board on Screen 0 of the workspace.
     * Captures an atomic snapshot of user favorites before rebuilding Screen 0.
     */
    @WorkerThread
    fun placeMikaBoard(context: Context, wallpaperRes: Int? = null): Boolean = runCatching {
        val app = context.applicationContext

        // 0. Safety Snapshot: Save screen 0 and hotseat before touching anything
        ThemeSnapshot.createSnapshot(app)

        // Activate Mika full icon theme
        MikaStore.setMikaActive(app, true)
        MikaIconTransformer.clearCache()

        // Apply wallpapers if specified
        if (wallpaperRes != null) {
            applyWallpaper(app, wallpaperRes)
        }

        // Request clean workspace reset so CyberLayout.onDefaultLayoutLoaded inserts MikaBoard
        CyberLayout.requestReset(app)

        MAIN_EXECUTOR.execute {
            // Hide system hotseat and bottom search bar on main thread (triggers activity.recreate)
            val prefs2 = PreferenceManager2.getInstance(app)
            prefs2.isHotseatEnabled.setBlocking(false)
            prefs2.hotseatMode.setBlocking(DisabledHotseat)

            LauncherAppState.getInstance(app).model.forceReload()
            LauncherAppState.getInstance(app).reloadIcons()
        }
        Log.i(TAG, "Mika board reset requested and model reload dispatched.")
        true
    }.onFailure { Log.w(TAG, "Failed to place Mika board", it) }.getOrDefault(false)

    /** Reverts the home screen back to the CyberHome Cozy Board */
    @WorkerThread
    fun placeCyberBoard(context: Context): Boolean = runCatching {
        val app = context.applicationContext

        // Deactivate Mika full icon theme
        MikaStore.setMikaActive(app, false)
        MikaIconTransformer.clearCache()

        // Request layout reset so onDefaultLayoutLoaded rebuilds with CozyBoard
        CyberLayout.requestReset(app)

        CyberWallpaper.apply(context)

        MAIN_EXECUTOR.execute {
            // Restore system hotseat and bottom search bar on main thread (triggers activity.recreate)
            val prefs2 = PreferenceManager2.getInstance(app)
            prefs2.isHotseatEnabled.setBlocking(true)
            prefs2.hotseatMode.setBlocking(LawnchairHotseat)

            LauncherAppState.getInstance(app).model.forceReload()
            LauncherAppState.getInstance(app).reloadIcons()
        }
        Log.i(TAG, "CyberHome board restored.")
        true
    }.onFailure { Log.w(TAG, "Failed to restore Cyber board", it) }.getOrDefault(false)
}
