// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.BatteryManager
import android.os.Looper
import androidx.core.content.ContextCompat
import app.lawnchair.preferences.PreferenceManager
import com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR
import com.techvrs.cyberhome.cyber.design.CyberLauncherStyle
import com.techvrs.cyberhome.cyber.design.CyberThemeMode
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.cyber.lock.LockOverlayController
import com.techvrs.cyberhome.cyber.theme.ThemeRepository
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.battery.BatteryStripWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.calendar.MonthCalendarWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.counter.DayCounterWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.music.MediaSessionTracker
import com.techvrs.cyberhome.cyber.widgets.water.WaterReminderWidgetProvider

/**
 * Runtime hooks that live in the launcher process. As the home app this process is almost always
 * running, so widgets get date, battery and media changes from runtime-registered receivers:
 * no alarms, no wake locks, no foreground service. (Most of these broadcasts cannot be received
 * from the manifest on Android 8+.)
 */
object CyberRuntime {
    @Volatile private var started = false
    private var lastBattery = -1

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext

        // Initialize Theme Engine and Lock Screen Controller
        ThemeRepository.init(app)
        LockOverlayController.start(app)

        val dateReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                CyberWidgets.refresh(c, MonthCalendarWidgetProvider::class.java)
                CyberWidgets.refresh(c, DayCounterWidgetProvider::class.java)
                CyberWidgets.refresh(c, WaterReminderWidgetProvider::class.java)
                if (intent.action == Intent.ACTION_LOCALE_CHANGED) CyberWidgets.refreshAll(c)
            }
        }
        ContextCompat.registerReceiver(
            app,
            dateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_LOCALE_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                // Only redraw when something visible changed (level or charging state).
                val key = level * 10 + if (plugged != 0) 1 else 0
                if (key != lastBattery) {
                    lastBattery = key
                    CyberWidgets.refresh(c, BatteryStripWidgetProvider::class.java)
                }
            }
        }
        ContextCompat.registerReceiver(
            app,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        MediaSessionTracker.start(app)
        watchTheme(app)
    }

    /**
     * Redraws all widgets (and swaps our wallpaper to the matching variant) when the light/dark
     * choice changes, or when the system switches day/night while the choice is "system".
     */
    private fun watchTheme(app: Context) {
        var lastDark = CyberThemeMode.isDark(app)
        val onChange = {
            val dark = CyberThemeMode.isDark(app)
            if (dark != lastDark) {
                lastDark = dark
                CyberWidgets.refreshAll(app)
                THREAD_POOL_EXECUTOR.execute {
                    CyberWallpaper.sync(app)
                    runCatching { CyberLauncherStyle.syncDock(app) }
                }
            }
        }
        themeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == THEME_PREF) onChange()
        }
        PreferenceManager.getInstance(app).sp.registerOnSharedPreferenceChangeListener(themeListener)
        app.registerComponentCallbacks(
            object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) = onChange()
                @Deprecated("Deprecated in Java")
                override fun onLowMemory() = Unit
            },
        )
    }

    private const val THEME_PREF = "pref_launcherTheme"

    // SharedPreferences keeps listeners only weakly.
    private var themeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
}

/**
 * Starts [CyberRuntime] when the app process starts (content providers are created before
 * Application.onCreate finishes). Keeps our hook out of upstream code.
 */
class CyberInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        // Runs when the main thread is idle, i.e. after the launcher drew its first frame,
        // so our receivers never delay the cold start.
        val app = context?.applicationContext ?: return true
        Looper.getMainLooper().queue.addIdleHandler {
            CyberRuntime.start(app)
            false
        }
        return true
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}
