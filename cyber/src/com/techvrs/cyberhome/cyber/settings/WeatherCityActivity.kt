// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.os.Bundle
import com.techvrs.cyberhome.cyber.design.CyberActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberButton
import com.techvrs.cyberhome.cyber.design.CyberRow
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextField
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.weather.City
import com.techvrs.cyberhome.cyber.widgets.weather.OpenMeteo
import com.techvrs.cyberhome.cyber.widgets.weather.WeatherStore
import com.techvrs.cyberhome.cyber.widgets.weather.WeatherWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.weather.WeatherWorker
import kotlinx.coroutines.launch

/** "Weather city": search a city by name (Open-Meteo geocoding), pick it, choose °C/°F. */
class WeatherCityActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WeatherCityScreen(onDone = ::finish) }
    }
}

@Composable
private fun WeatherCityScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { WeatherStore(context) }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(store.city?.name ?: "") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var fahrenheit by remember { mutableStateOf(store.fahrenheit) }

    val searching = stringResource(R.string.cyber_weather_searching)
    val noResults = stringResource(R.string.cyber_weather_no_results)
    val offline = stringResource(R.string.cyber_weather_offline)

    CyberScreen(
        title = stringResource(R.string.cyber_weather_title),
        subtitle = stringResource(R.string.cyber_weather_subtitle),
    ) {
        store.city?.let { CyberRow(stringResource(R.string.cyber_weather_current), "${it.name} · ${it.region}", R.drawable.cyber_ic_cloud) }
        CyberTextField(query, { query = it }, stringResource(R.string.cyber_weather_city_hint))
        CyberButton(stringResource(R.string.cyber_weather_search), enabled = query.isNotBlank()) {
            status = searching
            scope.launch {
                status = try {
                    results = OpenMeteo.searchCity(query)
                    if (results.isEmpty()) noResults else null
                } catch (e: Exception) {
                    offline
                }
            }
        }
        status?.let { Text(it, style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim))) }
        results.forEach { city ->
            CyberRow(city.name, city.region, R.drawable.cyber_ic_cloud) {
                store.city = city
                WeatherWorker.schedule(context)
                WeatherWorker.refreshNow(context)
                CyberWidgets.refresh(context, WeatherWidgetProvider::class.java)
                onDone()
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberButton("°C", Modifier.weight(1f), accent = if (!fahrenheit) colorResource(R.color.cyber_soft_blue) else colorResource(R.color.cyber_border)) {
                fahrenheit = false
                store.fahrenheit = false
                CyberWidgets.refresh(context, WeatherWidgetProvider::class.java)
            }
            CyberButton("°F", Modifier.weight(1f), accent = if (fahrenheit) colorResource(R.color.cyber_soft_blue) else colorResource(R.color.cyber_border)) {
                fahrenheit = true
                store.fahrenheit = true
                CyberWidgets.refresh(context, WeatherWidgetProvider::class.java)
            }
        }
        Text(stringResource(R.string.cyber_weather_privacy), style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim)))
    }
}
