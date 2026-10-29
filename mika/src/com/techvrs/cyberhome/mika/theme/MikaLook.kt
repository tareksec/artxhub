// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.theme

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import com.android.launcher3.widget.custom.CustomWidgetManager
import com.techvrs.cyberhome.cyber.board.CozyBoardWidget
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.cyber.theme.ThemeSnapshot
import com.techvrs.cyberhome.mika.board.MikaBoardWidget

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
     * Captures an atomic snapshot of user favorites before removing conflicting Screen 0 items.
     */
    @WorkerThread
    fun placeMikaBoard(context: Context): Boolean = runCatching {
        val app = context.applicationContext
        val db = LauncherAppState.getInstance(app).model.modelDbController
        val idp = InvariantDeviceProfile.INSTANCE.get(app)
        val provider = MikaBoardWidget.component()
        val widgetId = CustomWidgetManager.INSTANCE.get(app).allocateCustomAppWidgetId(provider)

        // 0. Safety Snapshot: Save screen 0 and hotseat before touching anything
        ThemeSnapshot.createSnapshot(app)

        // Activate Mika full icon theme and apply home & lockscreen wallpapers
        MikaStore.setMikaActive(app, true)
        MikaIconTransformer.clearCache()
        applyWallpaper(app)

        // 1. Clear items occupying Screen 0 of Desktop
        db.delete(
            Favorites.TABLE_NAME,
            "${Favorites.CONTAINER} = ${Favorites.CONTAINER_DESKTOP} AND ${Favorites.SCREEN} = 0",
            null,
        )

        // 2. Insert Mika Board covering Screen 0
        val values = ContentValues().apply {
            put(Favorites._ID, db.generateNewItemId())
            put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP)
            put(Favorites.SCREEN, 0)
            put(Favorites.CELLX, 0)
            put(Favorites.CELLY, 0)
            put(Favorites.SPANX, idp.numColumns)
            put(Favorites.SPANY, idp.numRows)
            put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_CUSTOM_APPWIDGET)
            put(Favorites.APPWIDGET_ID, widgetId)
            put(Favorites.APPWIDGET_PROVIDER, provider.flattenToString())
            put(Favorites.RESTORED, 0)
        }
        val inserted = db.insert(Favorites.TABLE_NAME, values) >= 0
        if (inserted) {
            MAIN_EXECUTOR.execute {
                LauncherAppState.getInstance(app).model.forceReload()
                LauncherAppState.getInstance(app).reloadIcons()
            }
            Log.i(TAG, "Mika board placed on workspace.")
        }
        inserted
    }.onFailure { Log.w(TAG, "Failed to place Mika board", it) }.getOrDefault(false)

    /** Reverts the home screen back to the CyberHome Cozy Board and restores user snapshot */
    @WorkerThread
    fun placeCyberBoard(context: Context): Boolean = runCatching {
        val app = context.applicationContext
        val db = LauncherAppState.getInstance(app).model.modelDbController
        val idp = InvariantDeviceProfile.INSTANCE.get(app)
        val provider = CozyBoardWidget.component()
        val widgetId = CustomWidgetManager.INSTANCE.get(app).allocateCustomAppWidgetId(provider)

        // Deactivate Mika full icon theme
        MikaStore.setMikaActive(app, false)
        MikaIconTransformer.clearCache()

        // 1. Try to restore previous user snapshot (including hotseat and desktop items)
        val restoredFromSnapshot = ThemeSnapshot.restoreLatestSnapshot(app)

        if (!restoredFromSnapshot) {
            // Fallback: Clear screen 0 and place CozyBoard
            db.delete(
                Favorites.TABLE_NAME,
                "${Favorites.CONTAINER} = ${Favorites.CONTAINER_DESKTOP} AND ${Favorites.SCREEN} = 0",
                null,
            )

            val values = ContentValues().apply {
                put(Favorites._ID, db.generateNewItemId())
                put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP)
                put(Favorites.SCREEN, 0)
                put(Favorites.CELLX, 0)
                put(Favorites.CELLY, 0)
                put(Favorites.SPANX, idp.numColumns)
                put(Favorites.SPANY, idp.numRows)
                put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_CUSTOM_APPWIDGET)
                put(Favorites.APPWIDGET_ID, widgetId)
                put(Favorites.APPWIDGET_PROVIDER, provider.flattenToString())
                put(Favorites.RESTORED, 0)
            }
            db.insert(Favorites.TABLE_NAME, values)
        }

        CyberWallpaper.apply(context)
        MAIN_EXECUTOR.execute {
            LauncherAppState.getInstance(app).model.forceReload()
            LauncherAppState.getInstance(app).reloadIcons()
        }
        Log.i(TAG, "CyberHome board restored (snapshot restored: $restoredFromSnapshot).")
        true
    }.onFailure { Log.w(TAG, "Failed to restore Cyber board", it) }.getOrDefault(false)
}
