// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.water

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.settings.WaterSettingsActivity
import com.techvrs.cyberhome.cyber.widgets.CyberFrameRenderer
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.iconColor
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.textColor
import com.techvrs.cyberhome.cyber.widgets.WidgetSize
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Today's glass count (resets each day), daily goal and reminder interval (0 = off). */
class WaterStore(context: Context) {
    private val prefs = context.getSharedPreferences("cyber_water", Context.MODE_PRIVATE)

    val count: Int
        get() = if (prefs.getLong(KEY_DAY, -1) == LocalDate.now().toEpochDay()) prefs.getInt(KEY_COUNT, 0) else 0

    var goal: Int
        get() = prefs.getInt(KEY_GOAL, 8)
        set(value) = prefs.edit { putInt(KEY_GOAL, value.coerceIn(1, 20)) }

    var intervalHours: Int
        get() = prefs.getInt(KEY_INTERVAL, 0)
        set(value) = prefs.edit { putInt(KEY_INTERVAL, value) }

    fun addGlass() = setCount(count + 1)

    fun setCount(value: Int) = prefs.edit {
        putLong(KEY_DAY, LocalDate.now().toEpochDay())
        putInt(KEY_COUNT, value.coerceIn(0, 99))
    }

    private companion object {
        const val KEY_DAY = "day"
        const val KEY_COUNT = "count"
        const val KEY_GOAL = "goal"
        const val KEY_INTERVAL = "interval_hours"
    }
}

/**
 * W9 Water reminder tile (teal "DRINK"). Tap logs a glass; the pencil opens settings.
 * Optional reminders: a WorkManager periodic job posts a notification between 08:00 and 22:00
 * while today's goal is not reached. Off by default; needs POST_NOTIFICATIONS on Android 13+.
 */
class WaterReminderWidgetProvider : CyberWidgetProvider() {

    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action == ACTION_LOG_GLASS) {
            WaterStore(context).addGlass()
            CyberWidgets.refresh(context, WaterReminderWidgetProvider::class.java)
            return
        }
        super.onReceive(context, intent)
    }

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        val store = WaterStore(context)
        val teal = ContextCompat.getColor(context, R.color.cyber_accent_teal)
        val done = store.count >= store.goal

        val text = ContextCompat.getColor(context, R.color.cyber_text)
        val views = RemoteViews(context.packageName, R.layout.cyber_widget_strip)
        views.setImageViewBitmap(
            R.id.cyber_widget_frame,
            CyberFrameRenderer.frame(context, size, ContextCompat.getColor(context, R.color.cyber_tint_teal)),
        )
        views.setImageViewResource(R.id.cyber_strip_icon, R.drawable.cyber_ic_drop)
        views.iconColor(teal, R.id.cyber_strip_icon)
        views.setTextViewText(R.id.cyber_strip_value, "${store.count}/${store.goal}")
        views.textColor(if (done) teal else text, R.id.cyber_strip_value)
        views.textColor(teal, R.id.cyber_strip_title)
        views.textColor(ContextCompat.getColor(context, R.color.cyber_text_dim), R.id.cyber_strip_caption)
        views.iconColor(ContextCompat.getColor(context, R.color.cyber_text_dim), R.id.cyber_strip_action)
        views.setTextViewText(R.id.cyber_strip_title, context.getString(R.string.cyber_water_title))
        views.setTextViewText(R.id.cyber_strip_caption, context.getString(R.string.cyber_water_caption))
        views.setContentDescription(R.id.cyber_widget_root, context.getString(R.string.cyber_water_log_glass))
        views.setOnClickPendingIntent(
            R.id.cyber_widget_root,
            CyberWidgets.selfIntent(context, WaterReminderWidgetProvider::class.java, ACTION_LOG_GLASS, appWidgetId),
        )
        views.setOnClickPendingIntent(
            R.id.cyber_strip_action,
            CyberWidgets.configIntent(context, WaterSettingsActivity::class.java, appWidgetId),
        )
        return views
    }

    companion object {
        const val ACTION_LOG_GLASS = "com.techvrs.cyberhome.cyber.water.LOG_GLASS"
    }
}

class WaterReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = WaterStore(applicationContext)
        val hour = LocalTime.now().hour
        if (store.intervalHours > 0 && hour in 8..21 && store.count < store.goal) notify(applicationContext, store)
        return Result.success()
    }

    companion object {
        private const val WORK = "cyber_water_reminder"
        private const val CHANNEL = "cyber_water"
        private const val NOTIFICATION_ID = 0xC7B0

        fun schedule(context: Context) {
            val hours = WaterStore(context).intervalHours
            val wm = WorkManager.getInstance(context)
            if (hours <= 0) {
                wm.cancelUniqueWork(WORK)
            } else {
                wm.enqueueUniquePeriodicWork(
                    WORK,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    PeriodicWorkRequestBuilder<WaterReminderWorker>(hours.toLong(), TimeUnit.HOURS).build(),
                )
            }
        }

        private fun notify(context: Context, store: WaterStore) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.cyber_water_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
            val log = PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, WaterReminderWidgetProvider::class.java).setAction(WaterReminderWidgetProvider.ACTION_LOG_GLASS),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.cyber_ic_drop)
                .setContentTitle(context.getString(R.string.cyber_water_notification_title))
                .setContentText(context.getString(R.string.cyber_water_notification_text, store.count, store.goal))
                .setColor(ContextCompat.getColor(context, R.color.cyber_accent_teal))
                .addAction(R.drawable.cyber_ic_plus, context.getString(R.string.cyber_water_log_glass), log)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
