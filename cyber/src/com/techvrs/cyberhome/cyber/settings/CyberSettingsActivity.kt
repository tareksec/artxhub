// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.techvrs.cyberhome.cyber.design.CyberActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.board.CozyBoard
import com.techvrs.cyberhome.cyber.design.CyberRow
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.cyber.design.CyberThemePicker
import com.techvrs.cyberhome.cyber.onboarding.CyberLook
import com.techvrs.cyberhome.cyber.onboarding.OnboardingActivity
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.card.CardStyle
import com.techvrs.cyberhome.cyber.widgets.card.CharacterCardProvider
import com.techvrs.cyberhome.cyber.widgets.counter.DayCounterWidgetProvider

/** CyberHome settings (PROJECT_SPEC.md §3.5), reachable from the top of the launcher settings. */
class CyberSettingsActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CyberSettingsScreen() }
    }

    companion object {
        fun open(context: Context) = context.startActivity(
            Intent(context, CyberSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun CyberSettingsScreen() {
    val context = LocalContext.current
    val start = { cls: Class<*>, id: Int? ->
        val intent = Intent(context, cls)
        if (id != null) intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        context.startActivity(intent)
    }

    CyberScreen(stringResource(R.string.cyber_settings_title)) {
        CyberThemePicker()
        CyberRow(
            stringResource(R.string.cyber_settings_reset),
            stringResource(R.string.cyber_settings_reset_desc),
            R.drawable.cyber_ic_settings,
            colorResource(R.color.cyber_accent_peach),
            colorResource(R.color.cyber_tint_peach),
        ) {
            OnboardingActivity.open(context, reset = true)
        }

        Text(stringResource(R.string.cyber_settings_board), style = CyberTextStyles.label)
        CyberRow(stringResource(R.string.cyber_board_edit_title), stringResource(R.string.cyber_board_edit_entry_desc), R.drawable.cyber_ic_edit, colorResource(R.color.cyber_accent_teal), colorResource(R.color.cyber_tint_teal)) {
            start(BoardEditActivity::class.java, null)
        }
        listOf(
            Triple(CozyBoard.SLOT_BLUE, CardStyle.BLUE, R.string.cyber_settings_card_blue),
            Triple(CozyBoard.SLOT_PINK, CardStyle.PINK, R.string.cyber_settings_card_pink),
            Triple(CozyBoard.SLOT_TEAL, CardStyle.TEAL, R.string.cyber_settings_card_teal),
        ).forEach { (slot, style, label) ->
            CyberRow(stringResource(label), null, R.drawable.cyber_ic_edit, colorResource(style.accent), colorResource(style.tint)) {
                start(CardEditActivity::class.java, slot)
            }
        }
        CyberRow(stringResource(R.string.cyber_settings_board_counter), null, R.drawable.cyber_ic_plus, colorResource(R.color.cyber_accent_pink), colorResource(R.color.cyber_tint_pink)) {
            start(DayCounterActivity::class.java, CozyBoard.SLOT_PINK)
        }
        CyberRow(stringResource(R.string.cyber_wallpaper_title), null, R.drawable.cyber_ic_sun, colorResource(R.color.cyber_accent_peach), colorResource(R.color.cyber_tint_peach)) {
            start(WallpaperActivity::class.java, null)
        }
        CyberRow(stringResource(R.string.cyber_settings_board_apps), null, R.drawable.cyber_ic_apps, colorResource(R.color.cyber_accent_blue), colorResource(R.color.cyber_tint_blue)) {
            start(BoardAppsActivity::class.java, null)
        }
        CyberRow(stringResource(R.string.cyber_pack_title), stringResource(R.string.cyber_pack_entry_desc), R.drawable.cyber_ic_plus, colorResource(R.color.cyber_accent_pink), colorResource(R.color.cyber_tint_pink)) {
            start(PackActivity::class.java, null)
        }

        Text(stringResource(R.string.cyber_settings_widgets), style = CyberTextStyles.label)
        val cards = CardStyle.entries.flatMap { style ->
            CyberWidgets.ids(context, CharacterCardProvider.providerOf(style)).map { style to it }
        }
        cards.forEachIndexed { i, (style, id) ->
            CyberRow(
                stringResource(R.string.cyber_settings_edit_card, i + 1),
                null,
                R.drawable.cyber_ic_edit,
                colorResource(style.accent),
                colorResource(style.tint),
            ) { start(CardEditActivity::class.java, id) }
        }
        CyberWidgets.ids(context, DayCounterWidgetProvider::class.java).forEachIndexed { i, id ->
            CyberRow(stringResource(R.string.cyber_settings_day_counter, i + 1), null, R.drawable.cyber_ic_plus, colorResource(R.color.cyber_accent_pink), colorResource(R.color.cyber_tint_pink)) {
                start(DayCounterActivity::class.java, id)
            }
        }
        CyberRow(stringResource(R.string.cyber_weather_title), null, R.drawable.cyber_ic_cloud, colorResource(R.color.cyber_accent_blue), colorResource(R.color.cyber_tint_blue)) {
            start(WeatherCityActivity::class.java, null)
        }
        CyberRow(stringResource(R.string.cyber_water_settings_title), null, R.drawable.cyber_ic_drop, colorResource(R.color.cyber_accent_teal), colorResource(R.color.cyber_tint_teal)) {
            start(WaterSettingsActivity::class.java, null)
        }
        CyberRow(stringResource(R.string.cyber_music_access_title), null, R.drawable.cyber_ic_music) {
            start(MusicAccessActivity::class.java, null)
        }

        Text(stringResource(R.string.cyber_settings_launcher), style = CyberTextStyles.label)
        CyberRow(stringResource(R.string.cyber_settings_switch_back), stringResource(R.string.cyber_settings_switch_back_desc), R.drawable.cyber_ic_apps) {
            runCatching { context.startActivity(CyberLook.homeSettingsIntent()) }
        }
        CyberRow(stringResource(R.string.cyber_about_title), null, R.drawable.ic_launcher_home_comp) {
            start(AboutActivity::class.java, null)
        }
    }
}
