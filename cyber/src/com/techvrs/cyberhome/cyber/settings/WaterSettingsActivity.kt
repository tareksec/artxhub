// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat
import com.techvrs.cyberhome.cyber.design.CyberActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberButton
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.water.WaterReminderWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.water.WaterReminderWorker
import com.techvrs.cyberhome.cyber.widgets.water.WaterStore

/** Water tile settings: today's count, daily goal, optional reminder interval. */
class WaterSettingsActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WaterScreen(onDone = ::finish) }
    }
}

@Composable
private fun WaterScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { WaterStore(context) }
    var count by remember { mutableIntStateOf(store.count) }
    var goal by remember { mutableIntStateOf(store.goal) }
    var interval by remember { mutableIntStateOf(store.intervalHours) }
    val teal = colorResource(R.color.cyber_accent_teal)
    val border = colorResource(R.color.cyber_border)
    // Without notification permission reminders stay silent; the screen says so (error state).
    fun notificationsOn() = NotificationManagerCompat.from(context).areNotificationsEnabled()
    var allowed by remember { mutableStateOf(notificationsOn()) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = notificationsOn() }

    CyberScreen(stringResource(R.string.cyber_water_settings_title), stringResource(R.string.cyber_water_settings_subtitle)) {
        Text(stringResource(R.string.cyber_water_today, count, goal), style = CyberTextStyles.number.copy(color = teal))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberButton("-1", Modifier.weight(1f), accent = border) { count = (count - 1).coerceAtLeast(0) }
            CyberButton("+1", Modifier.weight(1f), accent = teal) { count += 1 }
        }
        Text(stringResource(R.string.cyber_water_goal), style = CyberTextStyles.label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(6, 8, 10, 12).forEach { g ->
                CyberButton("$g", Modifier.weight(1f), accent = if (goal == g) teal else border) { goal = g }
            }
        }
        Text(stringResource(R.string.cyber_water_reminders), style = CyberTextStyles.label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 1, 2, 3).forEach { h ->
                val label = if (h == 0) stringResource(R.string.cyber_off) else stringResource(R.string.cyber_water_every_hours, h)
                CyberButton(label, Modifier.weight(1f), accent = if (interval == h) teal else border) {
                    interval = h
                    if (h > 0 && Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        if (interval > 0 && !allowed) {
            Text(stringResource(R.string.cyber_water_notifications_off), style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_accent_pink)))
            CyberButton(stringResource(R.string.cyber_open_notification_settings), accent = border) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            }
        }
        Text(stringResource(R.string.cyber_water_reminder_note), style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim)))
        CyberButton(stringResource(R.string.cyber_save), accent = teal) {
            store.setCount(count)
            store.goal = goal
            store.intervalHours = interval
            WaterReminderWorker.schedule(context)
            CyberWidgets.refresh(context, WaterReminderWidgetProvider::class.java)
            onDone()
        }
    }
}
