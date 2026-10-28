// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.weather

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class City(val name: String, val region: String, val latitude: Double, val longitude: Double)

data class WeatherNow(val temperatureC: Double, val code: Int, val updatedAt: Long)

/** Saved city, unit and the last successful reading (shown while offline). */
class WeatherStore(context: Context) {
    private val prefs = context.getSharedPreferences("cyber_weather", Context.MODE_PRIVATE)

    var city: City?
        get() {
            val name = prefs.getString(KEY_NAME, null) ?: return null
            return City(
                name,
                prefs.getString(KEY_REGION, "") ?: "",
                prefs.getFloat(KEY_LAT, 0f).toDouble(),
                prefs.getFloat(KEY_LON, 0f).toDouble(),
            )
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove(KEY_NAME)
            } else {
                putString(KEY_NAME, value.name)
                putString(KEY_REGION, value.region)
                putFloat(KEY_LAT, value.latitude.toFloat())
                putFloat(KEY_LON, value.longitude.toFloat())
            }
            remove(KEY_TEMP)
        }

    var fahrenheit: Boolean
        get() = prefs.getBoolean(KEY_FAHRENHEIT, false)
        set(value) = prefs.edit { putBoolean(KEY_FAHRENHEIT, value) }

    var last: WeatherNow?
        get() = if (!prefs.contains(KEY_TEMP)) {
            null
        } else {
            WeatherNow(prefs.getFloat(KEY_TEMP, 0f).toDouble(), prefs.getInt(KEY_CODE, 0), prefs.getLong(KEY_TIME, 0))
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove(KEY_TEMP)
            } else {
                putFloat(KEY_TEMP, value.temperatureC.toFloat())
                putInt(KEY_CODE, value.code)
                putLong(KEY_TIME, value.updatedAt)
            }
        }

    fun formatTemperature(now: WeatherNow): String {
        val t = if (fahrenheit) now.temperatureC * 9 / 5 + 32 else now.temperatureC
        return "${Math.round(t)}°"
    }

    private companion object {
        const val KEY_NAME = "city_name"
        const val KEY_REGION = "city_region"
        const val KEY_LAT = "city_lat"
        const val KEY_LON = "city_lon"
        const val KEY_FAHRENHEIT = "fahrenheit"
        const val KEY_TEMP = "temp_c"
        const val KEY_CODE = "code"
        const val KEY_TIME = "time"
    }
}

/**
 * Open-Meteo client (https://open-meteo.com, free, no API key). The only network access in
 * CyberHome's own code. Sends just the city name (search) or its coordinates (forecast).
 */
object OpenMeteo {
    suspend fun searchCity(query: String, language: String = Locale.getDefault().language): List<City> =
        withContext(Dispatchers.IO) {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val json = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=$language&format=json")
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            (0 until results.length()).map { i ->
                val r = results.getJSONObject(i)
                City(
                    name = r.getString("name"),
                    region = listOf(r.optString("admin1"), r.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                    latitude = r.getDouble("latitude"),
                    longitude = r.getDouble("longitude"),
                )
            }
        }

    suspend fun current(city: City): WeatherNow = withContext(Dispatchers.IO) {
        val json = get(
            "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}" +
                "&current=temperature_2m,weather_code",
        )
        val current = json.getJSONObject("current")
        WeatherNow(current.getDouble("temperature_2m"), current.getInt("weather_code"), System.currentTimeMillis())
    }

    private fun get(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /** WMO weather interpretation code -> HUD icon. */
    @DrawableRes
    fun iconFor(code: Int): Int = when (code) {
        0, 1 -> R.drawable.cyber_ic_sun
        2, 3 -> R.drawable.cyber_ic_cloud
        45, 48 -> R.drawable.cyber_ic_fog
        in 51..67, in 80..82 -> R.drawable.cyber_ic_rain
        in 71..77, 85, 86 -> R.drawable.cyber_ic_snow
        in 95..99 -> R.drawable.cyber_ic_storm
        else -> R.drawable.cyber_ic_cloud
    }
}

/** Fetches the current weather for the saved city; scheduled only while a weather widget exists. */
class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = WeatherStore(applicationContext)
        val city = store.city ?: return Result.success()
        return try {
            store.last = OpenMeteo.current(city)
            CyberWidgets.refresh(applicationContext, WeatherWidgetProvider::class.java)
            Result.success()
        } catch (e: Exception) {
            // Keep the cached value; the widget shows it as stale.
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val PERIODIC = "cyber_weather_periodic"
        private const val NOW = "cyber_weather_now"
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Every 60 minutes (WorkManager batches it with other work; no wakeups of its own). */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WeatherWorker>(60, TimeUnit.MINUTES).setConstraints(network).build(),
            )
        }

        fun refreshNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WeatherWorker>().setConstraints(network).build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
    }
}
