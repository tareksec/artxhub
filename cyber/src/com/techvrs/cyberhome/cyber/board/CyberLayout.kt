// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.board

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.R
import com.android.launcher3.model.ModelDbController
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo.CLS_CUSTOM_WIDGET_PREFIX
import com.android.launcher3.widget.custom.CustomWidgetManager

/**
 * Home screen layout of the CyberHome grid (cyber_4_by_8). Called from small hooks in
 * ModelDbController.loadDefaultFavoritesIfNecessary() ([consumeResetRequest] and):
 *  - [isCyberGrid]: our grid skips partner / auto-install layouts (on GMS phones those used to
 *    replace our layout with Google's default page).
 *  - [onDefaultLayoutLoaded]: after cyber_default_workspace.xml (dock + page 2 apps) is loaded,
 *    puts the Cozy Board or Mika Board on the whole first page. The board is an in-launcher custom widget, so
 *    no widget-bind permission is involved.
 */
object CyberLayout {
    private const val TAG = "CyberLayout"

    @JvmStatic
    fun isCyberGrid(context: Context): Boolean =
        InvariantDeviceProfile.INSTANCE.get(context).defaultLayoutId == R.xml.cyber_default_workspace

    private const val PREFS = "cyber_layout"
    private const val KEY_RESET = "reset_requested"

    /**
     * Asks the next workspace load on our grid to rebuild the default layout. The reset then
     * happens inside that load (one model reload), not as a separate wipe + reload after a grid
     * change (which loaded the workspace twice, BUG-2).
     */
    fun requestReset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_RESET, true).commit()
    }

    /** Third hook: true once (and clears the request) when a reset was requested on our grid. */
    @WorkerThread
    @JvmStatic
    fun consumeResetRequest(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_RESET, false) || !isCyberGrid(context)) return false
        prefs.edit().putBoolean(KEY_RESET, false).commit()
        Log.i(TAG, "reset requested: rebuilding the default layout")
        return true
    }

    /** True if the current grid's DB has the Cozy Board or Mika Board (checked after "Apply look"). */
    @WorkerThread
    fun hasBoard(context: Context): Boolean = runCatching {
        val cozy = CozyBoardWidget.component().flattenToString()
        val mika = ComponentName("android", CLS_CUSTOM_WIDGET_PREFIX + "com.techvrs.cyberhome.mika.board.MikaBoardWidget").flattenToString()
        LauncherAppState.getInstance(context).model.modelDbController.query(
            Favorites.TABLE_NAME, arrayOf(Favorites._ID),
            "${Favorites.APPWIDGET_PROVIDER} = ? OR ${Favorites.APPWIDGET_PROVIDER} = ?",
            arrayOf(cozy, mika), null,
        ).use { it.count > 0 }
    }.getOrDefault(false)

    @WorkerThread
    @JvmStatic
    fun onDefaultLayoutLoaded(context: Context, db: ModelDbController) {
        val idp = InvariantDeviceProfile.INSTANCE.get(context)
        val isMika = context.getSharedPreferences("mika_theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("mika_active", false)
        val provider = if (isMika) {
            ComponentName("android", CLS_CUSTOM_WIDGET_PREFIX + "com.techvrs.cyberhome.mika.board.MikaBoardWidget")
        } else {
            CozyBoardWidget.component()
        }
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
        if (db.insert(Favorites.TABLE_NAME, values) < 0) Log.w(TAG, "Could not add the Board")
    }
}
