// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.dial

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.iconColor
import com.techvrs.cyberhome.cyber.widgets.WidgetSize

/**
 * W11 Dial button. Sends Intent.ACTION_ALL_APPS to our own launcher activity, which Launcher3
 * handles in onNewIntent by opening the app drawer.
 */
class DialButtonWidgetProvider : CyberWidgetProvider() {

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.cyber_widget_dial)
        views.iconColor(ContextCompat.getColor(context, R.color.cyber_surface), R.id.cyber_dial_bg)
        views.iconColor(ContextCompat.getColor(context, R.color.cyber_soft_blue), R.id.cyber_dial_ring)
        views.iconColor(ContextCompat.getColor(context, R.color.cyber_accent_blue), R.id.cyber_dial_icon)
        val intent = Intent(Intent.ACTION_ALL_APPS)
            .setComponent(ComponentName(context.packageName, LAUNCHER_ACTIVITY))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        views.setOnClickPendingIntent(
            R.id.cyber_widget_root,
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
        return views
    }

    private companion object {
        const val LAUNCHER_ACTIVITY = "app.lawnchair.LawnchairLauncher"
    }
}
