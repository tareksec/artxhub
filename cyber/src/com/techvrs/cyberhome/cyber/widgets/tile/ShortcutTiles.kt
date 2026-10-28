// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.tile

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.widgets.CyberFrameRenderer
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.iconColor
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.textColor
import com.techvrs.cyberhome.cyber.widgets.WidgetSize

/**
 * W10 shortcut tiles: pastel card, rounded icon, small caption. Implemented as tiny widgets (not
 * plain favorites) because a default layout cannot give app shortcuts custom icons and captions.
 */
abstract class ShortcutTileProvider(
    @DrawableRes private val icon: Int,
    @StringRes private val label: Int,
    @ColorRes private val accent: Int,
    @ColorRes private val tint: Int,
) : CyberWidgetProvider() {

    abstract fun intents(context: Context): Array<Intent>

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        val color = ContextCompat.getColor(context, accent)
        val views = RemoteViews(context.packageName, R.layout.cyber_widget_tile)
        views.setImageViewBitmap(R.id.cyber_widget_frame, CyberFrameRenderer.frame(context, size, ContextCompat.getColor(context, tint)))
        views.setImageViewResource(R.id.cyber_tile_icon, icon)
        views.iconColor(color, R.id.cyber_tile_icon)
        views.setTextViewText(R.id.cyber_tile_label, context.getString(label))
        views.textColor(ContextCompat.getColor(context, R.color.cyber_text), R.id.cyber_tile_label)
        CyberWidgets.activityIntent(context, appWidgetId, *intents(context))
            ?.let { views.setOnClickPendingIntent(R.id.cyber_widget_root, it) }
        return views
    }
}

/** "SET UP": opens system Settings. */
class SettingsTileWidgetProvider : ShortcutTileProvider(
    R.drawable.cyber_ic_settings,
    R.string.cyber_tile_settings,
    R.color.cyber_accent_peach,
    R.color.cyber_tint_peach,
) {
    override fun intents(context: Context) = arrayOf(Intent(Settings.ACTION_SETTINGS))
}

/** "MESSAGE": opens the default SMS / messaging app. */
class MessagesTileWidgetProvider : ShortcutTileProvider(
    R.drawable.cyber_ic_message,
    R.string.cyber_tile_messages,
    R.color.cyber_accent_teal,
    R.color.cyber_tint_teal,
) {
    override fun intents(context: Context) = arrayOf(
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
        Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:")),
    )
}
