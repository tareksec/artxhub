// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.theme

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import com.android.launcher3.widget.custom.CustomWidgetManager
import com.techvrs.cyberhome.cyber.board.CozyBoardWidget
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.mika.board.MikaBoardWidget

/**
 * Controller to apply and switch the Mika Theme look:
 * - Safely replaces the Screen 0 custom widget avoiding LoaderCursor overlap deletions.
 * - Applies the selected cozy wallpaper.
 * - Supports reverting cleanly to CyberHome.
 */
object MikaLook {
    private const val TAG = "MikaLook"

    /** Applies the currently selected Mika wallpaper to the home screen */
    @WorkerThread
    fun applyWallpaper(context: Context, wallpaperRes: Int = MikaStore.getWallpaperRes(context)): Boolean = runCatching {
        val wm = WallpaperManager.getInstance(context)
        wm.setResource(wallpaperRes, WallpaperManager.FLAG_SYSTEM)
        Log.i(TAG, "Mika wallpaper ($wallpaperRes) applied successfully.")
        true
    }.onFailure { Log.w(TAG, "Failed to apply Mika wallpaper", it) }.getOrDefault(false)

    /**
     * Places the Mika Cozy Board on Screen 0 of the workspace.
     * Crucial fix: Deletes existing widgets on Screen 0 first to eliminate LoaderCursor position overlap!
     */
    @WorkerThread
    fun placeMikaBoard(context: Context): Boolean = runCatching {
        val app = context.applicationContext
        val db = LauncherAppState.getInstance(app).model.modelDbController
        val idp = InvariantDeviceProfile.INSTANCE.get(app)
        val provider = MikaBoardWidget.component()
        val widgetId = CustomWidgetManager.INSTANCE.get(app).allocateCustomAppWidgetId(provider)

        // 1. Clear any conflicting items currently occupying Screen 0 of Desktop and Hotseat
        db.delete(
            Favorites.TABLE_NAME,
            "${Favorites.CONTAINER} = ${Favorites.CONTAINER_DESKTOP} AND ${Favorites.SCREEN} = 0",
            null,
        )
        db.delete(
            Favorites.TABLE_NAME,
            "${Favorites.CONTAINER} = ${Favorites.CONTAINER_HOTSEAT}",
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
            }
            Log.i(TAG, "Mika board placed on workspace.")
        }
        inserted
    }.onFailure { Log.w(TAG, "Failed to place Mika board", it) }.getOrDefault(false)

    /** Reverts the home screen back to the CyberHome Cozy Board */
    @WorkerThread
    fun placeCyberBoard(context: Context): Boolean = runCatching {
        val app = context.applicationContext
        val db = LauncherAppState.getInstance(app).model.modelDbController
        val idp = InvariantDeviceProfile.INSTANCE.get(app)
        val provider = CozyBoardWidget.component()
        val widgetId = CustomWidgetManager.INSTANCE.get(app).allocateCustomAppWidgetId(provider)

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
        val inserted = db.insert(Favorites.TABLE_NAME, values) >= 0
        if (inserted) {
            CyberWallpaper.apply(context)
            MAIN_EXECUTOR.execute {
                LauncherAppState.getInstance(app).model.forceReload()
            }
            Log.i(TAG, "CyberHome board restored.")
        }
        inserted
    }.onFailure { Log.w(TAG, "Failed to restore Cyber board", it) }.getOrDefault(false)
}
