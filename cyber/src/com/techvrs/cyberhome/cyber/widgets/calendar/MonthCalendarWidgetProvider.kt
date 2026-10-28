// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberThemeMode
import com.techvrs.cyberhome.cyber.widgets.CyberFrameRenderer
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.WidgetSize
import java.text.DateFormatSymbols
import java.util.Calendar

/**
 * W3 Month Calendar: current month grid, today highlighted, week starts on the locale's first
 * day. No permissions (no events). Redrawn on date/time/locale changes by CyberRuntime.
 */
class MonthCalendarWidgetProvider : CyberWidgetProvider() {

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        val locale = context.resources.configuration.locales[0]
        val views = RemoteViews(context.packageName, R.layout.cyber_widget_calendar)
        views.setImageViewBitmap(
            R.id.cyber_widget_frame,
            CyberFrameRenderer.frame(context, size, ContextCompat.getColor(context, R.color.cyber_surface), border = true),
        )

        val today = Calendar.getInstance(locale)
        views.setTextViewText(
            R.id.cyber_cal_title,
            DateFormat.format(DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), today).toString(),
        )

        val text = ContextCompat.getColor(context, R.color.cyber_text)
        val dim = ContextCompat.getColor(context, R.color.cyber_text_dim)
        val onAccent = ContextCompat.getColor(context, R.color.cyber_on_accent)
        val accent = ContextCompat.getColor(context, R.color.cyber_accent_pink)
        views.setTextColor(R.id.cyber_cal_title, accent)
        // Drawables are resolved by the host, so pick the variant for the chosen mode here.
        val todayBg = if (CyberThemeMode.isDark(context)) {
            R.drawable.cyber_widget_calendar_today_dark
        } else {
            R.drawable.cyber_widget_calendar_today_light
        }
        // Fit 7 rows (header + 6 weeks) into the available height.
        val textSp = (size.heightDp / 13f).coerceIn(8f, 14f)

        views.removeAllViews(R.id.cyber_cal_rows)

        // Header: short weekday names starting at the locale's first day of week
        val names = DateFormatSymbols.getInstance(locale).shortWeekdays
        val first = today.firstDayOfWeek
        val header = RemoteViews(context.packageName, R.layout.cyber_widget_calendar_row)
        for (i in 0 until 7) {
            val day = (first - 1 + i) % 7 + 1
            header.addView(R.id.cyber_cal_row, cell(context, names[day].take(2), dim, textSp * 0.85f))
        }
        views.addView(R.id.cyber_cal_rows, header)

        // Weeks
        val cursor = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val lead = (cursor.get(Calendar.DAY_OF_WEEK) - first + 7) % 7
        cursor.add(Calendar.DAY_OF_MONTH, -lead)
        repeat(6) {
            val row = RemoteViews(context.packageName, R.layout.cyber_widget_calendar_row)
            repeat(7) {
                val inMonth = cursor.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                val isToday = inMonth && cursor.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
                val label = if (inMonth) cursor.get(Calendar.DAY_OF_MONTH).toString() else ""
                val c = cell(context, label, if (isToday) onAccent else if (inMonth) text else dim, textSp)
                if (isToday) c.setInt(R.id.cyber_cal_cell, "setBackgroundResource", todayBg)
                row.addView(R.id.cyber_cal_row, c)
                cursor.add(Calendar.DAY_OF_MONTH, 1)
            }
            views.addView(R.id.cyber_cal_rows, row)
        }

        CyberWidgets.activityIntent(
            context,
            appWidgetId,
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
            Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()),
        )?.let { views.setOnClickPendingIntent(R.id.cyber_widget_root, it) }
        return views
    }

    private fun cell(context: Context, label: String, color: Int, sp: Float) =
        RemoteViews(context.packageName, R.layout.cyber_widget_calendar_cell).apply {
            setTextViewText(R.id.cyber_cal_cell, label)
            setTextColor(R.id.cyber_cal_cell, color)
            setTextViewTextSize(R.id.cyber_cal_cell, TypedValue.COMPLEX_UNIT_SP, sp)
        }
}
