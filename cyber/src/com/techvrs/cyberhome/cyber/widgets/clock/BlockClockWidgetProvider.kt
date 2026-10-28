// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.clock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberThemeMode
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.iconColor
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.textColor

/**
 * W1 Clock + W2 date line: rounded digits on a soft card, minutes in the pink accent.
 *
 * The digits and the date are TextClocks, so the system redraws them every minute and follows
 * the 12/24h setting on its own; this provider only builds the RemoteViews when the widget is
 * added and when the locale changes (to refresh the localized date pattern).
 */
class BlockClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = buildViews(context)
        appWidgetIds.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) updateAll(context)
    }

    companion object {
        fun buildViews(base: Context): RemoteViews {
            val context = CyberThemeMode.themed(base)
            val views = RemoteViews(context.packageName, R.layout.cyber_widget_clock)
            val color = { id: Int -> ContextCompat.getColor(context, id) }
            views.iconColor(color(R.color.cyber_surface), R.id.cyber_clock_bg)
            views.iconColor(color(R.color.cyber_soft_pink), R.id.cyber_clock_divider)
            views.textColor(color(R.color.cyber_text), R.id.cyber_clock_hour)
            views.textColor(color(R.color.cyber_accent_pink), R.id.cyber_clock_minute)
            views.textColor(color(R.color.cyber_text_dim), R.id.cyber_clock_date)
            val datePattern = datePattern(context)
            views.setCharSequence(R.id.cyber_clock_date, "setFormat12Hour", datePattern)
            views.setCharSequence(R.id.cyber_clock_date, "setFormat24Hour", datePattern)
            clockAppIntent(context)?.let { views.setOnClickPendingIntent(R.id.cyber_clock_root, it) }
            return views
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BlockClockWidgetProvider::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, buildViews(context))
        }

        /** "Oct 09 · Wed" in English; month/day order and names follow the user's locale. */
        private fun datePattern(context: Context): String {
            val locale = context.resources.configuration.locales[0]
            val monthDay = DateFormat.getBestDateTimePattern(locale, "MMMdd")
            val weekday = DateFormat.getBestDateTimePattern(locale, "EEE")
            return "$monthDay · $weekday"
        }

        /** Opens the default clock app; falls back to date & time settings, or no action. */
        private fun clockAppIntent(context: Context): PendingIntent? {
            val pm = context.packageManager
            val intent = listOf(
                Intent(AlarmClock.ACTION_SHOW_ALARMS),
                Intent(Settings.ACTION_DATE_SETTINGS),
            ).firstOrNull { it.resolveActivity(pm) != null } ?: return null
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
