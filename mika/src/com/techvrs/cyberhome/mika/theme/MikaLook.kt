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
import com.android.launcher3.R
import com.android.launcher3.model.ModelDbController
import com.android.launcher3.widget.custom.CustomWidgetManager
import com.techvrs.cyberhome.mika.board.MikaBoardWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Controller to apply the Mika Theme look:
 * - Sets the cozy cat bed wallpaper (R.drawable.mika_wallpaper_cat_bed)
 * - Adds or replaces the Mika Board widget on Page 0
 */
object MikaLook {
    private const val TAG = "MikaLook"

    /** Applies the Mika cat wallpaper to the home screen */
    @WorkerThread
    fun applyWallpaper(context: Context): Boolean = runCatching {
        val wm = WallpaperManager.getInstance(context)
        wm.setResource(R.drawable.mika_wallpaper_cat_bed, WallpaperManager.FLAG_SYSTEM)
        Log.i(TAG, "Mika wallpaper applied successfully.")
        true
    }.onFailure { Log.w(TAG, "Failed to apply Mika wallpaper", it) }.getOrDefault(false)

    /** Places the Mika Cozy Board on page 0 if not already present */
    @WorkerThread
    fun placeMikaBoard(context: Context): Boolean = runCatching {
        val db = LauncherAppState.getInstance(context).model.modelDbController
        val idp = InvariantDeviceProfile.INSTANCE.get(context)
        val provider = MikaBoardWidget.component()
        val widgetId = CustomWidgetManager.INSTANCE.get(context).allocateCustomAppWidgetId(provider)

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
            LauncherAppState.getInstance(context).model.forceReload()
            Log.i(TAG, "Mika board placed on workspace.")
        }
        inserted
    }.onFailure { Log.w(TAG, "Failed to place Mika board", it) }.getOrDefault(false)
}
