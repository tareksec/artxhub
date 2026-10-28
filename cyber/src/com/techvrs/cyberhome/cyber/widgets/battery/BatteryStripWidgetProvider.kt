// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.widgets.CyberFrameRenderer
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.iconColor
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.textColor
import com.techvrs.cyberhome.cyber.widgets.WidgetSize

/**
 * W4 Battery strip. Reads the sticky ACTION_BATTERY_CHANGED intent (no permission); CyberRuntime
 * redraws it when the level or charging state changes. Bar colors: soft blue normal, soft teal
 * while charging, soft pink at 15% and below.
 */
class BatteryStripWidgetProvider : CyberWidgetProvider() {

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.let {
            val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && scale > 0) l * 100 / scale else null
        } ?: 0
        val charging = (status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0

        val color = ContextCompat.getColor(
            context,
            when {
                charging -> R.color.cyber_soft_teal
                level <= 15 -> R.color.cyber_soft_pink
                else -> R.color.cyber_soft_blue
            },
        )
        val off = ContextCompat.getColor(context, R.color.cyber_border)

        val views = RemoteViews(context.packageName, R.layout.cyber_widget_battery)
        views.setImageViewBitmap(
            R.id.cyber_widget_frame,
            CyberFrameRenderer.frame(context, size, ContextCompat.getColor(context, R.color.cyber_surface), border = true),
        )
        views.iconColor(ContextCompat.getColor(context, R.color.cyber_accent_peach), R.id.cyber_battery_icon)
        val lit = (level + 9) / 10
        SEGMENTS.forEachIndexed { i, id -> views.setInt(id, "setColorFilter", if (i < lit) color else off) }
        views.setTextViewText(R.id.cyber_battery_percent, "$level%")
        views.textColor(ContextCompat.getColor(context, R.color.cyber_text), R.id.cyber_battery_percent)
        views.setContentDescription(
            R.id.cyber_widget_root,
            context.getString(if (charging) R.string.cyber_widget_battery_charging else R.string.cyber_widget_battery_level, level),
        )

        CyberWidgets.activityIntent(context, appWidgetId, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
            ?.let { views.setOnClickPendingIntent(R.id.cyber_widget_root, it) }
        return views
    }

    private companion object {
        val SEGMENTS = intArrayOf(
            R.id.cyber_battery_seg0, R.id.cyber_battery_seg1, R.id.cyber_battery_seg2,
            R.id.cyber_battery_seg3, R.id.cyber_battery_seg4, R.id.cyber_battery_seg5,
            R.id.cyber_battery_seg6, R.id.cyber_battery_seg7, R.id.cyber_battery_seg8,
            R.id.cyber_battery_seg9,
        )
    }
}
