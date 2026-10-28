// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.counter

import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.settings.DayCounterActivity
import com.techvrs.cyberhome.cyber.widgets.CyberFrameRenderer
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.iconColor
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.textColor
import com.techvrs.cyberhome.cyber.widgets.WidgetSize
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Per-instance date + label. Default: the day the widget was first shown, label "Days". */
class DayCounterStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("cyber_counter", Context.MODE_PRIVATE)

    fun date(id: Int): LocalDate {
        val saved = prefs.getLong("$id.date", Long.MIN_VALUE)
        if (saved != Long.MIN_VALUE) return LocalDate.ofEpochDay(saved)
        val today = LocalDate.now()
        prefs.edit { putLong("$id.date", today.toEpochDay()) }
        return today
    }

    fun label(id: Int) = prefs.getString("$id.label", null) ?: context.getString(R.string.cyber_counter_default_label)

    fun save(id: Int, date: LocalDate, label: String) = prefs.edit {
        putLong("$id.date", date.toEpochDay())
        putString("$id.label", label)
    }

    fun delete(id: Int) = prefs.edit {
        remove("$id.date")
        remove("$id.label")
    }
}

/**
 * W8 Day counter pill: days since (past date) or until (future date) a chosen date.
 * Redrawn at midnight by CyberRuntime (ACTION_DATE_CHANGED).
 */
class DayCounterWidgetProvider : CyberWidgetProvider() {

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val store = DayCounterStore(context)
        appWidgetIds.forEach { store.delete(it) }
    }

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        val store = DayCounterStore(context)
        val days = ChronoUnit.DAYS.between(store.date(appWidgetId), LocalDate.now())
        val pink = ContextCompat.getColor(context, R.color.cyber_accent_pink)

        val views = RemoteViews(context.packageName, R.layout.cyber_widget_strip)
        views.setImageViewBitmap(
            R.id.cyber_widget_frame,
            CyberFrameRenderer.frame(context, size, ContextCompat.getColor(context, R.color.cyber_tint_pink)),
        )
        views.setImageViewResource(R.id.cyber_strip_icon, R.drawable.cyber_ic_plus)
        views.iconColor(pink, R.id.cyber_strip_icon)
        views.setTextViewText(R.id.cyber_strip_value, abs(days).toString())
        views.textColor(pink, R.id.cyber_strip_value)
        views.textColor(ContextCompat.getColor(context, R.color.cyber_text), R.id.cyber_strip_title)
        views.textColor(ContextCompat.getColor(context, R.color.cyber_text_dim), R.id.cyber_strip_caption)
        views.iconColor(ContextCompat.getColor(context, R.color.cyber_text_dim), R.id.cyber_strip_action)
        views.setTextViewText(R.id.cyber_strip_title, store.label(appWidgetId))
        views.setTextViewText(
            R.id.cyber_strip_caption,
            context.getString(if (days >= 0) R.string.cyber_counter_since else R.string.cyber_counter_until),
        )
        val edit = CyberWidgets.configIntent(context, DayCounterActivity::class.java, appWidgetId)
        views.setOnClickPendingIntent(R.id.cyber_widget_root, edit)
        views.setOnClickPendingIntent(R.id.cyber_strip_action, edit)
        return views
    }
}
