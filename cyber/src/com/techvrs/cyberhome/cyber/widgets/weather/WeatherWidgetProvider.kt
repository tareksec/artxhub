// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.weather

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.settings.WeatherCityActivity
import com.techvrs.cyberhome.cyber.widgets.CyberFrameRenderer
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.WidgetSize

/**
 * W6 Weather tile. Shows the cached reading for the user's manually chosen city (no location
 * permission). Without a city it shows "SET CITY"; a tap opens the city screen. Readings older
 * than 3 hours are dimmed (offline / stale).
 */
class WeatherWidgetProvider : CyberWidgetProvider() {

    override fun onEnabled(context: Context) = WeatherWorker.schedule(context)

    override fun onDisabled(context: Context) = WeatherWorker.cancel(context)

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, manager, appWidgetIds)
        WeatherWorker.schedule(context)
    }

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        val store = WeatherStore(context)
        val city = store.city
        val now = store.last
        val stale = now == null || System.currentTimeMillis() - now.updatedAt > 3 * 60 * 60 * 1000L

        val views = RemoteViews(context.packageName, R.layout.cyber_widget_weather)
        views.setImageViewBitmap(
            R.id.cyber_widget_frame,
            CyberFrameRenderer.frame(context, size, ContextCompat.getColor(context, R.color.cyber_tint_blue)),
        )
        val textColor = ContextCompat.getColor(context, if (stale) R.color.cyber_text_dim else R.color.cyber_text)
        val iconColor = ContextCompat.getColor(context, if (stale) R.color.cyber_text_dim else R.color.cyber_accent_blue)
        views.setTextColor(R.id.cyber_weather_city, ContextCompat.getColor(context, R.color.cyber_text_dim))
        when {
            city == null -> {
                views.setImageViewResource(R.id.cyber_weather_icon, R.drawable.cyber_ic_cloud)
                views.setTextViewText(R.id.cyber_weather_temp, "--")
                views.setTextViewText(R.id.cyber_weather_city, context.getString(R.string.cyber_widget_weather_set_city))
            }
            else -> {
                views.setImageViewResource(R.id.cyber_weather_icon, now?.let { OpenMeteo.iconFor(it.code) } ?: R.drawable.cyber_ic_cloud)
                views.setTextViewText(R.id.cyber_weather_temp, now?.let { store.formatTemperature(it) } ?: "--")
                views.setTextViewText(R.id.cyber_weather_city, city.name)
            }
        }
        views.setTextColor(R.id.cyber_weather_temp, textColor)
        views.setInt(R.id.cyber_weather_icon, "setColorFilter", iconColor)
        views.setViewVisibility(R.id.cyber_weather_city, if (size.heightDp >= 70) View.VISIBLE else View.GONE)
        views.setOnClickPendingIntent(
            R.id.cyber_widget_root,
            CyberWidgets.configIntent(context, WeatherCityActivity::class.java, appWidgetId),
        )
        return views
    }
}
