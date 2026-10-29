// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.lock

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import com.techvrs.cyberhome.cyber.widgets.music.MediaSessionTracker
import com.techvrs.cyberhome.cyber.widgets.weather.WeatherStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Combines system broadcasts (time, battery, locale), weather state, active media session,
 * and user preferences into a single live StateFlow<LockState>.
 */
class LockStateRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<LockState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> refreshTime()
                Intent.ACTION_BATTERY_CHANGED -> refreshBattery(intent)
                Intent.ACTION_LOCALE_CHANGED -> refreshAll()
            }
        }
    }

    private var registered = false

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true

        MediaSessionTracker.start(context)
        refreshAll()
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    fun refreshAll() {
        val prefs = context.getSharedPreferences("mika_lock_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("mika_lock_display_name", "Fonseca 🦋") ?: "Fonseca 🦋"
        val quote = prefs.getString("mika_lock_quote", "Small steps, big dreams ♡") ?: "Small steps, big dreams ♡"
        val dark = prefs.getBoolean("mika_dark_lock", false)

        val weatherStore = WeatherStore(context)
        val lastWeather = weatherStore.last
        val weatherState = if (lastWeather != null) {
            val cond = when (lastWeather.code) {
                0 -> "Clear"
                1, 2, 3 -> "Partly Cloudy"
                45, 48 -> "Fog"
                in 51..67, in 80..82 -> "Rain"
                in 71..77, 85, 86 -> "Snow"
                in 95..99 -> "Thunderstorm"
                else -> "Cloudy"
            }
            LockWeatherState(
                isAvailable = true,
                temperatureCelsius = lastWeather.temperatureC.toFloat(),
                conditionText = cond,
                wmoCode = lastWeather.code
            )
        } else {
            LockWeatherState()
        }

        val mediaState = LockMediaState(
            hasSession = MediaSessionTracker.title != null || MediaSessionTracker.playing,
            isPlaying = MediaSessionTracker.playing,
            title = MediaSessionTracker.title ?: "",
            artist = MediaSessionTracker.artist ?: "",
            albumArt = MediaSessionTracker.albumArt,
            packageName = MediaSessionTracker.packageName ?: "",
            positionMs = MediaSessionTracker.positionMs,
            durationMs = MediaSessionTracker.durationMs
        )

        val isSecure = keyguardManager?.isDeviceSecure ?: false
        val is24h = DateFormat.is24HourFormat(context)

        _state.value = _state.value.copy(
            timeMillis = System.currentTimeMillis(),
            is24HourFormat = is24h,
            displayName = name,
            noteQuote = quote,
            isDarkLock = dark,
            isDeviceSecure = isSecure,
            weather = weatherState,
            media = mediaState
        )
    }

    private fun refreshTime() {
        _state.value = _state.value.copy(
            timeMillis = System.currentTimeMillis(),
            is24HourFormat = DateFormat.is24HourFormat(context)
        )
    }

    private fun refreshBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        _state.value = _state.value.copy(
            batteryLevel = batteryPct,
            isCharging = isCharging
        )
    }

    private fun createInitialState(): LockState {
        val isSecure = keyguardManager?.isDeviceSecure ?: false
        val is24h = DateFormat.is24HourFormat(context)
        return LockState(
            timeMillis = System.currentTimeMillis(),
            is24HourFormat = is24h,
            isDeviceSecure = isSecure
        )
    }
}
