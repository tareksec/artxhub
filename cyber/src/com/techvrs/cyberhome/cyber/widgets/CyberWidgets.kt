// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import com.techvrs.cyberhome.cyber.board.CozyBoard
import com.techvrs.cyberhome.cyber.design.CyberThemeMode
import com.techvrs.cyberhome.cyber.widgets.battery.BatteryStripWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.calendar.MonthCalendarWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.card.CharacterCardWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.card.PhoneCardWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.card.TealCardWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.clock.BlockClockWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.counter.DayCounterWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.dial.DialButtonWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.music.MusicStripWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.tile.MessagesTileWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.tile.SettingsTileWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.water.WaterReminderWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.weather.WeatherWidgetProvider

/** Size of one widget instance in dp, as reported by the host for portrait orientation. */
data class WidgetSize(val widthDp: Int, val heightDp: Int)

/**
 * Base for CyberHome widgets: rebuilds each instance with its current size, so bitmaps drawn by
 * [CyberFrameRenderer] always match the cell size (no stretching), and re-renders on resize.
 */
abstract class CyberWidgetProvider : AppWidgetProvider() {

    abstract fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, manager, appWidgetId)
    }

    /** [buildViews] gets a context whose resources follow the light/dark choice (CyberThemeMode). */
    fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val size = sizeOf(context, manager, appWidgetId)
        manager.updateAppWidget(appWidgetId, buildViews(CyberThemeMode.themed(context), appWidgetId, size))
    }

    private fun sizeOf(context: Context, manager: AppWidgetManager, appWidgetId: Int): WidgetSize {
        val options = manager.getAppWidgetOptions(appWidgetId)
        // Portrait: min width x max height (Launcher3 convention), already in dp.
        val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (w > 0 && h > 0) return WidgetSize(w, h)
        // Not reported yet: fall back to the provider's minimum size (pixels -> dp).
        val density = context.resources.displayMetrics.density
        val info = manager.getAppWidgetInfo(appWidgetId)
        return WidgetSize(
            ((info?.minWidth ?: 300) / density).toInt().coerceAtLeast(40),
            ((info?.minHeight ?: 300) / density).toInt().coerceAtLeast(40),
        )
    }
}

object CyberWidgets {
    /** Every CyberHome widget provider, used to refresh them all (e.g. after a locale change). */
    val ALL: List<Class<out AppWidgetProvider>> = listOf(
        BlockClockWidgetProvider::class.java,
        MonthCalendarWidgetProvider::class.java,
        BatteryStripWidgetProvider::class.java,
        WeatherWidgetProvider::class.java,
        PhoneCardWidgetProvider::class.java,
        CharacterCardWidgetProvider::class.java,
        TealCardWidgetProvider::class.java,
        DayCounterWidgetProvider::class.java,
        WaterReminderWidgetProvider::class.java,
        MusicStripWidgetProvider::class.java,
        DialButtonWidgetProvider::class.java,
        SettingsTileWidgetProvider::class.java,
        MessagesTileWidgetProvider::class.java,
    )

    fun ids(context: Context, provider: Class<out AppWidgetProvider>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, provider))

    /** Re-renders all instances of [provider] (no-op when none is placed) and the Cozy Board. */
    fun refresh(context: Context, provider: Class<out AppWidgetProvider>) {
        CozyBoard.notifyChanged()
        val ids = ids(context, provider)
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(ComponentName(context, provider))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
        )
    }

    fun refreshAll(context: Context) = ALL.forEach { refresh(context, it) }

    /** Sets [color] on several TextViews (widget text colors always come from code). */
    fun RemoteViews.textColor(@ColorInt color: Int, @IdRes vararg ids: Int) = ids.forEach { setTextColor(it, color) }

    /** Tints several ImageViews (white/single-color vector icons) with [color]. */
    fun RemoteViews.iconColor(@ColorInt color: Int, @IdRes vararg ids: Int) = ids.forEach { setInt(it, "setColorFilter", color) }

    /** First of [intents] that some activity can handle, as an immutable PendingIntent. */
    fun activityIntent(context: Context, requestCode: Int, vararg intents: Intent): PendingIntent? {
        val intent = intents.firstOrNull { it.resolveActivity(context.packageManager) != null } ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Explicit broadcast back to [provider] with [action] (used for in-widget buttons). */
    fun selfIntent(
        context: Context,
        provider: Class<*>,
        action: String,
        appWidgetId: Int,
        requestCode: Int = appWidgetId,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, provider)
            .setAction(action)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Opens one of our own activities for a given widget instance (config screens). */
    fun configIntent(context: Context, activity: Class<*>, appWidgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, activity)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
