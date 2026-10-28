// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.appwidget.AppWidgetManager
import android.os.Bundle
import com.techvrs.cyberhome.cyber.design.CyberActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberButton
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextField
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.counter.DayCounterStore
import com.techvrs.cyberhome.cyber.widgets.counter.DayCounterWidgetProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** "Day counter": pick the date and the label of one counter widget. */
class DayCounterActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContent { DayCounterScreen(id, onDone = ::finish) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCounterScreen(id: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { DayCounterStore(context) }
    var label by remember { mutableStateOf(store.label(id)) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = store.date(id).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val pink = colorResource(R.color.cyber_accent_pink)

    CyberScreen(stringResource(R.string.cyber_counter_title), stringResource(R.string.cyber_counter_subtitle)) {
        CyberTextField(label, { label = it.take(24) }, stringResource(R.string.cyber_counter_label))
        DatePicker(
            state = dateState,
            colors = DatePickerDefaults.colors(
                containerColor = colorResource(R.color.cyber_surface),
                selectedDayContainerColor = pink,
                todayDateBorderColor = pink,
            ),
        )
        CyberButton(stringResource(R.string.cyber_save), accent = pink) {
            val millis = dateState.selectedDateMillis
            val date = millis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: LocalDate.now()
            store.save(id, date, label.ifBlank { context.getString(R.string.cyber_counter_default_label) })
            CyberWidgets.refresh(context, DayCounterWidgetProvider::class.java)
            onDone()
        }
    }
}
